
package com.yiwan.vaccinedispenser.system.ffmpeg;

import com.yiwan.vaccinedispenser.core.common.emun.RedisKeyConstant;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 78671
 */
@Component
public class FFmpegManager {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    private static final String FFMPEG_PATH = "D:\\software\\ffmpeg-7.1.1\\bin\\ffmpeg.exe";

    /** 保存流的状态（进程 + 引用计数） */
    private final Map<String, StreamInfo> streamMap = new ConcurrentHashMap<>();

    private final ExecutorService logExecutor = Executors.newCachedThreadPool();

    /** 启动流（带复用机制） */
    public synchronized void startFFmpeg(String rtspUrl, String streamName) {
        String countStr= valueOperations.get(String.format(RedisKeyConstant.CAMERA_OPEN_NUM,streamName));
        int countRedis;
        if(countStr==null){
            countRedis=1;
        }else {
            countRedis = Integer.parseInt(countStr)+1;
        }
        valueOperations.set(String.format(RedisKeyConstant.CAMERA_OPEN_NUM,streamName),String.valueOf(countRedis));

        StreamInfo info = streamMap.get(streamName);
        if (info != null && info.process.isAlive()) {
            // 已经在推流，复用
            int count = info.refCount.incrementAndGet();
            System.out.println("♻️ Reuse stream: " + streamName + " (ref=" + count + ")");
            return;
        }

//        // 没有流，创建新进程
//        List<String> cmd = Arrays.asList(
//                FFMPEG_PATH,
//                "-rtsp_transport", "tcp",
//                "-i", rtspUrl,
//                "-c:v", "libx264",
//                "-preset", "ultrafast",
//                "-tune", "zerolatency",
//                "-an",
//                "-f", "flv",
//                "rtmp://127.0.0.1:1935/live/" + streamName
//        );
        List<String> cmd = Arrays.asList(
                FFMPEG_PATH,
                "-rtsp_transport", "tcp",
                "-i", rtspUrl,
                "-c:v", "libx264",
                "-preset", "ultrafast",   // 回到ultrafast但配合其他优化
                "-tune", "zerolatency",
                "-r", "7",                // 非常低的帧率
                "-crf", "28",
                "-vf", "scale=480:270",   // 极低分辨率
                "-g", "100",              // 很少的关键帧
                "-x264-params", "threads=1:keyint=100:no-scenecut=1:bframes=0:weightp=0",
                "-maxrate", "400k",
                "-bufsize", "800k",
                "-an",
                "-f", "flv",
                "rtmp://127.0.0.1:1935/live/" + streamName
        );
        try {
            Process process = new ProcessBuilder(cmd).start();

            StreamInfo newInfo = new StreamInfo(process, rtspUrl);
            streamMap.put(streamName, newInfo);

            System.out.println("✅ FFmpeg started for stream: " + streamName);

            // 异步读取日志，避免阻塞
            logExecutor.submit(() -> logStream(process.getErrorStream(), streamName));

            // 守护线程：进程退出时清理
            CompletableFuture.runAsync(() -> {
                try {
                    int exitCode = process.waitFor();
                    System.out.println("⚠️ FFmpeg exited for stream: " + streamName + " (code=" + exitCode + ")");
                } catch (InterruptedException ignored) {
                } finally {
                    streamMap.remove(streamName);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void stopFFmpeg(String streamName) {
        StreamInfo info = streamMap.get(streamName);
        if (info == null) {
            return;
        }
        String redisKey = String.format(RedisKeyConstant.CAMERA_OPEN_NUM,streamName);
        String countStr= valueOperations.get(redisKey);

        if("1".equals(countStr) ||countStr==null){
            valueOperations.set(redisKey,"0");
        }else {
            //如果多用户访问 则计数-1
            valueOperations.set(redisKey,String.valueOf(Integer.parseInt(countStr)-1));
            return;
        }




        int refs = info.refCount.decrementAndGet();
        System.out.println("🔹 Stop request for " + streamName + ", refCount=" + refs);

        // 强制停止，无论 refCount
        Process p = info.process;
        if (p != null && p.isAlive()) {
            try {
                p.destroyForcibly();
                if (!p.waitFor(5, TimeUnit.SECONDS)) {
                    System.out.println("⚠️ FFmpeg process still alive: " + streamName);
                } else {
                    System.out.println("🛑 FFmpeg stopped forcibly: " + streamName);
                }
            } catch (InterruptedException ignored) {}
        }

        streamMap.remove(streamName);
    }


    @PreDestroy
    public void stopAll() {
        streamMap.keySet().forEach(this::stopFFmpeg);
        streamMap.clear();

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("taskkill /F /IM ffmpeg.exe");
                System.out.println("🧹 Forced kill all ffmpeg.exe (Windows)");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logExecutor.shutdownNow();
        System.out.println("🧹 All FFmpeg processes stopped.");
    }

    /** 日志输出 */
    private void logStream(java.io.InputStream inputStream, String streamName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg-" + streamName + "] " + line);
            }
        } catch (IOException ignored) {
        }
    }

    /** 内部类：存储进程 + 引用计数 + 原始URL */
    private static class StreamInfo {
        final Process process;
        final String rtspUrl;
        final AtomicInteger refCount = new AtomicInteger(1);

        StreamInfo(Process process, String rtspUrl) {
            this.process = process;
            this.rtspUrl = rtspUrl;
        }
    }
}
