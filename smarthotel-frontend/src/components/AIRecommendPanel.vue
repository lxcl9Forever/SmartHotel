<template>
  <section class="ai-panel">
    <div class="ai-heading">
      <div>
        <h2>智能选房</h2>
        <p>描述入住人数、天数、预算和偏好，查看符合实际价格与库存的房型。</p>
      </div>
    </div>
    <el-input v-model="description" type="textarea" :rows="3" maxlength="500" show-word-limit
      placeholder="例如：一家三口住三晚，预算1000，希望安静" />
    <div class="ai-actions">
      <el-date-picker v-model="checkInDate" type="date" value-format="YYYY-MM-DD"
        placeholder="入住日期（默认明天）" />
      <el-button type="primary" :loading="loading" @click="recommend">获取推荐</el-button>
    </div>
    <template v-if="result">
      <p class="ai-summary">
        {{ result.parsed.people }}人 · {{ result.parsed.days }}晚
        <span v-if="result.parsed.budget"> · 预算 ¥{{ result.parsed.budget }}</span>
        · {{ result.checkInDate }} 至 {{ result.checkOutDate }}
        · AI 模型解析
      </p>
      <div class="ai-reply" role="status">
        <strong>智能选房助手</strong>
        <p>{{ result.reply }}</p>
      </div>
      <el-empty v-if="!result.recommendations.length && !result.budgetAlternative"
        description="请调整日期或入住人数后重新查询" />
      <div v-else class="ai-results">
        <div v-for="room in result.recommendations" :key="room.roomTypeId" class="ai-result">
          <strong>{{ room.roomTypeName }}</strong>
          <span class="ai-price">合计 ¥{{ room.totalAmount }}</span>
          <p>{{ room.reason }}</p>
          <el-button type="primary" size="small" @click="$emit('select', result, room, false)">填写预订信息</el-button>
        </div>
        <div v-if="result.budgetAlternative" class="ai-result">
          <strong>{{ result.budgetAlternative.room.roomTypeName }}</strong>
          <span class="ai-price">{{ result.parsed.days }}晚合计 ¥{{ result.budgetAlternative.room.totalAmount }}</span>
          <p>{{ result.budgetAlternative.room.reason }}</p>
          <el-button type="primary" size="small"
            @click="$emit('select', result, result.budgetAlternative.room, false)">按原入住晚数预订</el-button>
          <el-button v-if="result.budgetAlternative.shorterCheckOutDate" size="small"
            @click="$emit('select', result, result.budgetAlternative.room, true)">
            少住一晚（¥{{ result.budgetAlternative.shorterTotalAmount }}）
          </el-button>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { recommendRooms } from '../api/ai'

defineEmits(['select'])
const description = ref('')
const checkInDate = ref('')
const loading = ref(false)
const result = ref(null)

async function recommend() {
  if (!description.value.trim()) {
    ElMessage.warning('请先描述选房需求')
    return
  }
  loading.value = true
  result.value = null
  try {
    const { data } = await recommendRooms({
      text: description.value.trim(),
      checkInDate: checkInDate.value || null
    })
    if (data.code !== 200) throw new Error(data.message)
    if (!data.data || typeof data.data.reply !== 'string'
        || !Object.prototype.hasOwnProperty.call(data.data, 'budgetAlternative')) {
      throw new Error('后端仍在返回旧版智能选房结果，请在 IDEA 停止并重新运行后端后重试')
    }
    result.value = data.data
  } catch (error) {
    const message = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
      ? '智能选房响应超时，请稍后重试'
      : error.response?.data?.message || error.message || '推荐失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-panel { width: 86%; max-width: 1180px; margin: 20px auto; padding: 24px; background: white; border-radius: 12px; }
.ai-heading h2 { margin: 0 0 8px; }
.ai-heading p { color: #667085; margin: 0 0 16px; }
.ai-actions { display: flex; gap: 12px; margin-top: 12px; flex-wrap: wrap; }
.ai-summary { margin: 18px 0 8px; color: #475467; }
.ai-reply { margin: 16px 0; padding: 16px 20px; border: 1px solid #eaecf0; border-radius: 10px; background: #fff; color: #1f2937; }
.ai-reply p { margin: 8px 0 0; line-height: 1.7; }
.ai-results { display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr)); gap: 12px; }
.ai-result { border: 1px solid #eaecf0; border-radius: 8px; padding: 16px; }
.ai-result p { min-height: 72px; line-height: 1.6; color: #475467; }
.ai-result .el-button { margin: 4px 8px 0 0; }
.ai-price { float: right; color: #e65b45; font-weight: 600; }
</style>
