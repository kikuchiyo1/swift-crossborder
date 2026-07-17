package com.swift.simulator;

import com.swift.util.XmlUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * SWIFT 网络模拟器（TCP Server）。
 *
 * 报文平台(Client) 连上发报 → 本类只读报文不写回执。
 * 回执(ACK/NACK) 通过收报端口推送，和 pacs.002 走同一条通道。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "simulator.enabled", havingValue = "true", matchIfMissing = true)
public class SwiftNetworkSimulator {

    private static final int PUSH_RETRY_MAX = 3;      // 推送失败最大重试次数
    private static final int PUSH_RETRY_DELAY_MS = 1000; // 推送重试间隔 1 秒

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ServerSocket server;
    private volatile boolean running;

    private final int swiftPort;
    private final String platformHost;
    private final int platformPort;
    private final int delaySeconds;
    @Getter
    private final String ourBic;
    private final String theirBic;

    public SwiftNetworkSimulator(
            @Value("${swift.swift-port}") int swiftPort,
            @Value("${swift.swift-host}") String swiftHost,
            @Value("${swift.inbound-port}") int platformPort,
            @Value("${swift.pacs002-delay-seconds}") int delaySeconds,
            @Value("${swift.bic}") String ourBic,
            @Value("${swift.counterparty-bic}") String theirBic) {
        this.swiftPort = swiftPort;
        this.platformHost = swiftHost.equals("localhost") ? "127.0.0.1" : swiftHost;
        this.platformPort = platformPort;
        this.delaySeconds = delaySeconds;
        this.ourBic = ourBic;
        this.theirBic = theirBic;
    }

    @PostConstruct
    void start() {
        running = true;
        Thread.startVirtualThread(() -> {
            try {
                server = new ServerSocket(swiftPort);
                log.info("[SWIFT-SIM] 发报端口 {} 已启动", swiftPort);
                while (running) {
                    Socket client = server.accept();
                    Thread.startVirtualThread(() -> handle(client));
                }
            } catch (IOException e) {
                if (running) log.error("[SWIFT-SIM] 异常: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    void stop() {
        running = false;
        if (server != null) try { server.close(); } catch (IOException e) { log.warn("[SWIFT-SIM] 关闭异常: {}", e.getMessage()); }
        scheduler.shutdown();
        log.info("[SWIFT-SIM] 已停止");
    }

    /** 读报文 → 推送回执到收报端口。pacs.008 额外延时推送 pacs.002。 */
    private void handle(Socket client) {
        try (client) {
            String xml = readToEof(client);

            if (xml == null || xml.isBlank()) {
                pushWithRetry(nack("报文为空", ""));
                return;
            }
            if (xml.contains("<SwAck>")) {
                pushWithRetry(ack(""));
                return;
            }

            boolean isPacs008 = xml.contains("FIToFICstmrCdtTrf");
            boolean isPacs002 = xml.contains("FIToFIPmtStsRpt");
            if (!isPacs008 && !isPacs002) {
                pushWithRetry(nack("无法识别", ""));
                return;
            }

            String msgId = XmlUtils.extractTag(xml, "BizMsgIdr");
            pushWithRetry(ack(msgId));
            log.info("[SWIFT-SIM] 收到 {}, 已推送 ACK 到收报端口", isPacs008 ? "pacs.008" : "pacs.002");

            if (isPacs008) schedulePacs002(xml);
        } catch (IOException e) {
            log.error("[SWIFT-SIM] IO异常: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[SWIFT-SIM] 未知异常: {}", e.getMessage());
        }
    }

    /** 延时推送 pacs.002 到报文平台收报端口 */
    private void schedulePacs002(String xml) {
        String msgId = XmlUtils.extractTag(xml, "BizMsgIdr");
        String frBic = XmlUtils.extractBic(xml, "Fr");
        String toBic = XmlUtils.extractBic(xml, "To");
        // pacs.002 的 From/To 与原始 pacs.008 相反：响应方为 Fr，原发送方为 To
        scheduler.schedule(() -> pushWithRetry(
                pacs002(msgId, toBic, frBic, "ACCC")),
                delaySeconds, TimeUnit.SECONDS);
        log.info("[SWIFT-SIM] {}s 后推送 pacs.002", delaySeconds);
    }

    /** 连接到报文平台收报端口推送报文，失败自动重试最多 3 次 */
    private void pushWithRetry(String xml) {
        for (int i = 0; i < PUSH_RETRY_MAX; i++) {
            try (Socket s = new Socket(platformHost, platformPort)) {
                s.getOutputStream().write(xml.getBytes(StandardCharsets.UTF_8));
                return; // 成功则退出
            } catch (IOException e) {
                if (i < PUSH_RETRY_MAX - 1) {
                    log.warn("[SWIFT-SIM] 推送失败(第{}次重试): {}", i + 1, e.getMessage());
                    try { Thread.sleep(PUSH_RETRY_DELAY_MS); } catch (InterruptedException ignored) {}
                } else {
                    log.error("[SWIFT-SIM] 推送彻底失败(已重试{}次): {}", PUSH_RETRY_MAX, e.getMessage());
                }
            }
        }
    }

    // IO
    private static String readToEof(Socket c) throws IOException {
        return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // 报文组装
    private static String ack(String orgnlMsgId) {
        return "<SwAck><OrgnlMsgId>" + orgnlMsgId + "</OrgnlMsgId><SwAckId>" + uid()
             + "</SwAckId><AckSts>ACK</AckSts><CreDtTm>" + Instant.now() + "</CreDtTm></SwAck>";
    }
    private static String nack(String reason, String orgnlMsgId) {
        return "<SwAck><OrgnlMsgId>" + orgnlMsgId + "</OrgnlMsgId><SwAckId>" + uid()
             + "</SwAckId><AckSts>NACK</AckSts><Rsn>" + reason + "</Rsn><CreDtTm>" + Instant.now() + "</CreDtTm></SwAck>";
    }
    private static String pacs002(String orgnlMsgId, String frBic, String toBic, String txSts) {
        String msgId = uid();
        String creDtTm = Instant.now().toString();
        return "<AppHdr>"
             + "<Fr><FIId><FinInstnId><BICFI>" + frBic + "</BICFI></FinInstnId></FIId></Fr>"
             + "<To><FIId><FinInstnId><BICFI>" + toBic + "</BICFI></FinInstnId></FIId></To>"
             + "<BizMsgIdr>" + msgId + "</BizMsgIdr>"
             + "<CreDtTm>" + creDtTm + "</CreDtTm>"
             + "</AppHdr>"
             + "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10\">"
             + "<FIToFIPmtStsRpt>"
             + "<GrpHdr><MsgId>" + msgId + "</MsgId><CreDtTm>" + creDtTm + "</CreDtTm></GrpHdr>"
             + "<OrgnlGrpInf><OrgnlMsgId>" + orgnlMsgId + "</OrgnlMsgId><OrgnlMsgNmId>pacs.008.001.08</OrgnlMsgNmId></OrgnlGrpInf>"
             + "<TxInfAndSts><TxSts>" + txSts + "</TxSts></TxInfAndSts>"
             + "</FIToFIPmtStsRpt>"
             + "</Document>";
    }
    private static String uid() { return UUID.randomUUID().toString().replace("-", ""); }


}
