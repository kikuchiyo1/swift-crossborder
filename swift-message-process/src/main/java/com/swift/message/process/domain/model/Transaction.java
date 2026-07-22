package com.swift.message.process.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** ISO 20022 pacs.008 交易详情或 pacs.002 交易状态详情。 */
@Getter
@Setter
public class Transaction {
        /** XML 路径：{@code CdtTrfTxInf/PmtId/InstrId}。含义：付款指令标识；pacs.008 非必传，pacs.002 使用 {@code TxInfAndSts/OrgnlInstrId}。 */
        private String instructionId;

        /**
         * XML 路径：{@code CdtTrfTxInf/PmtId/EndToEndId}。含义：端到端标识；pacs.008 必传。
         * 无法提供业务标识时，{@code EndToEndId} 可填写约定值 {@code NOTPROVIDED}，因此节点必传不代表一定有真实业务编号。
         */
        private String endToEndId;

        /** XML 路径：{@code CdtTrfTxInf/PmtId/TxId}。含义：交易标识；pacs.008 非必传，pacs.002 使用 {@code TxInfAndSts/OrgnlTxId}。 */
        private String transactionId;

        /**
         * XML 路径：{@code CdtTrfTxInf/PmtId/UETR}。含义：通用端到端交易追踪号；ISO 20022 基础结构中非必传，CBPR+ 等规范可能要求必传。
         * 不存在等价替代 XML 字段。
         */
        private String uetr;

        /** XML 路径：{@code PmtTpInf/InstrPrty}。含义：付款指令优先级；非必传。 */
        private String instructionPriority;

        /** XML 路径：{@code PmtTpInf/SvcLvl/Cd} 或 {@code PmtTpInf/SvcLvl/Prtry}。含义：服务等级；非必传。 */
        private String serviceLevel;

        /** XML 路径：{@code PmtTpInf/LclInstrm/Cd} 或 {@code PmtTpInf/LclInstrm/Prtry}。含义：本地支付工具；非必传。 */
        private String localInstrument;

        /** XML 路径：{@code PmtTpInf/CtgyPurp/Cd} 或 {@code PmtTpInf/CtgyPurp/Prtry}。含义：付款类别用途；非必传。 */
        private String categoryPurpose;

        /**
         * XML 路径：pacs.008 使用 {@code CdtTrfTxInf/IntrBkSttlmAmt}；含义：银行间结算金额；必传。
         * pacs.002 可通过 {@code TxInfAndSts/OrgnlTxRef/IntrBkSttlmAmt} 携带原金额，但该 XML 字段非必传；
         * SDK 会将两个路径下 {@code IntrBkSttlmAmt} 的值返回到该属性。
         */
        private String intrBkSttlmValue;

        /**
         * XML 属性：{@code CdtTrfTxInf/IntrBkSttlmAmt/@Ccy} 或 {@code TxInfAndSts/OrgnlTxRef/IntrBkSttlmAmt/@Ccy}。
         * 含义：结算金额币种；上述金额节点存在时必传，不存在等价替代 XML 属性；金额节点不存在时为 {@code null}。
         */
        private String intrBkSttlmCcy;

        /**
         * XML 路径：pacs.008 使用 {@code CdtTrfTxInf/InstdAmt}，pacs.002 使用 {@code TxInfAndSts/OrgnlTxRef/Amt/InstdAmt}。
         * 含义：指示金额；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * 当指示金额或 {@code InstdAmt/@Ccy} 与 {@code IntrBkSttlmAmt} 不同时，具体业务规范通常要求提供。
         */
        private String instdAmtValue;

        /**
         * XML 属性：pacs.008 使用 {@code CdtTrfTxInf/InstdAmt/@Ccy}，
         * pacs.002 使用 {@code TxInfAndSts/OrgnlTxRef/Amt/InstdAmt/@Ccy}。含义：指示金额币种。
         * {@code InstdAmt} 存在时必传；{@code InstdAmt} 不存在时该 XML 属性也不存在。
         */
        private String instdAmtCcy;

        /** XML 路径：{@code CdtTrfTxInf/IntrBkSttlmDt}。含义：银行间结算日期；pacs.008 必传，pacs.002 的 {@code TxInfAndSts} 中非必传。 */
        private String intrBkSttlmDt;

        /** XML 路径：{@code SttlmPrty}。含义：结算优先级；非必传。 */
        private String settlementPriority;

        /** XML 路径：{@code CdtTrfTxInf/ChrgBr}。含义：费用承担方式；pacs.008 必传，pacs.002 的 {@code TxInfAndSts} 中通常不提供。 */
        private String chargeBearer;

        /** XML 路径：{@code ChrgsInf}。含义：费用明细；非必传且可重复。 */
        private List<ChargeInfo> charges = List.of();

