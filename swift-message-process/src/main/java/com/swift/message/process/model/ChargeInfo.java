package com.swift.message.process.model;

import lombok.Getter;
import lombok.Setter;

/** ISO 20022 {@code ChrgsInf} 费用明细。 */
@Getter
@Setter
public class ChargeInfo {
        /** XML 路径：{@code ChrgsInf/Amt}。含义：费用金额；必传。 */
        private String amount;

        /** XML 属性：{@code ChrgsInf/Amt/@Ccy}。含义：费用币种；{@code Amt} 存在时必传。 */
        private String currency;

        /** XML 路径：{@code ChrgsInf/Agt/FinInstnId/BICFI}。含义：收取费用的代理行 BIC；非必传。 */
        private String agentBic;
}
