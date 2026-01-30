package com.dbdoctor.controller;

import com.dbdoctor.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    /**
     * 发送测试邮件
     *
     * @param params 请求参数
     * @return 发送结果
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/test")
    public Map<String, Object> sendTestEmail(@RequestBody Map<String, Object> params) {
        List<String> to = (List<String>) params.get("to");
        String subject = (String) params.getOrDefault("subject", "DB-Doctor 测试邮件");

        log.info("发送测试邮件: to={}, subject={}", to, subject);

        try {
            notifyService.sendTestEmail(to, subject);

            return Map.of(
                    "code", 200,
                    "message", "测试邮件发送成功",
                    "data", Map.of(
                            "to", to,
                            "sentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
            );
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            return Map.of(
                    "code", 500,
                    "message", "邮件发送失败: " + e.getMessage(),
                    "data", null
            );
        }
    }
}
