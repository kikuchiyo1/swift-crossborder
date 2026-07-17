-- 收到的报文表（来自外部SWIFT网络）
CREATE TABLE IF NOT EXISTS inbound_message (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    msg_id          VARCHAR(64)  NOT NULL COMMENT 'BizMsgIdr 去重键',
    msg_type        VARCHAR(20)  NOT NULL COMMENT 'pacs.008 / pacs.002 / ACK / NACK',
    sender_bic      VARCHAR(11)  COMMENT '发送方BIC（AppHdr.Fr）',
    receiver_bic    VARCHAR(11)  COMMENT '接收方BIC（AppHdr.To）',
    raw_content     MEDIUMTEXT   NOT NULL COMMENT '报文XML原文',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_ROUTING',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数，上限3',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_msg_id (msg_id),
    INDEX idx_status (status)
);

-- 待发送的报文表（发往外部SWIFT网络）
CREATE TABLE IF NOT EXISTS outbound_message (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    msg_id          VARCHAR(64)  NOT NULL COMMENT 'BizMsgIdr 去重键',
    msg_type        VARCHAR(20)  NOT NULL COMMENT 'pacs.008 / pacs.002 / ACK / NACK',
    sender_bic      VARCHAR(11)  COMMENT '发送方BIC（AppHdr.Fr）',
    receiver_bic    VARCHAR(11)  COMMENT '接收方BIC（AppHdr.To）',
    raw_content     MEDIUMTEXT   NOT NULL COMMENT '报文XML原文',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_ROUTING',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '重试次数，上限3',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_msg_id (msg_id),
    INDEX idx_status (status)
);