package com.swift.message.process.model;

/** ISO 20022 Business Application Header（head.001.001.02）中的常用信息。 */
public record AppHdr(
        /**
         * XML 字段：{@code CharSet}。含义：报文字符集名称；非必传。
         * 未提供时由 XML 声明或传输协议确定字符编码，本字段为 {@code null}。
         */
        String charSet,

        /**
         * XML 路径：{@code Fr/FIId/FinInstnId/BICFI}。含义：发报方 BIC。
         * {@code Fr} 必传，但参与方可以通过 {@code OrgId} 等其他身份信息表示，因此 BIC 字段本身并非绝对必传；
         * 使用其他身份信息时本字段为 {@code null}。
         */
        String senderBic,

        /**
         * XML 路径：{@code To/FIId/FinInstnId/BICFI}。含义：收报方 BIC。
         * {@code To} 必传，但参与方可以通过 {@code OrgId} 等其他身份信息表示，因此 BIC 字段本身并非绝对必传；
         * 使用其他身份信息时本字段为 {@code null}。
         */
        String receiverBic,

        /** XML 字段：{@code BizMsgIdr}。含义：业务报文唯一标识；必传。 */
        String uuid,

        /** XML 字段：{@code MsgDefIdr}。含义：报文定义标识，例如 {@code pacs.008.001.08}；必传。 */
        String msgType,

        /** XML 字段：{@code BizSvc}。含义：业务服务标识；ISO 20022 基础结构中非必传，但具体市场规范（如 CBPR+）可能要求必传。 */
        String bizSvc,

        /** XML 字段：{@code CreDt}。含义：报文头创建时间；必传。SDK 同时兼容非标准简化字段 {@code CreDtTm}。 */
        String createTime,

        /**
         * XML 字段：{@code CpyDplct}。含义：副本/重复类型；非必传。
         * 用于区分副本或重复报文（如 {@code COPY}、{@code CODU}、{@code DUPL}），不能由 {@code PssblDplct} 替代。
         */
        String dupTag,

        /**
         * XML 字段：{@code PssblDplct}。含义：是否可能为重复报文；非必传，未提供时为 {@code null}。
         * 这是布尔标志，与表示副本/重复类型的 {@code CpyDplct} 是两个不同字段。
         */
        Boolean psbDupTag,

        /** XML 字段：{@code Prty}。含义：处理优先级；非必传，通常取 {@code HIGH} 或 {@code NORM}。 */
        String priority) {
}
