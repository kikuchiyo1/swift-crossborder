package com.swift.message.process.application.service;

import com.swift.message.process.domain.exception.MessageParseException;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.message.process.domain.model.ChargeInfo;
import com.swift.message.process.domain.model.GroupHdr;
import com.swift.message.process.domain.model.PostalAddress;
import com.swift.message.process.domain.model.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.swift.message.process.infrastructure.xml.SwiftXmlConstants.*;

/** SWIFT ISO 20022 XML 报文解析入口。 */
public final class ParseMessage {
    private static final Logger LOGGER = Logger.getLogger(ParseMessage.class.getName());

    private ParseMessage() {
    }

    public static AppHdr appHdrParse(String message) {
        LOGGER.info("开始解析 AppHdr");
        Element header = requiredElement(parse(message), APP_HDR, "报文头 AppHdr");
        AppHdr result = new AppHdr();
        result.setCharSet(text(header, CHAR_SET));
        result.setSenderBic(partyBic(header, FROM));
        result.setReceiverBic(partyBic(header, TO));
        result.setUuid(text(header, BUSINESS_MESSAGE_ID));
        result.setMsgType(text(header, MESSAGE_DEFINITION_ID));
        result.setBizSvc(text(header, BUSINESS_SERVICE));
        result.setCreateTime(firstNonBlank(text(header, CREATION_DATE), text(header, CREATION_DATE_TIME)));
        result.setDupTag(text(header, COPY_DUPLICATE));
        result.setPsbDupTag(booleanValue(text(header, POSSIBLE_DUPLICATE)));
        result.setPriority(text(header, PRIORITY));
        LOGGER.info(() -> "AppHdr 解析完成, BizMsgIdr=" + result.getUuid());
        return result;
    }

