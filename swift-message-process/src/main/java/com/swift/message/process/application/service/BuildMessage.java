package com.swift.message.process.application.service;

import com.swift.message.process.domain.exception.MessageBuildException;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.message.process.domain.model.ChargeInfo;
import com.swift.message.process.domain.model.GroupHdr;
import com.swift.message.process.domain.model.PostalAddress;
import com.swift.message.process.domain.model.Transaction;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.swift.message.process.infrastructure.xml.SwiftXmlConstants.*;

/** SWIFT ISO 20022 XML 报文构造入口。 */
public final class BuildMessage {
    private static final Logger LOGGER = Logger.getLogger(BuildMessage.class.getName());

    private BuildMessage() {
    }

    /** 构造 {@code head.001.001.02 AppHdr} XML 片段。 */
    public static String buildAppHdr(AppHdr appHdr) {
        LOGGER.info("开始构造 AppHdr");
        if (appHdr == null) {
            throw buildError("appHdr 不能为空");
        }
        require(appHdr.getSenderBic(), "AppHdr/Fr/.../BICFI");
        require(appHdr.getReceiverBic(), "AppHdr/To/.../BICFI");
        require(appHdr.getUuid(), "AppHdr/BizMsgIdr");
        require(appHdr.getMsgType(), "AppHdr/MsgDefIdr");
        require(appHdr.getCreateTime(), "AppHdr/CreDt");

        String xml = writeXml(writer -> {
            writer.writeStartElement(APP_HDR);
            writer.writeDefaultNamespace(APP_HDR_NAMESPACE);
            element(writer, CHAR_SET, appHdr.getCharSet());
            headerParty(writer, FROM, appHdr.getSenderBic());
            headerParty(writer, TO, appHdr.getReceiverBic());
            element(writer, BUSINESS_MESSAGE_ID, appHdr.getUuid());
            element(writer, MESSAGE_DEFINITION_ID, appHdr.getMsgType());
            element(writer, BUSINESS_SERVICE, appHdr.getBizSvc());
            element(writer, CREATION_DATE, appHdr.getCreateTime());
            element(writer, COPY_DUPLICATE, appHdr.getDupTag());
            if (appHdr.getPsbDupTag() != null) {
                element(writer, POSSIBLE_DUPLICATE, appHdr.getPsbDupTag().toString());
            }
            element(writer, PRIORITY, appHdr.getPriority());
            writer.writeEndElement();
        });
        LOGGER.info(() -> "AppHdr 构造完成, BizMsgIdr=" + appHdr.getUuid());
        return xml;
    }

    /** 构造只包含一笔交易的 {@code pacs.008.001.08 Document} XML 片段。 */
    public static String buildMessageBody(GroupHdr groupHdr, Transaction transaction) {
        if (transaction == null) {
            throw buildError("transaction 不能为空");
        }
        return buildMessageBody(groupHdr, List.of(transaction));
    }

    /** 构造包含一笔或多笔交易的 {@code pacs.008.001.08 Document} XML 片段。 */
    public static String buildMessageBody(GroupHdr groupHdr, List<Transaction> transactions) {
        LOGGER.info(() -> "开始构造 pacs.008 报文体, transactionCount="
                + (transactions == null ? 0 : transactions.size()));
        validateBody(groupHdr, transactions);
        String xml = writeXml(writer -> {
            writer.writeStartElement(DOCUMENT);
            writer.writeDefaultNamespace(PACS_008_NAMESPACE);
            writer.writeStartElement(FI_TO_FI_CUSTOMER_CREDIT_TRANSFER);
            groupHeader(writer, groupHdr);
            for (Transaction transaction : transactions) {
                creditTransferTransaction(writer, transaction);
            }
            writer.writeEndElement();
            writer.writeEndElement();
        });
        LOGGER.info(() -> "pacs.008 报文体构造完成, MsgId=" + groupHdr.getMsgId()
                + ", transactionCount=" + transactions.size());
        return xml;
    }

