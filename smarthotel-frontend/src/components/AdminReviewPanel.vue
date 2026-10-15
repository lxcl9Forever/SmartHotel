<template>
  <section>
    <div class="toolbar">
      <el-select v-model="query.status" style="width: 150px" @change="changeStatus">
        <el-option label="全部评价" value="" />
        <el-option label="已发布" value="VISIBLE" />
        <el-option label="已隐藏" value="HIDDEN" />
      </el-select>
      <el-button @click="loadReviews" :loading="loading">刷新</el-button>
    </div>
    <el-table :data="reviews" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="customerName" label="客户" min-width="110" />
      <el-table-column label="评分" width="150">
        <template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
      <el-table-column label="时间" min-width="175">
        <template #default="{ row }">{{ String(row.createTime || '').replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'VISIBLE' ? 'success' : 'warning'">
            {{ row.status === 'VISIBLE' ? '已发布' : '已隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="105">
        <template #default="{ row }">
          <el-button link :type="row.status === 'VISIBLE' ? 'danger' : 'primary'"
                     :loading="updatingId === row.reviewId" @click="toggle(row)">
            {{ row.status === 'VISIBLE' ? '隐藏' : '恢复' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <AppPagination v-model:current-page="query.page" v-model:page-size="query.pageSize"
                   :total="total" :page-sizes="[10, 20, 50]" class="pagination"
                   @current-change="loadReviews" @size-change="changeStatus" />
  </section>
</template>

<script setup>
import AppPagination from './AppPagination.vue'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReviews, hideReview, showReview } from '../api/review'

const reviews = ref([])
const total = ref(0)
const loading = ref(false)
const updatingId = ref(null)
const query = reactive({ status: '', page: 1, pageSize: 10 })

const loadReviews = async () => {
  loading.value = true
  try {
    const { data } = await getAdminReviews(query)
    if (data.code !== 200) throw new Error(data.message)
    reviews.value = data.data.records || []
    total.value = data.data.total || 0
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '评价加载失败')
  } finally {
    loading.value = false
  }
}

const changeStatus = () => { query.page = 1; loadReviews() }

const toggle = async row => {
  const hiding = row.status === 'VISIBLE'
  try {
    await ElMessageBox.confirm(`确定${hiding ? '隐藏' : '恢复'}这条评价吗？`, '评价管理')
  } catch { return }
  updatingId.value = row.reviewId
  try {
    const { data } = await (hiding ? hideReview(row.reviewId) : showReview(row.reviewId))
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success(hiding ? '评价已隐藏' : '评价已恢复')
    await loadReviews()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '操作失败')
  } finally {
    updatingId.value = null
  }
}

onMounted(loadReviews)
</script>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 18px; }
.pagination { justify-content: flex-end; margin-top: 20px; }
</style>
