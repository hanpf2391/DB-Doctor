package com.dbdoctor.common.util;

import lombok.extern.slf4j.Slf4j;
import java.util.regex.Pattern;

/**
 * SQL 脱敏工具类
 * 用于识别和替换 SQL 中的敏感信息
 *
 * 脱敏规则：
 * 1. 手机号：13812345678 → 138****5678
 * 2. 身份证：110101199001011234 → 110101********1234
 * 3. 固定电话：010-12345678 → 010-****5678
 * 4. IP 地址：192.168.1.1 → 192.168.*.*
 * 5. 邮箱：user@example.com → u***@example.com
 * 6. 银行卡号：6222021234567890123 → 622202*******890123
 * 7. 密码/Token：password → ******
 * 8. 数据库连接字符串：host=127.0.0.1 → host=***.***.**.*
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Slf4j
public class SqlMaskingUtil {

    // ==================== 正则表达式模式 ====================

    /**
     * 中国手机号（1开头，11位）
     * 匹配：13812345678, 15912345678
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(1[3-9]\\d)\\d{4}(\\d{4})\\b"
    );

    /**
     * 身份证号（18位）
     * 匹配：110101199001011234
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "\\b([1-9]\\d{5})(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]\\b"
    );

    /**
     * 固定电话（区号-号码）
     * 匹配：010-12345678, 0755-1234567
     */
    private static final Pattern LANDLINE_PATTERN = Pattern.compile(
        "\\b(0\\d{2,3}-?)\\d{3,4}(\\d{4})\\b"
    );

    /**
     * IP 地址
     * 匹配：192.168.1.1, 10.0.0.1
     */
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.)(\\d{1,3})\\b"
    );

    /**
     * 邮箱地址
     * 匹配：user@example.com, test@test.org
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b([a-zA-Z0-9])[a-zA-Z0-9._%+-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b"
    );

    /**
     * 银行卡号（16-19位）
     * 匹配：6222021234567890123
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile(
        "\\b(\\d{4})(\\d{6,})(\\d{4,6})\\b"
    );

    /**
     * 密码字段
     * 匹配：password='123456', pwd="abc123"
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|pwd|passwd)\\s*[=:]\\s*['\"]([^'\"]+)['\"]",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Token/API Key
     * 匹配：token='abc123xyz', api_key="sk-xxxxx"
     */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(token|api_key|apikey|access_token|secret_key)\\s*[=:]\\s*['\"]([^'\"]{8,})['\"]",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 数据库连接字符串中的 IP
     * 匹配：host=192.168.1.1, server=10.0.0.1
     */
    private static final Pattern DB_HOST_PATTERN = Pattern.compile(
        "(host|server|hostname)\\s*[=:]\\s*[\"']?(\\d{1,3}\\.\\d{1,3}\\.)(\\d{1,3}\\.)?(\\d{1,3})[\"']?",
        Pattern.CASE_INSENSITIVE
    );

    // ==================== 脱敏方法 ====================

    /**
     * 脱敏 SQL 中的敏感信息
     *
     * @param sql 原始 SQL
     * @return 脱敏后的 SQL
     */
    public static String maskSensitiveData(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String masked = sql;

        try {
            // 1. 手机号脱敏
            masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");

            // 2. 身份证号脱敏
            masked = ID_CARD_PATTERN.matcher(masked).replaceAll("$1************$4");

            // 3. 固定电话脱敏
            masked = LANDLINE_PATTERN.matcher(masked).replaceAll("$1****$2");

            // 4. IP 地址脱敏
            masked = IP_PATTERN.matcher(masked).replaceAll("$1***");

            // 5. 邮箱脱敏
            masked = EMAIL_PATTERN.matcher(masked).replaceAll("$1***@$2");

            // 6. 银行卡号脱敏
            masked = BANK_CARD_PATTERN.matcher(masked).replaceAll("$1*******$3");

            // 7. 密码脱敏
            masked = PASSWORD_PATTERN.matcher(masked).replaceAll("$1='******'");

            // 8. Token/API Key 脱敏
            masked = TOKEN_PATTERN.matcher(masked).replaceAll("$1='******'");

            // 9. 数据库连接 IP 脱敏
            masked = DB_HOST_PATTERN.matcher(masked).replaceAll("$1='***.***.**.*'");

        } catch (Exception e) {
            log.error("SQL 脱敏失败: {}", sql, e);
            return sql; // 失败时返回原始 SQL
        }

        return masked;
    }

    /**
     * 快速脱敏（仅处理最常见的敏感数据）
     *
     * @param sql 原始 SQL
     * @return 脱敏后的 SQL
     */
    public static String quickMask(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        String masked = sql;

        try {
            // 仅处理手机号和 IP（最常见的敏感数据）
            masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
            masked = IP_PATTERN.matcher(masked).replaceAll("$1***");
            masked = EMAIL_PATTERN.matcher(masked).replaceAll("$1***@$2");

        } catch (Exception e) {
            log.error("快速脱敏失败: {}", sql, e);
            return sql;
        }

        return masked;
    }

    /**
     * 检测 SQL 是否包含敏感数据
     *
     * @param sql 原始 SQL
     * @return true-包含敏感数据，false-不包含
     */
    public static boolean containsSensitiveData(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }

        return PHONE_PATTERN.matcher(sql).find()
            || ID_CARD_PATTERN.matcher(sql).find()
            || IP_PATTERN.matcher(sql).find()
            || EMAIL_PATTERN.matcher(sql).find()
            || PASSWORD_PATTERN.matcher(sql).find()
            || TOKEN_PATTERN.matcher(sql).find();
    }

    // ==================== 测试用例 ====================

    /**
     * 测试脱敏功能
     */
    public static void main(String[] args) {
        // 测试用例
        String[] testCases = {
            "SELECT * FROM users WHERE phone = '13812345678'",
            "INSERT INTO logs (ip, action) VALUES ('192.168.1.100', 'login')",
            "UPDATE users SET email = 'test@example.com' WHERE id = 1",
            "SELECT * FROM users WHERE id_card = '110101199001011234'",
            "SELECT * FROM config WHERE password = '123456'",
            "SELECT * FROM orders WHERE card_no = '6222021234567890123'"
        };

        System.out.println("=== SQL 脱敏测试 ===\n");

        for (String sql : testCases) {
            String masked = maskSensitiveData(sql);
            boolean hasSensitive = containsSensitiveData(sql);

            System.out.println("原始: " + sql);
            System.out.println("脱敏: " + masked);
            System.out.println("包含敏感数据: " + hasSensitive);
            System.out.println("---");
        }
    }
}
