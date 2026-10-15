# SmartHotel 智慧酒店

基于 Spring Boot 3 和 Vue 3 的酒店预订与运营管理系统。代码分为：

- `smarthotel-backend/`：后端接口、数据库脚本和项目文档。
- `smarthotel-frontend/`：顾客、员工和管理员前端。

## 本地运行

1. 使用 JDK 17、MySQL、Redis 和 Node.js。后端依赖使用 Maven 安装。
2. 按文件名顺序执行 `smarthotel-backend/database/` 中适用的 SQL 脚本。
3. 参考 `smarthotel-backend/src/main/resources/application-example.yml` 创建本地 `application.yml`，填写数据库、Redis 和 JWT 配置。该本地文件已被 Git 忽略。
4. 在 `smarthotel-backend/` 运行 `mvn spring-boot:run`。
5. 在 `smarthotel-frontend/` 运行 `npm ci` 和 `npm run dev`。

支付宝沙箱、优惠活动等功能的配置说明见 `smarthotel-backend/docs/`。密钥与本地环境变量文件不要提交到仓库。

## 验证

- 后端：在 `smarthotel-backend/` 运行 `mvn test`。
- 前端：在 `smarthotel-frontend/` 运行 `npm run build`。
