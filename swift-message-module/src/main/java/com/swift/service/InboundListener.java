package com.swift.service;

import com.swift.entity.InboundMessage;
import com.swift.repository.InboundMessageMapper;
import com.swift.util.XmlUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 收报监听（TCP Server）。收到报文只落库 PENDING_ROUTING，然后触发异步路由处理
 */
@Slf4j
@Component
public class InboundListener {

    private final InboundMessageMapper repo;
    private final int port;
    private ServerSocket server;
    private volatile boolean running;
    
    @Autowired
    private AsyncRouter asyncRouter;

    public InboundListener(InboundMessageMapper repo,
                           @Value("${swift.inbound-port}") int port) {
        this.repo = repo;
        this.port = port;
    }

    @PostConstruct
    void start() {
        running = true;
        Thread.startVirtualThread(() -> {
            try {
                server = new ServerSocket(port);
                log.info("[收报监听] 端口 {} 已启动", port);
                while (running) {
                    Socket client = server.accept();
                    Thread.startVirtualThread(() -> receive(client));
                }
            } catch (IOException e) {
                if (running) log.error("[收报监听] 异常: {}", e.getMessage());
            }
        });
    }

    private void receive(Socket client) {
        String xml = null;
        try (client) {
            xml = new String(client.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 解析报文类型和ID
            String msgType = XmlUtils.detectType(xml);
            String msgId;
            
            // 根据报文类型决定如何提取ID
            if ("ACK".equals(msgType) || "NACK".equals(msgType)) {
                // ACK/NACK报文使用OrgnlMsgId作为原报文ID
                msgId = XmlUtils.extractTag(xml, "OrgnlMsgId");
                log.info("[收报监听] 收到{}报文，关联原报文ID: {}", msgType, msgId);
                // 如果无法提取 OrgnlMsgId，则使用随机生成的ID避免报文被丢弃
                if (msgId.isEmpty()) {
                    msgId = "TEMP_ID_" + System.currentTimeMillis();
                    log.warn("[收报监听] ACK/NACK报文无法提取OrgnlMsgId，使用临时ID: {}", msgId);
                }
            } else {
                // 普通报文使用BizMsgIdr
                msgId = XmlUtils.extractTag(xml, "BizMsgIdr");
                // 如果无法提取BizMsgIdr，尝试使用OrgnlMsgId（如pacs.002报文）
                if (msgId.isEmpty()) {
                    msgId = XmlUtils.extractTag(xml, "OrgnlMsgId");
                    log.info("[收报监听] 使用OrgnlMsgId作为msgId: {}", msgId);
                }
            }
            
            // 如果仍然无法提取ID，丢弃报文
            if (msgId.isEmpty()) {
                log.warn("[收报监听] 无法提取报文ID，报文将被丢弃");
                return;
            }
            
            log.info("[收报监听] 开始处理报文 msgId={}, type={}", msgId, msgType);
            
            InboundMessage message = new InboundMessage();
            message.setMsgId(msgId);
            message.setMsgType(msgType);
            message.setSenderBic(XmlUtils.extractBic(xml, "Fr"));
            message.setReceiverBic(XmlUtils.extractBic(xml, "To"));
            message.setRawContent(xml);
            message.setStatus(InboundMessage.PENDING_ROUTING);
            repo.save(message);
            
            log.info("[收报监听] 报文已落库 msgId={}, type={}", msgId, msgType);
            
            // 提交异步路由处理（AsyncRouter内部使用线程池处理）
            asyncRouter.routeInboundMessage(message.getId());
            log.info("[收报监听] 报文路由任务已提交 msgId={}", msgId);
        } catch (DuplicateKeyException e) {
            String msgId = xml != null ? XmlUtils.extractTag(xml, "BizMsgIdr") : "未知";
            log.warn("[收报监听] 重复报文, 已忽略: msgId={}", msgId);
        } catch (IOException e) {
            log.error("[收报监听] 接收失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[收报监听] 未知异常: {}", e.getMessage());
        }
    }

    @PreDestroy
    void stop() {
        running = false;
        if (server != null) {
            try { server.close(); } catch (IOException e) { log.warn("[收报监听] 关闭异常: {}", e.getMessage()); }
        }
        log.info("[收报监听] 已停止");
    }
}