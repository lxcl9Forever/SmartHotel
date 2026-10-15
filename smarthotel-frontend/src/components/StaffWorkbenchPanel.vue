<template>
  <section>
    <div class="stats">
      <el-card v-for="item in statItems" :key="item.key">
        <div>{{ item.label }}</div><strong>{{ stats[item.key] ?? '-' }}</strong>
      </el-card>
    </div>
    <el-tabs v-model="section">
      <el-tab-pane label="客房检索" name="rooms">
        <div class="filters">
          <el-input v-model="filters.roomNo" placeholder="房号" clearable style="width: 130px" />
          <el-input v-model="filters.floorNo" placeholder="楼层" clearable style="width: 110px" />
          <el-select v-model="filters.roomTypeId" placeholder="房型" clearable style="width: 170px">
            <el-option v-for="type in roomTypes" :key="type.roomTypeId" :label="type.typeName" :value="type.roomTypeId" />
          </el-select>
          <el-select v-model="filters.status" placeholder="房态" clearable style="width: 145px">
            <el-option v-for="status in ['VACANT', 'OCCUPIED', 'DIRTY', 'CLEANING', 'MAINTENANCE', 'OUT_OF_SERVICE']"
                       :key="status" :label="status" :value="status" />
          </el-select>
          <el-button type="primary" @click="loadRooms">查询</el-button>
        </div>
        <el-table :data="pagedRooms" v-loading="roomLoading" border>
          <el-table-column prop="roomNo" label="房号" />
          <el-table-column prop="floorNo" label="楼层" />
          <el-table-column label="房型"><template #default="{ row }">{{ typeName(row.roomTypeId) }}</template></el-table-column>
          <el-table-column prop="status" label="房态" />
        </el-table>
        <AppPagination v-model:current-page="roomPage" v-model:page-size="roomPageSize" :total="rooms.length" />
      </el-tab-pane>
      <el-tab-pane label="入住/退房历史" name="history" lazy>
        <el-button @click="loadHistory" :loading="historyLoading">刷新</el-button>
        <el-table :data="history" v-loading="historyLoading" border style="margin-top: 12px">
          <el-table-column prop="orderNo" label="订单号" min-width="170" />
          <el-table-column prop="contactName" label="联系人" />
          <el-table-column prop="roomNo" label="房号" />
          <el-table-column prop="floorNo" label="楼层" />
          <el-table-column prop="roomTypeName" label="房型" min-width="125" />
          <el-table-column label="入住时间" min-width="165"><template #default="{ row }">{{ format(row.actualCheckInTime) }}</template></el-table-column>
          <el-table-column label="退房时间" min-width="165"><template #default="{ row }">{{ format(row.actualCheckOutTime) }}</template></el-table-column>
          <el-table-column prop="checkInStaff" label="入住经办" />
          <el-table-column prop="checkOutStaff" label="退房经办" />
        </el-table>
        <AppPagination v-model:current-page="query.page" v-model:page-size="query.pageSize"
                       :total="total" :page-sizes="[10, 20, 50]"
                       style="justify-content: flex-end; margin-top: 15px" @current-change="loadHistory" @size-change="loadHistory" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
<script setup>
import AppPagination from './AppPagination.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getWorkbenchStats, getStayHistory, searchStaffRooms } from '../api/staff'
import { getRoomTypes } from '../api/room'
const section = ref('rooms')
const stats = ref({}), rooms = ref([]), roomTypes = ref([]), history = ref([])
const roomPage = ref(1), roomPageSize = ref(10)
const pagedRooms = computed(() => rooms.value.slice((roomPage.value - 1) * roomPageSize.value, roomPage.value * roomPageSize.value))
const roomLoading = ref(false), historyLoading = ref(false), total = ref(0)
const filters = reactive({ roomNo: '', floorNo: '', roomTypeId: null, status: '' })
const query = reactive({ page: 1, pageSize: 10 })
const statItems = [
  { key: 'pendingCheckIn', label: '待入住订单' }, { key: 'occupied', label: '在住房间' },
  { key: 'dirty', label: '待清洁房间' }, { key: 'todayCheckIns', label: '今日入住' },
  { key: 'todayCheckOuts', label: '今日退房' }
]
const format = value => value ? String(value).replace('T', ' ') : '-'
const typeName = id => roomTypes.value.find(item => item.roomTypeId === id)?.typeName || id
const loadRooms = async () => {
  if (filters.floorNo && (!/^\d+$/.test(filters.floorNo) || Number(filters.floorNo) < 1)) {
    ElMessage.warning('楼层请输入正整数'); return
  }
  roomLoading.value = true
  try {
    const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v !== null))
    const { data } = await searchStaffRooms(params)
    if (data.code !== 200) throw new Error(data.message)
    rooms.value = data.data || []
    roomPage.value = 1
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '客房加载失败') }
  finally { roomLoading.value = false }
}
const loadHistory = async () => {
  historyLoading.value = true
  try {
    const { data } = await getStayHistory(query)
    if (data.code !== 200) throw new Error(data.message)
    history.value = data.data.records || []
    total.value = data.data.total || 0
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '历史加载失败') }
  finally { historyLoading.value = false }
}
watch(section, value => { if (value === 'history') loadHistory() })
onMounted(async () => {
  try {
    const [s, t] = await Promise.all([getWorkbenchStats(), getRoomTypes()])
    stats.value = s.data.data || {}
    roomTypes.value = t.data.data || []
  } catch (error) { ElMessage.error(error.response?.data?.message || '统计加载失败') }
  loadRooms()
})
</script>
<style scoped>
.stats { display: grid; grid-template-columns: repeat(5, minmax(110px, 1fr)); gap: 12px; margin-bottom: 22px; }
.stats strong { font-size: 25px; color: #409eff; }
.filters { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
</style>
