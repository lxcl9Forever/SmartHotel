<template>
  <div class="staff-page">

    <!-- 顶部 -->
    <header class="header">
      <div>
        <h1>SmartHotel 前台工作台</h1>
        <p>入住、退房与客房状态管理</p>
      </div>

      <div class="header-right">
        <span>
          当前员工：
          {{ userStore.nickname || userStore.username }}
        </span>

        <el-button
            type="danger"
            plain
            @click="handleLogout"
        >
          退出登录
        </el-button>
      </div>
    </header>


    <main class="container">

      <el-tabs
          class="management-tabs"
          tab-position="left"
          v-model="activeTab"
          @tab-change="handleTabChange"
      >

        <el-tab-pane label="工作台概览" name="overview" lazy>
          <StaffWorkbenchPanel />
        </el-tab-pane>

        <!-- 待入住 -->
        <el-tab-pane
            label="待入住订单"
            name="confirmed"
        >

          <div class="no-show-actions">
            <span>已过入住日期的订单可标记为未入住，订单和支付记录会保留。</span>
            <el-button
                type="warning"
                plain
                :disabled="!overdueConfirmedCount || noShowSubmitting"
                :loading="noShowSubmitting"
                @click="handleMarkOverdueNoShows"
            >
              批量标记过期订单（{{ overdueConfirmedCount }}）
            </el-button>
          </div>

          <el-table
              v-loading="loading"
              :data="pagedList(confirmedOrders, 'confirmedOrders')"
              border
          >

            <el-table-column
                prop="orderNo"
                label="订单号"
                min-width="240"
            />

            <el-table-column
                prop="roomTypeName"
                label="房型"
                width="170"
            />

            <el-table-column
                label="入住日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkInDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="退房日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkOutDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="数量"
                width="90"
            >
              <template #default="{ row }">
                {{ row.roomCount }} 间
              </template>
            </el-table-column>

            <el-table-column
                prop="contactName"
                label="联系人"
                width="120"
            />

            <el-table-column
                prop="contactPhone"
                label="联系电话"
                width="140"
            />

            <el-table-column
                label="状态"
                width="110"
            >
              <template #default>
                <el-tag type="success">
                  已确认
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="225"

            >
              <template #default="{ row }">
                <el-button
                    type="primary"
                    size="small"
                    @click="openCheckIn(row)"
                >
                  办理入住
                </el-button>
                <el-button
                    v-if="isOverdueConfirmed(row)"
                    type="warning"
                    size="small"
                    :disabled="noShowSubmitting"
                    @click="handleMarkNoShow(row)"
                >
                  标记未入住
                </el-button>
              </template>
            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.confirmedOrders.page" v-model:page-size="listPages.confirmedOrders.pageSize" :total="confirmedOrders.length" />

        </el-tab-pane>


        <!-- 已入住 -->
        <el-tab-pane
            label="已入住订单"
            name="checkedIn"
        >

          <el-table
              v-loading="loading"
              :data="pagedList(checkedInOrders, 'checkedInOrders')"
              border
          >

            <el-table-column
                prop="orderNo"
                label="订单号"
                min-width="240"
            />

            <el-table-column
                prop="roomTypeName"
                label="房型"
                width="170"
            />

            <el-table-column
                prop="contactName"
                label="联系人"
                width="120"
            />

            <el-table-column
                label="入住日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkInDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="退房日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkOutDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="金额"
                width="120"
            >
              <template #default="{ row }">
                ¥{{ row.totalAmount }}
              </template>
            </el-table-column>

            <el-table-column
                label="状态"
                width="110"
            >
              <template #default>
                <el-tag>
                  已入住
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="130"

            >
              <template #default="{ row }">
                <el-button
                    type="primary"
                    size="small"
                    @click="openExtend(row)"
                >
                  续住
                </el-button>


                <el-button
                    type="warning"
                    size="small"
                    @click="openCheckOut(row)"
                >
                  办理退房
                </el-button>
              </template>
            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.checkedInOrders.page" v-model:page-size="listPages.checkedInOrders.pageSize" :total="checkedInOrders.length" />

        </el-tab-pane>

        <el-tab-pane
            label="已退房订单"
            name="CHECKED_OUT"
        >
          <el-table
              v-loading="loading"
              :data="pagedList(checkedOutOrders, 'checkedOutOrders')"
              border
          >

            <el-table-column
                prop="orderNo"
                label="订单号"
                min-width="240"
            />

            <el-table-column
                prop="roomTypeName"
                label="房型"
                width="170"
            />

            <el-table-column
                prop="contactName"
                label="联系人"
                width="120"
            />

            <el-table-column
                label="入住日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkInDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="退房日期"
                width="120"
            >
              <template #default="{ row }">
                {{ row.checkOutDate }}
              </template>
            </el-table-column>

            <el-table-column
                label="金额"
                width="120"
            >
              <template #default="{ row }">
                ¥{{ row.totalAmount }}
              </template>
            </el-table-column>

            <el-table-column
                label="状态"
                width="110"
            >
              <template #default>
                <el-tag>
                  已入住
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="130"

            >
              <template #default="{ row }">
                <el-button
                    type="success"
                    @click="handleComplete(row)"
                >
                  完成订单
                </el-button>
              </template>
            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.checkedOutOrders.page" v-model:page-size="listPages.checkedOutOrders.pageSize" :total="checkedOutOrders.length" />
        </el-tab-pane>

        <!-- 待清洁 -->
        <el-tab-pane
            label="待清洁房间"
            name="dirty"
        >

          <el-table
              v-loading="loading"
              :data="pagedList(dirtyRooms, 'dirtyRooms')"
              border
          >

            <el-table-column
                prop="roomNo"
                label="房号"
            />

            <el-table-column
                prop="floorNo"
                label="楼层"
            />

            <el-table-column
                prop="roomTypeId"
                label="房型ID"
            />

            <el-table-column label="房态">
              <template #default>
                <el-tag type="danger">
                  待清洁
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="140"
            >
              <template #default="{ row }">
                <el-button
                    type="primary"
                    size="small"
                    @click="handleStartCleaning(row)"
                >
                  开始清洁
                </el-button>
              </template>
            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.dirtyRooms.page" v-model:page-size="listPages.dirtyRooms.pageSize" :total="dirtyRooms.length" />

        </el-tab-pane>


        <!-- 清洁中 -->
        <el-tab-pane
            label="清洁中房间"
            name="cleaning"
        >

          <el-table
              v-loading="loading"
              :data="pagedList(cleaningRooms, 'cleaningRooms')"
              border
          >

            <el-table-column
                prop="roomNo"
                label="房号"
            />

            <el-table-column
                prop="floorNo"
                label="楼层"
            />

            <el-table-column
                prop="roomTypeId"
                label="房型ID"
            />

            <el-table-column label="房态">
              <template #default>
                <el-tag type="warning">
                  清洁中
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="140"
            >
              <template #default="{ row }">
                <el-button
                    type="success"
                    size="small"
                    @click="handleFinishCleaning(row)"
                >
                  清洁完成
                </el-button>
              </template>
            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.cleaningRooms.page" v-model:page-size="listPages.cleaningRooms.pageSize" :total="cleaningRooms.length" />

        </el-tab-pane>

        <!-- 空闲房间 -->
        <el-tab-pane
            label="空闲房间"
            name="vacant"
        >

          <el-table
              :data="pagedVacantRooms"
              border
              style="width: 100%"
          >

            <el-table-column
                prop="roomNo"
                label="房号"
                width="120"
            />

            <el-table-column
                prop="floorNo"
                label="楼层"
                width="100"
            />

            <el-table-column
                prop="roomTypeId"
                label="房型ID"
                width="100"
            />

            <el-table-column
                label="状态"
                width="110"
            >
              <template #default>
                <el-tag type="success">
                  空闲
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                min-width="220"
            >

              <template #default="{ row }">

                <el-button
                    type="warning"
                    size="small"
                    @click="
              openRoomStatusDialog(
                row,
                'MAINTENANCE'
              )
            "
                >
                  设为维修
                </el-button>

                <el-button
                    type="danger"
                    size="small"
                    @click="
              openRoomStatusDialog(
                row,
                'OUT_OF_SERVICE'
              )
            "
                >
                  停用
                </el-button>

              </template>

            </el-table-column>

          </el-table>

          <div
              style="
      display: flex;
      justify-content: flex-end;
      margin-top: 20px;
    "
          >
            <AppPagination
                v-model:current-page="vacantCurrentPage"
                v-model:page-size="vacantPageSize"
                :page-sizes="[10, 20, 50]"
                :total="vacantRooms.length"
            />
          </div>

        </el-tab-pane>

        <!-- 维修中房间 -->
        <el-tab-pane
            label="维修中房间"
            name="maintenance"
        >

          <el-table
              :data="pagedList(maintenanceRooms, 'maintenanceRooms')"
              border
              style="width: 100%"
          >

            <el-table-column
                prop="roomNo"
                label="房号"
                width="120"
            />

            <el-table-column
                prop="floorNo"
                label="楼层"
                width="100"
            />

            <el-table-column
                prop="roomTypeId"
                label="房型ID"
                width="100"
            />

            <el-table-column
                prop="remark"
                label="维修原因"
                min-width="220"
            />

            <el-table-column
                label="状态"
                width="120"
            >
              <template #default>
                <el-tag type="warning">
                  维修中
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="140"
            >

              <template #default="{ row }">

                <el-button
                    type="success"
                    size="small"
                    @click="
              openRoomStatusDialog(
                row,
                'RESTORE'
              )
            "
                >
                  恢复空闲
                </el-button>

              </template>

            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.maintenanceRooms.page" v-model:page-size="listPages.maintenanceRooms.pageSize" :total="maintenanceRooms.length" />

        </el-tab-pane>

        <!-- 停用房间 -->
        <el-tab-pane
            label="停用房间"
            name="outOfService"
        >

          <el-table
              :data="pagedList(outOfServiceRooms, 'outOfServiceRooms')"
              border
              style="width: 100%"
          >

            <el-table-column
                prop="roomNo"
                label="房号"
                width="120"
            />

            <el-table-column
                prop="floorNo"
                label="楼层"
                width="100"
            />

            <el-table-column
                prop="roomTypeId"
                label="房型ID"
                width="100"
            />

            <el-table-column
                prop="remark"
                label="停用原因"
                min-width="220"
            />

            <el-table-column
                label="状态"
                width="120"
            >
              <template #default>
                <el-tag type="danger">
                  停用
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
                label="操作"
                width="140"
            >

              <template #default="{ row }">

                <el-button
                    type="success"
                    size="small"
                    @click="
              openRoomStatusDialog(
                row,
                'RESTORE'
              )
            "
                >
                  恢复空闲
                </el-button>

              </template>

            </el-table-column>

          </el-table>
          <AppPagination v-model:current-page="listPages.outOfServiceRooms.page" v-model:page-size="listPages.outOfServiceRooms.pageSize" :total="outOfServiceRooms.length" />

        </el-tab-pane>

      </el-tabs>

    </main>


    <!-- 办理入住弹窗 -->
    <el-dialog
        v-model="checkInDialogVisible"
        title="办理入住"
        width="600px"
    >

      <template v-if="selectedOrder">

        <el-descriptions
            :column="2"
            border
            style="margin-bottom: 22px"
        >

          <el-descriptions-item label="订单">
            {{ selectedOrder.orderNo }}
          </el-descriptions-item>

          <el-descriptions-item label="房型">
            {{ selectedOrder.roomTypeName }}
          </el-descriptions-item>

          <el-descriptions-item label="房间数量">
            {{ selectedOrder.roomCount }} 间
          </el-descriptions-item>

          <el-descriptions-item label="入住人数">
            {{ selectedOrder.guestCount }} 人
          </el-descriptions-item>

        </el-descriptions>

        <!-- 入住人核验 -->
        <el-divider content-position="left">
          入住人核验
        </el-divider>


        <el-alert
            title="请核对入住人姓名与有效证件，确认无误后再办理入住"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 16px"
        />


        <el-table
            :data="orderGuests"
            border
            size="small"
            style="width: 100%; margin-bottom: 18px"
        >

          <el-table-column
              label="入住人"
              min-width="130"
          >

            <template #default="{ row }">

      <span>
        {{ row.guestName }}
      </span>

              <el-tag
                  v-if="row.isPrimary === 1"
                  type="success"
                  size="small"
                  style="margin-left: 6px"
              >
                主要入住人
              </el-tag>

            </template>

          </el-table-column>


          <el-table-column
              prop="phone"
              label="手机号"
              min-width="130"
          />


          <el-table-column
              label="证件类型"
              width="100"
          >

            <template #default="{ row }">
              {{ getIdTypeText(row.idType) }}
            </template>

          </el-table-column>


          <el-table-column
              prop="idNumber"
              label="证件号码"
              min-width="180"
          />

        </el-table>


        <el-checkbox
            v-model="checkInForm.guestVerified"
            style="margin-bottom: 22px"
        >
          已核验入住人身份信息，与有效证件一致
        </el-checkbox>

        <el-form label-width="90px">

          <el-form-item label="分配房间">

            <el-select
                v-model="checkInForm.roomIds"
                multiple
                placeholder="请选择具体房号"
                style="width: 100%"
            >

              <el-option
                  v-for="room in availableRooms"
                  :key="room.roomId"
                  :label="`${room.roomNo}（${room.floorNo}楼）`"
                  :value="room.roomId"
              />

            </el-select>

          </el-form-item>


          <el-form-item label="备注">
            <el-input
                v-model="checkInForm.remark"
                type="textarea"
                :rows="3"
            />
          </el-form-item>

        </el-form>

      </template>


      <template #footer>

        <el-button
            @click="checkInDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
            type="primary"
            :loading="submitting"
            @click="submitCheckIn"
        >
          确认入住
        </el-button>

      </template>

    </el-dialog>


    <!-- 退房弹窗 -->
    <el-dialog
        v-model="checkOutDialogVisible"
        title="办理退房"
        width="520px"
    >

      <template v-if="selectedOrder">

        <el-descriptions
            :column="1"
            border
            style="margin-bottom: 22px"
        >

          <el-descriptions-item label="订单">
            {{ selectedOrder.orderNo }}
          </el-descriptions-item>

          <el-descriptions-item label="房型">
            {{ selectedOrder.roomTypeName }}
          </el-descriptions-item>

          <el-descriptions-item label="订单金额">
            ¥{{ selectedOrder.totalAmount }}
          </el-descriptions-item>

        </el-descriptions>


        <el-form label-width="110px">

          <el-form-item label="备注">

            <el-input
                v-model="checkOutForm.remark"
                type="textarea"
                :rows="3"
            />

          </el-form-item>

        </el-form>

      </template>


      <template #footer>

        <el-button
            @click="checkOutDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
            type="warning"
            :loading="submitting"
            @click="submitCheckOut"
        >
          确认退房
        </el-button>

      </template>

    </el-dialog>

    <el-dialog
        v-model="extendDialogVisible"
        title="办理续住"
        width="450px"
    >

      <el-form>

        <el-form-item label="当前退房日期">

          {{ selectedOrder?.checkOutDate }}

        </el-form-item>


        <el-form-item label="新的退房日期">

          <el-date-picker
              v-model="extendForm.newCheckOutDate"
              type="date"
              placeholder="选择日期"
              :disabled-date="disabledExtendDate"
          />

        </el-form-item>


      </el-form>


      <div v-if="extendPreview">

        <p>
          增加晚数：
          {{extendPreview.extraNights}}晚
        </p>


        <p>
          增加金额：
          ¥{{extendPreview.extraAmount}}
        </p>

      </div>


      <template #footer>

        <el-button
            @click="previewExtend"
        >
          计算费用
        </el-button>


        <el-button
            type="success"
            @click="submitExtend"
        >
          确认续住
        </el-button>

      </template>


    </el-dialog>

    <!-- 房态变更弹窗 -->
    <el-dialog
        v-model="roomStatusDialogVisible"
        :title="
      roomStatusAction === 'MAINTENANCE'
        ? '设置维修'
        : roomStatusAction === 'OUT_OF_SERVICE'
          ? '停用房间'
          : '恢复空闲'
    "
        width="480px"
    >

      <el-form label-width="90px">

        <el-form-item label="房号">
          {{ selectedStatusRoom?.roomNo }}
        </el-form-item>

        <el-form-item label="楼层">
          {{ selectedStatusRoom?.floorNo }} 楼
        </el-form-item>

        <el-form-item label="房型ID">
          {{ selectedStatusRoom?.roomTypeId }}
        </el-form-item>

        <el-form-item label="变更原因">

          <el-input
              v-model="roomStatusReason"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="请输入本次房态变更原因"
          />

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button
            @click="roomStatusDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
            type="primary"
            @click="submitRoomStatusChange"
        >
          确认
        </el-button>

      </template>

    </el-dialog>

  </div>
