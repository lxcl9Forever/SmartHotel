<template>
  <div class="page">

    <div class="container">

      <div class="page-header">
        <div>
          <h1>我的订单</h1>
          <p>查看我的酒店预订记录</p>
        </div>

        <el-button @click="router.push('/home')">
          返回首页
        </el-button>
      </div>


      <div
          v-loading="loading"
          class="order-list"
      >

        <el-empty
            v-if="!loading && orders.length === 0"
            description="暂无订单"
        />


        <el-card
            v-for="order in pagedOrders"
            :key="order.orderId"
            class="order-card"
            shadow="hover"
        >

          <div class="order-top">

            <div>
              <div class="order-no">
                订单号：{{ order.orderNo }}
              </div>

              <div class="create-time">
                创建时间：{{ order.createTime }}
              </div>
            </div>


            <el-tag
                :type="getStatusType(order.orderStatus)"
            >
              {{ getStatusText(order.orderStatus) }}
            </el-tag>

          </div>


          <el-divider />


          <div class="order-content">

            <div class="room-info">

              <h2>
                {{ order.roomTypeName }}
              </h2>

              <p>
                入住：
                {{ order.checkInDate }}
              </p>

              <p>
                退房：
                {{ order.checkOutDate }}
              </p>

              <p>
                {{ order.nights }} 晚 ·
                {{ order.roomCount }} 间 ·
                {{ order.guestCount }} 人
              </p>

            </div>


            <div class="order-right">

              <div class="payment-status">
                支付状态：
                {{ getPaymentText(order.paymentStatus) }}
              </div>
              <PaymentCountdown v-if="order.orderStatus === 'WAIT_PAY' && order.expireTime" :expire-time="order.expireTime" />

              <div class="price">
                ¥{{ order.totalAmount }}
              </div>

              <el-button
                  type="primary"
                  @click="viewDetail(order.orderId)"
              >
                查看详情
              </el-button>

            </div>

          </div>

        </el-card>

      </div>
      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="orders.length" />

    </div>

  </div>
</template>


<script setup>
import { computed, onMounted, ref } from 'vue'
import AppPagination from '../components/AppPagination.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getMyBookings } from '../api/booking'
import PaymentCountdown from '../components/PaymentCountdown.vue'


const router = useRouter()

const loading = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const pagedOrders = computed(() => orders.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))


const loadOrders = async () => {

  try {

    loading.value = true

    const response = await getMyBookings()

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '订单加载失败'
      )
      return
    }

    orders.value = result.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '订单加载失败'
    )

  } finally {

    loading.value = false
  }
}


const viewDetail = (orderId) => {
  router.push(`/bookings/${orderId}`)
}


const getStatusText = (status) => {

  const map = {
    WAIT_PAY: '待支付',
    CONFIRMED: '已确认',
    NO_SHOW: '未入住',
    CHECKED_IN: '已入住',
    CHECKED_OUT: '已退房',
    COMPLETED: '已完成',
    CANCEL_REQUESTED: '申请取消',
    CANCELLED: '已取消',
    CLOSED: '已关闭',
    REFUNDED: '已退款'
  }

  return map[status] || status
}


const getStatusType = (status) => {

  const map = {
    WAIT_PAY: 'warning',
    CONFIRMED: 'success',
    NO_SHOW: 'warning',
    CHECKED_IN: 'success',
    CHECKED_OUT: 'info',
    COMPLETED: 'success',
    CANCEL_REQUESTED: 'warning',
    CANCELLED: 'danger',
    CLOSED: 'info',
    REFUNDED: 'info'
  }

  return map[status] || 'info'
}


const getPaymentText = (status) => {

  const map = {
    UNPAID: '未支付',
    PAID: '已支付',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PARTIAL_REFUND: '部分退款'
  }

  return map[status] || status
}


onMounted(() => {
  loadOrders()
})
</script>


<style scoped>
.page {
  min-height: 100vh;
  padding: 50px 20px;
  background: #f5f7fa;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 32px;
}

.page-header p {
  margin: 0;
  color: #909399;
}

.order-list {
  min-height: 300px;
}

.order-card {
  margin-bottom: 20px;
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-weight: 600;
}

.create-time {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.order-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.room-info h2 {
  margin: 0 0 15px;
  font-size: 21px;
}

.room-info p {
  margin: 7px 0;
  color: #606266;
}

.order-right {
  min-width: 180px;

  text-align: right;
}

.payment-status {
  margin-bottom: 10px;

  color: #606266;
}

.price {
  margin-bottom: 18px;

  font-size: 27px;
  font-weight: 700;

  color: #d97706;
}

@media (max-width: 700px) {

  .order-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-right {
    width: 100%;
    margin-top: 20px;

    text-align: left;
  }
}
</style>
