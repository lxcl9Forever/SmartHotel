<template>
  <div class="admin-page">

    <header class="header">

      <div>
        <h1>SmartHotel 管理后台</h1>
        <p>房型与实体客房管理</p>
      </div>

      <div class="header-right">

        <span>
          当前管理员：
          {{ userStore.nickname || userStore.username }}
        </span>

        <el-button
            type="danger"
            plain
            @click="handleLogout"
        >
          退出登录
        </el-button>

      </div>

    </header>


    <main class="container">

      <el-tabs
          class="management-tabs"
          tab-position="left"
          v-model="activeTab"
          @tab-change="handleTabChange"
      >

        <!-- 房型管理 -->
        <el-tab-pane
            label="房型管理"
            name="roomTypes"
        >

          <div class="section-header">

            <div>
              <h2>房型列表</h2>
              <p>
                共 {{ roomTypes.length }} 个房型
              </p>
            </div>

            <el-button
                :loading="roomTypeLoading"
                @click="loadRoomTypes"
            >
              刷新
            </el-button>

          </div>


          <el-table
              v-loading="roomTypeLoading"
              :data="pagedList(roomTypes, 'roomTypes')"
              border
              stripe
          >

            <el-table-column
                prop="typeCode"
                label="房型编码"
                width="120"
            />

            <el-table-column
                prop="typeName"
                label="房型名称"
                min-width="220"
            />

            <el-table-column
                label="面积"
                width="100"
            >
              <template #default="{ row }">
                {{ row.area }}㎡
              </template>
            </el-table-column>

            <el-table-column
                prop="bedType"
                label="床型"
                width="120"
            />

            <el-table-column
                prop="bedCount"
                label="床数"
                width="80"
            />

            <el-table-column
                prop="maxGuests"
                label="最多入住"
                width="100"
            />

            <el-table-column
                prop="totalRooms"
                label="实体房数"
                width="100"
            />

            <el-table-column
                label="基础价格"
                width="130"
            >
              <template #default="{ row }">
                ¥{{ row.basePrice }}
              </template>
            </el-table-column>

            <el-table-column
                label="销售状态"
                width="110"
            >

              <template #default="{ row }">

                <el-tag
                    v-if="row.status === 1"
                    type="success"
                >
                  销售中
                </el-tag>

                <el-tag
                    v-else
                    type="danger"
                >
                  已停售
                </el-tag>

              </template>

            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.roomTypes.page" v-model:page-size="listPages.roomTypes.pageSize" :total="roomTypes.length" />

        </el-tab-pane>


        <!-- 客房管理 -->
        <el-tab-pane
            label="客房管理"
            name="rooms"
        >

          <div class="filter-bar">

            <el-select
                v-model="roomQuery.roomTypeId"
                clearable
                placeholder="全部房型"
                style="width: 220px"
            >

              <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.roomTypeId"
                  :label="roomType.typeName"
                  :value="roomType.roomTypeId"
              />

            </el-select>


            <el-select
                v-model="roomQuery.status"
                clearable
                placeholder="全部房态"
                style="width: 180px"
            >

              <el-option
                  label="空闲 VACANT"
                  value="VACANT"
              />

              <el-option
                  label="已入住 OCCUPIED"
                  value="OCCUPIED"
              />

              <el-option
                  label="待清洁 DIRTY"
                  value="DIRTY"
              />

              <el-option
                  label="清洁中 CLEANING"
                  value="CLEANING"
              />

              <el-option
                  label="维修 MAINTENANCE"
                  value="MAINTENANCE"
              />

              <el-option
                  label="停用 OUT_OF_SERVICE"
                  value="OUT_OF_SERVICE"
              />

            </el-select>


            <el-button
                type="primary"
                :loading="roomLoading"
                @click="loadRooms"
            >
              查询
            </el-button>


            <el-button
                @click="resetRoomQuery"
            >
              重置
            </el-button>

          </div>


          <div class="room-summary">

            <span>
              当前共 {{ rooms.length }} 间客房
            </span>

          </div>


          <el-table
              v-loading="roomLoading"
              :data="pagedList(rooms, 'rooms')"
              border
              stripe
          >

            <el-table-column
                prop="roomId"
                label="ID"
                width="80"
            />

            <el-table-column
                prop="roomNo"
                label="房号"
                width="130"
            />

            <el-table-column
                label="房型"
                min-width="200"
            >

              <template #default="{ row }">
                {{ getRoomTypeName(row.roomTypeId) }}
              </template>

            </el-table-column>

            <el-table-column
                label="楼层"
                width="100"
            >

              <template #default="{ row }">
                {{ row.floorNo }} 楼
              </template>

            </el-table-column>

            <el-table-column
                label="当前房态"
                width="160"
            >

              <template #default="{ row }">

                <el-tag
                    :type="getRoomStatusType(row.status)"
                >
                  {{ getRoomStatusText(row.status) }}
                </el-tag>

              </template>

            </el-table-column>

            <el-table-column
                prop="remark"
                label="备注"
                min-width="180"
            />

          </el-table>
          <AppPagination v-model:current-page="listPages.rooms.page" v-model:page-size="listPages.rooms.pageSize" :total="rooms.length" />

        </el-tab-pane>

        <!-- 房量管理 -->
        <el-tab-pane
            label="房量管理"
            name="inventory"
        >

          <div class="filter-bar">

            <!-- 选择房型 -->
            <el-select
                v-model="inventoryQuery.roomTypeId"
                placeholder="选择房型"
                style="width: 240px"
            >

              <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.roomTypeId"
                  :label="roomType.typeName"
                  :value="roomType.roomTypeId"
              />

            </el-select>


            <!-- 开始日期 -->
            <el-date-picker
                v-model="inventoryQuery.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
            />


            <!-- 结束日期 -->
            <el-date-picker
                v-model="inventoryQuery.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
            />


            <!-- 查询按钮 -->
            <el-button
                type="primary"
                :loading="inventoryLoading"
                @click="loadInventories"
            >
              查询
            </el-button>

          </div>


          <!-- 房量表格 -->
          <el-table
              v-loading="inventoryLoading"
              :data="pagedList(inventories, 'inventories')"
              border
              stripe
          >

            <el-table-column
                prop="inventoryDate"
                label="日期"
                width="160"
            />


            <el-table-column
                label="房型"
                min-width="200"
            >

              <template #default="{ row }">
                {{ getRoomTypeName(row.roomTypeId) }}
              </template>

            </el-table-column>


            <el-table-column
                prop="totalQty"
                label="总房量"
                width="120"
            />


            <el-table-column
                prop="lockedQty"
                label="锁定量"
                width="120"
            />


            <el-table-column
                prop="soldQty"
                label="已售量"
                width="120"
            />


            <el-table-column
                label="当前可售"
                width="130"
            >

              <template #default="{ row }">

                <el-tag
                    :type="
            getAvailableQty(row) > 0
              ? 'success'
              : 'danger'
          "
                >
                  {{ getAvailableQty(row) }}
                </el-tag>

              </template>

            </el-table-column>

            <el-table-column
                label="操作"
                width="120"

            >

              <template #default="{ row }">

                <el-button
                    type="primary"
                    size="small"
                    @click="openInventoryEdit(row)"
                >
                  编辑
                </el-button>

              </template>

            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.inventories.page" v-model:page-size="listPages.inventories.pageSize" :total="inventories.length" />

        </el-tab-pane>

        <!-- 房价管理 -->
        <el-tab-pane
            label="房价管理"
            name="price"
        >

          <div class="filter-bar">

            <!-- 选择房型 -->
            <el-select
                v-model="priceQuery.roomTypeId"
                placeholder="选择房型"
                style="width: 240px"
            >

              <el-option
                  v-for="roomType in roomTypes"
                  :key="roomType.roomTypeId"
                  :label="roomType.typeName"
                  :value="roomType.roomTypeId"
              />

            </el-select>


            <!-- 开始日期 -->
            <el-date-picker
                v-model="priceQuery.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
            />


            <!-- 结束日期 -->
            <el-date-picker
                v-model="priceQuery.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
            />


            <!-- 查询 -->
            <el-button
                type="primary"
                :loading="priceLoading"
                @click="loadPrices"
            >
              查询
            </el-button>

          </div>


          <el-table
              v-loading="priceLoading"
              :data="pagedList(prices, 'prices')"
              border
              stripe
          >

            <el-table-column
                prop="priceDate"
                label="日期"
                width="180"
            />


            <el-table-column
                label="房型"
                min-width="240"
            >

              <template #default="{ row }">
                {{ getRoomTypeName(row.roomTypeId) }}
              </template>

            </el-table-column>


            <el-table-column
                label="每日售价"
                width="180"
            >

              <template #default="{ row }">

                <strong class="price-text">
                  ¥{{ row.salePrice }}
                </strong>

              </template>

            </el-table-column>

            <el-table-column
                label="操作"
                width="120"
            >

              <template #default="{ row }">

                <el-button
                    type="primary"
                    size="small"
                    @click="openPriceEdit(row)"
                >
                  编辑
                </el-button>

              </template>

            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.prices.page" v-model:page-size="listPages.prices.pageSize" :total="prices.length" />

        </el-tab-pane>

        <!-- 订单管理 -->
        <el-tab-pane
            label="订单管理"
            name="orders"
        >

          <!-- 查询条件 -->
          <div class="filter-bar">

            <el-input
                v-model="orderQuery.orderNo"
                clearable
                placeholder="订单号"
                style="width: 220px"
                @keyup.enter="searchOrders"
            />


            <el-select
                v-model="orderQuery.orderStatus"
                clearable
                placeholder="全部订单状态"
                style="width: 180px"
            >

              <el-option
                  label="待支付"
                  value="WAIT_PAY"
              />

              <el-option
                  label="已确认"
                  value="CONFIRMED"
              />
              <el-option label="未入住" value="NO_SHOW" />

              <el-option
                  label="入住中"
                  value="CHECKED_IN"
              />

              <el-option
                  label="已退房"
                  value="CHECKED_OUT"
              />

              <el-option
                  label="已完成"
                  value="COMPLETED"
              />

              <el-option
                  label="申请取消"
                  value="CANCEL_REQUESTED"
              />

              <el-option
                  label="已取消"
                  value="CANCELLED"
              />

              <el-option
                  label="已关闭"
                  value="CLOSED"
              />
              <el-option label="已退款" value="REFUNDED" />

            </el-select>


            <el-select
                v-model="orderQuery.paymentStatus"
                clearable
                placeholder="全部支付状态"
                style="width: 170px"
            >

              <el-option
                  label="未支付"
                  value="UNPAID"
              />

              <el-option
                  label="已支付"
                  value="PAID"
              />

              <el-option
                  label="退款中"
                  value="REFUNDING"
              />

              <el-option
                  label="已退款"
                  value="REFUNDED"
              />

              <el-option
                  label="部分退款"
                  value="PARTIAL_REFUND"
              />

            </el-select>


            <el-date-picker
                v-model="orderQuery.checkInDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="入住日期"
                style="width: 180px"
            />


            <el-button
                type="primary"
                :loading="orderLoading"
                @click="searchOrders"
            >
              查询
            </el-button>


            <el-button
                @click="resetOrderQuery"
            >
              重置
            </el-button>

          </div>


          <div class="room-summary">
            共 {{ orderTotal }} 个订单
          </div>


          <el-table
              v-loading="orderLoading"
              :data="orders"
              border
              stripe
              style="width: 100%"
          >

            <el-table-column
                prop="orderNo"
                label="订单号"
                min-width="190"
            />


            <el-table-column
                prop="roomTypeName"
                label="房型"
                min-width="170"
            />


            <el-table-column
                label="联系人"
                min-width="150"
            >

              <template #default="{ row }">
                <div>{{ row.contactName }}</div>
                <div style="color: #909399">
                  {{ row.contactPhone }}
                </div>
              </template>

            </el-table-column>


            <el-table-column
                label="入住日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkInDate }}
              </template>
            </el-table-column>


            <el-table-column
                label="离店日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkOutDate }}
              </template>
            </el-table-column>


            <el-table-column
                label="房间"
                width="90"
            >
              <template #default="{ row }">
                {{ row.roomCount }} 间
              </template>
            </el-table-column>


            <el-table-column
                label="金额"
                width="120"
            >
              <template #default="{ row }">
                ¥{{ row.totalAmount }}
              </template>
            </el-table-column>


            <el-table-column
                label="订单状态"
                width="110"
            >

              <template #default="{ row }">

                <el-tag
                    :type="
              getOrderStatusType(
                row.orderStatus
              )
            "
                >
                  {{
                    getOrderStatusText(
                        row.orderStatus
                    )
                  }}
                </el-tag>

              </template>

            </el-table-column>


            <el-table-column
                label="支付状态"
                width="110"
            >

              <template #default="{ row }">

                <el-tag
                    :type="
              getPaymentStatusType(
                row.paymentStatus
              )
            "
                >
                  {{
                    getPaymentStatusText(
                        row.paymentStatus
                    )
                  }}
                </el-tag>

              </template>

            </el-table-column>


            <el-table-column
                label="操作"
                width="100"

            >

              <template #default="{ row }">

                <el-button
                    type="primary"
                    link
                    @click="openOrderDetail(row)"
                >
                  详情
                </el-button>

              </template>

            </el-table-column>

          </el-table>


          <!-- 分页 -->
          <div class="pagination-bar">

            <AppPagination
                v-model:current-page="orderQuery.page"
                v-model:page-size="orderQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                :total="orderTotal"
                @current-change="handleOrderPageChange"
                @size-change="handleOrderPageSizeChange"
            />

          </div>

        </el-tab-pane>

        <el-tab-pane label="退款管理" name="refunds" lazy>
          <AdminRefundPanel @reviewed="loadOrders" />
        </el-tab-pane>

        <el-tab-pane label="评价管理" name="reviews" lazy>
          <AdminReviewPanel />
        </el-tab-pane>

        <el-tab-pane label="用户/员工管理" name="accounts" lazy><AdminAccountPanel /></el-tab-pane>
        <el-tab-pane label="支付记录" name="payments" lazy><AdminPaymentPanel /></el-tab-pane>
        <el-tab-pane label="优惠活动" name="promotions" lazy><AdminPromotionPanel /></el-tab-pane>
        <el-tab-pane label="每日优惠秒杀" name="dailyDeals" lazy><AdminDailyDealPanel /></el-tab-pane>
        <el-tab-pane label="每日签到管理" name="checkin" lazy><AdminCheckinPanel /></el-tab-pane>

        <el-tab-pane
            label="运营看板"
            name="statistics"
        >

          <div class="filter-bar">

            <el-date-picker
                v-model="statisticsQuery.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
            />

            <el-date-picker
                v-model="statisticsQuery.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
            />

            <el-button
                type="primary"
                :loading="statisticsLoading"
                @click="loadStatistics"
            >
              刷新统计
            </el-button>

          </div>


          <!-- 顶部经营指标 -->
          <div
              v-if="getLatestStatistics()"
              class="stat-cards"
          >

            <el-card>
              <div class="stat-title">
                今日新增订单
              </div>

              <div class="stat-value">
                {{
                  getLatestStatistics()
                      .newOrderCount
                }}
              </div>
            </el-card>


            <el-card>
              <div class="stat-title">
                今日入住
              </div>

              <div class="stat-value">
                {{
                  getLatestStatistics()
                      .checkInCount
                }}
              </div>
            </el-card>


            <el-card>
              <div class="stat-title">
                今日收入
              </div>

              <div class="stat-value">
                ¥{{
                  getLatestStatistics()
                      .totalRevenue
                }}
              </div>
            </el-card>


            <el-card>
              <div class="stat-title">
                入住率
              </div>

              <div class="stat-value">
                {{
                  (
                      Number(
                          getLatestStatistics()
                              .occupancyRate
                      ) * 100
                  ).toFixed(2)
                }}%
              </div>
            </el-card>

          </div>


          <!-- 图表 -->
          <div class="chart-grid">

            <el-card>

              <div
                  ref="revenueChartRef"
                  class="chart"
              />

            </el-card>


            <el-card>

              <div
                  ref="occupancyChartRef"
                  class="chart"
              />

            </el-card>

          </div>


          <el-card
              style="margin-top: 20px"
          >

            <div
                ref="operationChartRef"
                class="chart operation-chart"
            />

          </el-card>

        </el-tab-pane>

      </el-tabs>

    </main>

    <!-- 修改每日房量弹窗 -->
    <el-dialog
        v-model="inventoryDialogVisible"
        title="修改每日房量"
        width="520px"
    >

      <el-form
          label-width="110px"
      >

        <el-form-item label="房型">

          <el-input
              :model-value="
          getRoomTypeName(
            inventoryEditForm.roomTypeId
          )
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="日期">

          <el-input
              v-model="
          inventoryEditForm.inventoryDate
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="已锁定">

          <el-input
              :model-value="
          inventoryEditForm.lockedQty
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="已售">

          <el-input
              :model-value="
          inventoryEditForm.soldQty
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="总房量">

          <el-input-number
              v-model="
          inventoryEditForm.totalQty
        "
              :min="
          inventoryEditForm.lockedQty +
          inventoryEditForm.soldQty
        "
              :step="1"
              style="width: 100%"
          />

        </el-form-item>


        <el-form-item label="修改后可售">

          <strong>
            {{
              inventoryEditForm.totalQty
              -
              inventoryEditForm.lockedQty
              -
              inventoryEditForm.soldQty
            }}
          </strong>

        </el-form-item>

      </el-form>


      <template #footer>

        <el-button
            @click="
        inventoryDialogVisible = false
      "
        >
          取消
        </el-button>


        <el-button
            type="primary"
            :loading="inventorySubmitting"
            @click="submitInventoryEdit"
        >
          保存
        </el-button>

      </template>

    </el-dialog>

    <!-- 修改每日房价弹窗 -->
    <el-dialog
        v-model="priceDialogVisible"
        title="修改每日房价"
        width="500px"
    >

      <el-form
          label-width="100px"
      >

        <el-form-item label="房型">

          <el-input
              :model-value="
          getRoomTypeName(
            priceEditForm.roomTypeId
          )
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="日期">

          <el-input
              v-model="
          priceEditForm.priceDate
        "
              disabled
          />

        </el-form-item>


        <el-form-item label="每日售价">

          <el-input-number
              v-model="
          priceEditForm.salePrice
        "
              :min="0.01"
              :precision="2"
              :step="50"
              style="width: 100%"
          />

        </el-form-item>

      </el-form>


      <template #footer>

        <el-button
            @click="
        priceDialogVisible = false
      "
        >
          取消
        </el-button>


        <el-button
            type="primary"
            :loading="priceSubmitting"
            @click="submitPriceEdit"
        >
          保存
        </el-button>

      </template>

    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog
        v-model="orderDetailDialogVisible"
        title="订单详情"
        width="900px"
    >

      <div
          v-loading="orderDetailLoading"
          style="min-height: 180px"
      >

        <template v-if="orderDetail">

          <el-descriptions
              title="订单信息"
              :column="3"
              border
          >

            <el-descriptions-item label="订单号">
              {{ orderDetail.order.orderNo }}
            </el-descriptions-item>

            <el-descriptions-item label="客户ID">
              {{ orderDetail.order.customerId }}
            </el-descriptions-item>

            <el-descriptions-item label="房型">
              {{ orderDetail.order.roomTypeName }}
            </el-descriptions-item>


            <el-descriptions-item label="房间数量">
              {{ orderDetail.order.roomCount }} 间
            </el-descriptions-item>

            <el-descriptions-item label="入住人数">
              {{ orderDetail.order.guestCount }} 人
            </el-descriptions-item>

            <el-descriptions-item label="住宿晚数">
              {{ orderDetail.order.nights }} 晚
            </el-descriptions-item>


            <el-descriptions-item label="入住日期">
              {{ orderDetail.order.checkInDate }}
            </el-descriptions-item>

            <el-descriptions-item label="离店日期">
              {{ orderDetail.order.checkOutDate }}
            </el-descriptions-item>

            <el-descriptions-item label="订单金额">
              ¥{{ orderDetail.order.totalAmount }}
            </el-descriptions-item>


            <el-descriptions-item label="联系人">
              {{ orderDetail.order.contactName }}
            </el-descriptions-item>

            <el-descriptions-item label="联系电话">
              {{ orderDetail.order.contactPhone }}
            </el-descriptions-item>

            <el-descriptions-item label="创建时间">
              {{
                formatDateTime(
                    orderDetail.order.createTime
                )
              }}
            </el-descriptions-item>


            <el-descriptions-item label="订单状态">

              <el-tag
                  :type="
                getOrderStatusType(
                  orderDetail.order.orderStatus
                )
              "
              >
                {{
                  getOrderStatusText(
                      orderDetail.order.orderStatus
                  )
                }}
              </el-tag>

            </el-descriptions-item>


            <el-descriptions-item label="支付状态">

              <el-tag
                  :type="
                getPaymentStatusType(
                  orderDetail.order.paymentStatus
                )
              "
              >
                {{
                  getPaymentStatusText(
                      orderDetail.order.paymentStatus
                  )
                }}
              </el-tag>

            </el-descriptions-item>


            <el-descriptions-item label="备注">
              {{ orderDetail.order.remark || '-' }}
            </el-descriptions-item>

          </el-descriptions>


          <h3 style="margin-top: 28px">
            入住人信息
          </h3>


          <el-table
              :data="orderDetail.guests || []"
              border
              stripe
          >

            <el-table-column
                prop="guestName"
                label="姓名"
                width="130"
            />


            <el-table-column
                prop="phone"
                label="联系电话"
                width="150"
            />


            <el-table-column
                label="证件类型"
                width="120"
            >

              <template #default="{ row }">
                {{ getIdTypeText(row.idType) }}
              </template>

            </el-table-column>


            <el-table-column
                prop="idNumber"
                label="证件号码"
                min-width="200"
            />


            <el-table-column
                label="入住人类型"
                width="120"
            >

              <template #default="{ row }">

                <el-tag
                    v-if="row.isPrimary === 1"
                    type="success"
                >
                  主入住人
                </el-tag>

                <span v-else>
              同住人
            </span>

              </template>

            </el-table-column>

          </el-table>

        </template>

      </div>


      <template #footer>

        <el-button
            @click="
          orderDetailDialogVisible = false
        "
        >
          关闭
        </el-button>

      </template>

    </el-dialog>

  </div>
