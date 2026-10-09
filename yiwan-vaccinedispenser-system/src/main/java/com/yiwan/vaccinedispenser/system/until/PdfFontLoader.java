package com.yiwan.vaccinedispenser.system.until;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 同时支持外部字体文件和打包在 classpath 中的字体。
 */
public final class PdfFontLoader {

    private PdfFontLoader() {
    }

    public static Font load(String configuredPath) throws IOException, DocumentException {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            throw new IOException("未配置 PDF 字体路径");
        }

        if (!configuredPath.startsWith("classpath:")) {
            Path filePath = Paths.get(configuredPath).normalize();
            if (Files.isRegularFile(filePath)) {
                return createFont(filePath.toAbsolutePath().toString());
            }
        }

        String classpathPath = normalizeClasspathPath(configuredPath);
        ClassPathResource resource = new ClassPathResource(classpathPath);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] fontBytes = StreamUtils.copyToByteArray(inputStream);
            BaseFont baseFont = BaseFont.createFont(
                    classpathPath,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null
            );
            return new Font(baseFont, 12);
        }
    }

    private static Font createFont(String path) throws IOException, DocumentException {
        BaseFont baseFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(baseFont, 12);
    }

    private static String normalizeClasspathPath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        }
        int resourcesIndex = normalized.indexOf("src/main/resources/");
        if (resourcesIndex >= 0) {
            normalized = normalized.substring(resourcesIndex + "src/main/resources/".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
