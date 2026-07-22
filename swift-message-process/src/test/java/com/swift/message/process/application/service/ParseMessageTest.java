package com.swift.message.process.application.service;

import com.swift.message.process.domain.exception.MessageParseException;
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
                <GrpHdr><MsgId>MSG-001</MsgId><CreDtTm>2026-07-21T08:00:00Z</CreDtTm><BtchBookg>true</BtchBookg><NbOfTxs>1</NbOfTxs>
                  <CtrlSum>125.50</CtrlSum><TtlIntrBkSttlmAmt Ccy="USD">125.50</TtlIntrBkSttlmAmt>
                  <IntrBkSttlmDt>2026-07-22</IntrBkSttlmDt>
                  <SttlmInf><SttlmMtd>INDA</SttlmMtd><ClrSys><Cd>CHIPS</Cd></ClrSys></SttlmInf>
                  <InstgAgt><FinInstnId><BICFI>GROUPINSTGBIC</BICFI></FinInstnId></InstgAgt>
                  <InstdAgt><FinInstnId><BICFI>GROUPINSTDBIC</BICFI></FinInstnId></InstdAgt>
                </GrpHdr>
                <CdtTrfTxInf><PmtId><InstrId>I-1</InstrId><EndToEndId>E-1</EndToEndId><TxId>T-1</TxId><UETR>U-1</UETR></PmtId>
                  <PmtTpInf><InstrPrty>HIGH</InstrPrty><SvcLvl><Cd>SEPA</Cd></SvcLvl><LclInstrm><Prtry>LOCAL</Prtry></LclInstrm><CtgyPurp><Cd>SUPP</Cd></CtgyPurp></PmtTpInf>
                  <IntrBkSttlmAmt Ccy="USD">125.50</IntrBkSttlmAmt><InstdAmt Ccy="EUR">100.00</InstdAmt>
                  <IntrBkSttlmDt>2026-07-22</IntrBkSttlmDt><SttlmPrty>HIGH</SttlmPrty><ChrgBr>SHAR</ChrgBr>
                  <ChrgsInf><Amt Ccy="USD">2.50</Amt><Agt><FinInstnId><BICFI>CHARGEBIC</BICFI></FinInstnId></Agt></ChrgsInf>
                  <PrvsInstgAgt1><FinInstnId><BICFI>PREVIOUSBIC</BICFI></FinInstnId></PrvsInstgAgt1>
                  <IntrmyAgt1><FinInstnId><BICFI>INTERBIC1</BICFI></FinInstnId></IntrmyAgt1>
                  <Dbtr><Nm>Debtor Ltd</Nm><PstlAdr><StrtNm>Main Street</StrtNm><BldgNb>10</BldgNb><TwnNm>Berlin</TwnNm><Ctry>DE</Ctry><AdrLine>Floor 2</AdrLine></PstlAdr></Dbtr>
                  <DbtrAcct><Id><IBAN>DEBTOR-IBAN</IBAN></Id></DbtrAcct>
                  <DbtrAgt><FinInstnId><BICFI>DEBTORBIC</BICFI></FinInstnId></DbtrAgt>
                  <Cdtr><Nm>Creditor Ltd</Nm><PstlAdr><TwnNm>Paris</TwnNm><Ctry>FR</Ctry></PstlAdr></Cdtr>
                  <CdtrAcct><Id><Othr><Id>CREDITOR-ACCT</Id></Othr></Id></CdtrAcct>
                  <CdtrAgt><FinInstnId><BICFI>CREDITORBIC</BICFI></FinInstnId></CdtrAgt>
                  <InitgPty><Nm>Initiator Ltd</Nm></InitgPty><UltmtDbtr><Nm>Ultimate Debtor</Nm></UltmtDbtr><UltmtCdtr><Nm>Ultimate Creditor</Nm></UltmtCdtr>
                  <InstrForCdtrAgt><InstrInf>Call beneficiary</InstrInf></InstrForCdtrAgt><InstrForNxtAgt><InstrInf>Process immediately</InstrInf></InstrForNxtAgt>
                  <RgltryRptg><Dtls><Inf>REG-001</Inf></Dtls></RgltryRptg>
                  <Purp><Cd>GDDS</Cd></Purp><RmtInf><Ustrd>Invoice 2026-001</Ustrd><Strd><CdtrRefInf><Ref>RF18539007547034</Ref></CdtrRefInf></Strd></RmtInf>
                </CdtTrfTxInf>
              </FIToFICstmrCdtTrf>
            </Document>
            """;

    @Test
    void parsesApplicationHeaderFromNamespacedFragment() {
        var header = ParseMessage.appHdrParse(MESSAGE);
        assertEquals("UTF-8", header.getCharSet());
        assertEquals("SENDERBIC", header.getSenderBic());
        assertEquals("RECEIVERBIC", header.getReceiverBic());
        assertEquals("pacs.008.001.08", header.getMsgType());
        assertEquals("COPY", header.getDupTag());
        assertEquals(true, header.getPsbDupTag());
    }

    @Test
    void parsesGroupHeader() {
        var group = ParseMessage.groupHdrParse(MESSAGE);
        assertEquals("MSG-001", group.getMsgId());
        assertEquals(1, group.getNumberOfTransactions());
        assertEquals(true, group.getBatchBooking());
        assertEquals("125.50", group.getTtlIntrBkSttlmValue());
        assertEquals("USD", group.getTtlIntrBkSttlmCcy());
        assertEquals("2026-07-22", group.getIntrBkSttlmDt());
        assertEquals("CHIPS", group.getClearingSysCode());
        assertEquals("GROUPINSTGBIC", group.getInstructingAgentBic());
        assertEquals("GROUPINSTDBIC", group.getInstructedAgentBic());
    }

    @Test
    void parsesCreditTransferTransaction() {
        var transaction = ParseMessage.transactionParse(MESSAGE);
        assertEquals("T-1", transaction.getTransactionId());
        assertEquals("125.50", transaction.getIntrBkSttlmValue());
        assertEquals("USD", transaction.getIntrBkSttlmCcy());
        assertEquals("100.00", transaction.getInstdAmtValue());
        assertEquals("EUR", transaction.getInstdAmtCcy());
        assertEquals("HIGH", transaction.getInstructionPriority());
        assertEquals("SEPA", transaction.getServiceLevel());
        assertEquals("HIGH", transaction.getSettlementPriority());
        assertEquals("2.50", transaction.getCharges().getFirst().getAmount());
        assertEquals("Berlin", transaction.getDebtorAddress().getTownName());
        assertEquals("FR", transaction.getCreditorAddress().getCountry());
        assertEquals("Ultimate Debtor", transaction.getUltimateDebtorName());
        assertEquals("GDDS", transaction.getPurposeCd());
        assertEquals(2, transaction.getRmtInfUstrd().size());
        assertEquals("PREVIOUSBIC", transaction.getPreIntrmyAgtBics().getFirst());
        assertEquals("INTERBIC1", transaction.getIntrmyAgtBics().getFirst());
        assertEquals("Call beneficiary", transaction.getInstructionsForCreditorAgent().getFirst());
        assertEquals("Process immediately", transaction.getInstructionsForNextAgent().getFirst());
        assertEquals("REG-001", transaction.getRegulatoryReportingInformation().getFirst());
        assertEquals("CREDITOR-ACCT", transaction.getCreditorAccount());
    }

    @Test
    void parsesPaymentStatusTransaction() {
        String status = "<Document><FIToFIPmtStsRpt><TxInfAndSts><StsId>STATUS-1</StsId><OrgnlTxId>T-1</OrgnlTxId><OrgnlUETR>U-1</OrgnlUETR>"
                + "<TxSts>RJCT</TxSts><StsRsnInf><Rsn><Cd>AC01</Cd></Rsn></StsRsnInf><StsRsnInf><Rsn><Prtry>CUSTOM</Prtry></Rsn></StsRsnInf>"
                + "<AccptncDtTm>2026-07-21T08:01:00Z</AccptncDtTm><AcctSvcrRef>ASR-1</AcctSvcrRef><ClrSysRef>CSR-1</ClrSysRef>"
                + "<OrgnlTxRef><IntrBkSttlmAmt Ccy=\"USD\">125.50</IntrBkSttlmAmt>"
                + "<Amt><InstdAmt Ccy=\"EUR\">100.00</InstdAmt></Amt></OrgnlTxRef>"
                + "</TxInfAndSts></FIToFIPmtStsRpt></Document>";
        var transaction = ParseMessage.transactionParse(status);
        assertEquals("T-1", transaction.getOriginalTransactionId());
        assertEquals("STATUS-1", transaction.getStatusId());
        assertEquals("U-1", transaction.getOriginalUetr());
        assertEquals("125.50", transaction.getIntrBkSttlmValue());
        assertEquals("USD", transaction.getIntrBkSttlmCcy());
        assertEquals("100.00", transaction.getInstdAmtValue());
        assertEquals("EUR", transaction.getInstdAmtCcy());
        assertEquals("RJCT", transaction.getTransactionStatus());
        assertEquals("AC01", transaction.getStatusReasonCode());
        assertEquals(2, transaction.getStatusReasonCodes().size());
        assertEquals("ASR-1", transaction.getAccountServicerReference());
    }

    @Test
    void rejectsMissingLayerAndDoctype() {
        assertThrows(MessageParseException.class, () -> ParseMessage.groupHdrParse("<Document/>"));
        assertThrows(MessageParseException.class,
                () -> ParseMessage.appHdrParse("<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><AppHdr>&e;</AppHdr>"));
    }
}
