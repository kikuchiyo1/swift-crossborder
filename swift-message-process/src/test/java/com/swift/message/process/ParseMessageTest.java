package com.swift.message.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseMessageTest {
    private static final String MESSAGE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AppHdr xmlns="urn:iso:std:iso:20022:tech:xsd:head.001.001.02">
              <CharSet>UTF-8</CharSet>
              <Fr><FIId><FinInstnId><BICFI>SENDERBIC</BICFI></FinInstnId></FIId></Fr>
              <To><FIId><FinInstnId><BICFI>RECEIVERBIC</BICFI></FinInstnId></FIId></To>
              <BizMsgIdr>MSG-001</BizMsgIdr><MsgDefIdr>pacs.008.001.08</MsgDefIdr>
              <BizSvc>swift.cbprplus.02</BizSvc><CreDt>2026-07-21T08:00:00Z</CreDt>
              <CpyDplct>COPY</CpyDplct><PssblDplct>true</PssblDplct>
            </AppHdr>
            <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
              <FIToFICstmrCdtTrf>
                <GrpHdr><MsgId>MSG-001</MsgId><CreDtTm>2026-07-21T08:00:00Z</CreDtTm><NbOfTxs>1</NbOfTxs>
                  <IntrBkSttlmDt>2026-07-22</IntrBkSttlmDt>
                  <SttlmInf><SttlmMtd>INDA</SttlmMtd><ClrSys><Cd>CHIPS</Cd></ClrSys></SttlmInf>
                </GrpHdr>
                <CdtTrfTxInf><PmtId><InstrId>I-1</InstrId><EndToEndId>E-1</EndToEndId><TxId>T-1</TxId><UETR>U-1</UETR></PmtId>
                  <IntrBkSttlmAmt Ccy="USD">125.50</IntrBkSttlmAmt><InstdAmt Ccy="EUR">100.00</InstdAmt>
                  <IntrBkSttlmDt>2026-07-22</IntrBkSttlmDt><ChrgBr>SHAR</ChrgBr>
                  <PrvsInstgAgt1><FinInstnId><BICFI>PREVIOUSBIC</BICFI></FinInstnId></PrvsInstgAgt1>
                  <IntrmyAgt1><FinInstnId><BICFI>INTERBIC1</BICFI></FinInstnId></IntrmyAgt1>
                  <Dbtr><Nm>Debtor Ltd</Nm><PstlAdr><StrtNm>Main Street</StrtNm><BldgNb>10</BldgNb><TwnNm>Berlin</TwnNm><Ctry>DE</Ctry><AdrLine>Floor 2</AdrLine></PstlAdr></Dbtr>
                  <DbtrAcct><Id><IBAN>DEBTOR-IBAN</IBAN></Id></DbtrAcct>
                  <DbtrAgt><FinInstnId><BICFI>DEBTORBIC</BICFI></FinInstnId></DbtrAgt>
                  <Cdtr><Nm>Creditor Ltd</Nm><PstlAdr><TwnNm>Paris</TwnNm><Ctry>FR</Ctry></PstlAdr></Cdtr>
                  <CdtrAcct><Id><Othr><Id>CREDITOR-ACCT</Id></Othr></Id></CdtrAcct>
                  <CdtrAgt><FinInstnId><BICFI>CREDITORBIC</BICFI></FinInstnId></CdtrAgt>
                  <InitgPty><Nm>Initiator Ltd</Nm></InitgPty><UltmtDbtr><Nm>Ultimate Debtor</Nm></UltmtDbtr><UltmtCdtr><Nm>Ultimate Creditor</Nm></UltmtCdtr>
                  <Purp><Cd>GDDS</Cd></Purp><RmtInf><Ustrd>Invoice 2026-001</Ustrd><Strd><CdtrRefInf><Ref>RF18539007547034</Ref></CdtrRefInf></Strd></RmtInf>
                </CdtTrfTxInf>
              </FIToFICstmrCdtTrf>
            </Document>
            """;

    @Test
    void parsesApplicationHeaderFromNamespacedFragment() {
        var header = ParseMessage.appHdrParse(MESSAGE);
        assertEquals("UTF-8", header.charSet());
        assertEquals("SENDERBIC", header.senderBic());
        assertEquals("RECEIVERBIC", header.receiverBic());
        assertEquals("pacs.008.001.08", header.msgType());
        assertEquals("COPY", header.dupTag());
        assertEquals(true, header.psbDupTag());
    }

    @Test
    void parsesGroupHeader() {
        var group = ParseMessage.groupHdrParse(MESSAGE);
        assertEquals("MSG-001", group.msgId());
        assertEquals(1, group.numberOfTransactions());
        assertEquals("2026-07-22", group.intrBkSttlmDt());
        assertEquals("CHIPS", group.clearingSysCode());
    }

    @Test
    void parsesCreditTransferTransaction() {
        var transaction = ParseMessage.transactionParse(MESSAGE);
        assertEquals("T-1", transaction.transactionId());
        assertEquals("125.50", transaction.intrBkSttlmValue());
        assertEquals("USD", transaction.intrBkSttlmCcy());
        assertEquals("100.00", transaction.instdAmtValue());
        assertEquals("EUR", transaction.instdAmtCcy());
        assertEquals("Berlin", transaction.debtorAddress().townName());
        assertEquals("FR", transaction.creditorAddress().country());
        assertEquals("Ultimate Debtor", transaction.ultimateDebtorName());
        assertEquals("GDDS", transaction.purpose());
        assertEquals(2, transaction.remittanceInformation().size());
        assertEquals("PREVIOUSBIC", transaction.previousInstructingAgentBics().getFirst());
        assertEquals("INTERBIC1", transaction.intermediaryAgentBics().getFirst());
        assertEquals("CREDITOR-ACCT", transaction.creditorAccount());
    }

    @Test
    void parsesPaymentStatusTransaction() {
        String status = "<Document><FIToFIPmtStsRpt><TxInfAndSts><OrgnlTxId>T-1</OrgnlTxId>"
                + "<TxSts>RJCT</TxSts><StsRsnInf><Rsn><Cd>AC01</Cd></Rsn></StsRsnInf>"
                + "</TxInfAndSts></FIToFIPmtStsRpt></Document>";
        var transaction = ParseMessage.transactionParse(status);
        assertEquals("T-1", transaction.originalTransactionId());
        assertEquals("RJCT", transaction.transactionStatus());
        assertEquals("AC01", transaction.statusReasonCode());
    }

    @Test
    void rejectsMissingLayerAndDoctype() {
        assertThrows(MessageParseException.class, () -> ParseMessage.groupHdrParse("<Document/>"));
        assertThrows(MessageParseException.class,
                () -> ParseMessage.appHdrParse("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><AppHdr>&e;</AppHdr>"));
    }
}
