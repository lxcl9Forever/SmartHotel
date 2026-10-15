# 第一阶段：订单超时与库存并发验证

## 行为与配置

- 新订单保持 `WAIT_PAY/UNPAID`，`expire_time` 默认是创建后 **30 分钟**；前端倒计时读取该字段。
- RabbitMQ 位于 Ubuntu 虚拟机 `192.168.229.131`，这是当前默认连接地址；默认用户名是 `mall`，虚拟主机是 `/mall`。密码不保存在项目中。IDEA 的后端 Run/Debug Configuration → Environment variables 中设置 `RABBITMQ_PASSWORD` 和 `RABBITMQ_ENABLED=true`，再重启后端；如服务器配置变化，再设置 `RABBITMQ_HOST`、`RABBITMQ_USER` 或 `RABBITMQ_VHOST`。已核对 `mall` 账号对 `/mall` 虚拟主机有配置、写入和读取权限。
- RabbitMQ 使用 `order.delay.queue`（消息过期后死信投递）和 `order.timeout.queue`。消费者仅关闭已过期、仍为 `WAIT_PAY/UNPAID` 的订单，状态改为现有的 `CLOSED`，释放 MySQL 和 Redis 库存。每分钟的数据库扫描保留作兜底。
- 预订仍以 Redis Lua 原子扣减和 MySQL 库存行锁防超卖；没有另加 Redis 互斥锁。数据库事务回滚时会补回 Redis 扣减。
- `database/10_order_timeout_index.sql` 为兜底扫描建立索引，已支持重复执行。
- 如果日志出现 `Redis inventory release failed` 或 `rollback compensation failed`，应在停止新预订的维护时段，使用 ADMIN 账号调用 `POST /api/v1/test/redis/inventory/init`，按 MySQL 库存重新同步 Redis，然后恢复预订。

## RabbitMQ 验证

1. 启动 MySQL、项目配置的 Redis 和 RabbitMQ（AMQP 端口默认 5672），在 IDEA 配置上述环境变量并重启后端。启动日志不应出现 `ACCESS_REFUSED` 或队列声明失败；如果有，先核对 `mall` 密码和 vhost 权限。
2. 使用 CUSTOMER 账号创建订单，记录 `orderId` 与 `expireTime`。在 RabbitMQ 管理页面确认 `order.delay.queue` 收到消息。
3. 测试环境可将 `smarthotel.order.payment-timeout-minutes` 暂时设为 `1`，**先重启后端再创建新订单**，等待约 1–2 分钟。
4. 查询 `booking_order.order_status` 应为 `CLOSED`，`payment_status` 仍为 `UNPAID`，对应 `room_inventory.locked_qty` 减少。已经支付的订单不能被超时关闭。
5. RabbitMQ 不可用时，保持 `RABBITMQ_ENABLED=false`；定时扫描仍会处理已到期订单。

## JMeter 100 线程库存测试

本目录的 `inventory-concurrency.jmx` 对同一房型、同一入住区间同时发起 100 个创建订单请求。使用**测试数据库和测试账号**，因为成功请求会留下真实待支付订单。开始前确保该日期有房价与库存，并使用 ADMIN 测试账号调用 `POST /api/v1/test/redis/inventory/init`，将 MySQL 可售库存同步到 Redis。此测试接口已限制为 ADMIN。

1. 安装 Apache JMeter（建议 5.6.3），在浏览器登录一个 CUSTOMER 测试账号，取其 JWT；不要把令牌提交到仓库。
2. 选一个未来且库存已初始化的 `checkInDate`、`checkOutDate`，记下 `roomTypeId` 和测试前该日 `total_qty - locked_qty - sold_qty`。
3. 在 JMeter 中打开 JMX，填入 `token`、`roomTypeId`、日期等 JMeter 属性；也可用命令行 `-Jtoken=... -JroomTypeId=... -JcheckInDate=... -JcheckOutDate=...`。注意命令行参数会留在终端历史，测试令牌用完应失效。
4. 以 CLI 模式运行：`jmeter -n -t inventory-concurrency.jmx -l results.jtl -e -o report`。`report` 必须是空目录或不存在。
5. 报告中 `200` 是成功预订，库存不足导致的 `409` 是预期结果；`500`、连接错误和库存负数都需要调查。
6. 在 MySQL 执行下列只读检查，并保存 JMeter 的 `results.jtl` 与 HTML 报告。确认成功单数不超过测试前可售库存，且每天 `locked_qty >= 0`、`sold_qty >= 0`、`locked_qty + sold_qty <= total_qty`。

```sql
SET @room_type_id = 1;
SET @check_in_date = '2026-11-01';
SET @check_out_date = '2026-11-02';

SELECT inventory_date, total_qty, locked_qty, sold_qty,
       total_qty - locked_qty - sold_qty AS available_qty
FROM room_inventory
WHERE room_type_id = @room_type_id
  AND inventory_date >= @check_in_date
  AND inventory_date < @check_out_date
ORDER BY inventory_date;

SELECT COUNT(*) AS invalid_days
FROM room_inventory
WHERE locked_qty < 0 OR sold_qty < 0 OR locked_qty + sold_qty > total_qty;
```

如果没有安装 RabbitMQ/JMeter，代码可先编译和运行单元测试；**不要将未执行的并发测试写成通过的报告**。
