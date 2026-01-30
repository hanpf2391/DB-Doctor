package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 热重载结果
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotReloadResult {

    /**
     * 是否需要重启 JVM
     */
    private boolean requiresRestart = false;

    /**
     * 已刷新的 Bean 列表
     */
    private List<String> refreshedBeans = new ArrayList<>();

    /**
     * 重启提示消息
     */
    private String restartMessage;

    public static HotReloadResult success(List<String> refreshedBeans) {
        return new HotReloadResult(false, refreshedBeans, null);
    }

    public static HotReloadResult needRestart(String message) {
        return new HotReloadResult(true, new ArrayList<>(), message);
    }
}