</template>


<script setup>
import AppPagination from '../components/AppPagination.vue'

import {
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref
} from 'vue'

import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  getAdminRoomTypes,
  getAdminRooms,
  getAdminInventories,
  getAdminPrices,
  updateAdminInventory,
  updateAdminPrice,
  refreshStatisticsRange,
  getDailyStatistics,
  getAdminOrders,
  getAdminOrderDetail
} from '../api/admin'

import { useUserStore } from '../stores/user'
import AdminRefundPanel from '../components/AdminRefundPanel.vue'
import AdminReviewPanel from '../components/AdminReviewPanel.vue'
import AdminAccountPanel from '../components/AdminAccountPanel.vue'
import AdminPaymentPanel from '../components/AdminPaymentPanel.vue'
import AdminPromotionPanel from '../components/AdminPromotionPanel.vue'
import AdminDailyDealPanel from '../components/AdminDailyDealPanel.vue'
import AdminCheckinPanel from '../components/AdminCheckinPanel.vue'


const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('roomTypes')

const roomTypes = ref([])
const rooms = ref([])

const inventories = ref([])
const prices = ref([])

const orders = ref([])
const orderTotal = ref(0)
const listPages = reactive(Object.fromEntries(['roomTypes', 'rooms', 'inventories', 'prices'].map(key => [key, { page: 1, pageSize: 10 }])))
const pagedList = (items, key) => items.slice((listPages[key].page - 1) * listPages[key].pageSize, listPages[key].page * listPages[key].pageSize)


