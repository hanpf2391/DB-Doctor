package com.dbdoctor.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AI 提示词工具类
 * 负责从配置文件读取 AI 提示词
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Component
public class PromptUtil {
    @Value("classpath:prompts/agents/diagnosis-system.txt")
    private Resource systemPromptResource;

    @Value("classpath:prompts/templates/diagnosis-user.txt")
    private Resource userPromptTemplateResource;

    private String systemPrompt;
    private String userPromptTemplate;

    /**
     * 获取系统提示词
     *
     * @return 系统提示词内容
     */
    public String getSystemPrompt() {
        if (systemPrompt == null) {
            try {
                systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
                log.debug("加载系统提示词成功，长度: {}", systemPrompt.length());
            } catch (IOException e) {
                log.error("加载系统提示词失败", e);
                throw new RuntimeException("加载系统提示词失败", e);
            }
        }
        return systemPrompt;
    }

    /**
     * 获取用户任务提示词模板
     *
     * @return 用户任务提示词模板
     */
    public String getUserPromptTemplate() {
        if (userPromptTemplate == null) {
            try {
                userPromptTemplate = userPromptTemplateResource.getContentAsString(StandardCharsets.UTF_8);
                log.debug("加载用户任务提示词模板成功，长度: {}", userPromptTemplate.length());
            } catch (IOException e) {
                log.error("加载用户任务提示词模板失败", e);
                throw new RuntimeException("加载用户任务提示词模板失败", e);
            }
        }
        return userPromptTemplate;
    }

    /**
     * 格式化用户任务提示词
     *
     * @param database   数据库名
     * @param logTime    日志时间
     * @param queryTime  查询耗时（秒）
     * @param lockTime   锁等待时间（秒）
     * @param rowsExamined 扫描行数
     * @param rowsSent   返回行数
     * @param sql        SQL 语句
     * @return 格式化后的提示词
     */
    public String formatUserPrompt(String database, String logTime, Double queryTime,
                                    Double lockTime, Long rowsExamined, Long rowsSent, String sql) {
        String template = getUserPromptTemplate();
        return template
                .replace("{database}", database != null ? database : "unknown")
                .replace("{logTime}", logTime != null ? logTime : "unknown")
                .replace("{queryTime}", queryTime != null ? String.format("%.3f", queryTime) : "unknown")
                .replace("{lockTime}", lockTime != null ? String.format("%.3f", lockTime) : "0")
                .replace("{rowsExamined}", rowsExamined != null ? rowsExamined.toString() : "0")
                .replace("{rowsSent}", rowsSent != null ? rowsSent.toString() : "0")
                .replace("{sql}", sql != null ? sql : "");
    }
}
