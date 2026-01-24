package com.dbdoctor.common.constants;

/**
 * 正则表达式常量
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
public class RegexConstants {

    /**
     * Email 正则
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    /**
     * URL 正则
     */
    public static final String URL_REGEX = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";

    /**
     * 私有构造方法，防止实例化
     */
    private RegexConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