const orderLoading = ref(false)

const orderDetailDialogVisible = ref(false)
const orderDetailLoading = ref(false)

const orderDetail = ref(null)

const inventoryLoading = ref(false)
const priceLoading = ref(false)

const inventoryDialogVisible = ref(false)
const priceDialogVisible = ref(false)

const inventorySubmitting = ref(false)
const priceSubmitting = ref(false)

const roomTypeLoading = ref(false)
const roomLoading = ref(false)

const statisticsLoading = ref(false)

const dailyStatistics = ref([])

const revenueChartRef = ref(null)
const occupancyChartRef = ref(null)
const operationChartRef = ref(null)

let revenueChart = null
let occupancyChart = null
let operationChart = null

const statisticsQuery = reactive({
  startDate: '',
  endDate: ''
})

const roomQuery = reactive({
  roomTypeId: null,
  status: ''
})

const inventoryQuery = reactive({
  roomTypeId: null,
  startDate: '',
  endDate: ''
})

const priceQuery = reactive({
  roomTypeId: null,
  startDate: '',
  endDate: ''
})

const orderQuery = reactive({
  page: 1,
  pageSize: 10,
  orderNo: '',
  orderStatus: '',
  paymentStatus: '',
  checkInDate: ''
})

const inventoryEditForm = reactive({
  roomTypeId: null,
  inventoryDate: '',
  totalQty: 0,
  lockedQty: 0,
  soldQty: 0
})


