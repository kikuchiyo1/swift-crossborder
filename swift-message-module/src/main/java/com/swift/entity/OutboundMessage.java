package com.swift.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待发送的报文实体（发往外部SWIFT网络）
 * 映射 outbound_message 表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("outbound_message")
public class OutboundMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String msgType;
    private String senderBic;
    private String receiverBic;
    private String rawContent;
    private String status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- 状态常量 ---
    public static final String PENDING_ROUTING = "PENDING_ROUTING";
    public static final String IN_PROGRESS   = "IN_PROGRESS";
    public static final String DELIVERED     = "DELIVERED";
    public static final String FAILED        = "FAILED";
}