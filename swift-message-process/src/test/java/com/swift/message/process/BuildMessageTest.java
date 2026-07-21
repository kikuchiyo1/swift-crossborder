package com.swift.message.process;

import com.swift.message.process.model.AppHdr;
import com.swift.message.process.model.GroupHdr;
import com.swift.message.process.model.PostalAddress;
import com.swift.message.process.model.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildMessageTest {
    @Test
    void buildsApplicationHeaderThatCanBeParsed() {
        AppHdr header = new AppHdr();
        header.setCharSet("UTF-8");
        header.setSenderBic("SENDERBIC");
        header.setReceiverBic("RECEIVERBIC");
        header.setUuid("MSG-001");
        header.setMsgType("pacs.008.001.08");
        header.setBizSvc("swift.cbprplus.02");
        header.setCreateTime("2026-07-21T08:00:00Z");
        header.setPriority("HIGH");

        String xml = BuildMessage.buildAppHdr(header);
        AppHdr parsed = ParseMessage.appHdrParse(xml);

        assertEquals("SENDERBIC", parsed.getSenderBic());
        assertEquals("pacs.008.001.08", parsed.getMsgType());
    }

    @Test
    void buildsBodyWithStructuredAddressesAndEscapedText() {
        GroupHdr group = groupHeader();
        Transaction transaction = transaction();

        String xml = BuildMessage.buildMessageBody(group, transaction);
        Transaction parsed = ParseMessage.transactionParse(xml);

        assertTrue(xml.contains("<StrtNm>Main &amp; First</StrtNm>"));
        assertFalse(xml.contains("<AdrLine>"));
        assertEquals("Main & First", parsed.getDebtorAddress().getStreetName());
        assertEquals("Berlin", parsed.getDebtorAddress().getTownName());
        assertEquals("125.50", parsed.getIntrBkSttlmValue());
        assertEquals("USD", parsed.getIntrBkSttlmCcy());
    }

    @Test
    void rejectsAddressThatOnlyContainsAddressLines() {
        GroupHdr group = groupHeader();
        Transaction transaction = transaction();
        PostalAddress address = new PostalAddress();
        address.setAddressLines(List.of("Unstructured only"));
        transaction.setDebtorAddress(address);

        assertThrows(MessageBuildException.class, () -> BuildMessage.buildMessageBody(group, transaction));
    }

    private static GroupHdr groupHeader() {
        GroupHdr group = new GroupHdr();
        group.setMsgId("MSG-001");
        group.setCreDtTime("2026-07-21T08:00:00Z");
        group.setNumberOfTransactions(1);
        group.setIntrBkSttlmDt("2026-07-22");
        group.setSettlementMethod("INDA");
        group.setInstructingAgentBic("GROUPINSTGBIC");
        group.setInstructedAgentBic("GROUPINSTDBIC");
        return group;
    }

    private static Transaction transaction() {
        PostalAddress debtorAddress = new PostalAddress();
        debtorAddress.setStreetName("Main & First");
        debtorAddress.setBuildingNumber("10");
        debtorAddress.setTownName("Berlin");
        debtorAddress.setCountry("DE");
        debtorAddress.setAddressLines(List.of("Must not be emitted"));

        PostalAddress creditorAddress = new PostalAddress();
        creditorAddress.setTownName("Paris");
        creditorAddress.setCountry("FR");

        Transaction transaction = new Transaction();
        transaction.setInstructionId("I-1");
        transaction.setEndToEndId("E-1");
        transaction.setTransactionId("T-1");
        transaction.setUetr("U-1");
        transaction.setIntrBkSttlmValue("125.50");
        transaction.setIntrBkSttlmCcy("USD");
        transaction.setIntrBkSttlmDt("2026-07-22");
        transaction.setChargeBearer("SHAR");
        transaction.setDebtorName("Debtor Ltd");
        transaction.setDebtorAddress(debtorAddress);
        transaction.setDebtorAgentBic("DEBTORBIC");
        transaction.setCreditorName("Creditor Ltd");
        transaction.setCreditorAddress(creditorAddress);
        transaction.setCreditorAgentBic("CREDITORBIC");
        return transaction;
    }
}