const priceEditForm = reactive({
  roomTypeId: null,
  priceDate: '',
  salePrice: 0
})

const formatDate = (date) => {

  const year = date.getFullYear()

  const month =
      String(date.getMonth() + 1)
          .padStart(2, '0')

  const day =
      String(date.getDate())
          .padStart(2, '0')

  return `${year}-${month}-${day}`
}

const initDateRange = () => {

  const today = new Date()

  const end = new Date()

  end.setDate(
      today.getDate() + 6
  )

  const startDate =
      formatDate(today)

  const endDate =
      formatDate(end)

  inventoryQuery.startDate =
      startDate

  inventoryQuery.endDate =
      endDate

  priceQuery.startDate =
      startDate

  priceQuery.endDate =
      endDate

  statisticsQuery.startDate =
      startDate

  statisticsQuery.endDate =
      endDate

  const statisticsEnd =
      new Date()

  const statisticsStart =
      new Date()

  statisticsStart.setDate(
      statisticsEnd.getDate() - 6
  )

  statisticsQuery.startDate =
      formatDate(statisticsStart)

  statisticsQuery.endDate =
      formatDate(statisticsEnd)
}

const loadInventories = async () => {

  if (!inventoryQuery.roomTypeId) {
    ElMessage.warning('请选择房型')
    return
  }

  if (
      !inventoryQuery.startDate ||
      !inventoryQuery.endDate
  ) {
    ElMessage.warning('请选择日期范围')
    return
  }

  try {

    inventoryLoading.value = true

    const response =
        await getAdminInventories({
          roomTypeId:
          inventoryQuery.roomTypeId,

          startDate:
          inventoryQuery.startDate,

          endDate:
          inventoryQuery.endDate
        })

    const result = response.data

    if (result.code !== 200) {

      ElMessage.error(
          result.message ||
          '房量加载失败'
      )

      return
    }

    inventories.value =
        result.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房量加载失败'
    )

  } finally {

    inventoryLoading.value = false
  }
}

