package com.yiwan.vaccinedispenser.system.test;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yiwan.vaccinedispenser.system.sys.data.request.vac.DrugRecordRequest;
import com.yiwan.vaccinedispenser.system.zyc.ZcyFunction;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author 78671
 */
@RestController
@Slf4j
public class UploadController {


    @Value("${upload.uploadPath}")
    private  String uploadPath;

    @Autowired
    private ZcyFunction zcyFunction;


    public  void exportAndUpload(String hospitalName,String fileType, String dbName, String user, String password, String logFilePath, boolean compress) throws Exception {
        // 根据日期生成目录路径
        String date = LocalDate.now().toString();
        String uploadDir = "D:\\yiwan\\backend\\" + ("log".equals(fileType) ? "logs" : "db") + "/" + date;

        // 如果是数据库类型，导出 MySQL 数据库
        if ("db".equals(fileType)) {
            exportDatabase(hospitalName,dbName, user, password, uploadDir, compress);
        } else if ("log".equals(fileType)) {
            // 如果是日志文件类型，上传日志文件
            uploadFile(new File(logFilePath), "log", hospitalName,compress);
        } else {
            throw new IllegalArgumentException("Invalid file type. Use 'log' or 'db'.");
        }
    }





    // 上传文件到远程服务器（通用上传函数）
    private  void uploadFile(File file, String type,String hospitalName, boolean compress) throws IOException {
//        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
//        URL url = new URL("http://47.97.48.26:5080/upload");

        // 如果需要压缩，先创建临时 ZIP 文件
        File fileToUpload = file;
        if (compress) {
            fileToUpload = createZipFile(file);
        }

        String boundary = "----WebKitFormBoundary" ;
//        URL url = new URL("http://127.0.0.1:5080/upload");
        URL url = new URL(uploadPath+"/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {

            // 字段1：type
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"type\"\r\n\r\n");
            writer.append(type).append("\r\n");
            writer.flush();

            // 字段2：hospitalName
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"hospitalName\"\r\n\r\n");
            writer.append(hospitalName).append("\r\n");
            writer.flush();

            // 字段3：文件
            String filename = fileToUpload.getName();
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            // 写入文件内容（直接读取文件，不涉及 GZIP 压缩）
            try (FileInputStream fis = new FileInputStream(fileToUpload)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
                output.flush();
            }

