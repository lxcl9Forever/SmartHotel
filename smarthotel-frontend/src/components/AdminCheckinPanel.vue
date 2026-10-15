<template>
  <section>
    <div class="panel-heading">
      <div>
        <h2>每日签到管理</h2>
        <p>调整后只影响今后领取的优惠券；已发放的金额保持不变。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rewards" border stripe>
      <el-table-column label="连续签到" width="150">
        <template #default="{ row }">第 {{ row.dayNumber }} 天</template>
      </el-table-column>
      <el-table-column label="无门槛券金额" min-width="220">
        <template #default="{ row }">
          <el-input-number v-model="drafts[row.dayNumber]" :min="0.01" :max="10000"
                           :precision="2" :step="10" /> 元
        </template>
      </el-table-column>
      <el-table-column prop="claimCount" label="累计领取次数" width="170" />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button type="primary" link :loading="saving === row.dayNumber" @click="save(row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="footnote">连续签到第 7 天后，次日从第 1 天重新开始；漏签后也从第 1 天开始。签到券领取后 24 小时内有效，可与自动优惠活动叠加，每笔订单限用一张。</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCheckinRewards, updateCheckinReward } from '../api/checkin'

const rewards = ref([])
const drafts = reactive({})
const loading = ref(false)
const saving = ref(null)

async function load() {
  loading.value = true
  try {
    const { data } = await getCheckinRewards()
    if (data.code !== 200) throw new Error(data.message)
    rewards.value = data.data || []
    rewards.value.forEach(item => { drafts[item.dayNumber] = Number(item.amount) })
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '加载签到奖励失败')
  } finally {
    loading.value = false
  }
}

async function save(row) {
  saving.value = row.dayNumber
  try {
    await updateCheckinReward(row.dayNumber, drafts[row.dayNumber])
    ElMessage.success('奖励金额已更新')
    await load()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.panel-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.panel-heading h2 { margin: 0 0 8px; }
.panel-heading p, .footnote { color: #667085; line-height: 1.6; }
.panel-heading p { margin: 0; }
.footnote { margin: 18px 0 0; }
</style>
