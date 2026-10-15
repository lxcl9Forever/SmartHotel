# 第四阶段：智能选房

## 修改文件

- 后端 `dto/ai/RecommendRequest.java`、`dto/ai/RoomPreference.java`、`vo/AIRecommendationVO.java`、`vo/RecommendedRoomVO.java`、`controller/AIRecommendController.java`、`service/AIRecommendService.java`、`service/LLMClient.java`、`mapper/RoomRatingMapper.java`。
- 后端 `application.yml`、`application.example.yml`：可选 LLM 配置。
- 前端 `src/api/ai.js`、`src/components/AIRecommendPanel.vue`、`src/views/HomeView.vue`：自然语言输入、推荐卡片与房型搜索联动。
- `AIRecommendServiceTest.java`：模型不可用时报错、逐日库存、价格和预算筛选。

## 数据库

无需新增表。读取现有 `room_type`、`room_inventory`、`room_price`、`order_review`、`booking_order`、`promotion`。推荐中的库存和价格只反映查询当刻，最终预订仍以创建订单时的服务端校验为准。

## 接口

CUSTOMER：`POST /api/v1/customer/ai/recommend`，JSON 例如 `{"text":"一家三口入住三晚，预算1000，希望安静","checkInDate":"2026-10-16"}`。入住日期可省略，默认明天。返回解析出的人数、晚数、预算、偏好和最多三条有真实库存、逐日价格且符合预算的房型。

## 运行方式及限制

智能选房只使用 DeepSeek 模型解析需求，不再使用本地规则解析。已预设 DeepSeek 的 Chat Completions 地址和 `deepseek-flash` 模型。在 IDEA 顶部选择后端运行配置 → **Edit Configurations...** → 选择 `SmarthotelBackendApplication` → **Environment variables** → 点击右侧编辑按钮，新增以下两行：

```text
AI_LLM_ENABLED=true
AI_LLM_API_KEY=你自己的密钥
```

点击 **OK** 保存，停止并重新运行后端。`AI_LLM_URL` 和 `AI_LLM_MODEL` 可选，只有需要更换接口或模型时再设置。后端使用 Bearer 密钥调用 Chat Completions，让模型提取人数、晚数、预算与偏好，并生成礼貌的开场语；本项目数据库负责核算实际库存、价格、评价和优惠。顾客看到的回复会接上已核实的房价、库存与预算建议，避免模型编造价格。模型未配置、调用失败或返回无效内容时，接口明确报错，不再回退本地解析。密钥只保存在运行环境，不放入仓库，也不要发送到聊天中。

推荐结果中的房型可直接点击“填写预订信息”，系统会重新查询当前日期的库存和价格，再打开已有预订表单。预算内没有房型时，返回同一入住区间里超预算金额最少、且有库存的房型；回复明确显示需要增加的预算，并按实际价格与优惠计算少住一晚的金额。可分别按原入住晚数或少住一晚预订。若没有满足人数与库存的房型，则不会虚构预算替代方案。

房间是否“安静”只根据已有房型名称和介绍匹配；资料没有明示的偏好会在推荐理由中提示向酒店确认。库存和价格不做凭空推测。

## 测试

1. 客户登录，在首页“智能选房”输入示例句，可选入住日期，点击“获取推荐”。
2. 检查人数 3、晚数 3、预算 1000 的解析、正式礼貌的助手回复和推荐理由。点击推荐房型的“填写预订信息”，应直接打开该房型的预订表单。
3. 将预算调得低于所有房型总价，应显示离预算最近的房型、所需增加金额和少住一晚的实际价格。
4. 暂时关闭 `AI_LLM_ENABLED` 后重启再试，应看到模型未配置错误，不应出现本地解析结果。
5. 运行 `mvn test` 和前端 `npm run build`。实际模型调用仍需使用你本机保存的密钥联调。