    private static void groupHeader(XMLStreamWriter writer, GroupHdr group) throws XMLStreamException {
        writer.writeStartElement(GROUP_HDR);
        element(writer, MESSAGE_ID, group.getMsgId());
        element(writer, CREATION_DATE_TIME, group.getCreDtTime());
        if (group.getBatchBooking() != null) {
            element(writer, BATCH_BOOKING, group.getBatchBooking().toString());
        }
        element(writer, NUMBER_OF_TRANSACTIONS, group.getNumberOfTransactions().toString());
        element(writer, CONTROL_SUM, group.getCtrlSum());
        amount(writer, TOTAL_INTERBANK_SETTLEMENT_AMOUNT, group.getTtlIntrBkSttlmValue(), group.getTtlIntrBkSttlmCcy());
        element(writer, INTERBANK_SETTLEMENT_DATE, group.getIntrBkSttlmDt());
        writer.writeStartElement(SETTLEMENT_INFORMATION);
        element(writer, SETTLEMENT_METHOD, group.getSettlementMethod());
        if (hasText(group.getClearingSysCode())) {
            writer.writeStartElement(CLEARING_SYSTEM);
            element(writer, CODE, group.getClearingSysCode());
            writer.writeEndElement();
        }
        writer.writeEndElement();
        agent(writer, INSTRUCTING_AGENT, group.getInstructingAgentBic());
        agent(writer, INSTRUCTED_AGENT, group.getInstructedAgentBic());
        writer.writeEndElement();
    }

    private static void creditTransferTransaction(XMLStreamWriter writer, Transaction transaction) throws XMLStreamException {
        writer.writeStartElement(CREDIT_TRANSFER_TX_INFO);
        writer.writeStartElement(PAYMENT_ID);
        element(writer, INSTRUCTION_ID, transaction.getInstructionId());
        element(writer, END_TO_END_ID, transaction.getEndToEndId());
        element(writer, TRANSACTION_ID, transaction.getTransactionId());
        element(writer, UETR, transaction.getUetr());
        writer.writeEndElement();
        paymentType(writer, transaction);
        amount(writer, INTERBANK_SETTLEMENT_AMOUNT, transaction.getIntrBkSttlmValue(), transaction.getIntrBkSttlmCcy());
        element(writer, INTERBANK_SETTLEMENT_DATE, transaction.getIntrBkSttlmDt());
        element(writer, SETTLEMENT_PRIORITY, transaction.getSettlementPriority());
        amount(writer, INSTRUCTED_AMOUNT, transaction.getInstdAmtValue(), transaction.getInstdAmtCcy());
        element(writer, CHARGE_BEARER, transaction.getChargeBearer());
        for (ChargeInfo charge : transaction.getCharges()) {
            charge(writer, charge);
        }
        agents(writer, PREVIOUS_INSTRUCTING_AGENT_PREFIX, transaction.getPreIntrmyAgtBics());
        agent(writer, INSTRUCTING_AGENT, transaction.getInstructingAgentBic());
        agent(writer, INSTRUCTED_AGENT, transaction.getInstructedAgentBic());
        agents(writer, INTERMEDIARY_AGENT_PREFIX, transaction.getIntrmyAgtBics());
        party(writer, ULTIMATE_DEBTOR, transaction.getUltimateDebtorName(), null);
        party(writer, INITIATING_PARTY, transaction.getInitiatingPartyName(), null);
        party(writer, DEBTOR, transaction.getDebtorName(), transaction.getDebtorAddress());
        account(writer, DEBTOR_ACCOUNT, transaction.getDebtorAccount());
        agent(writer, DEBTOR_AGENT, transaction.getDebtorAgentBic());
        agent(writer, CREDITOR_AGENT, transaction.getCreditorAgentBic());
        party(writer, CREDITOR, transaction.getCreditorName(), transaction.getCreditorAddress());
        account(writer, CREDITOR_ACCOUNT, transaction.getCreditorAccount());
        party(writer, ULTIMATE_CREDITOR, transaction.getUltimateCreditorName(), null);
        instructions(writer, INSTRUCTION_FOR_CREDITOR_AGENT, transaction.getInstructionsForCreditorAgent());
        instructions(writer, INSTRUCTION_FOR_NEXT_AGENT, transaction.getInstructionsForNextAgent());
        choice(writer, PURPOSE, transaction.getPurposeCd());
        regulatoryReporting(writer, transaction.getRegulatoryReportingInformation());
        remittance(writer, transaction.getRmtInfUstrd());
        writer.writeEndElement();
    }