        /**
         * XML 路径：{@code CdtTrfTxInf/Dbtr/Nm}。含义：付款人名称。
         * pacs.008 中 {@code Dbtr} 必传，但 {@code Nm} 可由 {@code Dbtr/Id/OrgId} 或 {@code Dbtr/Id/PrvtId} 等身份信息替代，
         * 因此 {@code Nm} 本身非绝对必传；pacs.002 中非必传。
         */
        private String debtorName;

        /**
         * XML 路径：{@code CdtTrfTxInf/Dbtr/PstlAdr}。含义：付款人地址；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * CBPR+ 等业务规范可根据付款人身份信息和业务场景要求提供；采用结构化或混合地址时，还需满足地址子字段的条件必填规则。
         */
        private PostalAddress debtorAddress;

        /**
         * XML 路径：{@code CdtTrfTxInf/DbtrAcct/Id/IBAN} 或 {@code CdtTrfTxInf/DbtrAcct/Id/Othr/Id}；{@code DbtrAcct} 非必传。
         * {@code IBAN} 与 {@code Othr/Id} 互为替代，SDK 优先返回 {@code IBAN}。
         */
        private String debtorAccount;

        /**
         * XML 路径：{@code CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI}。含义：付款人代理行 BIC。
         * pacs.008 中 {@code DbtrAgt} 通常必传，但 {@code BICFI} 可由 {@code ClrSysMmbId}、{@code LEI}、{@code Nm} 等字段替代，
         * 因此 {@code BICFI} 本身非绝对必传；pacs.002 中非必传。
         */
        private String debtorAgentBic;

        /**
         * XML 路径：{@code CdtTrfTxInf/Cdtr/Nm}。含义：收款人名称。
         * pacs.008 中 {@code Cdtr} 必传，但 {@code Nm} 可由 {@code Cdtr/Id/OrgId} 或 {@code Cdtr/Id/PrvtId} 等身份信息替代，
         * 因此 {@code Nm} 本身非绝对必传；pacs.002 中非必传。
         */
        private String creditorName;

        /**
         * XML 路径：{@code CdtTrfTxInf/Cdtr/PstlAdr}。含义：收款人地址；ISO 20022 基础 XSD 基数为 {@code 0..1}。
         * CBPR+ 等业务规范可根据收款人身份信息和业务场景要求提供；采用结构化或混合地址时，还需满足地址子字段的条件必填规则。
         */
        private PostalAddress creditorAddress;

        /**
         * XML 路径：{@code CdtTrfTxInf/CdtrAcct/Id/IBAN} 或 {@code CdtTrfTxInf/CdtrAcct/Id/Othr/Id}；{@code CdtrAcct} 非必传。
         * {@code IBAN} 与 {@code Othr/Id} 互为替代，SDK 优先返回 {@code IBAN}。
         */
        private String creditorAccount;

        /**
         * XML 路径：{@code CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI}。含义：收款人代理行 BIC。
         * pacs.008 中 {@code CdtrAgt} 通常必传，但 {@code BICFI} 可由 {@code ClrSysMmbId}、{@code LEI}、{@code Nm} 等字段替代，
         * 因此 {@code BICFI} 本身非绝对必传；pacs.002 中非必传。
         */
        private String creditorAgentBic;

        /** XML 路径：{@code CdtTrfTxInf/InitgPty/Nm}。含义：发起方名称；{@code InitgPty} 非必传。 */
        private String initgPtyNm;

        /** XML 路径：{@code CdtTrfTxInf/UltmtDbtr/Nm}。含义：最终付款人名称；{@code UltmtDbtr} 非必传。 */
        private String ultmtDbtrNm;

        /** XML 路径：{@code CdtTrfTxInf/UltmtCdtr/Nm}。含义：最终收款人名称；{@code UltmtCdtr} 非必传。 */
        private String ultmtCdtrNm;

        /**
         * XML 路径：{@code CdtTrfTxInf/Purp/Cd} 或 {@code CdtTrfTxInf/Purp/Prtry}。含义：付款用途；非必传。
         * SDK 优先返回 {@code Cd}，没有 {@code Cd} 时返回 {@code Prtry}。
         */
        private String purposeCd;

        /**
         * XML 路径：{@code CdtTrfTxInf/RmtInf/Ustrd} 和 {@code CdtTrfTxInf/RmtInf/Strd/CdtrRefInf/Ref}。
         * 含义：非结构化附言及结构化收款人参考信息；{@code RmtInf} 非必传，两个 XML 字段均可重复。
         */
        private List<String> rmtInfUstrd = List.of();