</template>


<script setup>
import AppPagination from '../components/AppPagination.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  getStaffOrders,
  getAvailableRooms,
  getOrderGuests,
  checkIn,
  checkOut,
  getStaffRooms,
  startCleaning,
  finishCleaning,
  startMaintenance,
  setOutOfService,
  restoreRoom,
  previewStaffExtend,
  extendStaff,
  completeOrder,
  markOrderNoShow,
  markOverdueNoShows
} from '../api/staff'

import { useUserStore } from '../stores/user'
import StaffWorkbenchPanel from '../components/StaffWorkbenchPanel.vue'


const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('overview')

const loading = ref(false)
const submitting = ref(false)

const confirmedOrders = ref([])
const noShowSubmitting = ref(false)
const localToday = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const isOverdueConfirmed = order => order.paymentStatus === 'PAID' && order.checkInDate && order.checkInDate < localToday()
const overdueConfirmedCount = computed(() => confirmedOrders.value.filter(isOverdueConfirmed).length)
const checkedInOrders = ref([])

const checkedOutOrders = ref([])

const dirtyRooms = ref([])
const cleaningRooms = ref([])

const selectedOrder = ref(null)

const extendDialogVisible = ref(false)


const extendPreview = ref(null)


const extendForm = reactive({

  newCheckOutDate:null

})

