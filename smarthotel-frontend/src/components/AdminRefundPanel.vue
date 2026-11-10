<template>
  <section>
    <div class="toolbar">
      <el-select v-model="query.status" style="width: 170px" @change="changeStatus">
        <el-option label="待审核" value="PENDING" />
        <el-option label="全部状态" value="" />
        <el-option label="已同意" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
      <el-button :loading="loading" @click="loadRefunds">刷新</el-button>
      <span>共 {{ total }} 条退款申请</span>
    </div>

    <el-table v-loading="loading" :data="refunds" border stripe style="width: 100%">
      <el-table-column prop="refundId" label="申请 ID" width="95" />
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column label="客户" min-width="150">
        <template #default="{ row }">
          <div>{{ row.contactName || row.username }}</div>
          <small>{{ row.username }} · ID {{ row.userId }}</small>
        </template>
      </el-table-column>
      <el-table-column label="退款金额" width="125">
        <template #default="{ row }">¥{{ row.refundAmount }}</template>
      </el-table-column>
      <el-table-column prop="refundReason" label="退款原因" min-width="160" show-overflow-tooltip />
      <el-table-column prop="refundNo" label="退款流水号" min-width="210" show-overflow-tooltip>
        <template #default="{ row }">{{ row.refundNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="库存同步" width="100">
        <template #default="{ row }">
          <span v-if="!row.inventorySyncStatus">-</span>
          <el-tag v-else :type="row.inventorySyncStatus === 'SYNCED' ? 'success' : 'warning'">
            {{ row.inventorySyncStatus === 'SYNCED' ? '已同步' : '待同步' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" min-width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.refundStatus] || 'info'">
            {{ statusText[row.refundStatus] || row.refundStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <template v-if="row.refundStatus === 'PENDING'">
            <el-button type="success" link :loading="submittingId === row.refundId" @click="review(row, true)">同意</el-button>
            <el-button type="danger" link :disabled="submittingId !== null" @click="review(row, false)">拒绝</el-button>
          </template>
          <span v-else>已审核</span>
        </template>
      </el-table-column>
    </el-table>

    <AppPagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      @current-change="loadRefunds"
      @size-change="changePageSize"
    />
  </section>
</template>

<script setup>
import AppPagination from './AppPagination.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveAdminRefund, getAdminRefunds, rejectAdminRefund } from '../api/admin'

const emit = defineEmits(['reviewed'])
const refunds = ref([])
const total = ref(0)
const loading = ref(false)
const submittingId = ref(null)
const query = reactive({ status: 'PENDING', page: 1, pageSize: 10 })
const statusText = { PENDING: '待审核', APPROVED: '已同意', REJECTED: '已拒绝' }
const statusType = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }

const formatTime = value => value ? String(value).replace('T', ' ') : '-'

const loadRefunds = async () => {
  try {
    loading.value = true
    const { data } = await getAdminRefunds(query)
    if (data.code !== 200) throw new Error(data.message || '退款申请加载失败')
    refunds.value = data.data.records || []
    total.value = data.data.total || 0
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '退款申请加载失败')
  } finally {
    loading.value = false
  }
}

const changeStatus = () => {
  query.page = 1
  loadRefunds()
}

const changePageSize = () => {
  query.page = 1
  loadRefunds()
}

const review = async (row, approve) => {
  try {
    await ElMessageBox.confirm(
      approve
        ? `确定同意订单 ${row.orderNo} 的退款申请吗？同意后将按原支付方式退还 ¥${row.refundAmount}。`
        : `确定拒绝订单 ${row.orderNo} 的退款申请吗？`,
      '退款审核',
      { type: approve ? 'warning' : 'info', confirmButtonText: '是', cancelButtonText: '否' }
    )
  } catch {
    return
  }

  try {
    submittingId.value = row.refundId
    const { data } = await (approve ? approveAdminRefund(row.refundId) : rejectAdminRefund(row.refundId))
    if (data.code !== 200) throw new Error(data.message || '审核失败')
    ElMessage.success(approve ? '已同意退款申请' : '已拒绝退款申请')
    await loadRefunds()
    emit('reviewed')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '审核失败')
  } finally {
    submittingId.value = null
  }
}

onMounted(loadRefunds)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.toolbar span { color: #606266; }
.pagination { justify-content: flex-end; margin-top: 20px; }
small { color: #909399; }
</style>