    public static Transaction transactionParse(String message) {
        LOGGER.info("开始解析交易详情");
        Document document = parse(message);
        Element transaction = firstElement(document, CREDIT_TRANSFER_TX_INFO);
        if (transaction == null) {
            transaction = firstElement(document, TX_INFO_AND_STATUS);
        }
        if (transaction == null) {
            LOGGER.warning("交易详情解析失败: 缺少 CdtTrfTxInf 或 TxInfAndSts");
            throw new MessageParseException("报文中缺少交易详情 CdtTrfTxInf 或 TxInfAndSts");
        }

        Element amount = firstNonNull(
                element(transaction, INTERBANK_SETTLEMENT_AMOUNT),
                element(transaction, ORIGINAL_TX_REFERENCE, INTERBANK_SETTLEMENT_AMOUNT));
        Element instructedAmount = firstNonNull(
                element(transaction, INSTRUCTED_AMOUNT),
                element(transaction, ORIGINAL_TX_REFERENCE, AMOUNT, INSTRUCTED_AMOUNT),
                element(transaction, ORIGINAL_TX_REFERENCE, INSTRUCTED_AMOUNT));
        Element transactionDetails = firstNonNull(element(transaction, ORIGINAL_TX_REFERENCE), transaction);
        Element paymentType = element(transactionDetails, PAYMENT_TYPE_INFORMATION);
        List<String> statusReasons = statusReasons(transaction);
        Transaction result = new Transaction();
        result.setInstructionId(text(transaction, PAYMENT_ID, INSTRUCTION_ID));
        result.setEndToEndId(text(transaction, PAYMENT_ID, END_TO_END_ID));
        result.setTransactionId(text(transaction, PAYMENT_ID, TRANSACTION_ID));
        result.setUetr(text(transaction, PAYMENT_ID, UETR));
        result.setInstructionPriority(text(paymentType, INSTRUCTION_PRIORITY));
        result.setServiceLevel(choice(paymentType, SERVICE_LEVEL));
        result.setLocalInstrument(choice(paymentType, LOCAL_INSTRUMENT));
        result.setCategoryPurpose(choice(paymentType, CATEGORY_PURPOSE));
        result.setIntrBkSttlmValue(value(amount));
        result.setIntrBkSttlmCcy(attribute(amount, CURRENCY));
        result.setInstdAmtValue(value(instructedAmount));
        result.setInstdAmtCcy(attribute(instructedAmount, CURRENCY));
        result.setIntrBkSttlmDt(text(transactionDetails, INTERBANK_SETTLEMENT_DATE));
        result.setSettlementPriority(text(transactionDetails, SETTLEMENT_PRIORITY));
        result.setChargeBearer(text(transactionDetails, CHARGE_BEARER));
        result.setCharges(charges(transaction));
        result.setDebtorName(text(transactionDetails, DEBTOR, NAME));
        result.setDebtorAddress(postalAddress(transactionDetails, DEBTOR));
        result.setDebtorAccount(account(transactionDetails, DEBTOR_ACCOUNT));
        result.setDebtorAgentBic(bic(transactionDetails, DEBTOR_AGENT));
        result.setCreditorName(text(transactionDetails, CREDITOR, NAME));
        result.setCreditorAddress(postalAddress(transactionDetails, CREDITOR));
        result.setCreditorAccount(account(transactionDetails, CREDITOR_ACCOUNT));
        result.setCreditorAgentBic(bic(transactionDetails, CREDITOR_AGENT));
        result.setInitiatingPartyName(text(transactionDetails, INITIATING_PARTY, NAME));
        result.setUltimateDebtorName(text(transactionDetails, ULTIMATE_DEBTOR, NAME));
        result.setUltimateCreditorName(text(transactionDetails, ULTIMATE_CREDITOR, NAME));
        result.setPurposeCd(firstNonBlank(text(transactionDetails, PURPOSE, CODE), text(transactionDetails, PURPOSE, PROPRIETARY)));
        result.setRmtInfUstrd(remittanceInformation(transactionDetails));
        result.setPreIntrmyAgtBics(bics(transaction, PREVIOUS_INSTRUCTING_AGENT_1, PREVIOUS_INSTRUCTING_AGENT_2, PREVIOUS_INSTRUCTING_AGENT_3));
        result.setInstructingAgentBic(bic(transaction, INSTRUCTING_AGENT));
        result.setInstructedAgentBic(bic(transaction, INSTRUCTED_AGENT));
        result.setIntrmyAgtBics(bics(transaction, INTERMEDIARY_AGENT_1, INTERMEDIARY_AGENT_2, INTERMEDIARY_AGENT_3));
        result.setInstructionsForCreditorAgent(instructionInformation(transactionDetails, INSTRUCTION_FOR_CREDITOR_AGENT));
        result.setInstructionsForNextAgent(instructionInformation(transactionDetails, INSTRUCTION_FOR_NEXT_AGENT));
        result.setRegulatoryReportingInformation(regulatoryReportingInformation(transactionDetails));
        result.setStatusId(text(transaction, STATUS_ID));
        result.setOriginalInstructionId(text(transaction, ORIGINAL_INSTRUCTION_ID));
        result.setOriginalEndToEndId(text(transaction, ORIGINAL_END_TO_END_ID));
        result.setOriginalTransactionId(text(transaction, ORIGINAL_TRANSACTION_ID));
        result.setOriginalUetr(text(transaction, ORIGINAL_UETR));
        result.setTransactionStatus(text(transaction, TRANSACTION_STATUS));
        result.setAcceptanceDateTime(text(transaction, ACCEPTANCE_DATE_TIME));
        result.setAccountServicerReference(text(transaction, ACCOUNT_SERVICER_REFERENCE));
        result.setClearingSystemReference(text(transaction, CLEARING_SYSTEM_REFERENCE));
        result.setStatusReasonCode(statusReasons.isEmpty() ? null : statusReasons.getFirst());
        result.setStatusReasonCodes(statusReasons);
        LOGGER.info(() -> "交易详情解析完成, TxId=" + firstNonBlank(
                result.getTransactionId(), result.getOriginalTransactionId()));
        return result;
    }