const availableRooms = ref([])

const orderGuests = ref([])

const checkInDialogVisible = ref(false)
const checkOutDialogVisible = ref(false)

const vacantRooms = ref([])
const listPages = reactive(Object.fromEntries(['confirmedOrders', 'checkedInOrders', 'checkedOutOrders', 'dirtyRooms', 'cleaningRooms', 'maintenanceRooms', 'outOfServiceRooms'].map(key => [key, { page: 1, pageSize: 10 }])))
const pagedList = (items, key) => items.slice((listPages[key].page - 1) * listPages[key].pageSize, listPages[key].page * listPages[key].pageSize)

const maintenanceRooms = ref([])
const outOfServiceRooms = ref([])

// 空闲房间分页
const vacantCurrentPage = ref(1)

const vacantPageSize = ref(10)

const pagedVacantRooms = computed(() => {

  const start =
      (vacantCurrentPage.value - 1)
      * vacantPageSize.value

  const end =
      start + vacantPageSize.value

  return vacantRooms.value.slice(
      start,
      end
  )
})

const roomStatusDialogVisible = ref(false)

const roomStatusAction = ref('')

const selectedStatusRoom = ref(null)

const roomStatusReason = ref('')

const openRoomStatusDialog = (
    room,
    action
) => {

  selectedStatusRoom.value = room

  roomStatusAction.value = action

  roomStatusReason.value = ''

  roomStatusDialogVisible.value = true
}

