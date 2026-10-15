# 第二阶段：支付宝沙箱与操作日志

## 当前状态

支付宝沙箱代码已接入，默认关闭，等待配置沙箱密钥。原有模拟支付接口保留；它与支付宝支付都会写入现有的 `payment` 表。支付宝创建支付时记录 `PENDING`，验签通过的异步通知或顾客主动触发的支付宝服务端交易查询确认付款后，才会改为 `SUCCESS`、把订单改为 `CONFIRMED/PAID` 并把库存从 locked 转入 sold。重复确认按支付流水号幂等处理。已关闭的订单收到付款结果时，不会再次占用库存，日志会提示人工核对；沙箱联调时应检查这一边界情况。

操作日志使用 `@OperationLog(value = "操作名称", module = "模块")` 标注控制器方法。登录成功和失败、创建订单、模拟支付、发起支付宝支付、回调、退款和管理员修改已有数据会记入 `operation_log`。日志保存失败只写后端错误日志，不中断原业务。日志不保存请求体、密码、JWT、私钥或支付通知原文。

## 接口和数据库

- `POST /api/v1/payment/create?orderId=...`：CUSTOMER，仅能支付本人未过期的待支付订单；返回 `formHtml`，供浏览器跳转沙箱支付页面。
- `POST /api/v1/payment/query?orderId=...`：CUSTOMER，仅能查询本人订单；后端向支付宝查询交易并核对订单号、金额和交易状态，确认成功后更新本地订单。返回 `PAID`、`PENDING` 或 `CLOSED`。
- `POST /api/v1/payment/notify`：支付宝表单回调，无 JWT；验签、核对 AppID、商户号（若已配置）、金额和支付流水后返回纯文本 `success`，否则返回 `failure`。
- 原有 `POST /api/v1/bookings/{orderId}/mock-pay` 保持可用。
- 使用现有 `payment` 表，无需另建重复的 `payment_record`；新增 `database/11_operation_log.sql` 中的 `operation_log` 表，该迁移已在本机 `smart_hotel` 执行。

## 开通沙箱后配置

1. 在支付宝开放平台开通沙箱应用，准备沙箱 AppID、应用私钥和支付宝公钥。私钥、公钥文件放在项目目录之外，不要提交到 Git。当前实现使用公钥模式。
2. 后端 IDEA Run Configuration 的 Environment variables 中配置：`ALIPAY_ENABLED=true`、`ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY_FILE`、`ALIPAY_PUBLIC_KEY_FILE`。本机沙箱联调可以不设置 `ALIPAY_NOTIFY_URL`，付款后点击订单页“查询支付结果”；部署后若使用异步通知，再配置支付宝服务器可访问的公网回调地址 `/api/v1/payment/notify`。可选 `ALIPAY_SELLER_ID` 用于异步通知额外校验商户号。沙箱网关默认 `https://openapi-sandbox.dl.alipaydev.com/gateway.do`。
3. 在前端项目的本地 `.env.local` 设置 `VITE_ALIPAY_ENABLED=true`，重启 Vite。订单详情页的支付按钮会显示“支付宝沙箱支付”。后端和前端这两个开关要一起启用。
4. 使用沙箱买家账号支付，不要使用真实支付账号。返回订单详情后点击“查询支付结果”，确认订单变为 `CONFIRMED/PAID`，`payment` 中的 `ALIPAY` 记录变为 `SUCCESS` 并保存 `third_party_no`。

## 不需要沙箱账号即可测试的部分

1. 后端运行时登录一次，创建一个测试订单，使用模拟支付或申请退款，再用管理员修改一项测试数据。
2. 查询 `SELECT id,user_id,username,operation,module,request_url,ip,success,create_time FROM operation_log ORDER BY id DESC LIMIT 20;`，核对每次操作只增加一条，且没有密码或令牌。
3. 未配置沙箱时调用 `POST /api/v1/payment/create` 应得到“支付宝沙箱尚未启用”，现有模拟支付仍可用。

> 用户已开通公钥模式沙箱并持有 AppID、应用私钥和支付宝公钥。尚无公网 HTTPS 回调地址时可使用主动交易查询完成本地联调；异步通知链路仍需公网地址才能单独验证。支付宝平台的“授权回调地址”与支付异步通知 URL 是不同设置。开启前应先用单笔小额沙箱订单验证完整链路。详细步骤见 [本机沙箱支付教程](alipay-sandbox-local.md)。
