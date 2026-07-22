package com.swift.service;

import com.swift.entity.InboundMessage;
import com.swift.message.process.application.service.ParseMessage;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.repository.InboundMessageMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 收报监听（TCP Server）。
 * 报文解析使用 SDK ParseMessage，技术回执使用轻量 extractTag。
 */
@Slf4j
@Component
public class InboundListener {

    private final InboundMessageMapper repo;
    private final int port;
    private ServerSocket server;
    private volatile boolean running;
    private final AsyncRouter asyncRouter;

    public InboundListener(InboundMessageMapper repo,
                           @Value("${swift.inbound-port}") int port,
                           AsyncRouter asyncRouter) {
        this.repo = repo;
        this.port = port;
        this.asyncRouter = asyncRouter;
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

            // 优先用 SDK 解析 AppHdr（跳过技术回执格式）
            AppHdr appHdr = parseAppHdrSafely(xml);
            String msgType;
            String msgId;

            if (appHdr != null && appHdr.getMsgType() != null) {
                msgType = appHdr.getMsgType().contains("pacs.008") ? "pacs.008" :
                          appHdr.getMsgType().contains("pacs.002") ? "pacs.002" :
                          appHdr.getMsgType();
                msgId = appHdr.getUuid();
                log.info("[收报监听] 收到 {} 报文, msgId={}", msgType, msgId);
            } else if (xml.contains("<DeliveryNotif>")) {
                msgType = "ACK";
                msgId = extractTag(xml, "OrgnlMsgId");
                log.info("[收报监听] 收到 DeliveryNotif 回执, 关联原报文ID: {}", msgId);
            } else if (xml.contains("<FwdFailedNtfctn>")) {
                msgType = "NACK";
                msgId = extractTag(xml, "OrgnlMsgId");
                log.info("[收报监听] 收到 FwdFailedNtfctn 回执, 关联原报文ID: {}", msgId);
                if (msgId.isEmpty()) {
                    msgId = "TEMP_" + System.currentTimeMillis() + "_" + Thread.currentThread().threadId();
                    log.warn("[收报监听] 无法提取 OrgnlMsgId，使用临时ID: {}", msgId);
                }
            } else {
                msgType = xml.contains("FIToFICstmrCdtTrf") ? "pacs.008" :
                          xml.contains("FIToFIPmtStsRpt")  ? "pacs.002" : "UNKNOWN";
                msgId = extractTag(xml, "BizMsgIdr");
                if (msgId.isEmpty()) msgId = extractTag(xml, "OrgnlMsgId");
                log.info("[收报监听] 兜底解析: type={}, msgId={}", msgType, msgId);
            }

            if (msgId.isEmpty()) {
                log.warn("[收报监听] 无法提取报文ID，报文将被丢弃");
                return;
            }

            log.info("[收报监听] 开始处理报文 msgId={}, type={}", msgId, msgType);

            InboundMessage message = new InboundMessage();
            message.setMsgId(msgId);
            message.setMsgType(msgType);
            message.setSenderBic(appHdr != null ? appHdr.getSenderBic() : null);
            message.setReceiverBic(appHdr != null ? appHdr.getReceiverBic() : null);
            message.setRawContent(xml);
            message.setStatus(InboundMessage.PENDING_ROUTING);
            repo.save(message);

            log.info("[收报监听] 报文已落库 msgId={}, type={}", msgId, msgType);

            asyncRouter.routeInboundMessage(message.getId());
            log.info("[收报监听] 报文路由任务已提交 msgId={}", msgId);
        } catch (DuplicateKeyException e) {
            String msgId = xml != null ? extractTag(xml, "BizMsgIdr") : "unknown";
            if (msgId.isEmpty()) msgId = extractTag(xml, "OrgnlMsgId");
            log.warn("[收报监听] 重复报文, 已忽略: msgId={}", msgId);
        } catch (IOException e) {
            log.error("[收报监听] 接收失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[收报监听] 未知异常: {}", e.getMessage());
        }
    }

    /** SDK 无法解析技术回执格式，提前跳过，避免日志 WARN */
    private static AppHdr parseAppHdrSafely(String xml) {
        if (xml.contains("<DeliveryNotif>") || xml.contains("<FwdFailedNtfctn>")) return null;
        try {
            return ParseMessage.appHdrParse(xml);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractTag(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">", s);
        return (s < 0 || e < 0) ? "" : xml.substring(s + tag.length() + 2, e);
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