const submitRoomStatusChange = async () => {

  if (!roomStatusReason.value.trim()) {

    ElMessage.warning(
        '请输入状态变更原因'
    )

    return
  }


  try {

    const roomId =
        selectedStatusRoom.value.roomId


    if (
        roomStatusAction.value ===
        'MAINTENANCE'
    ) {

      await startMaintenance(
          roomId,
          roomStatusReason.value
      )

      ElMessage.success(
          '房间已进入维修状态'
      )

    } else if (
        roomStatusAction.value ===
        'OUT_OF_SERVICE'
    ) {

      await setOutOfService(
          roomId,
          roomStatusReason.value
      )

      ElMessage.success(
          '房间已停用'
      )

    } else if (
        roomStatusAction.value ===
        'RESTORE'
    ) {

      await restoreRoom(
          roomId,
          roomStatusReason.value
      )

      ElMessage.success(
          '房间已恢复为空闲'
      )
    }


    roomStatusDialogVisible.value = false


    /*
     * 重新加载所有相关房态
     */
    await Promise.all([
      loadVacantRooms(),
      loadMaintenanceRooms(),
      loadOutOfServiceRooms()
    ])

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '房态修改失败'
    )
  }
}

const checkInForm = reactive({
  roomIds: [],
  remark: '',
  guestVerified: false
})


