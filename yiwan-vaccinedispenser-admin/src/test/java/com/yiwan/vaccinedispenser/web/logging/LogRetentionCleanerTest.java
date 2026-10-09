package com.yiwan.vaccinedispenser.web.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogRetentionCleanerTest {

    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @TempDir
    Path tempDirectory;

    @Test
    void shouldDeleteOnlyRecognizedLogsOlderThanRetentionPeriod() throws IOException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Path expiredDailyLog = createLog(today.minusDays(61).format(FILE_DATE_FORMATTER) + ".log");
        Path expiredHourlyLog = createLog(today.minusDays(70).format(FILE_DATE_FORMATTER) + "-08.log");
        Path expiredCompressedLog = createLog(today.minusDays(80).format(FILE_DATE_FORMATTER) + ".log.gz");
        Path boundaryLog = createLog(today.minusDays(60).format(FILE_DATE_FORMATTER) + ".log");
        Path recentLog = createLog(today.minusDays(1).format(FILE_DATE_FORMATTER) + ".log");
        Path unrelatedFile = createLog("important-data.txt");

        LogRetentionCleaner cleaner = new LogRetentionCleaner(tempDirectory.toString(), 60, "Asia/Shanghai");
        cleaner.cleanExpiredLogs("测试", today);

        assertFalse(Files.exists(expiredDailyLog));
        assertFalse(Files.exists(expiredHourlyLog));
        assertFalse(Files.exists(expiredCompressedLog));
        assertTrue(Files.exists(boundaryLog));
        assertTrue(Files.exists(recentLog));
        assertTrue(Files.exists(unrelatedFile));
    }

    @Test
    void shouldNotDeleteAnythingWhenRetentionDaysIsInvalid() throws IOException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Path oldLog = createLog(today.minusDays(365).format(FILE_DATE_FORMATTER) + ".log");

        LogRetentionCleaner cleaner = new LogRetentionCleaner(tempDirectory.toString(), 0, "Asia/Shanghai");
        cleaner.cleanExpiredLogs("测试", today);

        assertTrue(Files.exists(oldLog));
    }

    private Path createLog(String fileName) throws IOException {
        return Files.createFile(tempDirectory.resolve(fileName));
    }
}
