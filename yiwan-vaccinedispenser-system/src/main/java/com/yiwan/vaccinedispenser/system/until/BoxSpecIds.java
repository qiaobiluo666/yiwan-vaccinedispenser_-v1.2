package com.yiwan.vaccinedispenser.system.until;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 解析疫苗配置的逗号分隔仓位规格 ID。
 */
public final class BoxSpecIds {

    private BoxSpecIds() {
    }

    public static List<Long> parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(BoxSpecIds::parseLong)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