const checkOutForm = reactive({
  remark: ''
})


const loadConfirmedOrders = async () => {

  try {

    loading.value = true

    const response =
        await getStaffOrders('CONFIRMED')

    confirmedOrders.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '待入住订单加载失败'
    )

  } finally {

    loading.value = false
  }
}

const handleMarkNoShow = async order => {
  try {
    await ElMessageBox.confirm(
      `将订单 ${order.orderNo} 标记为未入住并移出待入住列表？支付记录会保留，此操作不会发起退款。`,
      '确认标记未入住',
      { type: 'warning', confirmButtonText: '确认标记', cancelButtonText: '取消' }
    )
    noShowSubmitting.value = true
    await markOrderNoShow(order.orderId)
    ElMessage.success('已标记未入住')
    await loadConfirmedOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '标记未入住失败')
  } finally {
    noShowSubmitting.value = false
  }
}

const handleMarkOverdueNoShows = async () => {
  try {
    await ElMessageBox.confirm(
      `将全部已过入住日期且已支付的待入住订单标记为未入住？当前列表有 ${overdueConfirmedCount.value} 笔。支付记录会保留，此操作不会发起退款。`,
      '确认批量标记',
      { type: 'warning', confirmButtonText: '确认标记', cancelButtonText: '取消' }
    )
    noShowSubmitting.value = true
    const response = await markOverdueNoShows()
    ElMessage.success(`已标记 ${response.data.data ?? 0} 笔未入住订单`)
    await loadConfirmedOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '批量标记失败')
  } finally {
    noShowSubmitting.value = false
  }
}


