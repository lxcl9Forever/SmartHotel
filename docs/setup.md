# 本地运行指南

本指南用于在独立开发数据库中启动 SmartHotel，不会导入真实业务数据。不要把其他项目或真实客户数据库当作演示库。

## 1 环境准备

安装 JDK 17、Maven 3.9.x、Node.js 22.23.2 或兼容当前锁文件的版本、MySQL 8.x 和 Redis。数据库脚本使用 MySQL 8 的排序规则和 CTE，不适用于 MySQL 5.7。RabbitMQ、支付宝沙箱、外部 AI 模型是可选项。

## 2 数据库初始化

在 Navicat 或 MySQL 客户端先执行：

```sql
CREATE DATABASE IF NOT EXISTS smart_hotel
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

在新建的空库中按以下顺序执行 `smarthotel-backend/database/` 的脚本：

1. `01_user_auth.sql`
2. `02_hotel_resource.sql`
3. `02_room_seed_data.sql`
4. `03_inventory_price.sql`
5. `04_booking.sql`
6. `05_payment.sql`
7. `06_refund_payment.sql`
8. `07_order_review.sql`
9. `08_refund_order_index.sql`
10. `09_room_media.sql`
11. `10_order_timeout_index.sql`
12. `11_operation_log.sql`
13. `12_promotion.sql`
14. `13_customer_saved_guest.sql`
15. `14_daily_checkin_coupon.sql`
16. `15_daily_deal.sql`

这些文件包含 `USE smart_hotel`，在客户端选择其他数据库不会改变它们的目标。脚本用于初始化和按序迁移，不保证全部可重复执行；遇到重复索引或已有数据时不要强行重跑整套脚本。

实体房脚本预置 354 间房；库存和价格脚本按执行当日生成未来 90 天数据。旧数据库需要检查入住日期范围内是否有每日库存和价格。

## 3 后端配置

复制 `src/main/resources/application-example.yml` 为同目录的 `application.yml`，只修改本地副本。该文件已忽略，不要提交。

在 IDEA 的运行配置环境变量中设置，或使用当前终端的环境变量：

| 变量 | 用途 |
| --- | --- |
| `MYSQL_PASSWORD` | MySQL 密码；数据库主机、端口和用户名可在本地 YAML 中修改 |
| `JWT_SECRET` | 自己生成的随机密钥，UTF-8 编码至少 32 字节，不要使用示例占位符 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 主机和端口，默认 localhost:6379 |
| `REDIS_PASSWORD` | Redis 密码，没有密码时留空 |
| `REDIS_DATABASE` | Redis 库号，默认 0；避免与其他项目共享库存键 |

IDEA 启动类为 `com.smarthotel.SmarthotelBackendApplication`。也可以在后端目录执行 `mvn spring-boot:run`。当前 POM 跳过可执行 JAR 重打包，不应直接把普通打包结果当作可执行 JAR 使用。

## 4 前端与首个账号

在前端目录执行 `npm ci`，然后 `npm run dev`。普通演示无需创建 `.env.local`；启用沙箱支付时参考 `.env.example` 与专题文档。

脚本只初始化角色和业务基础数据，不提供通用默认管理员密码。先在前端注册顾客账号；密码至少 8 位，手机号使用隔离测试资料。

在自己的演示数据库中，将刚注册的账号提升为首位管理员。把 `your_registered_username` 替换为实际用户名：

```sql
UPDATE sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
SET u.role_id = r.role_id
WHERE u.username = 'your_registered_username';
```

确认只影响目标账号，然后退出并重新登录，以获得管理员角色令牌。员工账号通过管理员的账号管理创建，不要绕过密码哈希直接写入明文密码。

## 5 初始化库存缓存

新环境需在 MySQL 房量数据准备好后，用管理员令牌调用：

```text
POST /api/v1/test/redis/inventory/init
Authorization: Bearer <管理员登录令牌>
```

此操作会同步库存缓存；不要在正常预订进行中反复初始化。之后用新的顾客账号验证房型查询、预订和开发模拟支付。项目需要的入住身份资料请使用合法的测试资料，不要把真实身份证号上传到仓库。

## 6 可选服务

- RabbitMQ：启用前配置 host、virtual-host、用户名和密码，并设置 `RABBITMQ_ENABLED=true`。未启用时订单超时仍有定时扫描处理逻辑。
- 支付宝：参考后端专题文档；私钥放在项目外部，前后端开关同时配置。这里是沙箱接入，不能直接用于真实收款。
- AI：配置模型 URL、模型名、API Key 及 `AI_LLM_ENABLED`；未启用时按代码中的本地推荐逻辑运行。
- 压测：只在独立数据库、独立 Redis 的隔离环境运行，按 `loadtest/README.md` 操作。不要为了压测关闭生产鉴权。

## 常见问题

- `Unknown collation utf8mb4_0900_ai_ci`：检查是否误用了 MySQL 5.7。
- 登录或验证码异常：检查 Redis 地址、密码以及后端日志。
- JWT 密钥长度错误：设置至少 32 字节的随机密钥并重启。
- 前端接口报错：检查后端 8080 端口和 Vite `/api` 代理。
- 页面查不到可订日期：核对当日及未来日期的库存、价格和 Redis 初始化情况。