const getAvailableQty = (row) => {

  return (
      row.totalQty -
      row.lockedQty -
      row.soldQty
  )
}

const openInventoryEdit = (row) => {

  inventoryEditForm.roomTypeId =
      row.roomTypeId

  inventoryEditForm.inventoryDate =
      row.inventoryDate

  inventoryEditForm.totalQty =
      row.totalQty

  inventoryEditForm.lockedQty =
      row.lockedQty

  inventoryEditForm.soldQty =
      row.soldQty

  inventoryDialogVisible.value = true
}

const submitInventoryEdit = async () => {

  /*
   * 最小允许值：
   * lockedQty + soldQty
   */
  const minTotalQty =
      inventoryEditForm.lockedQty +
      inventoryEditForm.soldQty


  if (
      inventoryEditForm.totalQty <
      minTotalQty
  ) {

    ElMessage.warning(
        `总房量不能小于已占用数量 ${minTotalQty}`
    )

    return
  }


  try {

    inventorySubmitting.value = true


    await updateAdminInventory({

      roomTypeId:
      inventoryEditForm.roomTypeId,

      inventoryDate:
      inventoryEditForm.inventoryDate,

      totalQty:
      inventoryEditForm.totalQty
    })


    ElMessage.success(
        '房量修改成功'
    )


    inventoryDialogVisible.value =
        false


    /*
     * 重新查询当前表格
     */
    await loadInventories()


  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房量修改失败'
    )

  } finally {

    inventorySubmitting.value =
        false
  }
}

