package com.dbdoctor.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 指纹工具类测试
 */
class SqlFingerprintUtilTest {

    @Test
    void testExtractTemplate() {
        // 测试用例1：手机号参数化
        String sql1 = "SELECT * FROM customers WHERE phone = '13812345678'";
        String template1 = SqlFingerprintUtil.extractTemplate(sql1);
        System.out.println("原始SQL: " + sql1);
        System.out.println("模板化: " + template1);
        assertTrue(template1.contains("?"), "应该包含参数占位符 ?");

        // 测试用例2：ID 参数化
        String sql2 = "SELECT * FROM users WHERE id = 123";
        String template2 = SqlFingerprintUtil.extractTemplate(sql2);
        System.out.println("\n原始SQL: " + sql2);
        System.out.println("模板化: " + template2);
        assertTrue(template2.contains("?"), "应该包含参数占位符 ?");

        // 测试用例3：多参数化
        String sql3 = "SELECT * FROM orders WHERE user_id = 1 AND status = 'pending'";
        String template3 = SqlFingerprintUtil.extractTemplate(sql3);
        System.out.println("\n原始SQL: " + sql3);
        System.out.println("模板化: " + template3);
        assertTrue(template3.contains("?"), "应该包含参数占位符 ?");
    }

    @Test
    void testCalculateFingerprint() {
        // 测试用例：相同结构的 SQL 应该产生相同的指纹
        String sql1 = "SELECT * FROM customers WHERE phone = '13812345678'";
        String sql2 = "SELECT * FROM customers WHERE phone = '15987654321'";

        String fingerprint1 = SqlFingerprintUtil.calculateFingerprint(sql1);
        String fingerprint2 = SqlFingerprintUtil.calculateFingerprint(sql2);

        System.out.println("\nSQL1指纹: " + fingerprint1);
        System.out.println("SQL2指纹: " + fingerprint2);

        assertEquals(fingerprint1, fingerprint2, "相同结构的 SQL 应该有相同的指纹");
    }

    @Test
    void testMaskSensitiveData() {
        // 测试用例：SQL 脱敏
        String sql = "SELECT * FROM users WHERE phone = '13812345678' AND ip = '192.168.1.1'";
        String masked = SqlMaskingUtil.maskSensitiveData(sql);

        System.out.println("\n原始SQL: " + sql);
        System.out.println("脱敏后: " + masked);

        assertFalse(masked.contains("13812345678"), "手机号应该被脱敏");
        assertFalse(masked.contains("192.168.1.1"), "IP地址应该被脱敏");
        assertTrue(masked.contains("138****5678"), "应该包含脱敏后的手机号");
    }

    public static void main(String[] args) {
        System.out.println("=== SQL 模板化测试 ===\n");

        // 手动运行测试
        SqlFingerprintUtilTest test = new SqlFingerprintUtilTest();
        test.testExtractTemplate();
        test.testCalculateFingerprint();
        test.testMaskSensitiveData();
    }
}
