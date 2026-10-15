<template>
  <section>
    <el-button @click="load" :loading="loading">刷新</el-button>
    <el-table :data="records" v-loading="loading" border style="margin-top: 12px">
      <el-table-column prop="paymentNo" label="支付流水号" min-width="210" />
      <el-table-column prop="orderNo" label="订单号" min-width="190" />
      <el-table-column prop="paymentMethod" label="方式" width="100" />
      <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
      <el-table-column prop="paymentStatus" label="状态" width="105" />
      <el-table-column prop="thirdPartyNo" label="第三方流水" min-width="160" />
      <el-table-column label="支付时间" min-width="170"><template #default="{ row }">{{ row.paidTime ? String(row.paidTime).replace('T', ' ') : '-' }}</template></el-table-column>
    </el-table>
    <AppPagination v-model:current-page="query.page" v-model:page-size="query.pageSize"
                   :total="total" :page-sizes="[10, 20, 50]"
                   style="justify-content: flex-end; margin-top: 15px" @current-change="load" @size-change="load" />
  </section>
</template>
<script setup>
import AppPagination from './AppPagination.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminPayments } from '../api/admin'
const records = ref([]), total = ref(0), loading = ref(false)
const query = reactive({ page: 1, pageSize: 10 })
const load = async () => {
  loading.value = true
  try {
    const { data } = await getAdminPayments(query)
    if (data.code !== 200) throw new Error(data.message)
    records.value = data.data.records || []; total.value = data.data.total || 0
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '支付记录加载失败') }
  finally { loading.value = false }
}
onMounted(load)
</script>