const openPriceEdit = (row) => {

  priceEditForm.roomTypeId =
      row.roomTypeId

  priceEditForm.priceDate =
      row.priceDate

  priceEditForm.salePrice =
      Number(row.salePrice)

  priceDialogVisible.value = true
}

const submitPriceEdit = async () => {

  if (
      priceEditForm.salePrice === null ||
      priceEditForm.salePrice <= 0
  ) {

    ElMessage.warning(
        '售价必须大于0'
    )

    return
  }


  try {

    priceSubmitting.value = true


    await updateAdminPrice({

      roomTypeId:
      priceEditForm.roomTypeId,

      priceDate:
      priceEditForm.priceDate,

      salePrice:
      priceEditForm.salePrice
    })


    ElMessage.success(
        '房价修改成功'
    )


    priceDialogVisible.value =
        false


    await loadPrices()


  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房价修改失败'
    )

  } finally {

    priceSubmitting.value =
        false
  }
}

const loadPrices = async () => {

  if (!priceQuery.roomTypeId) {
    ElMessage.warning('请选择房型')
    return
  }

  if (
      !priceQuery.startDate ||
      !priceQuery.endDate
  ) {
    ElMessage.warning('请选择日期范围')
    return
  }

  try {

    priceLoading.value = true

    const response =
        await getAdminPrices({
          roomTypeId:
          priceQuery.roomTypeId,

          startDate:
          priceQuery.startDate,

          endDate:
          priceQuery.endDate
        })

    const result = response.data

    if (result.code !== 200) {

      ElMessage.error(
          result.message ||
          '房价加载失败'
      )

      return
    }

    prices.value =
        result.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房价加载失败'
    )

  } finally {

    priceLoading.value = false
  }
}

const loadRoomTypes = async () => {

  try {

    roomTypeLoading.value = true

    const response =
        await getAdminRoomTypes()

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '房型加载失败'
      )
      return
    }

    roomTypes.value =
        result.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房型加载失败'
    )

  } finally {

    roomTypeLoading.value = false
  }
}


const loadRooms = async () => {

  try {

    roomLoading.value = true

    const params = {}

    if (roomQuery.roomTypeId) {
      params.roomTypeId =
          roomQuery.roomTypeId
    }

    if (roomQuery.status) {
      params.status =
          roomQuery.status
    }

    const response =
        await getAdminRooms(params)

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '客房加载失败'
      )
      return
    }

    rooms.value =
        result.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '客房加载失败'
    )

  } finally {

    roomLoading.value = false
  }
}


const resetRoomQuery = () => {

  roomQuery.roomTypeId = null
  roomQuery.status = ''

  loadRooms()
}


const getRoomTypeName = (roomTypeId) => {

  const roomType =
      roomTypes.value.find(
          item =>
              item.roomTypeId === roomTypeId
      )

  return roomType
      ? roomType.typeName
      : `房型 ${roomTypeId}`
}


const getRoomStatusText = (status) => {

  const map = {
    VACANT: '空闲',
    OCCUPIED: '已入住',
    DIRTY: '待清洁',
    CLEANING: '清洁中',
    MAINTENANCE: '维修',
    OUT_OF_SERVICE: '停用'
  }

  return map[status] || status
}


const getRoomStatusType = (status) => {

  const map = {
    VACANT: 'success',
    OCCUPIED: '',
    DIRTY: 'danger',
    CLEANING: 'warning',
    MAINTENANCE: 'warning',
    OUT_OF_SERVICE: 'info'
  }

  return map[status] || 'info'
}