            // 写入结束边界
            writer.append("\r\n--").append(boundary).append("--\r\n").flush();
        } finally {
            // 如果是临时 ZIP 文件，上传后删除
            if (compress && fileToUpload != null && fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }

        // 打印响应
        int code = conn.getResponseCode();
        System.out.println("HTTP Code: " + code);
        try (InputStream in = code == 200 ? conn.getInputStream() : conn.getErrorStream()) {
            String resp = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Response: " + resp);
        }
    }

    // 创建 ZIP 文件（单文件压缩）
    private static File createZipFile(File file) throws IOException {
        File zipFile = File.createTempFile(file.getName(), ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(file)) {

            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
        return zipFile;
    }



    // 导出 MySQL 数据库为 SQL 文件
    private  void exportDatabase(String hospitalName,String dbName, String user, String password, String uploadDir, boolean compress) throws IOException, InterruptedException {
        String fileName = dbName + "-" + LocalDate.now() + ".sql";
        File outputFile = new File(uploadDir, fileName);

        // 确保目录存在
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // 使用 mysqldump 命令导出数据库
        String command = String.format("mysqldump -u%s -p%s --max_allowed_packet=512M  --skip-extended-insert --skip-lock-tables    %s -r \"%s\"", user, password, dbName, outputFile.getAbsolutePath());
        Process process = Runtime.getRuntime().exec(command);
        int result = process.waitFor();

        if (result == 0) {
            System.out.println("数据库导出成功: " + outputFile.getAbsolutePath());
            // 上传导出的数据库文件
            uploadFile(outputFile, "db", hospitalName,compress);
        } else {
            throw new IOException("Database export failed with exit code: " + result);
        }
    }


    /**
     * 发送code 给服务器 返回疫苗具体信息
     */
    public DrugRecordRequest getZcyCode(String code){
        try {
            String uuid = String.valueOf(UUID.randomUUID());
            // 构建表单内容
            String formData = String.format("uuid=%s&supervisedCode=%s",
                    URLEncoder.encode(uuid, StandardCharsets.UTF_8),
                    URLEncoder.encode(code, StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            String url = uploadPath+"/zcy/submit-code";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ 请求成功：" + response.body());
                //请求成功后 根据uuid 和 code 请求 url  get-vaccine-msg 拿到 对象 DrugRecordRequest
                int maxAttempts = 10;
                int intervalMillis = 500;

                DrugRecordRequest data = new DrugRecordRequest();
                for (int i = 1; i <= maxAttempts; i++) {
                    data = getVaccineMsg(uuid, code);

                    if (data != null) {
                        log.info("✅ 第 {} 次成功获取数据：{}", i, JSON.toJSONString(data));

                        break;
                    } else {
                        log.warn("第 {} 次未获取到数据，等待重试...", i);
                        try {
                            Thread.sleep(intervalMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("线程被中断", e);
                            break;
                        }
                    }
                }

                if (data == null) {
                    log.error("❌ 经过 {} 次尝试后仍未获取到数据", maxAttempts);
                }else {
                    data.setIsReturn(false);
                }
            return  data;
            } else {

                System.err.println("❌ 请求失败，状态码：" + response.statusCode());
                System.err.println("响应内容：" + response.body());
                return  null;
            }

        } catch (Exception e) {
            System.err.println("❗ 请求异常：" + e.getMessage());
            e.printStackTrace();
            return  null;
        }


    }

    /**
     *
     * @param uuid
     * @param code
     * @return
     * @throws IOException
     * 获取服务器疫苗的具体信息
     */
    public  DrugRecordRequest getVaccineMsg(String uuid, String code) throws IOException {
        DrugRecordRequest data = new DrugRecordRequest();
        data.setUuid(uuid);
        data.setSupervisedCode(code);

        RequestBody body = RequestBody.create(
                JSON.toJSONString(data),
                MediaType.parse("application/json")
        );

        String url = uploadPath+"/zcy/get-vaccine-msg";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            assert response.body() != null;
            String jsonStr = response.body().string();
            log.info(jsonStr);
            // 用 fastjson 解析 JSON
            JSONObject jsonObj = JSON.parseObject(jsonStr);
            JSONObject dataObj = jsonObj.getJSONObject("data");

            return dataObj == null ? null : dataObj.toJavaObject(DrugRecordRequest.class);
        }catch (Exception e){
            return null;
        }
    }



    /**
     * 获取redis 电子监管码所有队列
     */
    public List<DrugRecordRequest> getVaccineCodeList() throws IOException, InterruptedException {

        String url =uploadPath+"/zcy/getCode";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String jsonStr = response.body().string();
            // 用 fastjson 解析 JSON
            JSONObject jsonObj = JSON.parseObject(jsonStr);
            JSONArray dataArray = jsonObj.getJSONArray("data");
            List<DrugRecordRequest> list = new ArrayList<>();
            if (dataArray != null) {
                list = dataArray.toList(DrugRecordRequest.class);
            }
            return list;
        }
    }

    /**
     *
     * @throws IOException
     * @throws InterruptedException
     * 每条数据获取参数信息 上传给服务器
     */
    public void getZycVaccineMsg() throws Exception {
        long time = System.currentTimeMillis();
        List<DrugRecordRequest>  list= getVaccineCodeList();
        if(list!=null){
            for(DrugRecordRequest record:list){
                //获取政采云的电子监管码信息
                DrugRecordRequest data = zcyFunction.getVaccineMsgByCode(record.getSupervisedCode());
                data.setUuid(record.getUuid());
                log.info("拿到政采云疫苗信息：{}", com.alibaba.fastjson.JSON.toJSONString(data));
                //通过接口返回给服务器
                String url =uploadPath+"/zcy/check-code-result";
                // 序列化成 JSON 字符串
                String json = JSON.toJSONString(data);
                log.info(json);
                // 创建 OkHttp 请求体
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                // 执行请求
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String resp = response.body().string();
                        System.out.println("返回内容：" + resp);
                    } else {
                        System.out.println("请求失败，状态码: " + response.code());
                    }
                }
            }
        }
    }



}