        /**
         * XML 路径：{@code CdtTrfTxInf/PrvsInstgAgt1/FinInstnId/BICFI} 至 {@code PrvsInstgAgt3/FinInstnId/BICFI}。
         * 含义：前序指示代理行 BIC，按数字顺序返回；各 XML 字段均非必传。
         */
        private List<String> preIntrmyAgtBics = List.of();

        /** XML 路径：{@code CdtTrfTxInf/InstgAgt/FinInstnId/BICFI}。含义：指示代理行 BIC；非必传。 */
        private String instructingAgentBic;

        /** XML 路径：{@code CdtTrfTxInf/InstdAgt/FinInstnId/BICFI}。含义：被指示代理行 BIC；非必传。 */
        private String instructedAgentBic;

        /**
         * XML 路径：{@code CdtTrfTxInf/IntrmyAgt1/FinInstnId/BICFI} 至 {@code IntrmyAgt3/FinInstnId/BICFI}。
         * 含义：中间代理行 BIC，按数字顺序返回；各 XML 字段均非必传。
         */
        private List<String> intrmyAgtBics = List.of();

        /** XML 路径：{@code InstrForCdtrAgt/InstrInf}。含义：给收款人代理行的指令；非必传且可重复。 */
        private List<String> instructionsForCreditorAgent = List.of();

        /** XML 路径：{@code InstrForNxtAgt/InstrInf}。含义：给下一代理行的指令；非必传且可重复。 */
        private List<String> instructionsForNextAgent = List.of();

        /** XML 路径：{@code RgltryRptg/Dtls/Inf}。含义：监管报告信息；非必传且可重复。 */
        private List<String> regulatoryReportingInformation = List.of();

        /** XML 路径：{@code TxInfAndSts/StsId}。含义：交易状态记录标识；非必传。 */
        private String statusId;

        /** XML 路径：{@code TxInfAndSts/OrgnlInstrId}。含义：pacs.002 原付款指令标识；非必传，不存在等价替代 XML 字段。 */
        private String originalInstructionId;

        /** XML 路径：{@code TxInfAndSts/OrgnlEndToEndId}。含义：pacs.002 原端到端标识；非必传，不存在等价替代 XML 字段。 */
        private String originalEndToEndId;

        /** XML 路径：{@code TxInfAndSts/OrgnlTxId}。含义：pacs.002 原交易标识；非必传，不能由 {@code OrgnlInstrId} 或 {@code OrgnlEndToEndId} 等价替代。 */
        private String originalTransactionId;

        /** XML 路径：{@code TxInfAndSts/OrgnlUETR}。含义：原通用端到端交易追踪号；非必传。 */
        private String originalUetr;

        /** XML 路径：{@code TxInfAndSts/TxSts}。含义：pacs.002 交易状态；非必传，未提供时不能据此推断交易状态。 */
        private String transactionStatus;

        /** XML 路径：{@code TxInfAndSts/AccptncDtTm}。含义：接受日期时间；非必传。 */
        private String acceptanceDateTime;

        /** XML 路径：{@code TxInfAndSts/AcctSvcrRef}。含义：账户服务机构参考号；非必传。 */
        private String accountServicerReference;

        /** XML 路径：{@code TxInfAndSts/ClrSysRef}。含义：清算系统参考号；非必传。 */
        private String clearingSystemReference;

        /**
         * XML 路径：{@code TxInfAndSts/StsRsnInf/Rsn/Cd}。含义：pacs.002 状态原因；非必传。
         * {@code Cd} 可由 {@code TxInfAndSts/StsRsnInf/Rsn/Prtry} 替代，SDK 会优先返回 {@code Cd}，
         * 没有 {@code Cd} 时返回 {@code Prtry}。
         */
        private String statusReasonCode;

        /** XML 路径：{@code TxInfAndSts/StsRsnInf/Rsn/Cd|Prtry}。含义：全部状态原因；非必传且可重复。 */
        private List<String> statusReasonCodes = List.of();

    public void setRmtInfUstrd(List<String> values) {
        this.rmtInfUstrd = immutable(values);
    }

    public void setPreIntrmyAgtBics(List<String> values) {
        this.preIntrmyAgtBics = immutable(values);
    }

    public void setIntrmyAgtBics(List<String> values) {
        this.intrmyAgtBics = immutable(values);
    }

    public void setCharges(List<ChargeInfo> values) {
        this.charges = immutable(values);
    }

    public void setInstructionsForCreditorAgent(List<String> values) {
        this.instructionsForCreditorAgent = immutable(values);
    }

    public void setInstructionsForNextAgent(List<String> values) {
        this.instructionsForNextAgent = immutable(values);
    }

    public void setRegulatoryReportingInformation(List<String> values) {
        this.regulatoryReportingInformation = immutable(values);
    }

    public void setStatusReasonCodes(List<String> values) {
        this.statusReasonCodes = immutable(values);
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
