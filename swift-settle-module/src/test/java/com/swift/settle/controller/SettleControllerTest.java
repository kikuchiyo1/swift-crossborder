package com.swift.settle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettleController.class)
class SettleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void settlePaymentReceivesMessageReference() throws Exception {
        mockMvc.perform(post("/api/v1/swift/settle/receive")
                        .contentType("application/json")
                        .content("""
                                {
                                  "msgId": "msg-001",
                                  "msgType": "pacs.008.001.08"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
