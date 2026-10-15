<template>
  <div>
    <div class="toolbar"><h2>每日优惠 · 限量秒杀</h2><div><el-button @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新建秒杀</el-button></div></div>
    <el-alert title="设置房型、入住日期、限量间数和折扣率。活动需手动开启；已产生订单的活动只可开关，修改规则请新建活动。秒杀不叠加优惠券或其他活动。" type="info" show-icon :closable="false" />
    <el-table v-loading="loading" :data="pagedItems" border stripe style="margin-top: 16px">
      <el-table-column prop="id" label="活动 ID" width="95" />
      <el-table-column label="房型" min-width="140"><template #default="{ row }">{{ roomName(row.roomTypeId) }}</template></el-table-column>
      <el-table-column label="入住日期" min-width="205"><template #default="{ row }">{{ row.checkInDate }} 至 {{ row.checkOutDate }}</template></el-table-column>
      <el-table-column prop="quota" label="限量间数" width="100" />
      <el-table-column label="折扣" width="90"><template #default="{ row }">{{ Number(row.discountRate) * 10 }} 折</template></el-table-column>
      <el-table-column label="秒杀时间" min-width="280"><template #default="{ row }">{{ time(row.startTime) }} 至 {{ time(row.endTime) }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '已开启' : '已关闭' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="{ row }">
        <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 'ACTIVE' ? '关闭' : '开启' }}</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="items.length" />

    <el-dialog v-model="visible" :title="editingId ? '编辑秒杀' : '新建秒杀'" width="min(620px, 94vw)">
      <el-form label-width="110px">
        <el-form-item label="秒杀房型"><el-select v-model="form.roomTypeId" placeholder="选择房型" style="width: 100%"><el-option v-for="room in rooms" :key="room.roomTypeId" :label="room.typeName" :value="room.roomTypeId" /></el-select></el-form-item>
        <el-form-item label="入住日期"><el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="退房日期"><el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="限量间数"><el-input-number v-model="form.quota" :min="1" :max="10000" /></el-form-item>
        <el-form-item label="折扣率"><el-input-number v-model="form.discountRate" :min="0.0001" :max="0.9999" :precision="4" :step="0.1" /><span class="help">0.8 表示八折</span></el-form-item>
        <el-form-item label="秒杀开始"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="秒杀结束"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppPagination from './AppPagination.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoomTypes } from '../api/room'
import { listAdminDailyDeals, createDailyDeal, updateDailyDeal, setDailyDealEnabled, deleteDailyDeal } from '../api/dailyDeal'

const items = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const pagedItems = computed(() => items.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const rooms = ref([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref(null)
const empty = () => ({ roomTypeId: null, checkInDate: '', checkOutDate: '', quota: 1, discountRate: 0.8, startTime: '', endTime: '' })
const form = reactive(empty())
const message = error => error.response?.data?.message || error.message || '操作失败'
const time = value => value?.replace('T', ' ').slice(0, 16)
const roomName = id => rooms.value.find(room => room.roomTypeId === id)?.typeName || `房型 ${id}`

async function load() {
  loading.value = true
  try {
    const [deals, roomTypes] = await Promise.all([listAdminDailyDeals(), getRoomTypes()])
    items.value = deals.data.data || []
    rooms.value = roomTypes.data.data || []
  } catch (error) { ElMessage.error(message(error)) }
  finally { loading.value = false }
}
function openCreate() { editingId.value = null; Object.assign(form, empty()); visible.value = true }
function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, { roomTypeId: row.roomTypeId, checkInDate: row.checkInDate, checkOutDate: row.checkOutDate,
    quota: row.quota, discountRate: Number(row.discountRate), startTime: row.startTime?.slice(0, 19), endTime: row.endTime?.slice(0, 19) })
  visible.value = true
}
async function save() {
  if (!form.roomTypeId || !form.checkInDate || !form.checkOutDate || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写房型、入住日期及秒杀时间'); return
  }
  saving.value = true
  try {
    if (editingId.value) await updateDailyDeal(editingId.value, { ...form })
    else await createDailyDeal({ ...form })
    visible.value = false
    ElMessage.success(editingId.value ? '修改成功' : '创建成功，请开启活动')
    await load()
  } catch (error) { ElMessage.error(message(error)) }
  finally { saving.value = false }
}
async function toggle(row) {
  try { await setDailyDealEnabled(row.id, row.status !== 'ACTIVE'); await load() }
  catch (error) { ElMessage.error(message(error)) }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除 ${roomName(row.roomTypeId)} 的秒杀吗？`, '删除秒杀')
    await deleteDailyDeal(row.id)
    await load()
  } catch (error) { if (error !== 'cancel') ElMessage.error(message(error)) }
}
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; }
.toolbar h2 { margin: 0; }
.help { margin-left: 12px; color: #667085; }
</style>
