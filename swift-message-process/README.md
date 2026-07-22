# SWIFT Message Process SDK

无外部运行时依赖的 ISO 20022 XML 解析 SDK，要求 JDK 21。

## 接口详情

### 解析接口

```java
import com.swift.message.process.application.service.ParseMessage;
import com.swift.message.process.application.service.BuildMessage;
import com.swift.message.process.domain.exception.MessageBuildException;
import com.swift.message.process.domain.exception.MessageParseException;
import com.swift.message.process.domain.model.AppHdr;
import com.swift.message.process.domain.model.GroupHdr;
import com.swift.message.process.domain.model.Transaction;

AppHdr header = ParseMessage.appHdrParse(message);
Transaction transaction = ParseMessage.transactionParse(message);
GroupHdr groupHeader = ParseMessage.groupHdrParse(message);
```

| 接口 | 入参 | 返回值 | 目标节点 |
|---|---|---|---|
| `ParseMessage.appHdrParse(String message)` | 完整 XML 或双根报文片段 | `AppHdr` | `AppHdr` |
| `ParseMessage.groupHdrParse(String message)` | 完整 XML 或双根报文片段 | `GroupHdr` | `GrpHdr` |
| `ParseMessage.transactionParse(String message)` | 完整 XML 或双根报文片段 | `Transaction` | 首个 `CdtTrfTxInf`，不存在时读取首个 `TxInfAndSts` |

三个接口均接受完整 XML 文档或 `AppHdr` + `Document` 双根报文片段。空报文、非法 XML、缺少目标层时抛出
`MessageParseException`。解析器禁用 DTD 和外部实体。解析过程不校验目标节点内部的 ISO 20022 必传字段；节点中
不存在的字段返回 `null` 或模型中对应的空列表。

### 构建接口

```java
String appHdrXml = BuildMessage.buildAppHdr(appHdr);
String bodyXml = BuildMessage.buildMessageBody(groupHdr, transaction);
String multiTransactionBodyXml = BuildMessage.buildMessageBody(groupHdr, transactions);
```

| 接口 | 用途 | 返回值 |
|---|---|---|
| `BuildMessage.buildAppHdr(AppHdr appHdr)` | 构造 `head.001.001.02 AppHdr` | 不含 XML 声明的 `AppHdr` XML 字符串 |
| `BuildMessage.buildMessageBody(GroupHdr groupHdr, Transaction transaction)` | 构造包含一笔交易的 `pacs.008.001.08 Document` | `Document` XML 字符串 |
| `BuildMessage.buildMessageBody(GroupHdr groupHdr, List<Transaction> transactions)` | 构造包含一笔或多笔交易的 `pacs.008.001.08 Document` | `Document` XML 字符串 |

入参为空、必传字段为空白或字段间约束不满足时抛出 `MessageBuildException`。报文头和报文体分别构造，调用方可按
传输协议要求进行组合。

## 构建必传字段

“必传”是指当前 SDK 构建 XML 时会执行强制校验；字符串不能为 `null`、空串或纯空白。

### AppHdr

| Java 字段 | XML 路径 | 说明 |
|---|---|---|
| `senderBic` | `AppHdr/Fr/FIId/FinInstnId/BICFI` | 发送方 BIC |
| `receiverBic` | `AppHdr/To/FIId/FinInstnId/BICFI` | 接收方 BIC |
| `uuid` | `AppHdr/BizMsgIdr` | 业务报文唯一标识 |
| `msgType` | `AppHdr/MsgDefIdr` | 报文定义标识，例如 `pacs.008.001.08` |
| `createTime` | `AppHdr/CreDt` | 报文头创建时间 |

`charSet`、`bizSvc`、`dupTag`、`psbDupTag`、`priority` 为可选字段。

### GroupHdr

| Java 字段 | XML 路径 | 说明 |
|---|---|---|
| `msgId` | `GrpHdr/MsgId` | 组报文唯一标识 |
| `creDtTime` | `GrpHdr/CreDtTm` | 组头创建时间 |
| `numberOfTransactions` | `GrpHdr/NbOfTxs` | 交易笔数，必须等于传入的 `transactions` 数量 |
| `settlementMethod` | `GrpHdr/SttlmInf/SttlmMtd` | 结算方式，例如 `INDA` |

`batchBooking`、`ctrlSum`、`intrBkSttlmDt`、`clearingSysCode`、`instructingAgentBic`、
`instructedAgentBic` 为可选字段。`ttlIntrBkSttlmValue` 存在时，`ttlIntrBkSttlmCcy` 必传。

### Transaction

| Java 字段 | XML 路径 | 说明 |
|---|---|---|
| `endToEndId` | `CdtTrfTxInf/PmtId/EndToEndId` | 端到端标识；无业务编号时可按规范使用 `NOTPROVIDED` |
| `intrBkSttlmValue` | `CdtTrfTxInf/IntrBkSttlmAmt` | 银行间结算金额 |
| `intrBkSttlmCcy` | `CdtTrfTxInf/IntrBkSttlmAmt/@Ccy` | 银行间结算币种 |
| `intrBkSttlmDt` | `CdtTrfTxInf/IntrBkSttlmDt` | 银行间结算日期 |
| `chargeBearer` | `CdtTrfTxInf/ChrgBr` | 费用承担方，例如 `SHAR` |
| `debtorName` | `CdtTrfTxInf/Dbtr/Nm` | 付款人名称 |
| `debtorAgentBic` | `CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI` | 付款行 BIC |
| `creditorAgentBic` | `CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI` | 收款行 BIC |
| `creditorName` | `CdtTrfTxInf/Cdtr/Nm` | 收款人名称 |

其余交易字段为可选字段。`instdAmtValue` 存在时，`instdAmtCcy` 必传；`charges` 可以为空列表，但其中每个
`ChargeInfo` 都不能为 `null`，且 `amount` 和 `currency` 均为必传字段。

### 集合和地址约束

- `transactions` 不能为空、不能是空列表、不能包含 `null`，且数量必须等于 `GroupHdr.numberOfTransactions`。
- `Transaction` 中的列表字段默认使用空列表；通过 setter 传入 `null` 时会归一化为空列表。
- `debtorAddress` 或 `creditorAddress` 存在时，必须至少包含一个结构化地址字段，例如 `streetName`、`townName`、
  `postCode` 或 `country`。
- 构建器不输出 `PostalAddress.addressLines` 对应的 `AdrLine`；地址只设置 `addressLines` 会抛出
  `MessageBuildException`。

## DDD 目录结构

```text
com.swift.message.process
├── application/service  # 报文构建、解析应用服务
├── domain/model         # ISO 20022 领域模型
├── domain/exception     # 领域异常
└── infrastructure/xml   # XML 协议常量等基础设施实现
```

依赖方向为应用层编排领域对象并使用 XML 基础设施；领域层不依赖应用层或基础设施层。

## 构建

```bash
mvn clean test
```
