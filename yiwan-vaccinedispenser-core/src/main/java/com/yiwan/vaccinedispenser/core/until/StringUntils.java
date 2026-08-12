package com.yiwan.vaccinedispenser.core.until;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUntils {

    private static final Pattern PACK_SIZE_PATTERN = Pattern.compile("-(\\d+)/");

    /**
     * 从字符串中提取药盒规格（-N/ 中的 N）
     * <p>
     * 例如 "国-13价肺炎（CRM197/破伤风类毒素）-康希诺-1/0.5ml/支-液预注1" → "1"
     *      "国-ACYW135-沃森-1/复0.5ml/瓶-冻西注1" → "1"
     *
     * @param input 输入字符串
     * @return 提取的值，如果未找到则返回 "1"
     */
    public static String extractValue(String input) {
        if (input == null) {
            return "1";
        }
        Matcher matcher = PACK_SIZE_PATTERN.matcher(input);
        // 取最后一个匹配的 -N/ 中的 N
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (lastMatch != null) {
            return lastMatch;
        }
        return "1";
    }
}