    private static void paymentType(XMLStreamWriter writer, Transaction transaction) throws XMLStreamException {
        if (!anyText(transaction.getInstructionPriority(), transaction.getServiceLevel(),
                transaction.getLocalInstrument(), transaction.getCategoryPurpose())) {
            return;
        }
        writer.writeStartElement(PAYMENT_TYPE_INFORMATION);
        element(writer, INSTRUCTION_PRIORITY, transaction.getInstructionPriority());
        choice(writer, SERVICE_LEVEL, transaction.getServiceLevel());
        choice(writer, LOCAL_INSTRUMENT, transaction.getLocalInstrument());
        choice(writer, CATEGORY_PURPOSE, transaction.getCategoryPurpose());
        writer.writeEndElement();
    }

    private static void postalAddress(XMLStreamWriter writer, PostalAddress address) throws XMLStreamException {
        if (address == null) {
            return;
        }
        if (!hasStructuredAddress(address)) {
            throw buildError("PstlAdr 必须包含结构化地址字段，不能只提供 AdrLine");
        }
        writer.writeStartElement(POSTAL_ADDRESS);
        if (anyText(address.getAddressTypeCode(), address.getAddressTypeProprietary())) {
            writer.writeStartElement(ADDRESS_TYPE);
            if (hasText(address.getAddressTypeCode())) {
                element(writer, CODE, address.getAddressTypeCode());
            } else {
                writer.writeStartElement(PROPRIETARY);
                element(writer, ID, address.getAddressTypeProprietary());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        element(writer, DEPARTMENT, address.getDepartment());
        element(writer, SUB_DEPARTMENT, address.getSubDepartment());
        element(writer, STREET_NAME, address.getStreetName());
        element(writer, BUILDING_NUMBER, address.getBuildingNumber());
        element(writer, BUILDING_NAME, address.getBuildingName());
        element(writer, FLOOR, address.getFloor());
        element(writer, POST_BOX, address.getPostBox());
        element(writer, ROOM, address.getRoom());
        element(writer, POST_CODE, address.getPostCode());
        element(writer, TOWN_NAME, address.getTownName());
        element(writer, TOWN_LOCATION_NAME, address.getTownLocationName());
        element(writer, DISTRICT_NAME, address.getDistrictName());
        element(writer, COUNTRY_SUB_DIVISION, address.getCountrySubDivision());
        element(writer, COUNTRY, address.getCountry());
        writer.writeEndElement();
    }

    private static void party(XMLStreamWriter writer, String name, String partyName, PostalAddress address)
            throws XMLStreamException {
        if (!hasText(partyName) && address == null) {
            return;
        }
        writer.writeStartElement(name);
        element(writer, NAME, partyName);
        postalAddress(writer, address);
        writer.writeEndElement();
    }

    private static void headerParty(XMLStreamWriter writer, String name, String bic) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeStartElement(FI_ID);
        writer.writeStartElement(FINANCIAL_INSTITUTION_ID);
        element(writer, BICFI, bic);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void agent(XMLStreamWriter writer, String name, String bic) throws XMLStreamException {
        if (!hasText(bic)) {
            return;
        }
        writer.writeStartElement(name);
        writer.writeStartElement(FINANCIAL_INSTITUTION_ID);
        element(writer, BICFI, bic);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void agents(XMLStreamWriter writer, String prefix, List<String> bics) throws XMLStreamException {
        for (int index = 0; index < bics.size(); index++) {
            agent(writer, prefix + (index + 1), bics.get(index));
        }
    }

    private static void account(XMLStreamWriter writer, String name, String account) throws XMLStreamException {
        if (!hasText(account)) {
            return;
        }
        writer.writeStartElement(name);
        writer.writeStartElement(ID);
        if (account.matches("[A-Z]{2}[0-9A-Z]{13,32}")) {
            element(writer, IBAN, account);
        } else {
            writer.writeStartElement(OTHER);
            element(writer, ID, account);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void amount(XMLStreamWriter writer, String name, String value, String currency)
            throws XMLStreamException {
        if (!hasText(value)) {
            return;
        }
        require(currency, name + "/@Ccy");
        writer.writeStartElement(name);
        writer.writeAttribute(CURRENCY, currency);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    private static void charge(XMLStreamWriter writer, ChargeInfo charge) throws XMLStreamException {
        writer.writeStartElement(CHARGES_INFORMATION);
        amount(writer, AMOUNT, charge.getAmount(), charge.getCurrency());
        agent(writer, AGENT, charge.getAgentBic());
        writer.writeEndElement();
    }

    private static void instructions(XMLStreamWriter writer, String name, List<String> instructions)
            throws XMLStreamException {
        for (String instruction : instructions) {
            writer.writeStartElement(name);
            element(writer, INSTRUCTION_INFORMATION, instruction);
            writer.writeEndElement();
        }
    }

    private static void regulatoryReporting(XMLStreamWriter writer, List<String> information)
            throws XMLStreamException {
        for (String value : information) {
            writer.writeStartElement(REGULATORY_REPORTING);
            writer.writeStartElement(DETAILS);
            element(writer, INFORMATION, value);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private static void remittance(XMLStreamWriter writer, List<String> information) throws XMLStreamException {
        if (information.isEmpty()) {
            return;
        }
        writer.writeStartElement(REMITTANCE_INFORMATION);
        for (String value : information) {
            element(writer, UNSTRUCTURED, value);
        }
        writer.writeEndElement();
    }

    private static void choice(XMLStreamWriter writer, String name, String code) throws XMLStreamException {
        if (!hasText(code)) {
            return;
        }
        writer.writeStartElement(name);
        element(writer, CODE, code);
        writer.writeEndElement();
    }

    private static void element(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        if (!hasText(value)) {
            return;
        }
        writer.writeStartElement(name);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }

    private static void validateBody(GroupHdr groupHdr, List<Transaction> transactions) {
        if (groupHdr == null) {
            throw buildError("groupHdr 不能为空");
        }
        if (transactions == null || transactions.isEmpty()) {
            throw buildError("transactions 不能为空");
        }
        require(groupHdr.getMsgId(), "GrpHdr/MsgId");
        require(groupHdr.getCreDtTime(), "GrpHdr/CreDtTm");
        require(groupHdr.getSettlementMethod(), "GrpHdr/SttlmInf/SttlmMtd");
        if (groupHdr.getNumberOfTransactions() == null) {
            throw buildError("缺少必传字段 GrpHdr/NbOfTxs");
        }
        if (groupHdr.getNumberOfTransactions() != transactions.size()) {
            throw buildError("GrpHdr/NbOfTxs 与 transactions 数量不一致");
        }
        for (Transaction transaction : transactions) {
            if (transaction == null) {
                throw buildError("transactions 不能包含 null");
            }
            require(transaction.getEndToEndId(), "CdtTrfTxInf/PmtId/EndToEndId");
            require(transaction.getIntrBkSttlmValue(), "CdtTrfTxInf/IntrBkSttlmAmt");
            require(transaction.getIntrBkSttlmCcy(), "CdtTrfTxInf/IntrBkSttlmAmt/@Ccy");
            require(transaction.getIntrBkSttlmDt(), "CdtTrfTxInf/IntrBkSttlmDt");
            require(transaction.getChargeBearer(), "CdtTrfTxInf/ChrgBr");
            require(transaction.getDebtorName(), "CdtTrfTxInf/Dbtr/Nm");
            require(transaction.getDebtorAgentBic(), "CdtTrfTxInf/DbtrAgt/.../BICFI");
            require(transaction.getCreditorAgentBic(), "CdtTrfTxInf/CdtrAgt/.../BICFI");
            require(transaction.getCreditorName(), "CdtTrfTxInf/Cdtr/Nm");
        }
    }

    private static boolean hasStructuredAddress(PostalAddress address) {
        return anyText(address.getAddressTypeCode(), address.getAddressTypeProprietary(), address.getDepartment(),
                address.getSubDepartment(), address.getStreetName(), address.getBuildingNumber(), address.getBuildingName(),
                address.getFloor(), address.getPostBox(), address.getRoom(), address.getPostCode(), address.getTownName(),
                address.getTownLocationName(), address.getDistrictName(), address.getCountrySubDivision(), address.getCountry());
    }

    private static void require(String value, String path) {
        if (!hasText(value)) {
            throw buildError("缺少必传字段 " + path);
        }
    }

    private static MessageBuildException buildError(String message) {
        LOGGER.warning(() -> "报文构造失败: " + message);
        return new MessageBuildException(message);
    }

    private static boolean anyText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String writeXml(XmlWriterAction action) {
        StringWriter output = new StringWriter();
        try {
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            action.write(writer);
            writer.flush();
            writer.close();
            return output.toString();
        } catch (XMLStreamException exception) {
            LOGGER.log(Level.WARNING, "SWIFT XML 报文构造失败", exception);
            throw new MessageBuildException("构造 SWIFT XML 报文失败", exception);
        }
    }

    @FunctionalInterface
    private interface XmlWriterAction {
        void write(XMLStreamWriter writer) throws XMLStreamException;
    }
}
