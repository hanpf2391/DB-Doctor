package com.dbdoctor.monitoring.websocket;

import com.dbdoctor.entity.AlertHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 告警 WebSocket 处理器
 *
 * <p>将告警实时推送给所有连接的客户端</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的 WebSocket 会话
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("[WebSocket] 客户端连接: sessionId={}, 当前连接数={}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("[WebSocket] 客户端断开: sessionId={}, 当前连接数={}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发送的消息（如心跳检测）
        String payload = message.getPayload();
        log.debug("[WebSocket] 收到客户端消息: sessionId={}, message={}", session.getId(), payload);

        // 如果是心跳消息，回复 pong
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WebSocket] 传输错误: sessionId={}", session.getId(), exception);
        sessions.remove(session);
    }

    /**
     * 广播告警消息给所有客户端
     *
     * @param alert 告警对象
     */
    public void broadcastAlert(AlertHistory alert) {
        if (sessions.isEmpty()) {
            log.debug("[WebSocket] 没有活跃连接，跳过广播: alertId={}", alert.getId());
            return;
        }

        try {
            // 将告警对象转换为 JSON
            String message = objectMapper.writeValueAsString(alert);

            // 广播给所有连接的客户端
            int successCount = 0;
            int failCount = 0;

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                        successCount++;
                    } catch (Exception e) {
                        log.error("[WebSocket] 发送消息失败: sessionId={}", session.getId(), e);
                        failCount++;
                    }
                }
            }

            log.info("[WebSocket] 告警广播完成: alertId={}, 成功={}, 失败={}",
                    alert.getId(), successCount, failCount);

        } catch (Exception e) {
            log.error("[WebSocket] 广播告警失败: alertId={}", alert.getId(), e);
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * 向指定会话发送消息
     *
     * @param sessionId 会话ID
     * @param message 消息内容
     */
    public void sendMessageToSession(String sessionId, String message) {
        for (WebSocketSession session : sessions) {
            if (session.getId().equals(sessionId) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.debug("[WebSocket] 消息已发送: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.error("[WebSocket] 发送消息失败: sessionId={}", sessionId, e);
                }
                break;
            }
        }
    }
}