    public static GroupHdr groupHdrParse(String message) {
        LOGGER.info("开始解析 GrpHdr");
        Element group = requiredElement(parse(message), GROUP_HDR, "组头 GrpHdr");
        Element totalAmount = element(group, TOTAL_INTERBANK_SETTLEMENT_AMOUNT);
        GroupHdr result = new GroupHdr();
        result.setMsgId(text(group, MESSAGE_ID));
        result.setCreDtTime(text(group, CREATION_DATE_TIME));
        result.setBatchBooking(booleanValue(text(group, BATCH_BOOKING)));
        result.setNumberOfTransactions(integerValue(text(group, NUMBER_OF_TRANSACTIONS), NUMBER_OF_TRANSACTIONS));
        result.setCtrlSum(text(group, CONTROL_SUM));
        result.setTtlIntrBkSttlmValue(value(totalAmount));
        result.setTtlIntrBkSttlmCcy(attribute(totalAmount, CURRENCY));
        result.setIntrBkSttlmDt(text(group, INTERBANK_SETTLEMENT_DATE));
        result.setSettlementMethod(text(group, SETTLEMENT_INFORMATION, SETTLEMENT_METHOD));
        result.setClearingSysCode(firstNonBlank(
                text(group, SETTLEMENT_INFORMATION, CLEARING_SYSTEM, CODE),
                text(group, SETTLEMENT_INFORMATION, CLEARING_SYSTEM, PROPRIETARY)));
        result.setInstructingAgentBic(bic(group, INSTRUCTING_AGENT));
        result.setInstructedAgentBic(bic(group, INSTRUCTED_AGENT));
        LOGGER.info(() -> "GrpHdr 解析完成, MsgId=" + result.getMsgId());
        return result;
    }

