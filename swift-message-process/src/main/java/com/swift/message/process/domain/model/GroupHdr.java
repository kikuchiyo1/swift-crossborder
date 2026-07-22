package com.swift.message.process.domain.model;

import lombok.Getter;
import lombok.Setter;

/** ISO 20022 pacs.008 或 pacs.002 Group Header 中的常用信息。 */
@Getter
@Setter
public class GroupHdr {
        /** XML 路径：{@code GrpHdr/MsgId}。含义：组报文唯一标识；pacs.008 和 pacs.002 均必传。 */
        private String msgId;

        /** XML 路径：{@code GrpHdr/CreDtTm}。含义：组头创建时间；pacs.008 和 pacs.002 均必传。 */
        private String creDtTime;

        /** XML 路径：{@code GrpHdr/BtchBookg}。含义：是否批量记账；非必传。 */
        private Boolean batchBooking;

        /**
         * XML 路径：{@code GrpHdr/NbOfTxs}。含义：报文包含的交易笔数；pacs.008 必传。
         * pacs.002 的 {@code GrpHdr} 通常没有 {@code NbOfTxs}，因此解析 pacs.002 时可以为 {@code null}；不存在替代 XML 字段。
         */
        private Integer numberOfTransactions;

        /** XML 路径：{@code GrpHdr/CtrlSum}。含义：交易金额控制总和；非必传，不存在等价替代 XML 字段。 */
        private String ctrlSum;

        /** XML 路径：{@code GrpHdr/TtlIntrBkSttlmAmt}。含义：银行间结算总金额；非必传。 */
        private String ttlIntrBkSttlmValue;

        /** XML 属性：{@code GrpHdr/TtlIntrBkSttlmAmt/@Ccy}。含义：银行间结算总金额币种；金额存在时必传。 */
        private String ttlIntrBkSttlmCcy;

        /**
         * XML 路径：{@code GrpHdr/IntrBkSttlmDt}。含义：银行间结算日期；pacs.008 中非必传。
         * pacs.002 的 {@code GrpHdr} 通常没有 {@code IntrBkSttlmDt}，因此解析 pacs.002 时为 {@code null}；不存在替代 XML 字段。
         */
        private String intrBkSttlmDt;

        /**
         * XML 路径：{@code GrpHdr/SttlmInf/SttlmMtd}。含义：清算方式；pacs.008 中 {@code SttlmInf} 和 {@code SttlmMtd} 必传。
         * pacs.002 的 {@code GrpHdr} 通常不包含 {@code SttlmInf}，因此解析 pacs.002 时可以为 {@code null}。
         */
        private String settlementMethod;

        /**
         * XML 路径：{@code GrpHdr/SttlmInf/ClrSys/Cd}。含义：清算系统代码；非必传。
         * {@code Cd} 可由专有清算系统标识 {@code SttlmInf/ClrSys/Prtry} 替代，SDK 会优先返回 {@code Cd}，
         * 没有 {@code Cd} 时返回 {@code Prtry}。
         */
        private String clearingSysCode;

        /**
         * XML 路径：{@code GrpHdr/InstgAgt/FinInstnId/BICFI}。含义：指示代理行 BIC；非必传。
         * 即使存在 {@code InstgAgt}，机构也可用清算会员号、LEI、名称等其他标识代替 BIC，此时本字段为 {@code null}。
         */
        private String instructingAgentBic;

        /**
         * XML 路径：{@code GrpHdr/InstdAgt/FinInstnId/BICFI}。含义：被指示代理行 BIC；非必传。
         * 即使存在 {@code InstdAgt}，机构也可用清算会员号、LEI、名称等其他标识代替 BIC，此时本字段为 {@code null}。
         */
        private String instructedAgentBic;
}