const loadCheckedInOrders = async () => {

  try {

    loading.value = true

    const response =
        await getStaffOrders('CHECKED_IN')

    checkedInOrders.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '已入住订单加载失败'
    )

  } finally {

    loading.value = false
  }
}

const loadCheckedOutOrders = async()=>{

  try{

    loading.value = true

    const res =
        await getStaffOrders(
            "CHECKED_OUT"
        )


    checkedOutOrders.value =
        res.data.data || []

  }catch(error){

    ElMessage.error(
        error.response?.data?.message ||
        '已退房订单加载失败'
    )

  }finally{

    loading.value = false

  }

}

const loadDirtyRooms = async () => {

  try {

    loading.value = true

    const response =
        await getStaffRooms('DIRTY')

    dirtyRooms.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '待清洁房间加载失败'
    )

  } finally {

    loading.value = false
  }
}


const loadCleaningRooms = async () => {

  try {

    loading.value = true

    const response =
        await getStaffRooms('CLEANING')

    cleaningRooms.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '清洁中房间加载失败'
    )

  } finally {

    loading.value = false
  }
}

const handleTabChange = () => {

  if (activeTab.value === 'confirmed') {
    loadConfirmedOrders()
  }

  if (activeTab.value === 'checkedIn') {
    loadCheckedInOrders()
  }

  if (activeTab.value === 'CHECKED_OUT') {

    loadCheckedOutOrders()

  }

  if (activeTab.value === 'dirty') {
    loadDirtyRooms()
  }

  if (activeTab.value === 'cleaning') {
    loadCleaningRooms()
  }

  if (activeTab.value === 'vacant') {
    loadVacantRooms()
  }

  if (activeTab.value === 'maintenance') {
    loadMaintenanceRooms()
  }

  if (activeTab.value === 'outOfService') {
    loadOutOfServiceRooms()
  }
}

