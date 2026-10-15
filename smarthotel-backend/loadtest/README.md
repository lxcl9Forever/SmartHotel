# 每日优惠：1000 人抢同一房型

此脚本准备 1000 个不同的客户账号，再同时向同一 `dailyDealId` 发送预订请求。注册和登录在计时前完成。脚本**只创建待支付订单，不调用支付接口**。

## 运行前

1. 在隔离环境启动后端，使用**独立的 MySQL 数据库和 Redis 库/实例**。不要连接日常开发或真实业务数据。关闭支付宝；RabbitMQ 可关闭，订单超时仍由定时扫描处理。后端可在 `8081` 端口运行。可在 IDEA 的测试运行配置中覆盖数据库 URL、Redis 地址或库号、端口，并设置 `smarthotel.alipay.enabled=false`、`smarthotel.rabbitmq.enabled=false`。
2. 在隔离数据库执行项目建表及种子 SQL，包括 `database/15_daily_deal.sql`。**项目 SQL 文件含有 `USE smart_hotel`**：建议使用独立 MySQL 实例中的同名数据库；若使用同一实例的其他库，先在副本中修改这些 `USE`，不要直接执行原文件。确保目标房型在选定入住区间内每天都有房价和库存，再以管理员身份调用隔离后端的 `POST /api/v1/test/redis/inventory/init` 将这些库存同步到隔离 Redis。
3. 用管理员页面“每日优惠秒杀”创建并开启活动。建议房型库存至少 20 间、秒杀名额 20 间、折扣率 `0.8`，入住日期选未来，活动保持开放至少 30 分钟。记下活动 ID。
4. 确认测试环境的订单支付超时时间长于整个测试，且没有其他请求在抢同一活动。

在 `smarthotel-backend` 目录的 PowerShell 终端运行：

```powershell
$env:LOAD_TEST_ISOLATED = 'YES'
$env:BASE_URL = 'http://127.0.0.1:8081'
$env:DEAL_ID = '填入隔离环境中的活动ID'
node .\loadtest\flash-sale-1000.mjs
```

脚本只接受本机地址，并要求显式设置 `LOAD_TEST_ISOLATED=YES`。`USERS` 默认为 1000；可先设 `$env:USERS = '20'` 检查环境，但正式测试请使用**新的活动**，避免预检订单占用名额。账号准备并发数默认为 20，可通过 `PREPARE_CONCURRENCY` 调整；单个抢订请求超时默认 120 秒，可通过 `REQUEST_TIMEOUT_MS` 调整。

## 结果判定

若活动名额与实际库存均为 20 间，且没有其他请求，理想结果是 20 个成功订单、980 个“名额已抢完”或“库存不足”的 409 响应。重点看报告中的：

- `noOversell`、`uniqueOrderIds`、`correctPriceAndNoStacking`、`filledAvailableQuota` 均为 `true`。
- `unexpectedFailures` 为 0；任何 500、鉴权失败、连接错误或超时都需要单独排查。
- `latencyMs.p50/p95/p99` 和 `burstMs` 衡量排队与数据库锁竞争；名额正确不代表吞吐或响应时间达标。
- `remainingDecreasedBySuccesses` 为 `true`。若活动在测试期间结束、订单超时或有外部流量，这项对账可能失去可比性。

报告默认写入 `loadtest/report-<时间戳>.json`，仅包含汇总和错误类别，不保存账号密码或令牌。测试结束后销毁隔离数据库及 Redis 数据。
