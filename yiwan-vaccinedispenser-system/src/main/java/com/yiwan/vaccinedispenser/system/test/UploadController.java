package com.yiwan.vaccinedispenser.system.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.zip.GZIPInputStream;

/**
 * @author 78671
 */
@RestController
public class UploadController {



    //访问服务器 将日志、数据库上传到服务器
    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("type") String type) {
        try {
            // 获取今天日期    2025-04-22
            String dateStr = java.time.LocalDate.now().toString();
            String baseDir = type.equals("log") ? "D:\\work\\yiwan\\疫苗发药机\\test\\log\\" : "D:\\work\\yiwan\\疫苗发药机\\test\\db\\";
            String targetDirPath = baseDir + dateStr + "/";

            // 创建目录
            File targetDir = new File(targetDirPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // 存文件
            File destFile = new File(targetDirPath + file.getOriginalFilename());
            file.transferTo(destFile);

            return ResponseEntity.ok("Upload successful: " + destFile.getAbsolutePath());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }





    // 统一上传日志或数据库文件的函数
    public  void exportAndUpload(String fileType, String dbName, String user, String password, String logFilePath, boolean compress) throws Exception {
        // 根据日期生成目录路径
        String date = LocalDate.now().toString();
        String uploadDir = "D:\\work\\yiwan\\疫苗发药机\\test\\" + ("log".equals(fileType) ? "logs" : "db") + "/" + date;

        // 如果是数据库类型，导出 MySQL 数据库
        if ("db".equals(fileType)) {
            exportDatabase(dbName, user, password, uploadDir, compress);
        } else if ("log".equals(fileType)) {
            // 如果是日志文件类型，上传日志文件
            uploadFile(new File(logFilePath), "log", compress);
        } else {
            throw new IllegalArgumentException("Invalid file type. Use 'log' or 'db'.");
        }
    }





    // 上传文件到远程服务器（通用上传函数）
    private  void uploadFile(File file, String type, boolean compress) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL("http://192.168.1.105:5160/upload");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {

            // 传type字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"type\"\r\n\r\n");
            writer.append(type).append("\r\n");

            // 传文件字段
            writer.append("--").append(boundary).append("\r\n");
            String uploadName = file.getName() + (compress ? ".gz" : "");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(uploadName).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            // 压缩或直接上传
            try (InputStream fileInput = new FileInputStream(file);
                 InputStream input = compress ?
                         new BufferedInputStream(new GZIPInputStream(fileInput)) :
                         new BufferedInputStream(fileInput)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            }

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--").append("\r\n");
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            System.out.println("Upload successful: " + file.getName());
        } else {
            System.out.println("Upload failed: " + responseCode);
        }
    }





    // 导出 MySQL 数据库为 SQL 文件
    private  void exportDatabase(String dbName, String user, String password, String uploadDir, boolean compress) throws IOException, InterruptedException {
        String fileName = dbName + "-" + LocalDate.now() + ".sql";
        File outputFile = new File(uploadDir, fileName);

        // 确保目录存在
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        // 使用 mysqldump 命令导出数据库
        String command = String.format("mysqldump -u%s -p%s %s -r \"%s\"", user, password, dbName, outputFile.getAbsolutePath());
        Process process = Runtime.getRuntime().exec(command);
        int result = process.waitFor();

        if (result == 0) {
            System.out.println("数据库导出成功: " + outputFile.getAbsolutePath());
            // 上传导出的数据库文件
            uploadFile(outputFile, "db", compress);
        } else {
            throw new IOException("Database export failed with exit code: " + result);
        }
    }



}
