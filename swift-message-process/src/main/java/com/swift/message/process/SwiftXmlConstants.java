package com.swift.message.process;

/** SWIFT ISO 20022 报文使用的 XML 元素名、属性名和命名空间。 */
public final class SwiftXmlConstants {
    public static final String APP_HDR_NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:head.001.001.02";
    public static final String PACS_008_NAMESPACE = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";

    public static final String APP_HDR = "AppHdr";
    public static final String DOCUMENT = "Document";
    public static final String FI_TO_FI_CUSTOMER_CREDIT_TRANSFER = "FIToFICstmrCdtTrf";
    public static final String GROUP_HDR = "GrpHdr";
    public static final String CREDIT_TRANSFER_TX_INFO = "CdtTrfTxInf";
    public static final String TX_INFO_AND_STATUS = "TxInfAndSts";

    public static final String CHAR_SET = "CharSet";
    public static final String FROM = "Fr";
    public static final String TO = "To";
    public static final String FI_ID = "FIId";
    public static final String FINANCIAL_INSTITUTION_ID = "FinInstnId";
    public static final String BICFI = "BICFI";
    public static final String ORGANISATION_ID = "OrgId";
    public static final String ANY_BIC = "AnyBIC";
    public static final String BUSINESS_MESSAGE_ID = "BizMsgIdr";
    public static final String MESSAGE_DEFINITION_ID = "MsgDefIdr";
    public static final String BUSINESS_SERVICE = "BizSvc";
    public static final String CREATION_DATE = "CreDt";
    public static final String CREATION_DATE_TIME = "CreDtTm";
    public static final String COPY_DUPLICATE = "CpyDplct";
    public static final String POSSIBLE_DUPLICATE = "PssblDplct";
    public static final String PRIORITY = "Prty";

    public static final String MESSAGE_ID = "MsgId";
    public static final String BATCH_BOOKING = "BtchBookg";
    public static final String NUMBER_OF_TRANSACTIONS = "NbOfTxs";
    public static final String CONTROL_SUM = "CtrlSum";
    public static final String TOTAL_INTERBANK_SETTLEMENT_AMOUNT = "TtlIntrBkSttlmAmt";
    public static final String INTERBANK_SETTLEMENT_DATE = "IntrBkSttlmDt";
    public static final String SETTLEMENT_INFORMATION = "SttlmInf";
    public static final String SETTLEMENT_METHOD = "SttlmMtd";
    public static final String CLEARING_SYSTEM = "ClrSys";
    public static final String CODE = "Cd";
    public static final String PROPRIETARY = "Prtry";
    public static final String INSTRUCTING_AGENT = "InstgAgt";
    public static final String INSTRUCTED_AGENT = "InstdAgt";

    public static final String PAYMENT_ID = "PmtId";
    public static final String INSTRUCTION_ID = "InstrId";
    public static final String END_TO_END_ID = "EndToEndId";
    public static final String TRANSACTION_ID = "TxId";
    public static final String UETR = "UETR";
    public static final String PAYMENT_TYPE_INFORMATION = "PmtTpInf";
    public static final String INSTRUCTION_PRIORITY = "InstrPrty";
    public static final String SERVICE_LEVEL = "SvcLvl";
    public static final String LOCAL_INSTRUMENT = "LclInstrm";
    public static final String CATEGORY_PURPOSE = "CtgyPurp";
    public static final String INTERBANK_SETTLEMENT_AMOUNT = "IntrBkSttlmAmt";
    public static final String INSTRUCTED_AMOUNT = "InstdAmt";
    public static final String AMOUNT = "Amt";
    public static final String CURRENCY = "Ccy";
    public static final String SETTLEMENT_PRIORITY = "SttlmPrty";
    public static final String CHARGE_BEARER = "ChrgBr";
    public static final String CHARGES_INFORMATION = "ChrgsInf";
    public static final String AGENT = "Agt";

