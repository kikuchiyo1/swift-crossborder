# SWIFT Message Process SDK

无外部运行时依赖的 ISO 20022 XML 解析 SDK，要求 JDK 21。

## 接口

```java
import com.swift.message.process.ParseMessage;

AppHdr header = ParseMessage.appHdrParse(message);
Transaction transaction = ParseMessage.transactionParse(message);
GroupHdr groupHeader = ParseMessage.groupHdrParse(message);
```

- `appHdrParse` 解析 Business Application Header (`AppHdr`)。
- `transactionParse` 解析首个 `CdtTrfTxInf`（pacs.008）或 `TxInfAndSts`（pacs.002）。
- `groupHdrParse` 解析 `GrpHdr`。

三个接口均接受完整 XML 文档或 `AppHdr` + `Document` 双根报文片段。空报文、非法 XML、缺少目标层时抛出
`MessageParseException`。解析器禁用 DTD 和外部实体。

## 报文构造

```java
String appHdrXml = BuildMessage.buildAppHdr(appHdr);
String bodyXml = BuildMessage.buildMessageBody(groupHdr, transaction);
String multiTransactionBodyXml = BuildMessage.buildMessageBody(groupHdr, transactions);
```

`buildMessageBody` 构造 `pacs.008.001.08 Document`。付款人和收款人地址只输出 `PostalAddress`
中的结构化字段，不输出 `AdrLine`；地址只包含 `AdrLine` 时抛出 `MessageBuildException`。

## 构建

```bash
mvn clean test
```
