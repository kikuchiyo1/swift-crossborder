package com.swift.message.process.application.service;

import com.swift.message.process.domain.exception.MessageBuildException;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.message.process.domain.model.ChargeInfo;
import com.swift.message.process.domain.model.GroupHdr;
import com.swift.message.process.domain.model.PostalAddress;
import com.swift.message.process.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildMessageTest {
    @Test
    void buildsApplicationHeaderThatCanBeParsed() {
        AppHdr header = applicationHeader();

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

    @ParameterizedTest(name = "rejects missing AppHdr field: {0}")
    @MethodSource("missingApplicationHeaderFields")
    void rejectsMissingApplicationHeaderFields(String field, Consumer<AppHdr> removeField) {
        AppHdr header = applicationHeader();
        removeField.accept(header);

        assertThrows(MessageBuildException.class, () -> BuildMessage.buildAppHdr(header));
    }

    @ParameterizedTest(name = "rejects missing GroupHdr field: {0}")
    @MethodSource("missingGroupHeaderFields")
    void rejectsMissingGroupHeaderFields(String field, Consumer<GroupHdr> removeField) {
        GroupHdr group = groupHeader();
        removeField.accept(group);

        assertThrows(MessageBuildException.class, () -> BuildMessage.buildMessageBody(group, transaction()));
    }

    @ParameterizedTest(name = "rejects missing transaction field: {0}")
    @MethodSource("missingTransactionFields")
    void rejectsMissingTransactionFields(String field, Consumer<Transaction> removeField) {
        Transaction transaction = transaction();
        removeField.accept(transaction);

        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(groupHeader(), transaction));
    }

    @Test
    void rejectsTransactionCountMismatchAndNullEntries() {
        GroupHdr group = groupHeader();
        group.setNumberOfTransactions(2);
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(group, List.of(transaction())));

        group.setNumberOfTransactions(1);
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(group, java.util.Collections.singletonList(null)));
    }

    @Test
    void rejectsMissingConditionalCurrencies() {
        GroupHdr group = groupHeader();
        group.setTtlIntrBkSttlmCcy(null);
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(group, transaction()));

        group.setTtlIntrBkSttlmCcy("USD");
        Transaction transaction = transaction();
        transaction.setInstdAmtValue("100.00");
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(group, transaction));
    }

    @Test
    void normalizesNullCollectionsAndRejectsInvalidCharges() {
        Transaction nullCollectionTransaction = transaction();
        nullCollectionTransaction.setRmtInfUstrd(null);
        String xml = BuildMessage.buildMessageBody(groupHeader(), nullCollectionTransaction);
        assertFalse(xml.contains("<RmtInf>"));

        Transaction missingAmountTransaction = transaction();
        missingAmountTransaction.setCharges(List.of(new ChargeInfo()));
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(groupHeader(), missingAmountTransaction));

        ChargeInfo chargeWithoutCurrency = new ChargeInfo();
        chargeWithoutCurrency.setAmount("2.50");
        Transaction missingChargeCurrencyTransaction = transaction();
        missingChargeCurrencyTransaction.setCharges(List.of(chargeWithoutCurrency));
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(groupHeader(), missingChargeCurrencyTransaction));
    }

    @Test
    void buildsMultipleTransactions() {
        GroupHdr group = groupHeader();
        group.setNumberOfTransactions(2);
        group.setCtrlSum("251.00");
        group.setTtlIntrBkSttlmValue("251.00");
        Transaction first = transaction();
        Transaction second = transaction();
        second.setEndToEndId("E-2");

        String xml = BuildMessage.buildMessageBody(group, List.of(first, second));

        assertEquals(2, xml.split("<CdtTrfTxInf>", -1).length - 1);
        assertTrue(xml.contains("<EndToEndId>E-2</EndToEndId>"));
    }

    @Test
    void rejectsControlSumThatDoesNotMatchTransactions() {
        GroupHdr group = groupHeader();
        group.setCtrlSum("999.99");

        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(group, transaction()));
    }

    @Test
    void rejectsTotalAmountOrCurrencyThatDoesNotMatchTransactions() {
        GroupHdr amountMismatch = groupHeader();
        amountMismatch.setTtlIntrBkSttlmValue("999.99");
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(amountMismatch, transaction()));

        GroupHdr currencyMismatch = groupHeader();
        currencyMismatch.setTtlIntrBkSttlmCcy("EUR");
        assertThrows(MessageBuildException.class,
                () -> BuildMessage.buildMessageBody(currencyMismatch, transaction()));
    }

    private static AppHdr applicationHeader() {
        AppHdr header = new AppHdr();
        header.setCharSet("UTF-8");
        header.setSenderBic("SENDERBIC");
        header.setReceiverBic("RECEIVERBIC");
        header.setUuid("MSG-001");
        header.setMsgType("pacs.008.001.08");
        header.setBizSvc("swift.cbprplus.02");
        header.setCreateTime("2026-07-21T08:00:00Z");
        header.setPriority("HIGH");
        return header;
    }

    private static Stream<Arguments> missingApplicationHeaderFields() {
        return Stream.of(
                Arguments.of("senderBic", (Consumer<AppHdr>) value -> value.setSenderBic(null)),
                Arguments.of("receiverBic", (Consumer<AppHdr>) value -> value.setReceiverBic(" ")),
                Arguments.of("uuid", (Consumer<AppHdr>) value -> value.setUuid(null)),
                Arguments.of("msgType", (Consumer<AppHdr>) value -> value.setMsgType(null)),
                Arguments.of("createTime", (Consumer<AppHdr>) value -> value.setCreateTime(null)));
    }

    private static Stream<Arguments> missingGroupHeaderFields() {
        return Stream.of(
                Arguments.of("msgId", (Consumer<GroupHdr>) value -> value.setMsgId(null)),
                Arguments.of("creDtTime", (Consumer<GroupHdr>) value -> value.setCreDtTime(null)),
                Arguments.of("ctrlSum", (Consumer<GroupHdr>) value -> value.setCtrlSum(null)),
                Arguments.of("ttlIntrBkSttlmValue", (Consumer<GroupHdr>) value -> value.setTtlIntrBkSttlmValue(null)),
                Arguments.of("ttlIntrBkSttlmCcy", (Consumer<GroupHdr>) value -> value.setTtlIntrBkSttlmCcy(null)),
                Arguments.of("numberOfTransactions", (Consumer<GroupHdr>) value -> value.setNumberOfTransactions(null)),
                Arguments.of("settlementMethod", (Consumer<GroupHdr>) value -> value.setSettlementMethod(null)));
    }

    private static Stream<Arguments> missingTransactionFields() {
        return Stream.of(
                Arguments.of("endToEndId", (Consumer<Transaction>) value -> value.setEndToEndId(null)),
                Arguments.of("intrBkSttlmValue", (Consumer<Transaction>) value -> value.setIntrBkSttlmValue(null)),
                Arguments.of("intrBkSttlmCcy", (Consumer<Transaction>) value -> value.setIntrBkSttlmCcy(null)),
                Arguments.of("intrBkSttlmDt", (Consumer<Transaction>) value -> value.setIntrBkSttlmDt(null)),
                Arguments.of("chargeBearer", (Consumer<Transaction>) value -> value.setChargeBearer(null)),
                Arguments.of("debtorName", (Consumer<Transaction>) value -> value.setDebtorName(null)),
                Arguments.of("debtorAgentBic", (Consumer<Transaction>) value -> value.setDebtorAgentBic(null)),
                Arguments.of("creditorAgentBic", (Consumer<Transaction>) value -> value.setCreditorAgentBic(null)),
                Arguments.of("creditorName", (Consumer<Transaction>) value -> value.setCreditorName(null)));
    }

    private static GroupHdr groupHeader() {
        GroupHdr group = new GroupHdr();
        group.setMsgId("MSG-001");
        group.setCreDtTime("2026-07-21T08:00:00Z");
        group.setNumberOfTransactions(1);
        group.setCtrlSum("125.50");
        group.setTtlIntrBkSttlmValue("125.50");
        group.setTtlIntrBkSttlmCcy("USD");
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