const loadOrders = async () => {

  try {

    orderLoading.value = true

    const params = {
      page: orderQuery.page,
      pageSize: orderQuery.pageSize
    }

    if (orderQuery.orderNo) {
      params.orderNo =
          orderQuery.orderNo.trim()
    }

    if (orderQuery.orderStatus) {
      params.orderStatus =
          orderQuery.orderStatus
    }

    if (orderQuery.paymentStatus) {
      params.paymentStatus =
          orderQuery.paymentStatus
    }

    if (orderQuery.checkInDate) {
      params.checkInDate =
          orderQuery.checkInDate
    }

    const response =
        await getAdminOrders(params)

    const result = response.data

    if (result.code !== 200) {

      ElMessage.error(
          result.message ||
          '订单加载失败'
      )

      return
    }

    orders.value =
        result.data?.records || []

    orderTotal.value =
        result.data?.total || 0

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '订单加载失败'
    )

  } finally {

    orderLoading.value = false
  }
}


const searchOrders = () => {

  orderQuery.page = 1

  loadOrders()
}


const resetOrderQuery = () => {

  orderQuery.page = 1
  orderQuery.pageSize = 10

  orderQuery.orderNo = ''
  orderQuery.orderStatus = ''
  orderQuery.paymentStatus = ''
  orderQuery.checkInDate = ''

  loadOrders()
}


const handleOrderPageChange = (page) => {

  orderQuery.page = page

  loadOrders()
}


const handleOrderPageSizeChange = (pageSize) => {

  orderQuery.pageSize = pageSize
  orderQuery.page = 1

  loadOrders()
}


const openOrderDetail = async (row) => {

  try {

    orderDetailLoading.value = true

    orderDetail.value = null

    orderDetailDialogVisible.value = true

    const response =
        await getAdminOrderDetail(
            row.orderId
        )

    const result = response.data

    if (result.code !== 200) {

      ElMessage.error(
          result.message ||
          '订单详情加载失败'
      )

      return
    }

    orderDetail.value =
        result.data

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '订单详情加载失败'
    )

  } finally {

    orderDetailLoading.value = false
  }
}

const getOrderStatusText = (status) => {

  const map = {
    WAIT_PAY: '待支付',
    CONFIRMED: '已确认',
    NO_SHOW: '未入住',
    CHECKED_IN: '入住中',
    CHECKED_OUT: '已退房',
    COMPLETED: '已完成',
    CANCEL_REQUESTED: '申请取消',
    CANCELLED: '已取消',
    CLOSED: '已关闭',
    REFUNDED: '已退款'
  }

  return map[status] || status
}


const getOrderStatusType = (status) => {

  const map = {
    WAIT_PAY: 'warning',
    CONFIRMED: 'success',
    NO_SHOW: 'warning',
    CHECKED_IN: '',
    CHECKED_OUT: 'info',
    COMPLETED: 'success',
    CANCEL_REQUESTED: 'warning',
    CANCELLED: 'danger',
    CLOSED: 'info',
    REFUNDED: 'info'
  }

  return map[status] || 'info'
}


const getPaymentStatusText = (status) => {

  const map = {
    UNPAID: '未支付',
    PAID: '已支付',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PARTIAL_REFUND: '部分退款'
  }

  return map[status] || status
}


const getPaymentStatusType = (status) => {

  const map = {
    UNPAID: 'warning',
    PAID: 'success',
    REFUNDING: 'warning',
    REFUNDED: 'info',
    PARTIAL_REFUND: 'warning'
  }

  return map[status] || 'info'
}


const getIdTypeText = (type) => {

  const map = {
    ID_CARD: '身份证',
    PASSPORT: '护照',
    OTHER: '其他证件'
  }

  return map[type] || type
}


const formatDateTime = (value) => {

  if (!value) {
    return '-'
  }

  return String(value)
      .replace('T', ' ')
}

const handleLogout = async () => {

  try {

    await ElMessageBox.confirm(
        '确定退出管理员账号吗？',
        '退出登录',
        {
          confirmButtonText: '是',
          cancelButtonText: '否',
          type: 'warning'
        }
    )

    userStore.logout()

    router.push('/login')

  } catch {
    // 用户取消
  }
}


onMounted(async () => {

  initDateRange()

  await loadRoomTypes()

  await loadRooms()

  if (roomTypes.value.length > 0) {

    inventoryQuery.roomTypeId =
        roomTypes.value[0].roomTypeId

    priceQuery.roomTypeId =
        roomTypes.value[0].roomTypeId

    await loadInventories()

    await loadPrices()
  }
})

const handleTabChange = async (name) => {

  /*
   * 订单管理
   */
  if (name === 'orders') {

    if (orders.value.length === 0) {
      await loadOrders()
    }

    return
  }


  /*
   * 其他普通 Tab 不需要额外操作
   */
  if (name !== 'statistics') {
    return
  }


  /*
   * 第一次进入运营看板
   */
  if (dailyStatistics.value.length === 0) {

    await loadStatistics()

    return
  }


  /*
   * ECharts 尺寸重新计算
   */
  await nextTick()

  revenueChart?.resize()

  occupancyChart?.resize()

  operationChart?.resize()
}