    private static Document parse(String message) {
        if (message == null || message.isBlank()) {
            LOGGER.warning("报文解析失败: message 为空");
            throw new MessageParseException("message 不能为空");
        }
        String xml = message.strip().replaceFirst("^<\\?xml[^?]*\\?>", "");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader("<SwiftMessage>" + xml + "</SwiftMessage>")));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "SWIFT XML 报文解析失败", exception);
            throw new MessageParseException("message 不是合法的 SWIFT XML 报文", exception);
        }
    }

    private static Element requiredElement(Document document, String name, String description) {
        Element element = firstElement(document, name);
        if (element == null) {
            LOGGER.warning(() -> "报文层解析失败: 缺少 " + description);
            throw new MessageParseException("报文中缺少" + description);
        }
        return element;
    }

    private static Element firstElement(Document document, String name) {
        NodeList nodes = document.getElementsByTagNameNS("*", name);
        if (nodes.getLength() == 0) {
            nodes = document.getElementsByTagName(name);
        }
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private static String text(Element start, String... path) {
        return value(element(start, path));
    }

    private static Element element(Element start, String... path) {
        if (start == null) {
            return null;
        }
        Element current = start;
        for (String name : path) {
            current = child(current, name);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    private static String localName(Element element) {
        return element.getLocalName() == null ? element.getTagName() : element.getLocalName();
    }

    private static String value(Element element) {
        if (element == null) {
            return null;
        }
        String value = element.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static String attribute(Element element, String name) {
        if (element == null || !element.hasAttribute(name)) {
            return null;
        }
        String value = element.getAttribute(name).trim();
        return value.isEmpty() ? null : value;
    }

    private static String account(Element transaction, String partyAccount) {
        return firstNonBlank(
                text(transaction, partyAccount, ID, IBAN),
                text(transaction, partyAccount, ID, OTHER, ID));
    }

    private static PostalAddress postalAddress(Element transaction, String party) {
        Element partyElement = child(transaction, party);
        Element address = partyElement == null ? null : child(partyElement, POSTAL_ADDRESS);
        if (address == null) {
            return null;
        }
        PostalAddress result = new PostalAddress();
        result.setAddressTypeCode(text(address, ADDRESS_TYPE, CODE));
        result.setAddressTypeProprietary(text(address, ADDRESS_TYPE, PROPRIETARY, ID));
        result.setDepartment(text(address, DEPARTMENT));
        result.setSubDepartment(text(address, SUB_DEPARTMENT));
        result.setStreetName(text(address, STREET_NAME));
        result.setBuildingNumber(text(address, BUILDING_NUMBER));
        result.setBuildingName(text(address, BUILDING_NAME));
        result.setFloor(text(address, FLOOR));
        result.setPostBox(text(address, POST_BOX));
        result.setRoom(text(address, ROOM));
        result.setPostCode(text(address, POST_CODE));
        result.setTownName(text(address, TOWN_NAME));
        result.setTownLocationName(text(address, TOWN_LOCATION_NAME));
        result.setDistrictName(text(address, DISTRICT_NAME));
        result.setCountrySubDivision(text(address, COUNTRY_SUB_DIVISION));
        result.setCountry(text(address, COUNTRY));
        result.setAddressLines(childTexts(address, ADDRESS_LINE));
        return result;
    }

    private static List<String> remittanceInformation(Element transaction) {
        Element remittance = child(transaction, REMITTANCE_INFORMATION);
        if (remittance == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>(childTexts(remittance, UNSTRUCTURED));
        for (Element structured : children(remittance, STRUCTURED)) {
            String reference = text(structured, CREDITOR_REFERENCE_INFORMATION, REFERENCE);
            if (reference != null) {
                values.add(reference);
            }
        }
        return List.copyOf(values);
    }

    private static List<String> bics(Element transaction, String... agents) {
        List<String> values = new ArrayList<>();
        for (String agent : agents) {
            String value = bic(transaction, agent);
            if (value != null) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String choice(Element parent, String elementName) {
        if (parent == null) {
            return null;
        }
        return firstNonBlank(
                text(parent, elementName, CODE),
                text(parent, elementName, PROPRIETARY));
    }

    private static List<ChargeInfo> charges(Element transaction) {
        List<ChargeInfo> values = new ArrayList<>();
        for (Element charge : children(transaction, CHARGES_INFORMATION)) {
            Element amount = element(charge, AMOUNT);
            ChargeInfo result = new ChargeInfo();
            result.setAmount(value(amount));
            result.setCurrency(attribute(amount, CURRENCY));
            result.setAgentBic(bic(charge, AGENT));
            values.add(result);
        }
        return List.copyOf(values);
    }

    private static List<String> instructionInformation(Element transaction, String elementName) {
        List<String> values = new ArrayList<>();
        for (Element instruction : children(transaction, elementName)) {
            String information = text(instruction, INSTRUCTION_INFORMATION);
            if (information != null) {
                values.add(information);
            }
        }
        return List.copyOf(values);
    }

    private static List<String> regulatoryReportingInformation(Element transaction) {
        List<String> values = new ArrayList<>();
        for (Element reporting : children(transaction, REGULATORY_REPORTING)) {
            for (Element details : children(reporting, DETAILS)) {
                values.addAll(childTexts(details, INFORMATION));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> statusReasons(Element transaction) {
        List<String> values = new ArrayList<>();
        for (Element information : children(transaction, STATUS_REASON_INFORMATION)) {
            String reason = firstNonBlank(text(information, REASON, CODE), text(information, REASON, PROPRIETARY));
            if (reason != null) {
                values.add(reason);
            }
        }
        return List.copyOf(values);
    }

    private static List<String> childTexts(Element parent, String name) {
        List<String> values = new ArrayList<>();
        for (Element element : children(parent, name)) {
            String value = value(element);
            if (value != null) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> values = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(localName(element))) {
                values.add(element);
            }
        }
        return values;
    }

    private static String bic(Element parent, String agent) {
        return firstNonBlank(
                text(parent, agent, FINANCIAL_INSTITUTION_ID, BICFI),
                text(parent, agent, FI_ID, FINANCIAL_INSTITUTION_ID, BICFI));
    }

    private static String partyBic(Element header, String party) {
        return firstNonBlank(
                text(header, party, FI_ID, FINANCIAL_INSTITUTION_ID, BICFI),
                text(header, party, ORGANISATION_ID, ID, ORGANISATION_ID, ANY_BIC));
    }

    private static Boolean booleanValue(String value) {
        return value == null ? null : Boolean.valueOf(value);
    }

    private static Integer integerValue(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            LOGGER.log(Level.WARNING, field + " 整数转换失败", exception);
            throw new MessageParseException(field + " 不是合法整数: " + value, exception);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Element firstNonNull(Element... elements) {
        for (Element element : elements) {
            if (element != null) {
                return element;
            }
        }
        return null;
    }
}
