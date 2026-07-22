package com.swift.simulator;

import com.swift.message.process.application.service.BuildMessage;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.message.process.domain.model.GroupHdr;
import com.swift.message.process.domain.model.Transaction;
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
 * 报文组装使用 SDK BuildMessage，技术回执使用真实 SWIFT 格式。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "simulator.enabled", havingValue = "true", matchIfMissing = true)
public class SwiftNetworkSimulator {

    private static final int PUSH_RETRY_MAX = 3;
    private static final int PUSH_RETRY_DELAY_MS = 1000;

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

    private void handle(Socket client) {
        try (client) {
            String xml = readToEof(client);

            if (xml == null || xml.isBlank()) {
                pushWithRetry(nack("报文为空", ""));
                return;
            }
            if (xml.contains("<DeliveryNotif>") || xml.contains("<FwdFailedNtfctn>")) {
                pushWithRetry(ack(""));
                return;
            }

            boolean isPacs008 = xml.contains("FIToFICstmrCdtTrf");
            boolean isPacs002 = xml.contains("FIToFIPmtStsRpt");
            if (!isPacs008 && !isPacs002) {
                pushWithRetry(nack("无法识别", ""));
                return;
            }

            String msgId = extractTag(xml, "BizMsgIdr");
            pushWithRetry(ack(msgId));
            log.info("[SWIFT-SIM] 收到 {}, 已推送 DeliveryNotif", isPacs008 ? "pacs.008" : "pacs.002");

            if (isPacs008) schedulePacs002(xml);
        } catch (IOException e) {
            log.error("[SWIFT-SIM] IO 异常: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[SWIFT-SIM] 未知异常: {}", e.getMessage());
        }
    }

    private void schedulePacs002(String xml) {
        String msgId = extractTag(xml, "BizMsgIdr");
        String frBic = extractBic(xml, "Fr");
        String toBic = extractBic(xml, "To");
        scheduler.schedule(() -> pushWithRetry(
                buildPacs002(msgId, toBic, frBic, "ACCC")),
                delaySeconds, TimeUnit.SECONDS);
        log.info("[SWIFT-SIM] {} 秒后推送 pacs.002", delaySeconds);
    }

    private String buildPacs002(String orgnlMsgId, String frBic, String toBic, String txSts) {
        String now = Instant.now().toString();
        AppHdr appHdr = new AppHdr();
        appHdr.setSenderBic(frBic);
        appHdr.setReceiverBic(toBic);
        appHdr.setUuid(UUID.randomUUID().toString());
        appHdr.setMsgType("pacs.002.001.10");
        appHdr.setCreateTime(now);

        String body = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10\">"
                + "<FIToFIPmtStsRpt>"
                + "<GrpHdr><MsgId>" + UUID.randomUUID().toString().replace("-", "")
                + "</MsgId><CreDtTm>" + now + "</CreDtTm></GrpHdr>"
                + "<OrgnlGrpInf><OrgnlMsgId>" + orgnlMsgId
                + "</OrgnlMsgId><OrgnlMsgNmId>pacs.008.001.08</OrgnlMsgNmId></OrgnlGrpInf>"
                + "<TxInfAndSts><TxSts>" + txSts + "</TxSts></TxInfAndSts>"
                + "</FIToFIPmtStsRpt>"
                + "</Document>";
        return BuildMessage.buildAppHdr(appHdr) + body;
    }

    public void sendInboundPacs008(String debtorName, String creditorName,
                                   String amount, String currency) {
        String msgId = "PACS008-" + UUID.randomUUID().toString().substring(0, 8);
        String now = Instant.now().toString();

        AppHdr appHdr = new AppHdr();
        appHdr.setSenderBic(theirBic);
        appHdr.setReceiverBic(ourBic);
        appHdr.setUuid(msgId);
        appHdr.setMsgType("pacs.008.001.08");
        appHdr.setCreateTime(now);

        GroupHdr groupHdr = new GroupHdr();
        groupHdr.setMsgId("GRP-" + msgId);
        groupHdr.setCreDtTime(now);
        groupHdr.setNumberOfTransactions(1);
        groupHdr.setSettlementMethod("COVE");

        Transaction tx = new Transaction();
        tx.setEndToEndId("E2E-" + msgId);
        tx.setIntrBkSttlmValue(amount);
        tx.setIntrBkSttlmCcy(currency);
        tx.setIntrBkSttlmDt(Instant.now().toString().substring(0, 10));
        tx.setChargeBearer("SLEV");
        tx.setDebtorName(debtorName);
        tx.setDebtorAgentBic(theirBic);
        tx.setCreditorName(creditorName);
        tx.setCreditorAgentBic(ourBic);

        String xml = BuildMessage.buildAppHdr(appHdr)
                + BuildMessage.buildMessageBody(groupHdr, tx);
        log.info("[SWIFT-SIM] 模拟汇入 pacs.008, msgId={}, {} -> {}, {}{}",
                msgId, debtorName, creditorName, amount, currency);
        sendInboundMessage(xml);
    }

    private void pushWithRetry(String xml) {
        for (int i = 0; i < PUSH_RETRY_MAX; i++) {
            try (Socket s = new Socket(platformHost, platformPort)) {
                s.getOutputStream().write(xml.getBytes(StandardCharsets.UTF_8));
                return;
            } catch (IOException e) {
                if (i < PUSH_RETRY_MAX - 1) {
                    log.warn("[SWIFT-SIM] 推送失败(第 {} 次重试): {}", i + 1, e.getMessage());
                    try { Thread.sleep(PUSH_RETRY_DELAY_MS); } catch (InterruptedException ignored) {}
                } else {
                    log.error("[SWIFT-SIM] 推送彻底失败(已重试 {} 次): {}", PUSH_RETRY_MAX, e.getMessage());
                }
            }
        }
    }

    public void sendInboundMessage(String xml) {
        log.info("[SWIFT-SIM] 模拟外部推送入站报文, 长度={}", xml.length());
        pushWithRetry(xml);
    }

    // ======================== 工具方法 ========================

    private static String readToEof(Socket c) throws IOException {
        return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** SWIFT 投递通知 DeliveryNotif（系统级正向回执） */
    private static String ack(String orgnlMsgId) {
        return "<DeliveryNotif>"
             + "<OrgnlMsgId>" + orgnlMsgId + "</OrgnlMsgId>"
             + "<Sts>DLVD</Sts>"
             + "<CreDtTm>" + Instant.now() + "</CreDtTm>"
             + "</DeliveryNotif>";
    }

    /** SWIFT 转发失败通知 FwdFailedNtfctn（系统级负向回执） */
    private static String nack(String reason, String orgnlMsgId) {
        return "<FwdFailedNtfctn>"
             + "<OrgnlMsgId>" + orgnlMsgId + "</OrgnlMsgId>"
             + "<Rsn>" + reason + "</Rsn>"
             + "<CreDtTm>" + Instant.now() + "</CreDtTm>"
             + "</FwdFailedNtfctn>";
    }

    private static String extractTag(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">", s);
        return (s < 0 || e < 0) ? "" : xml.substring(s + tag.length() + 2, e);
    }

    private static String extractBic(String xml, String tag) {
        int s = xml.indexOf("<" + tag + ">");
        int e = xml.indexOf("</" + tag + ">", s);
        if (s < 0 || e < 0) return null;
        return extractTag(xml.substring(s, e), "BICFI");
    }
}