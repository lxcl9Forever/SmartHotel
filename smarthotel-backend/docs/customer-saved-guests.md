# 顾客常用入住人

## 数据库

`database/13_customer_saved_guest.sql` 新增 `customer_saved_guest` 表，按 `user_id` 归属顾客账号。包含姓名、手机号、证件类型和可选证件号码。已在本机 `smart_hotel` 执行迁移。每个账号最多保存 20 位；相同账号下同一证件类型和证件号码不能重复。

## 接口

全部要求 CUSTOMER JWT，用户 ID 从认证信息取得：

- `GET /api/v1/customer/saved-guests`：列出当前顾客的常用入住人。
- `POST /api/v1/customer/saved-guests`：新增。
- `PUT /api/v1/customer/saved-guests/{guestId}`：编辑自己的记录。
- `DELETE /api/v1/customer/saved-guests/{guestId}`：删除自己的记录。

新增或编辑的 JSON 示例：`{"guestName":"张三","phone":"13812345678","idType":"ID_CARD","idNumber":""}`。`idType` 可为 `ID_CARD`、`PASSPORT`、`OTHER`。

## 前端使用

顾客登录后进入“个人资料”，在“常用入住人”区域新增、编辑、删除。预订弹窗的每位入住人都有“常用入住人”选择框；选中后带入姓名、手机号和证件信息。选中主要入住人时，还会带入联系人姓名和电话。选择后的内容仍可手动修改，订单会复制当时表单中的信息，不会与常用入住人记录保持引用关系。列表中手机号与证件号码会遮挡显示。

## 测试

1. 用顾客 A 保存两位入住人，返回首页预订两人，逐位选择，核对自动填写及联系人。
2. 用顾客 B 登录，列表不应看到顾客 A 的记录；使用 A 的 `guestId` 请求编辑或删除应得到 404。
3. 编辑、删除一位常用入住人，检查列表更新；已创建订单的入住人信息保持不变。
4. 运行后端 `mvn -o -q -Dtest=CustomerSavedGuestServiceTest test` 和前端 `npm run build`。