const openCheckIn = async (order) => {

  try {

    selectedOrder.value = order


    /*
     * 同时加载：
     * 1. 可分配房间
     * 2. 入住人信息
     */
    const [
      roomsResponse,
      guestsResponse
    ] = await Promise.all([

      getAvailableRooms(
          order.orderId
      ),

      getOrderGuests(
          order.orderId
      )
    ])


    availableRooms.value =
        roomsResponse.data.data || []


    orderGuests.value =
        guestsResponse.data.data || []


    /*
     * 初始化入住表单
     */
    checkInForm.roomIds = []

    checkInForm.remark = ''

    checkInForm.guestVerified = false


    checkInDialogVisible.value = true

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '入住信息加载失败'
    )
  }
}

const getIdTypeText = (type) => {

  const map = {
    ID_CARD: '身份证',
    PASSPORT: '护照',
    OTHER: '其他证件'
  }

  return map[type] || type || '-'
}

const submitCheckIn = async () => {

  if (!checkInForm.guestVerified) {

    ElMessage.warning(
        '请先核验入住人身份信息'
    )

    return
  }

  /*
   * 分配房间数量必须正确
   */
  if (
      checkInForm.roomIds.length !==
      selectedOrder.value.roomCount
  ) {

    ElMessage.warning(
        `请选择 ${selectedOrder.value.roomCount} 个房间`
    )

    return
  }


  try {

    submitting.value = true

    await checkIn({
      orderId: selectedOrder.value.orderId,
      roomIds: checkInForm.roomIds,
      remark: checkInForm.remark
    })

    ElMessage.success('办理入住成功')

    checkInDialogVisible.value = false

    await loadConfirmedOrders()

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '办理入住失败'
    )

  } finally {

    submitting.value = false
  }
}


const openCheckOut = (order) => {

  selectedOrder.value = order

  checkOutForm.remark = ''

  checkOutDialogVisible.value = true
}

const openExtend = (order)=>{

  selectedOrder.value = order

  extendForm.newCheckOutDate = null

  extendPreview.value = null

  extendDialogVisible.value = true

}

const submitCheckOut = async () => {

  try {

    submitting.value = true

    await checkOut({
      orderId: selectedOrder.value.orderId,
      remark:
      checkOutForm.remark
    })

    ElMessage.success('办理退房成功')

    checkOutDialogVisible.value = false

    await loadCheckedInOrders()

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '办理退房失败'
    )

  } finally {

    submitting.value = false
  }
}


const handleStartCleaning = async (room) => {

  try {

    await ElMessageBox.confirm(
        `确认开始清洁房间 ${room.roomNo} 吗？`,
        '开始清洁'
    )

    await startCleaning(room.roomId)

    ElMessage.success('房间已进入清洁中状态')

    await loadDirtyRooms()

  } catch (error) {

    if (error === 'cancel') {
      return
    }

    ElMessage.error(
        error.response?.data?.message ||
        '操作失败'
    )
  }
}


