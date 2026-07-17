package com.swift.service;

import com.swift.entity.OutboundMessage;
import com.swift.repository.OutboundMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * 发报定时发送器。扫描 PENDING_ROUTING 的 OUTWARD 报文，通过 TCP 发送到 SWIFT 网络。
 */
@Slf4j
@Component
public class OutboundSender {

    private static final int CONNECT_TIMEOUT_MS = 5000; // TCP 连接超时 5 秒

    private final OutboundMessageMapper repo;
    private final String swiftHost;
    private final int swiftPort;

    public OutboundSender(OutboundMessageMapper repo,
                          @Value("${swift.swift-host}") String swiftHost,
                          @Value("${swift.swift-port}") int swiftPort) {
        this.repo = repo;
        this.swiftHost = swiftHost;
        this.swiftPort = swiftPort;
    }

    @Scheduled(fixedDelayString = "${swift.outbound-scan-interval-ms:2000}")
    public void send() {
        for (OutboundMessage m : repo.findByStatus(OutboundMessage.PENDING_ROUTING, 50)) {
            // 防御：retry_count 已超限的直接标 FAILED
            if (m.getRetryCount() >= 3) {
                repo.updateStatus(m.getId(), OutboundMessage.FAILED);
                log.warn("[发报服务] 报文重试次数已超限，标记为FAILED，msgId={}", m.getMsgId());
                continue;
            }

            log.info("[发报服务] 开始发送报文 msgId={}, type={}, retryCount={}", m.getMsgId(), m.getMsgType(), m.getRetryCount());
            
            try {
                // 创建 Socket 并设置连接超时，防止 SWIFT 模拟器不可达时永久阻塞
                Socket s = new Socket();
                s.connect(new InetSocketAddress(swiftHost, swiftPort), CONNECT_TIMEOUT_MS);
                try (s) {
                    s.getOutputStream().write(m.getRawContent().getBytes(StandardCharsets.UTF_8));
                    repo.updateStatus(m.getId(), OutboundMessage.IN_PROGRESS);
                    log.info("[发报服务] 报文发送成功，msgId={}，状态更新为IN_PROGRESS", m.getMsgId());
                }
            } catch (IOException e) {
                log.error("[发报服务] 发送失败 msgId={}: {}", m.getMsgId(), e.getMessage());
                if (m.getRetryCount() < 2) {
                    repo.retry(m.getId());
                    log.warn("[发报服务] 报文发送失败，第{}次重试，msgId={}", m.getRetryCount() + 1, m.getMsgId());
                } else {
                    repo.updateStatus(m.getId(), OutboundMessage.FAILED);
                    log.error("[发报服务] 报文发送失败次数超限（3次），标记为FAILED，msgId={}", m.getMsgId());
                }
            }
        }
    }
}
