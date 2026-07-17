# Swift 跨境支付系统

报文模块（:8080）→ 清算模块（:8081）→ 支付模块（:8082），三个独立 Spring Boot 服务通过 REST 通信。

## 技术栈

| 项 | 版本 | 说明 |
|----|------|------|
| JDK | **21** | 强制，不要用 17 或 22+ |
| Spring Boot | 3.3.0 | |
| MySQL | 8.0+ | 三个模块各用一个 database，字符集统一 utf8mb4 |
| MyBatis-Plus | 3.5.9 | |
| Maven | 3.6+ | |

## 开发规范

- **端口固定**：报文 8080 / 清算 8081 / 支付 8082，不要改
- **数据库**：
  ```sql
  CREATE DATABASE swift_message   DEFAULT CHARSET utf8mb4;
  CREATE DATABASE swift_settle    DEFAULT CHARSET utf8mb4;
  CREATE DATABASE swift_payment   DEFAULT CHARSET utf8mb4;
  ```
- **各模块只改自己的目录和数据库**，不跨模块
- **接口前缀**：报文 /api/message，清算 /api/v1/swift，支付 /api/v1/payment

## 协作流程

```bash
git pull                # 先拉最新
git add -A
git commit -m "xxx"     # feat: 新功能 / fix: 修bug / docs: 文档
git push
```

- 所有人直接推 main 分支
- 各人只改自己负责的模块文件夹
