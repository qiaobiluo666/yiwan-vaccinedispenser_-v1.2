package com.yiwan.vaccinedispenser.system.test;

import cn.hutool.core.lang.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author 78671
 */
@RestController
public class UploadController {


    @Value("${upload.uploadPath}")
    private  String uploadPath;

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
        URL url = new URL(uploadPath);
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


}
