# 第三阶段：优惠活动

## 修改文件

- `database/12_promotion.sql`：新增活动表与订单价格快照字段。
- `entity/Promotion.java`、`mapper/PromotionMapper.java`、`dto/promotion/PromotionRequest.java`、`service/PromotionService.java`、`controller/PromotionController.java`：活动管理及最优优惠计算。
- `entity/BookingOrder.java`、`vo/BookingOrderVO.java`、`service/impl/BookingServiceImpl.java`：创建订单时保存原价、减免、实付及活动 ID；订单详情返回快照。
- 前端 `src/api/promotion.js`、`src/components/AdminPromotionPanel.vue`、`src/views/AdminDashboard.vue`、`src/views/BookingDetailView.vue`：活动管理入口与订单优惠展示。
- `PromotionServiceTest.java`：最优优惠及无优惠场景。

## 数据库

已对本机 `smart_hotel` 执行 `12_promotion.sql`。新增 `promotion` 表，以及 `booking_order.original_amount`、`discount_amount`、`promotion_id`。已有订单保持原实付金额；历史订单没有活动快照。删除活动是软删除，不影响旧订单。

## 接口

ADMIN：`POST /api/v1/admin/promotion`、`GET /api/v1/admin/promotion/list`、`PUT /api/v1/admin/promotion/{id}`、`DELETE /api/v1/admin/promotion/{id}`、`PUT /api/v1/admin/promotion/{id}/enabled?enabled=true|false`。客户创建订单仍使用原 `POST /api/v1/bookings`，自动选取当时生效且减免最高的一项活动，不叠加。

## 测试

1. 管理员进入“优惠活动”，新建折扣或满减活动并启用。活动时间需覆盖当前时间。
2. 客户选择有库存和价格的日期，创建一笔新订单；在详情页核对原价、优惠和实付金额。
3. 管理员关闭活动，再创建同条件订单；新订单不再享受优惠，旧订单的价格快照不变。
4. 运行 `mvn test` 和前端 `npm run build`。
