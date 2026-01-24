package com.dbdoctor.common.constants;

/**
 * 通用常量类
 * 定义系统中常用的常量
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
public class CommonConstants {

    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 成功标记
     */
    public static final String SUCCESS = "success";

    /**
     * 失败标记
     */
    public static final String ERROR = "error";

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大的每页条数
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 私有构造方法，防止实例化
     */
    private CommonConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