    public static final String DEBTOR = "Dbtr";
    public static final String DEBTOR_ACCOUNT = "DbtrAcct";
    public static final String DEBTOR_AGENT = "DbtrAgt";
    public static final String CREDITOR = "Cdtr";
    public static final String CREDITOR_ACCOUNT = "CdtrAcct";
    public static final String CREDITOR_AGENT = "CdtrAgt";
    public static final String INITIATING_PARTY = "InitgPty";
    public static final String ULTIMATE_DEBTOR = "UltmtDbtr";
    public static final String ULTIMATE_CREDITOR = "UltmtCdtr";
    public static final String NAME = "Nm";
    public static final String ID = "Id";
    public static final String IBAN = "IBAN";
    public static final String OTHER = "Othr";

    public static final String POSTAL_ADDRESS = "PstlAdr";
    public static final String ADDRESS_TYPE = "AdrTp";
    public static final String DEPARTMENT = "Dept";
    public static final String SUB_DEPARTMENT = "SubDept";
    public static final String STREET_NAME = "StrtNm";
    public static final String BUILDING_NUMBER = "BldgNb";
    public static final String BUILDING_NAME = "BldgNm";
    public static final String FLOOR = "Flr";
    public static final String POST_BOX = "PstBx";
    public static final String ROOM = "Room";
    public static final String POST_CODE = "PstCd";
    public static final String TOWN_NAME = "TwnNm";
    public static final String TOWN_LOCATION_NAME = "TwnLctnNm";
    public static final String DISTRICT_NAME = "DstrctNm";
    public static final String COUNTRY_SUB_DIVISION = "CtrySubDvsn";
    public static final String COUNTRY = "Ctry";
    public static final String ADDRESS_LINE = "AdrLine";

    public static final String PREVIOUS_INSTRUCTING_AGENT_PREFIX = "PrvsInstgAgt";
    public static final String INTERMEDIARY_AGENT_PREFIX = "IntrmyAgt";
    public static final String PREVIOUS_INSTRUCTING_AGENT_1 = "PrvsInstgAgt1";
    public static final String PREVIOUS_INSTRUCTING_AGENT_2 = "PrvsInstgAgt2";
    public static final String PREVIOUS_INSTRUCTING_AGENT_3 = "PrvsInstgAgt3";
    public static final String INTERMEDIARY_AGENT_1 = "IntrmyAgt1";
    public static final String INTERMEDIARY_AGENT_2 = "IntrmyAgt2";
    public static final String INTERMEDIARY_AGENT_3 = "IntrmyAgt3";
    public static final String INSTRUCTION_FOR_CREDITOR_AGENT = "InstrForCdtrAgt";
    public static final String INSTRUCTION_FOR_NEXT_AGENT = "InstrForNxtAgt";
    public static final String INSTRUCTION_INFORMATION = "InstrInf";
    public static final String PURPOSE = "Purp";
    public static final String REGULATORY_REPORTING = "RgltryRptg";
    public static final String DETAILS = "Dtls";
    public static final String INFORMATION = "Inf";
    public static final String REMITTANCE_INFORMATION = "RmtInf";
    public static final String UNSTRUCTURED = "Ustrd";
    public static final String STRUCTURED = "Strd";
    public static final String CREDITOR_REFERENCE_INFORMATION = "CdtrRefInf";
    public static final String REFERENCE = "Ref";

    public static final String ORIGINAL_TX_REFERENCE = "OrgnlTxRef";
    public static final String STATUS_ID = "StsId";
    public static final String ORIGINAL_INSTRUCTION_ID = "OrgnlInstrId";
    public static final String ORIGINAL_END_TO_END_ID = "OrgnlEndToEndId";
    public static final String ORIGINAL_TRANSACTION_ID = "OrgnlTxId";
    public static final String ORIGINAL_UETR = "OrgnlUETR";
    public static final String TRANSACTION_STATUS = "TxSts";
    public static final String ACCEPTANCE_DATE_TIME = "AccptncDtTm";
    public static final String ACCOUNT_SERVICER_REFERENCE = "AcctSvcrRef";
    public static final String CLEARING_SYSTEM_REFERENCE = "ClrSysRef";
    public static final String STATUS_REASON_INFORMATION = "StsRsnInf";
    public static final String REASON = "Rsn";

    private SwiftXmlConstants() {
    }
}
