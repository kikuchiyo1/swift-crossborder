package com.swift.message.process;

import com.swift.message.process.model.AppHdr;
import com.swift.message.process.model.GroupHdr;
import com.swift.message.process.model.PostalAddress;
import com.swift.message.process.model.Transaction;
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

/** SWIFT ISO 20022 XML 报文解析入口。 */
public final class ParseMessage {
    private ParseMessage() {
    }

    public static AppHdr appHdrParse(String message) {
        Element header = requiredElement(parse(message), "AppHdr", "报文头 AppHdr");
        return new AppHdr(
                text(header, "CharSet"),
                partyBic(header, "Fr"),
                partyBic(header, "To"),
                text(header, "BizMsgIdr"),
                text(header, "MsgDefIdr"),
                text(header, "BizSvc"),
                firstNonBlank(text(header, "CreDt"), text(header, "CreDtTm")),
                text(header, "CpyDplct"),
                booleanValue(text(header, "PssblDplct")),
                text(header, "Prty"));
    }

    public static Transaction transactionParse(String message) {
        Document document = parse(message);
        Element transaction = firstElement(document, "CdtTrfTxInf");
        if (transaction == null) {
            transaction = firstElement(document, "TxInfAndSts");
        }
        if (transaction == null) {
            throw new MessageParseException("报文中缺少交易详情 CdtTrfTxInf 或 TxInfAndSts");
        }

        Element amount = descendant(transaction, "IntrBkSttlmAmt");
        Element instructedAmount = descendant(transaction, "InstdAmt");
        return new Transaction(
                text(transaction, "PmtId", "InstrId"),
                text(transaction, "PmtId", "EndToEndId"),
                text(transaction, "PmtId", "TxId"),
                text(transaction, "PmtId", "UETR"),
                value(amount),
                attribute(amount, "Ccy"),
                value(instructedAmount),
                attribute(instructedAmount, "Ccy"),
                text(transaction, "IntrBkSttlmDt"),
                text(transaction, "ChrgBr"),
                text(transaction, "Dbtr", "Nm"),
                postalAddress(transaction, "Dbtr"),
                account(transaction, "DbtrAcct"),
                bic(transaction, "DbtrAgt"),
                text(transaction, "Cdtr", "Nm"),
                postalAddress(transaction, "Cdtr"),
                account(transaction, "CdtrAcct"),
                bic(transaction, "CdtrAgt"),
                text(transaction, "InitgPty", "Nm"),
                text(transaction, "UltmtDbtr", "Nm"),
                text(transaction, "UltmtCdtr", "Nm"),
                firstNonBlank(text(transaction, "Purp", "Cd"), text(transaction, "Purp", "Prtry")),
                remittanceInformation(transaction),
                bics(transaction, "PrvsInstgAgt1", "PrvsInstgAgt2", "PrvsInstgAgt3"),
                bic(transaction, "InstgAgt"),
                bic(transaction, "InstdAgt"),
                bics(transaction, "IntrmyAgt1", "IntrmyAgt2", "IntrmyAgt3"),
                text(transaction, "OrgnlInstrId"),
                text(transaction, "OrgnlEndToEndId"),
                text(transaction, "OrgnlTxId"),
                text(transaction, "TxSts"),
                firstNonBlank(
                        text(transaction, "StsRsnInf", "Rsn", "Cd"),
                        text(transaction, "StsRsnInf", "Rsn", "Prtry")));
    }

    public static GroupHdr groupHdrParse(String message) {
        Element group = requiredElement(parse(message), "GrpHdr", "组头 GrpHdr");
        return new GroupHdr(
                text(group, "MsgId"),
                text(group, "CreDtTm"),
                integerValue(text(group, "NbOfTxs"), "NbOfTxs"),
                text(group, "CtrlSum"),
                text(group, "IntrBkSttlmDt"),
                text(group, "SttlmInf", "SttlmMtd"),
                firstNonBlank(
                        text(group, "SttlmInf", "ClrSys", "Cd"),
                        text(group, "SttlmInf", "ClrSys", "Prtry")),
                bic(group, "InstgAgt"),
                bic(group, "InstdAgt"));
    }

    private static Document parse(String message) {
        if (message == null || message.isBlank()) {
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
            throw new MessageParseException("message 不是合法的 SWIFT XML 报文", exception);
        }
    }

    private static Element requiredElement(Document document, String name, String description) {
        Element element = firstElement(document, name);
        if (element == null) {
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
        Element current = start;
        for (String name : path) {
            current = child(current, name);
            if (current == null) {
                return null;
            }
        }
        return value(current);
    }

    private static Element child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    private static Element descendant(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(name);
        }
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
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
                text(transaction, partyAccount, "Id", "IBAN"),
                text(transaction, partyAccount, "Id", "Othr", "Id"));
    }

    private static PostalAddress postalAddress(Element transaction, String party) {
        Element partyElement = child(transaction, party);
        Element address = partyElement == null ? null : child(partyElement, "PstlAdr");
        if (address == null) {
            return null;
        }
        return new PostalAddress(
                text(address, "AdrTp", "Cd"),
                text(address, "AdrTp", "Prtry", "Id"),
                text(address, "Dept"),
                text(address, "SubDept"),
                text(address, "StrtNm"),
                text(address, "BldgNb"),
                text(address, "BldgNm"),
                text(address, "Flr"),
                text(address, "PstBx"),
                text(address, "Room"),
                text(address, "PstCd"),
                text(address, "TwnNm"),
                text(address, "TwnLctnNm"),
                text(address, "DstrctNm"),
                text(address, "CtrySubDvsn"),
                text(address, "Ctry"),
                childTexts(address, "AdrLine"));
    }

    private static List<String> remittanceInformation(Element transaction) {
        Element remittance = child(transaction, "RmtInf");
        if (remittance == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>(childTexts(remittance, "Ustrd"));
        for (Element structured : children(remittance, "Strd")) {
            String reference = text(structured, "CdtrRefInf", "Ref");
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
                text(parent, agent, "FinInstnId", "BICFI"),
                text(parent, agent, "FIId", "FinInstnId", "BICFI"));
    }

    private static String partyBic(Element header, String party) {
        return firstNonBlank(
                text(header, party, "FIId", "FinInstnId", "BICFI"),
                text(header, party, "OrgId", "Id", "OrgId", "AnyBIC"));
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
}
