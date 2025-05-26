package com.yiwan.vaccinedispenser.core.until;

public class StringUntils {
    /**
     * 从字符串中提取目标值
     *
     * @param input 输入字符串
     * @return 提取的值，如果未找到则返回空字符串
     */
    public static String extractValue(String input) {
        String[] parts = input.split("/");
        if (parts.length > 0) {
            String firstPart = parts[0];
            if (firstPart.length() > 0) {
                char lastChar = firstPart.charAt(firstPart.length() - 1);
                if (Character.isDigit(lastChar)) {
                    return String.valueOf(lastChar);
                }
            }
        }
        return "1";
    }
}