const loadStatistics = async () => {

  if (
      !statisticsQuery.startDate ||
      !statisticsQuery.endDate
  ) {

    ElMessage.warning(
        '请选择统计日期'
    )

    return
  }


  try {

    statisticsLoading.value = true


    /*
     * 先重新聚合这段时间
     */
    await refreshStatisticsRange({
      startDate:
      statisticsQuery.startDate,

      endDate:
      statisticsQuery.endDate
    })


    /*
     * 再查询统计结果
     */
    const response =
        await getDailyStatistics({

          startDate:
          statisticsQuery.startDate,

          endDate:
          statisticsQuery.endDate
        })


    dailyStatistics.value =
        response.data.data || []


    /*
     * 等Vue把DOM渲染出来
     */
    await nextTick()


    renderCharts()


  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '经营统计加载失败'
    )

  } finally {

    statisticsLoading.value = false
  }
}

const getLatestStatistics = () => {

  if (
      dailyStatistics.value.length === 0
  ) {
    return null
  }

  return dailyStatistics.value[
  dailyStatistics.value.length - 1
      ]
}

const renderCharts = () => {

  renderRevenueChart()

  renderOccupancyChart()

  renderOperationChart()
}

const renderRevenueChart = () => {

  if (!revenueChartRef.value) {
    return
  }


  if (!revenueChart) {

    revenueChart =
        echarts.init(
            revenueChartRef.value
        )
  }


  revenueChart.setOption({

    title: {
      text: '营业收入趋势'
    },

    tooltip: {
      trigger: 'axis'
    },

    xAxis: {
      type: 'category',

      data:
          dailyStatistics.value.map(
              item => item.statDate
          )
    },

    yAxis: {
      type: 'value',
      name: '元'
    },

    series: [
      {
        name: '总收入',

        type: 'line',

        smooth: true,

        data:
            dailyStatistics.value.map(
                item =>
                    Number(
                        item.totalRevenue
                    )
            )
      }
    ]
  })
}

const renderOccupancyChart = () => {

  if (!occupancyChartRef.value) {
    return
  }


  if (!occupancyChart) {

    occupancyChart =
        echarts.init(
            occupancyChartRef.value
        )
  }


  occupancyChart.setOption({

    title: {
      text: '入住率趋势'
    },

    tooltip: {
      trigger: 'axis'
    },

    xAxis: {
      type: 'category',

      data:
          dailyStatistics.value.map(
              item => item.statDate
          )
    },

    yAxis: {
      type: 'value',
      name: '%',
      max: 100
    },

    series: [
      {
        name: '入住率',

        type: 'line',

        smooth: true,

        data:
            dailyStatistics.value.map(
                item =>
                    (
                        Number(
                            item.occupancyRate
                        ) * 100
                    ).toFixed(2)
            )
      }
    ]
  })
}

const renderOperationChart = () => {

  if (!operationChartRef.value) {
    return
  }


  if (!operationChart) {

    operationChart =
        echarts.init(
            operationChartRef.value
        )
  }


  operationChart.setOption({

    title: {
      text: '每日业务量'
    },

    tooltip: {
      trigger: 'axis'
    },

    legend: {
      data: [
        '新增订单',
        '入住',
        '退房'
      ]
    },

    xAxis: {
      type: 'category',

      data:
          dailyStatistics.value.map(
              item => item.statDate
          )
    },

    yAxis: {
      type: 'value'
    },

    series: [
      {
        name: '新增订单',
        type: 'bar',

        data:
            dailyStatistics.value.map(
                item =>
                    item.newOrderCount
            )
      },

      {
        name: '入住',
        type: 'bar',

        data:
            dailyStatistics.value.map(
                item =>
                    item.checkInCount
            )
      },

      {
        name: '退房',
        type: 'bar',

        data:
            dailyStatistics.value.map(
                item =>
                    item.checkOutCount
            )
      }
    ]
  })
}

onBeforeUnmount(() => {

  revenueChart?.dispose()

  occupancyChart?.dispose()

  operationChart?.dispose()
})

</script>


<style scoped>

.admin-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  padding: 22px 5%;

  display: flex;
  align-items: center;
  justify-content: space-between;

  background: white;
  border-bottom: 1px solid #ebeef5;
}

.header h1 {
  margin: 0 0 5px;
}

.header p {
  margin: 0;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.container {
  width: 96%;
  max-width: none;

  margin: 30px auto;
  padding: 25px;

  background: white;

  border-radius: 10px;
}

.management-tabs :deep(.el-tabs__header.is-left) {
  width: 190px;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
}
.management-tabs :deep(.el-tabs__item.is-left) { justify-content: flex-start; padding-left: 18px; }
.management-tabs :deep(.el-tabs__content) { min-width: 0; padding: 0 12px 0 20px; }
@media (max-width: 760px) {
  .management-tabs :deep(.el-tabs__header.is-left) { width: 125px; }
  .management-tabs :deep(.el-tabs__content) { padding-left: 8px; }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0 0 5px;
}

.section-header p {
  margin: 0;
  color: #909399;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  margin-bottom: 20px;
}

.room-summary {
  margin-bottom: 16px;

  color: #606266;
}

.price-text {
  font-size: 16px;
  color: #303133;
}

.stat-cards {
  display: grid;

  grid-template-columns:
    repeat(4, 1fr);

  gap: 18px;

  margin-bottom: 22px;
}

.stat-title {
  color: #909399;

  margin-bottom: 12px;
}

.stat-value {
  font-size: 28px;

  font-weight: 600;

  color: #303133;
}

.chart-grid {
  display: grid;

  grid-template-columns:
    1fr 1fr;

  gap: 20px;
}

.chart {
  width: 100%;

  height: 350px;
}

.operation-chart {
  height: 400px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;

  margin-top: 20px;
}
</style>
