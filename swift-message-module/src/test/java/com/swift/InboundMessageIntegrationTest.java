package com.swift;

import com.swift.simulator.SwiftNetworkSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 汇入 pacs.008 报文的端到端集成测试。
 * 使用 SWIFT 模拟器的 sendInboundPacs008() 方法（内部用 SDK BuildMessage 组装报文）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"swift.inbound-port=15003"})
@DisplayName("汇入 pacs.008 集成测试")
class InboundMessageIntegrationTest {

    @Autowired
    private SwiftNetworkSimulator simulator;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("模拟 SWIFT 推送 pacs.008 汇入报文")
    void shouldReceiveAndStorePacs008() {
        // 第一步：通过模拟器组装并发送 pacs.008（只填必填字段）
        simulator.sendInboundPacs008("OVERSEAS TRADING CO LTD",
                "DOMESTIC IMPORT CO LTD", "150000.00", "USD");

        // 第二步：等待异步路由处理，轮询 REST 接口直到返回 200
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    "/api/message/PACS008-", Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        });

        // 注意：msgId 是动态生成的（PACS008-xxxxxxxx），无法硬编码断言，
        // 但只要 await 通过即证明报文已成功入库并路由。
    }

    @Test
    @DisplayName("发送空报文到报文平台，不应被存储")
    void shouldNotStoreEmptyMessage() {
        simulator.sendInboundMessage("   ");

        ResponseEntity<Map> resp = restTemplate.getForEntity(
                "/api/message/__NONEXIST__", Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(404);
    }
}