package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 环境检查报告
 *
 * 用于：
 * - 测试连接时的详细反馈
 * - 启动时的环境验证
 * - 前端展示错误和修复建议
 *
 * @author DB-Doctor
 * @version 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvCheckReport {

    /**
     * 检查状态
     */
    private CheckStatus status;

    /**
     * 是否可连接（基础连接测试）
     */
    private boolean connectionSuccess;

    /**
     * 连接错误信息（连接失败时）
     */
    private String connectionError;

    /**
     * 检查项列表
     */
    private List<CheckItem> items;

    /**
     * 摘要信息
     */
    private String summary;

    /**
     * 可用的数据库列表（连接成功后获取）
     */
    private List<String> availableDatabases;

    /**
     * 总体通过（所有必选项都通过）
     */
    public boolean isOverallPassed() {
        if (!connectionSuccess) {
            return false;
        }

        if (items == null || items.isEmpty()) {
            return true;
        }

        // 所有必选项都通过才算通过
        return items.stream()
            .filter(CheckItem::isRequired)
            .allMatch(CheckItem::isPassed);
    }

    /**
     * 添加检查项
     */
    public void addItem(CheckItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    /**
     * 检查状态枚举
     */
    public enum CheckStatus {
        /**
         * 未配置（H2 中没有数据库配置）
         */
        NOT_CONFIGURED,

        /**
         * 检查通过
         */
        PASSED,

        /**
         * 检查失败（但不阻止启动）
         */
        FAILED,

        /**
         * 严重错误（阻止保存配置）
         */
        CRITICAL
    }

    /**
     * 检查项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckItem {

        /**
         * 检查项名称
         */
        private String name;

        /**
         * 是否必选（false=警告但允许，true=必须通过）
         */
        private boolean required;

        /**
         * 是否通过
         */
        private boolean passed;

        /**
         * 当前值
         */
        private String currentValue;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 修复命令（SQL 命令）
         */
        private String fixCommand;

        /**
         * 错误代码（用于前端国际化）
         */
        private String errorCode;

        /**
         * 帮助文档链接
         */
        private String helpUrl;

        /**
         * 预定义的检查项常量
         */
        public static class Constants {
            public static final String SLOW_QUERY_LOG = "slow_query_log";
            public static final String LOG_OUTPUT = "log_output";
            public static final String LONG_QUERY_TIME = "long_query_time";
            public static final String SLOW_LOG_ACCESS = "mysql.slow_log";

            public static final String ERROR_CODE_NOT_ENABLED = "ENV_001";
            public static final String ERROR_CODE_NO_TABLE = "ENV_002";
            public static final String ERROR_CODE_THRESHOLD = "ENV_003";
            public static final String ERROR_CODE_NO_PERMISSION = "ENV_004";
            public static final String ERROR_CODE_CONNECTION_FAILED = "CONN_001";
            public static final String ERROR_CODE_AUTH_FAILED = "CONN_002";
            public static final String ERROR_CODE_TIMEOUT = "CONN_003";
            public static final String ERROR_CODE_UNKNOWN = "CONN_999";
        }
    }
}