const handleFinishCleaning = async (room) => {

  try {

    await ElMessageBox.confirm(
        `确认房间 ${room.roomNo} 已完成清洁吗？`,
        '完成清洁'
    )

    await finishCleaning(room.roomId)

    ElMessage.success(
        '清洁完成，房间已恢复可用'
    )

    await loadCleaningRooms()

  } catch (error) {

    if (error === 'cancel') {
      return
    }

    ElMessage.error(
        error.response?.data?.message ||
        '操作失败'
    )
  }
}


const handleLogout = async () => {

  try {

    await ElMessageBox.confirm(
        '确定退出当前账号吗？',
        '退出登录'
    )

    userStore.logout()

    router.push('/login')

  } catch {
    // 取消退出
  }
}

const loadVacantRooms = async () => {

  try {

    const response =
        await getStaffRooms('VACANT')

    vacantRooms.value =
        response.data.data || []

    vacantCurrentPage.value = 1

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '空闲房间加载失败'
    )
  }
}


const loadMaintenanceRooms = async () => {

  try {

    const response =
        await getStaffRooms('MAINTENANCE')

    maintenanceRooms.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '维修房间加载失败'
    )
  }
}


const loadOutOfServiceRooms = async () => {

  try {

    const response =
        await getStaffRooms('OUT_OF_SERVICE')

    outOfServiceRooms.value =
        response.data.data || []

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '停用房间加载失败'
    )
  }
}

const previewExtend = async()=>{


  const res =
      await previewStaffExtend({

        orderId:
        selectedOrder.value.orderId,


        newCheckOutDate:
            formatDate(
                extendForm.newCheckOutDate
            )

      })


  extendPreview.value =
      res.data.data || res.data

}

const submitExtend = async()=>{


  await extendStaff({

    orderId:
    selectedOrder.value.orderId,


    newCheckOutDate:
        formatDate(
            extendForm.newCheckOutDate
        )

  })


  ElMessage.success(
      '续住成功'
  )


  extendDialogVisible.value=false


  await loadCheckedInOrders()

}

const formatDate=(date)=>{

  const y=date.getFullYear()

  const m=
      String(date.getMonth()+1)
          .padStart(2,'0')


  const d=
      String(date.getDate())
          .padStart(2,'0')


  return `${y}-${m}-${d}`

}

onMounted(() => {
  loadConfirmedOrders()
})

const disabledExtendDate = (date)=>{

  if(!selectedOrder.value){
    return false
  }

  return date <= new Date(selectedOrder.value.checkOutDate)

}

const handleComplete = async(row)=>{

  try{

    await completeOrder(row.orderId)

    ElMessage.success(
        "订单已完成"
    )

    loadCheckedOutOrders()

  }catch(e){

    ElMessage.error(
        e.response?.data?.message ||
        "操作失败"
    )

  }

}

</script>


<style scoped>
.staff-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  padding: 22px 5%;

  display: flex;
  align-items: center;
  justify-content: space-between;

  background: white;
  border-bottom: 1px solid #ebeef5;
}

.header h1 {
  margin: 0 0 5px;
}

.header p {
  margin: 0;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.container {
  width: 96%;
  max-width: none;

  margin: 30px auto;

  padding: 25px;

  background: white;

  border-radius: 10px;
}

.no-show-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: #606266;
}
.management-tabs :deep(.el-tabs__header.is-left) {
  width: 190px;
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
}
.management-tabs :deep(.el-tabs__item.is-left) { justify-content: flex-start; padding-left: 18px; }
.management-tabs :deep(.el-tabs__content) { min-width: 0; padding: 0 12px 0 20px; }
@media (max-width: 760px) {
  .management-tabs :deep(.el-tabs__header.is-left) { width: 125px; }
  .management-tabs :deep(.el-tabs__content) { padding-left: 8px; }
}
</style>
