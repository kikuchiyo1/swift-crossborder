package com.swift.message.process.model;

import java.util.List;

/** ISO 20022 pacs.008 交易详情或 pacs.002 交易状态详情。 */
public record Transaction(
        /** XML 路径：{@code CdtTrfTxInf/PmtId/InstrId}。含义：付款指令标识；pacs.008 非必传，pacs.002 使用 {@code TxInfAndSts/OrgnlInstrId}。 */
        String instructionId,

        /**
         * XML 路径：{@code CdtTrfTxInf/PmtId/EndToEndId}。含义：端到端标识；pacs.008 必传。
         * 无法提供业务标识时，{@code EndToEndId} 可填写约定值 {@code NOTPROVIDED}，因此节点必传不代表一定有真实业务编号。
         */
        String endToEndId,

        /** XML 路径：{@code CdtTrfTxInf/PmtId/TxId}。含义：交易标识；pacs.008 非必传，pacs.002 使用 {@code TxInfAndSts/OrgnlTxId}。 */
        String transactionId,

        /**
         * XML 路径：{@code CdtTrfTxInf/PmtId/UETR}。含义：通用端到端交易追踪号；ISO 20022 基础结构中非必传，CBPR+ 等规范可能要求必传。
         * 不存在等价替代 XML 字段。
         */
        String uetr,

        /**
         * XML 路径：pacs.008 使用 {@code CdtTrfTxInf/IntrBkSttlmAmt}；含义：银行间结算金额；必传。
         * pacs.002 可通过 {@code TxInfAndSts/OrgnlTxRef/IntrBkSttlmAmt} 携带原金额，但该 XML 字段非必传；
         * SDK 会将两个路径下 {@code IntrBkSttlmAmt} 的值返回到该属性。
         */
        String intrBkSttlmValue,

        /**
         * XML 属性：{@code CdtTrfTxInf/IntrBkSttlmAmt/@Ccy} 或 {@code TxInfAndSts/OrgnlTxRef/IntrBkSttlmAmt/@Ccy}。
         * 含义：结算金额币种；上述金额节点存在时必传，不存在等价替代 XML 属性；金额节点不存在时为 {@code null}。
         */
        String intrBkSttlmCcy,

        /**
         * XML 路径：{@code CdtTrfTxInf/InstdAmt}。含义：指示金额；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * 当指示金额或 {@code InstdAmt/@Ccy} 与 {@code IntrBkSttlmAmt} 不同时，具体业务规范通常要求提供。
         */
        String instdAmtValue,

        /**
         * XML 属性：{@code CdtTrfTxInf/InstdAmt/@Ccy}。含义：指示金额币种。
         * {@code InstdAmt} 存在时必传；{@code InstdAmt} 不存在时该 XML 属性也不存在。
         */
        String instdAmtCcy,

        /** XML 路径：{@code CdtTrfTxInf/IntrBkSttlmDt}。含义：银行间结算日期；pacs.008 必传，pacs.002 的 {@code TxInfAndSts} 中非必传。 */
        String intrBkSttlmDt,

        /** XML 路径：{@code CdtTrfTxInf/ChrgBr}。含义：费用承担方式；pacs.008 必传，pacs.002 的 {@code TxInfAndSts} 中通常不提供。 */
        String chargeBearer,

        /**
         * XML 路径：{@code CdtTrfTxInf/Dbtr/Nm}。含义：付款人名称。
         * pacs.008 中 {@code Dbtr} 必传，但 {@code Nm} 可由 {@code Dbtr/Id/OrgId} 或 {@code Dbtr/Id/PrvtId} 等身份信息替代，
         * 因此 {@code Nm} 本身非绝对必传；pacs.002 中非必传。
         */
        String debtorName,

        /**
         * XML 路径：{@code CdtTrfTxInf/Dbtr/PstlAdr}。含义：付款人地址；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * CBPR+ 等业务规范可根据付款人身份信息和业务场景要求提供；采用结构化或混合地址时，还需满足地址子字段的条件必填规则。
         */
        PostalAddress debtorAddress,

        /**
         * XML 路径：{@code CdtTrfTxInf/DbtrAcct/Id/IBAN} 或 {@code CdtTrfTxInf/DbtrAcct/Id/Othr/Id}；{@code DbtrAcct} 非必传。
         * {@code IBAN} 与 {@code Othr/Id} 互为替代，SDK 优先返回 {@code IBAN}。
         */
        String debtorAccount,

        /**
         * XML 路径：{@code CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI}。含义：付款人代理行 BIC。
         * pacs.008 中 {@code DbtrAgt} 通常必传，但 {@code BICFI} 可由 {@code ClrSysMmbId}、{@code LEI}、{@code Nm} 等字段替代，
         * 因此 {@code BICFI} 本身非绝对必传；pacs.002 中非必传。
         */
        String debtorAgentBic,

        /**
         * XML 路径：{@code CdtTrfTxInf/Cdtr/Nm}。含义：收款人名称。
         * pacs.008 中 {@code Cdtr} 必传，但 {@code Nm} 可由 {@code Cdtr/Id/OrgId} 或 {@code Cdtr/Id/PrvtId} 等身份信息替代，
         * 因此 {@code Nm} 本身非绝对必传；pacs.002 中非必传。
         */
        String creditorName,

        /**
         * XML 路径：{@code CdtTrfTxInf/Cdtr/PstlAdr}。含义：收款人地址；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * CBPR+ 等业务规范可根据收款人身份信息和业务场景要求提供；采用结构化或混合地址时，还需满足地址子字段的条件必填规则。
         */
        PostalAddress creditorAddress,

        /**
         * XML 路径：{@code CdtTrfTxInf/CdtrAcct/Id/IBAN} 或 {@code CdtTrfTxInf/CdtrAcct/Id/Othr/Id}；{@code CdtrAcct} 非必传。
         * {@code IBAN} 与 {@code Othr/Id} 互为替代，SDK 优先返回 {@code IBAN}。
         */
        String creditorAccount,

        /**
         * XML 路径：{@code CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI}。含义：收款人代理行 BIC。
         * pacs.008 中 {@code CdtrAgt} 通常必传，但 {@code BICFI} 可由 {@code ClrSysMmbId}、{@code LEI}、{@code Nm} 等字段替代，
         * 因此 {@code BICFI} 本身非绝对必传；pacs.002 中非必传。
         */
        String creditorAgentBic,

        /** XML 路径：{@code CdtTrfTxInf/InitgPty/Nm}。含义：发起方名称；{@code InitgPty} 非必传。 */
        String initiatingPartyName,

        /** XML 路径：{@code CdtTrfTxInf/UltmtDbtr/Nm}。含义：最终付款人名称；{@code UltmtDbtr} 非必传。 */
        String ultimateDebtorName,

        /** XML 路径：{@code CdtTrfTxInf/UltmtCdtr/Nm}。含义：最终收款人名称；{@code UltmtCdtr} 非必传。 */
        String ultimateCreditorName,

        /**
         * XML 路径：{@code CdtTrfTxInf/Purp/Cd} 或 {@code CdtTrfTxInf/Purp/Prtry}。含义：付款用途；非必传。
         * SDK 优先返回 {@code Cd}，没有 {@code Cd} 时返回 {@code Prtry}。
         */
        String purposeCd,

        /**
         * XML 路径：{@code CdtTrfTxInf/RmtInf/Ustrd} 和 {@code CdtTrfTxInf/RmtInf/Strd/CdtrRefInf/Ref}。
         * 含义：非结构化附言及结构化收款人参考信息；{@code RmtInf} 非必传，两个 XML 字段均可重复。
         */
        List<String> rmtInfUstrd,

        /**
         * XML 路径：{@code CdtTrfTxInf/PrvsInstgAgt1/FinInstnId/BICFI} 至 {@code PrvsInstgAgt3/FinInstnId/BICFI}。
         * 含义：前序指示代理行 BIC，按数字顺序返回；各 XML 字段均非必传。
         */
        List<String> preIntrmyAgtBics,

        /** XML 路径：{@code CdtTrfTxInf/InstgAgt/FinInstnId/BICFI}。含义：指示代理行 BIC；非必传。 */
        String instructingAgentBic,

        /** XML 路径：{@code CdtTrfTxInf/InstdAgt/FinInstnId/BICFI}。含义：被指示代理行 BIC；非必传。 */
        String instructedAgentBic,

        /**
         * XML 路径：{@code CdtTrfTxInf/IntrmyAgt1/FinInstnId/BICFI} 至 {@code IntrmyAgt3/FinInstnId/BICFI}。
         * 含义：中间代理行 BIC，按数字顺序返回；各 XML 字段均非必传。
         */
        List<String> intrmyAgtBics,

        /** XML 路径：{@code TxInfAndSts/OrgnlInstrId}。含义：pacs.002 原付款指令标识；非必传，不存在等价替代 XML 字段。 */
        String originalInstructionId,

        /** XML 路径：{@code TxInfAndSts/OrgnlEndToEndId}。含义：pacs.002 原端到端标识；非必传，不存在等价替代 XML 字段。 */
        String originalEndToEndId,

        /** XML 路径：{@code TxInfAndSts/OrgnlTxId}。含义：pacs.002 原交易标识；非必传，不能由 {@code OrgnlInstrId} 或 {@code OrgnlEndToEndId} 等价替代。 */
        String originalTransactionId,

        /** XML 路径：{@code TxInfAndSts/TxSts}。含义：pacs.002 交易状态；非必传，未提供时不能据此推断交易状态。 */
        String transactionStatus,

        /**
         * XML 路径：{@code TxInfAndSts/StsRsnInf/Rsn/Cd}。含义：pacs.002 状态原因；非必传。
         * {@code Cd} 可由 {@code TxInfAndSts/StsRsnInf/Rsn/Prtry} 替代，SDK 会优先返回 {@code Cd}，
         * 没有 {@code Cd} 时返回 {@code Prtry}。
         */
        String statusReasonCode) {

    public Transaction {
        rmtInfUstrd = immutable(rmtInfUstrd);
        preIntrmyAgtBics = immutable(preIntrmyAgtBics);
        intrmyAgtBics = immutable(intrmyAgtBics);
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
