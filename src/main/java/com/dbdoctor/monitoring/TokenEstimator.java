package com.dbdoctor.monitoring;

import lombok.extern.slf4j.Slf4j;

/**
 * Token 估算工具类
 *
 * <p>用于在 LangChain4j API 不支持 Token 统计时的备用方案</p>
 *
 * <p>估算规则：</p>
 * <ul>
 *   <li>英文：约 4 字符 / Token</li>
 *   <li>中文：约 1.5 字符 / Token</li>
 *   <li>代码（SQL）：约 3 字符 / Token</li>
 *   <li>混合内容：加权平均</li>
 * </ul>
 *
 * <p>注意：这是估算值，准确度约 70-80%</p>
 *
 * @author DB-Doctor
 * @version 2.3.1
 * @since 2.3.1
 */
@Slf4j
public class TokenEstimator {

    /**
     * 英文 Token 估算系数（字符数 / Token 数）
     */
    private static final double ENGLISH_RATIO = 4.0;

    /**
     * 中文 Token 估算系数（字符数 / Token 数）
     */
    private static final double CHINESE_RATIO = 1.5;

    /**
     * 代码（SQL）Token 估算系数（字符数 / Token 数）
     */
    private static final double CODE_RATIO = 3.0;

    /**
     * 估算输入 Token 数
     *
     * @param text 输入文本（Prompt）
     * @return 估算的 Token 数
     */
    public static int estimateInputTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int length = text.length();

        // 统计中文字符数
        int chineseChars = countChineseCharacters(text);
        // 统计英文字符数（字母 + 空格）
        int englishChars = countEnglishCharacters(text);
        // 其余视为代码/特殊字符
        int otherChars = length - chineseChars - englishChars;

        // 估算 Token 数
        double chineseTokens = chineseChars / CHINESE_RATIO;
        double englishTokens = englishChars / ENGLISH_RATIO;
        double otherTokens = otherChars / CODE_RATIO;

        return (int) Math.ceil(chineseTokens + englishTokens + otherTokens);
    }

    /**
     * 估算输出 Token 数
     *
     * @param text 输出文本（Response）
     * @return 估算的 Token 数
     */
    public static int estimateOutputTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 使用与输入相同的估算逻辑
        return estimateInputTokens(text);
    }

    /**
     * 估算总 Token 数
     *
     * @param inputText 输入文本
     * @param outputText 输出文本
     * @return 总 Token 数
     */
    public static int estimateTotalTokens(String inputText, String outputText) {
        return estimateInputTokens(inputText) + estimateOutputTokens(outputText);
    }

    /**
     * 统计中文字符数
     *
     * @param text 文本
     * @return 中文字符数
     */
    private static int countChineseCharacters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isChineseCharacter(c)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 统计英文字符数（字母 + 空格）
     *
     * @param text 文本
     * @return 英文字符数
     */
    private static int countEnglishCharacters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (isEnglishCharacter(c) || c == ' ') {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断是否为中文字符
     *
     * @param c 字符
     * @return 如果是中文返回 true
     */
    private static boolean isChineseCharacter(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 判断是否为英文字符
     *
     * @param c 字符
     * @return 如果是英文返回 true
     */
    private static boolean isEnglishCharacter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * 计算估算准确度（用于日志）
     *
     * @param estimated 估算值
     * @param actual 实际值（如果有）
     * @return 准确度百分比
     */
    public static double calculateAccuracy(int estimated, Integer actual) {
        if (actual == null || actual == 0) {
            return 0.0;
        }

        double error = Math.abs(estimated - actual) * 100.0 / actual;
        return 100.0 - error;
    }
}
