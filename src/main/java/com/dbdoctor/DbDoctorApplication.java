package com.dbdoctor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DB-Doctor 应用启动类
 * MySQL 慢查询智能诊疗系统
 *
 * @author DB-Doctor
 * @version ${db-doctor.version}
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class DbDoctorApplication {

    @Value("${db-doctor.version:1.0.0}")
    private String projectVersion;

    @Value("${db-doctor.name:DB-Doctor}")
    private String projectName;

    public static void main(String[] args) {
        SpringApplication.run(DbDoctorApplication.class, args);
    }

    /**
     * 应用启动完成后输出欢迎信息
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========================================");
        log.info("{} 启动成功！", projectName);
        log.info("MySQL 慢查询智能诊疗系统");
        log.info("版本: {}", projectVersion);
        log.info("========================================");
    }
}
