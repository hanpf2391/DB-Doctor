package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 配置数据传输对象
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDTO {

    /**
     * 配置分类: DB, AI, NOTIFY, SYSTEM
     */
    private String category;

    /**
     * 配置键值对
     */
    private Map<String, String> configs;
}
