package com.yiwan.vaccinedispenser;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
*  Name：杭州医万智能装备有限公司 疫苗智能终端后端系统V1.0
*  Date：2024-02-26
*  Author：yiwan tech
*
 * @author 78671*/


@EnableScheduling
@DependsOn(value = "springContextUtil")
@EnableAsync
@SpringBootApplication
@Slf4j
public class YiwanVaccinedispenserApplication {

	public static void main(String[] args) {
		SpringApplication.run(YiwanVaccinedispenserApplication.class, args);
	}


//	private static final String[] RTSP_URLS = {
//			"rtsp://192.168.1.1:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.2:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.3:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.4:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.5:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.6:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.7:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.9:554/user=admin&password=123456&channel=1&stream=0.sdp"
//	};

//	private static final String[] RTSP_URLS = {
//			"rtsp://192.168.1.2:554/user=admin&password=123456&channel=1&stream=0.sdp",
//			"rtsp://192.168.1.3:554/user=admin&password=123456&channel=2&stream=0.sdp",
//			"rtsp://192.168.1.4:554/user=admin&password=123456&channel=3&stream=0.sdp"
//	};
//
//	@PostConstruct
//	public void startAllFFmpeg() {
//		for (int i = 0; i < RTSP_URLS.length; i++) {
//			final int index = i;
//			new Thread(() -> startFFmpeg(RTSP_URLS[index], index + 1)).start();
//		}
//	}
//
//	private void startFFmpeg(String rtspUrl, int camNumber) {
//		String hlsPath = String.format("D:/work/yiwan/video/hls/cam%d.m3u8", camNumber);
//		String cmd = String.format(
//				"D:\\software\\ffmpeg-7.1.1\\bin\\ffmpeg.exe -i \"%s\" -c:v libx264 -preset veryfast -tune zerolatency " +
//						"-c:a aac -f hls -hls_time 0.5 -hls_list_size 3 -hls_flags delete_segments -fflags nobuffer -analyzeduration 0 -probesize 32 \"%s\"",
//				rtspUrl, hlsPath
//		);
//		log.info(cmd);
//
//		try {
//			Runtime.getRuntime().exec(cmd);
//			System.out.println("FFmpeg started for camera " + camNumber);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}


}
