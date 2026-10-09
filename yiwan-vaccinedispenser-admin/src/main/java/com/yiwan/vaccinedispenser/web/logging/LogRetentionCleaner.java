package com.yiwan.vaccinedispenser.web.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志保留保护：用于清理 Logback 历史配置遗留的超期日志。
 * 仅处理 yyyyMMdd.log、yyyyMMdd-HH.log 以及对应的 .gz 文件，避免误删其他文件。
 */
@Component
@Slf4j
public class LogRetentionCleaner {

    private static final Pattern LOG_FILE_PATTERN =
            Pattern.compile("^(\\d{8})(?:-\\d{2})?\\.log(?:\\.gz)?$");
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final Path logDirectory;
    private final int retentionDays;
    private final ZoneId zoneId;

    public LogRetentionCleaner(@Value("${logging.file.path:./logs}") String logDirectory,
                               @Value("${logging.retention.days:60}") int retentionDays,
                               @Value("${logging.retention.cleanup-zone:Asia/Shanghai}") String cleanupZone) {
        this.logDirectory = Paths.get(logDirectory).toAbsolutePath().normalize();
        this.retentionDays = retentionDays;
        this.zoneId = ZoneId.of(cleanupZone);
    }

    @PostConstruct
    public void cleanOnStartup() {
        cleanExpiredLogs("启动");
    }

    @Scheduled(cron = "${logging.retention.cleanup-cron:0 10 8 * * ?}",
            zone = "${logging.retention.cleanup-zone:Asia/Shanghai}")
    public void cleanOnSchedule() {
        cleanExpiredLogs("定时");
    }

    void cleanExpiredLogs(String trigger) {
        cleanExpiredLogs(trigger, LocalDate.now(zoneId));
    }

    void cleanExpiredLogs(String trigger, LocalDate today) {
        if (retentionDays <= 0) {
            log.error("[日志清理报警] 保留天数配置无效，已停止清理: retentionDays={}", retentionDays);
            return;
        }
        if (!Files.isDirectory(logDirectory, LinkOption.NOFOLLOW_LINKS)) {
            log.warn("[日志清理] 日志目录不存在，跳过清理: trigger={}, path={}", trigger, logDirectory);
            return;
        }

        LocalDate cutoffDate = today.minusDays(retentionDays);
        int deletedCount = 0;
        int failedCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDirectory)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }

                LocalDate logDate = parseLogDate(file.getFileName().toString());
                if (logDate == null || !logDate.isBefore(cutoffDate)) {
                    continue;
                }

                try {
                    if (Files.deleteIfExists(file)) {
                        deletedCount++;
                        log.info("[日志清理] 已删除超期日志: file={}, logDate={}, cutoffDate={}",
                                file, logDate, cutoffDate);
                    }
                } catch (IOException | SecurityException e) {
                    failedCount++;
                    log.error("[日志清理报警] 删除超期日志失败: file={}, logDate={}, cutoffDate={}",
                            file, logDate, cutoffDate, e);
                }
            }
        } catch (IOException | SecurityException e) {
            log.error("[日志清理报警] 扫描日志目录失败: trigger={}, path={}", trigger, logDirectory, e);
            return;
        }

        if (failedCount > 0) {
            log.error("[日志清理报警] 清理完成但存在失败文件: trigger={}, path={}, deleted={}, failed={}",
                    trigger, logDirectory, deletedCount, failedCount);
        } else {
            log.info("[日志清理] 清理完成: trigger={}, path={}, retentionDays={}, cutoffDate={}, deleted={}",
                    trigger, logDirectory, retentionDays, cutoffDate, deletedCount);
        }
    }

    private LocalDate parseLogDate(String fileName) {
        Matcher matcher = LOG_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return LocalDate.parse(matcher.group(1), LOG_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("[日志清理] 日志文件名日期无效，已跳过: fileName={}", fileName);
            return null;
        }
    }
}
