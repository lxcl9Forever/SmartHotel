<template>
  <el-card class="review-card" v-loading="loading">
    <template #header>入住评价</template>
    <template v-if="review">
      <el-rate :model-value="review.rating" disabled show-score />
      <p class="review-content">{{ review.content }}</p>
      <el-tag :type="review.status === 'VISIBLE' ? 'success' : 'warning'">
        {{ review.status === 'VISIBLE' ? '已发布' : '管理员已隐藏' }}
      </el-tag>
    </template>
    <template v-else-if="canReview">
      <el-rate v-model="rating" show-score />
      <el-input v-model="content" type="textarea" :rows="4" maxlength="500" show-word-limit
                placeholder="写下这次入住体验" />
      <el-button type="primary" :loading="submitting" :disabled="!rating || !content.trim()"
                 @click="submit">提交评价</el-button>
    </template>
    <el-empty v-else description="退房后可以评价本次入住" :image-size="72" />
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createReview, getMyReview } from '../api/review'

const props = defineProps({ orderId: { type: [Number, String], required: true }, orderStatus: String })
const review = ref(null)
const loading = ref(false)
const submitting = ref(false)
const rating = ref(0)
const content = ref('')
const canReview = ref(false)

watch(() => [props.orderId, props.orderStatus], async () => {
  canReview.value = ['CHECKED_OUT', 'COMPLETED'].includes(props.orderStatus)
  loading.value = true
  try {
    const { data } = await getMyReview(props.orderId)
    if (data.code !== 200) throw new Error(data.message)
    review.value = data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '评价加载失败')
  } finally {
    loading.value = false
  }
}, { immediate: true })

const submit = async () => {
  if (submitting.value || !rating.value || !content.value.trim()) return
  submitting.value = true
  try {
    const { data } = await createReview(props.orderId, { rating: rating.value, content: content.value.trim() })
    if (data.code !== 200) throw new Error(data.message)
    const response = await getMyReview(props.orderId)
    review.value = response.data.data
    ElMessage.success('评价提交成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '评价提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.review-card { margin-top: 20px; }
.review-content { white-space: pre-wrap; overflow-wrap: anywhere; }
.el-textarea { margin: 12px 0; }
</style>
