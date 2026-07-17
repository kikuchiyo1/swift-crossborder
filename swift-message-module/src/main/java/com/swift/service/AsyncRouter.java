package com.swift.service;

import com.swift.entity.InboundMessage;
import com.swift.entity.OutboundMessage;
import com.swift.repository.InboundMessageMapper;
import com.swift.repository.OutboundMessageMapper;
import com.swift.util.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;

/**
 * 异步报文路由处理器
 * 负责将收到的报文按类型分发到对应模块
 */
@Slf4j
@Service
public class AsyncRouter {

    private final InboundMessageMapper inboundRepo;
    private final OutboundMessageMapper outboundRepo;
    private final RestTemplate restTemplate;
    private final String clearingBaseUrl;
    private final String paymentBaseUrl;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);  // 异步处理线程池
    
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public AsyncRouter(InboundMessageMapper inboundRepo,
                       OutboundMessageMapper outboundRepo,
                       @Value("${swift.clearing-base-url}") String clearingBaseUrl,
                       @Value("${swift.payment-base-url}") String paymentBaseUrl) {
        this.inboundRepo = inboundRepo;
        this.outboundRepo = outboundRepo;
        this.restTemplate = new RestTemplate();
        this.clearingBaseUrl = clearingBaseUrl;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    /**
     * 异步路由收到的报文
     * @param messageId 报文ID
     */
    public void routeInboundMessage(Long messageId) {
        // 提交到线程池异步处理，避免阻塞调用线程
        CompletableFuture.runAsync(() -> {
            try {
                // 查询报文详情
                InboundMessage message = inboundRepo.selectById(messageId);
                if (message == null) {
                    log.error("[异步路由] 报文不存在，ID: {}", messageId);
                    return;
                }

                log.info("[异步路由] 开始路由报文 msgId={}, type={}, status={}", 
                         message.getMsgId(), message.getMsgType(), message.getStatus());

                // 更新状态为处理中，防止重复路由
                inboundRepo.updateStatus(messageId, InboundMessage.IN_PROGRESS);
                log.info("[异步路由] 报文状态已更新为IN_PROGRESS，msgId={}", message.getMsgId());

                // 根据报文类型进行路由
                switch (message.getMsgType()) {
                    case "pacs.008" -> {
                        log.info("[异步路由] 路由pacs.008到清算模块: {}", message.getMsgId());
                        handleForwardWithRetry(message);
                    }
                    case "pacs.002" -> {
                        log.info("[异步路由] 路由pacs.002到清算模块: {}", message.getMsgId());
                        handleForwardWithRetry(message);
                    }
                    case "ACK" -> {
                        log.info("[异步路由] 处理ACK回执: {}", message.getMsgId());
                        handleAck(message);
                    }
                    case "NACK" -> {
                        log.info("[异步路由] 处理NACK回执: {}", message.getMsgId());
                        handleNack(message);
                    }
                    default -> {
                        log.info("[异步路由] 未知报文类型，标记为DELIVERED: {}", message.getMsgId());
                        inboundRepo.updateStatus(messageId, InboundMessage.DELIVERED);
                        log.info("[异步路由] 未知报文状态已更新为DELIVERED，msgId={}", message.getMsgId());
                    }
                }

            } catch (Exception e) {
                log.error("[异步路由] 路由报文时异常 msgId={}, error={}", messageId, e.getMessage(), e);
                        
                // 更新状态失败，可能需要重试机制
                InboundMessage message = inboundRepo.selectById(messageId);
                if (message != null) {
                    if (message.getRetryCount() < 3) {
                        inboundRepo.incrementRetryKeepStatus(messageId);
                        log.info("[异步路由] 报文路由异常，已递增重试计数，ID: {}", messageId);
                    } else {
                        inboundRepo.updateStatus(messageId, InboundMessage.FAILED);
                        log.error("[异步路由] 报文路由异常且重试次数超限，标记为FAILED，ID: {}", messageId);
                    }
                }
            }
        }, executor);
    }

    /**
     * 转发报文到清算模块
     */
    private boolean forwardToClearingModule(InboundMessage message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> requestBody = Map.of(
                "msgId", message.getMsgId(),
                "msgType", message.getMsgType()
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = clearingBaseUrl + "/api/v1/swift/payment/settle";
            log.info("[异步路由] 发送请求到清算模块: {}", url);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                inboundRepo.updateStatus(message.getId(), InboundMessage.DELIVERED);
                log.info("[异步路由] 报文成功转发到清算模块 msgId={}", message.getMsgId());
                return true;
            } else {
                log.warn("[异步路由] 清算模块返回非2xx状态: {}", response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("[异步路由] 转发到清算模块失败 msgId={}, error={}", message.getMsgId(), e.getMessage());
            // 不抛出异常，让调用方决定是否重试
            return false;
        }
    }

    /**
     * 转发报文到清算模块并处理重试。
     * 首次失败：递增 retry_count（保持 IN_PROGRESS），新开线程循环重试。
     * 3 次全部失败 → FAILED。
     */
    private void handleForwardWithRetry(InboundMessage message) {
        if (forwardToClearingModule(message)) {
            return; // 首次转发成功
        }

        // 首次失败：递增重试计数，状态保持 IN_PROGRESS
        inboundRepo.incrementRetryKeepStatus(message.getId());
        int currentRetries = message.getRetryCount() + 1;
        log.warn("[异步路由] 首次转发失败 msgId={}, retryCount={}", message.getMsgId(), currentRetries);

        if (currentRetries >= 3) {
            inboundRepo.updateStatus(message.getId(), InboundMessage.FAILED);
            log.error("[异步路由] 转发失败次数超限，标记为FAILED msgId={}", message.getMsgId());
            return;
        }

        // 新开线程进行带退避的循环重试
        int remainingAttempts = 3 - currentRetries;
        CompletableFuture.runAsync(() ->
                retryForwardLoop(message.getId(), remainingAttempts), executor);
        log.info("[异步路由] 转发失败，已新开线程重试，剩余{}次 msgId={}", remainingAttempts, message.getMsgId());
    }

    /**
     * 循环重试转发，每次失败后递增退避延迟。
     * @param messageId   报文ID
     * @param maxRetries  最多重试次数
     */
    private void retryForwardLoop(Long messageId, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Thread.sleep(1000L * attempt); // 递增退避：1s, 2s, ...
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            InboundMessage msg = inboundRepo.selectById(messageId);
            if (msg == null) return;
            if (InboundMessage.DELIVERED.equals(msg.getStatus())) {
                log.info("[异步路由] 报文已在重试期间被标记为DELIVERED，停止重试 msgId={}", msg.getMsgId());
                return;
            }

            log.info("[异步路由] 第{}次重试转发 msgId={}", attempt, msg.getMsgId());

            if (forwardToClearingModule(msg)) {
                log.info("[异步路由] 重试转发成功 msgId={}", msg.getMsgId());
                return;
            }

            inboundRepo.incrementRetryKeepStatus(messageId);
            log.warn("[异步路由] 第{}次重试转发失败 msgId={}", attempt, msg.getMsgId());
        }

        // 所有重试均失败
        inboundRepo.updateStatus(messageId, InboundMessage.FAILED);
        log.error("[异步路由] 重试次数耗尽，标记为FAILED msgId={}", messageId);
    }

    /**
     * 处理ACK回执
     */
    private void handleAck(InboundMessage ackMessage) {
        try {
            // 提取原报文ID
            String originalMsgId = XmlUtils.extractTag(ackMessage.getRawContent(), "OrgnlMsgId");
            
            if (originalMsgId.isEmpty()) {
                log.warn("[异步路由] ACK中未找到原报文ID: {}", ackMessage.getMsgId());
                inboundRepo.updateStatus(ackMessage.getId(), InboundMessage.DELIVERED);
                return;
            }

            log.info("[异步路由] ACK关联原报文: {} -> {}", ackMessage.getMsgId(), originalMsgId);
            
            // 更新原报文状态（原报文是发出去的，存储在outbound表中）
            OutboundMessage originalMessage = outboundRepo.findByMsgId(originalMsgId);
            if (originalMessage != null) {
                String oldStatus = originalMessage.getStatus();
                if (!OutboundMessage.FAILED.equals(oldStatus)) {
                    outboundRepo.updateStatus(originalMessage.getId(), OutboundMessage.DELIVERED);
                    log.info("[异步路由] 原报文状态变更: {} → DELIVERED, msgId={}", oldStatus, originalMsgId);
                } else {
                    log.warn("[异步路由] 原报文已是FAILED状态，忽略迟到的ACK: {}", originalMsgId);
                }
            } else {
                log.warn("[异步路由] 未找到原报文: {}", originalMsgId);
            }

            // ACK自身标记为已处理
            inboundRepo.updateStatus(ackMessage.getId(), InboundMessage.DELIVERED);
            
        } catch (Exception e) {
            log.error("[异步路由] 处理ACK时异常 msgId={}, error={}", ackMessage.getMsgId(), e.getMessage());
        }
    }

    /**
     * 处理NACK回执
     */
    private void handleNack(InboundMessage nackMessage) {
        try {
            // 提取原报文ID
            String originalMsgId = XmlUtils.extractTag(nackMessage.getRawContent(), "OrgnlMsgId");
            
            if (originalMsgId.isEmpty()) {
                log.warn("[异步路由] NACK中未找到原报文ID: {}", nackMessage.getMsgId());
                inboundRepo.updateStatus(nackMessage.getId(), InboundMessage.DELIVERED);
                return;
            }

            log.info("[异步路由] NACK关联原报文: {} -> {}", nackMessage.getMsgId(), originalMsgId);
            
            // 更新原报文状态（可能需要重试）
            OutboundMessage originalMessage = outboundRepo.findByMsgId(originalMsgId);
            if (originalMessage != null) {
                if (!OutboundMessage.DELIVERED.equals(originalMessage.getStatus())) {
                    if (originalMessage.getRetryCount() < 2) {
                        // 重试机制：将状态重置为PENDING_ROUTING，由OutboundSender定时任务自动重试
                        outboundRepo.retry(originalMessage.getId());
                        log.info("[异步路由] 原报文NACK，触发第{}次重发: {}", originalMessage.getRetryCount() + 1, originalMsgId);
                    } else {
                        outboundRepo.updateStatus(originalMessage.getId(), OutboundMessage.FAILED);
                        log.warn("[异步路由] 原报文NACK，重试次数超限（3次），标记为FAILED: {}", originalMsgId);
                    }
                } else {
                    log.info("[异步路由] 原报文已是DELIVERED状态，跳过重试: {}", originalMsgId);
                }
            } else {
                log.warn("[异步路由] 未找到原报文: {}", originalMsgId);
            }

            // NACK自身标记为已处理
            inboundRepo.updateStatus(nackMessage.getId(), InboundMessage.DELIVERED);
            
        } catch (Exception e) {
            log.error("[异步路由] 处理NACK时异常 msgId={}, error={}", nackMessage.getMsgId(), e.getMessage());
        }
    }
}