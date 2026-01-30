package com.dbdoctor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统信息控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    @Value("${db-doctor.version:2.2.0}")
    private String version;

    @Value("${db-doctor.build-time:未知}")
    private String buildTime;

    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommit;

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSystemInfo() {
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "version", "v" + version,
                        "buildTime", buildTime,
                        "gitCommit", gitCommit
                )
        );
    }
}
