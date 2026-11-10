# SmartHotel 智慧酒店

基于 Spring Boot 3、Vue 3 和 MySQL 8 的酒店预订与运营管理项目，提供顾客、员工和管理员三类操作入口。仓库包含前后端源码、数据库初始化脚本、自动化测试、运行说明和可编辑设计图源。

## 功能范围

- 顾客：注册登录、房型查询、预订与订单管理、常用入住人、续住、退款、评价。
- 员工：入住、退房、住客与客房状态管理。
- 管理员：账号、房型房间、每日库存房价、订单、退款和促销管理。
- 优惠：优惠券、每日签到、每日优惠秒杀；库存使用 MySQL 与 Redis Lua 协作处理。
- 支付：开发模拟支付及可配置的支付宝沙箱；AI 推荐可按配置接入外部模型。

这是学习与演示项目。源码中的功能入口不等于生产环境验收通过；实际验证范围见 [验证记录](docs/verification.md)。支付宝沙箱与 AI 的实际调用需要自己的配置。

## 目录

```text
SmartHotel/
├── smarthotel-backend/          后端源码、单元测试、数据库脚本及专题文档
│   ├── database/               建表、迁移和演示房型数据
│   ├── docs/                   支付、促销、续住、AI 等实现说明
│   ├── loadtest/               隔离环境压测脚本
│   └── perf/                   JMeter 示例
├── smarthotel-frontend/         Vue 前端及图片等静态资源
└── docs/
    ├── setup.md                从零启动与初始化账号
    ├── verification.md         本次验证结果和限制
    └── design/                 历史设计说明书及两套可编辑图源
```

## 环境

| 组件 | 本项目要求 |
| --- | --- |
| Java | JDK 17 |
| Maven | 3.9.x，本次使用 3.9.15 |
| Node.js | 本次使用 22.23.2；前端版本由 package-lock.json 锁定 |
| MySQL | 8.x，SQL 使用 utf8mb4_0900_ai_ci 和递归 CTE，不支持直接导入 MySQL 5.7 |
| Redis | 需要独立可用实例，用于验证码、限流与库存 |
| RabbitMQ | 可选，默认关闭业务消息功能 |

## 启动

完整步骤见 [运行指南](docs/setup.md)。先创建 `smart_hotel` 数据库，再按指南顺序执行 SQL；复制后端配置模板到本地 `application.yml`，设置数据库、Redis 和 JWT，然后分别启动：

```bash
# 后端目录
mvn spring-boot:run
```

```bash
# 前端目录
npm ci
npm run dev
```

后端默认 `http://localhost:8080`；前端以 Vite 启动时终端显示的地址为准。开发代理将 `/api` 请求转发到后端。

## 验证命令

```bash
# 后端目录，先完成本地配置
mvn test
```

```bash
# 前端目录
npm run build
```

## 配置与专题文档

- [后端配置模板](smarthotel-backend/src/main/resources/application-example.yml)
- [前端环境变量模板](smarthotel-frontend/.env.example)
- [支付宝沙箱配置](smarthotel-backend/docs/alipay-sandbox-local.md)
- [退款与支付审计](smarthotel-backend/docs/phase2-payment-audit.md)
- [促销功能](smarthotel-backend/docs/phase3-promotion.md)
- [AI 推荐](smarthotel-backend/docs/phase4-ai-recommendation.md)
- [续住](smarthotel-backend/docs/customer-stay-extension.md)
- [常用入住人](smarthotel-backend/docs/customer-saved-guests.md)
- [隔离压测](smarthotel-backend/loadtest/README.md)
- [设计资料说明](docs/design/README.md)

本地密码、密钥、真实数据库备份、依赖包、构建产物和文档生成临时文件不上传。公开前后均应检查提交内容；`.gitignore` 不会移除已经进入历史的文件。
