<template>
  <div>
    <div class="toolbar">
      <h2>优惠活动</h2>
      <div>
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建活动</el-button>
      </div>
    </div>
    <el-alert title="活动按下单时间生效；只有已开启且处于有效期内的活动参与新订单计价，多项活动只取优惠最多的一项。" type="info" show-icon :closable="false" />
    <el-table v-loading="loading" :data="pagedItems" border stripe style="margin-top: 16px">
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column label="规则" min-width="170">
        <template #default="{ row }">
          {{ row.type === 'DISCOUNT' ? `${Number(row.discountRate) * 10} 折` : `满 ¥${row.minAmount} 减 ¥${row.reduceAmount}` }}
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" min-width="170" />
      <el-table-column prop="endTime" label="结束时间" min-width="170" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="promotionStatus(row).type">{{ promotionStatus(row).text }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="245">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 'ACTIVE' ? '关闭' : '开启' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="items.length" />

    <el-dialog v-model="visible" :title="editingId ? '编辑优惠活动' : '新建优惠活动'" width="560px">
      <el-form label-width="110px">
        <el-form-item label="活动名称"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="折扣" value="DISCOUNT" />
            <el-option label="满减" value="FULL_REDUCTION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'DISCOUNT'" label="折扣率"><el-input-number v-model="form.discountRate" :min="0.0001" :max="0.9999" :precision="4" :step="0.1" /></el-form-item>
        <template v-else>
          <el-form-item label="满额"><el-input-number v-model="form.minAmount" :min="0.01" :precision="2" /></el-form-item>
          <el-form-item label="减额"><el-input-number v-model="form.reduceAmount" :min="0.01" :precision="2" /></el-form-item>
        </template>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppPagination from './AppPagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPromotions, createPromotion, updatePromotion, deletePromotion, setPromotionEnabled } from '../api/promotion'

const items = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const pagedItems = computed(() => items.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref(null)
const form = reactive({ name: '', type: 'DISCOUNT', discountRate: 0.8, minAmount: 500, reduceAmount: 100, startTime: '', endTime: '' })
const message = error => error.response?.data?.message || '操作失败'
const promotionStatus = row => {
  if (row.status !== 'ACTIVE') return { text: '已关闭', type: 'info' }
  const now = Date.now()
  if (now < new Date(row.startTime).getTime()) return { text: '未开始', type: 'warning' }
  if (now > new Date(row.endTime).getTime()) return { text: '已结束', type: 'info' }
  return { text: '进行中', type: 'success' }
}

async function load() {
  loading.value = true
  try { items.value = (await listPromotions()).data.data || [] }
  catch (error) { ElMessage.error(message(error)) }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', type: 'DISCOUNT', discountRate: 0.8, minAmount: 500, reduceAmount: 100, startTime: '', endTime: '' })
  visible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, type: row.type, discountRate: Number(row.discountRate || 0.8), minAmount: Number(row.minAmount || 500), reduceAmount: Number(row.reduceAmount || 100), startTime: row.startTime?.slice(0, 19), endTime: row.endTime?.slice(0, 19) })
  visible.value = true
}

async function save() {
  if (!form.name.trim() || !form.startTime || !form.endTime) { ElMessage.warning('请填写名称和时间'); return }
  const payload = { name: form.name.trim(), type: form.type, startTime: form.startTime, endTime: form.endTime,
    discountRate: form.type === 'DISCOUNT' ? form.discountRate : null,
    minAmount: form.type === 'FULL_REDUCTION' ? form.minAmount : null,
    reduceAmount: form.type === 'FULL_REDUCTION' ? form.reduceAmount : null }
  saving.value = true
  try {
    if (editingId.value) await updatePromotion(editingId.value, payload)
    else await createPromotion(payload)
    visible.value = false
    ElMessage.success(editingId.value ? '修改成功' : '创建成功；新活动默认关闭')
    await load()
  } catch (error) { ElMessage.error(message(error)) }
  finally { saving.value = false }
}

async function toggle(row) {
  try { await setPromotionEnabled(row.id, row.status !== 'ACTIVE'); ElMessage.success('状态已更新'); await load() }
  catch (error) { ElMessage.error(message(error)) }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.name}”吗？已有订单的优惠记录仍会保留。`, '删除活动')
    await deletePromotion(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) { if (error !== 'cancel') ElMessage.error(message(error)) }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.toolbar h2 { margin: 0; }
</style>
