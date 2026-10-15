<template>
  <div class="page">

    <!-- 顶部导航 -->
    <header class="header">
      <div class="brand">
        <div class="brand-title">智慧酒店</div>
      </div>

      <div class="user-area">
        <el-button type="warning" plain @click="openCheckin">签到领券</el-button>

  <span>
    欢迎，{{ userStore.nickname || userStore.username }}
  </span>

        <el-button
            type="primary"
            plain
            @click="router.push('/bookings')"
        >
          我的订单
        </el-button>

        <el-button @click="router.push('/profile')">个人资料</el-button>

        <el-button
            type="danger"
            plain
            @click="handleLogout"
        >
          退出登录
        </el-button>

      </div>
    </header>


    <!-- 酒店介绍区域 -->
    <section class="hero">
      <div class="hero-content">

        <h1>
          发现理想的入住体验
        </h1>

        <p>
          在线查询房型、价格和库存，
          快速完成酒店预订。
        </p>
      </div>
    </section>

    <div class="hotel-search-layout">
      <section v-if="hotel" class="hotel-info">
        <h2>{{ hotelDisplayName }}</h2>
        <p>{{ hotelDisplayDescription }}</p>
        <p>地址：{{ hotel.address }}  电话：{{hotel.phone }}</p>
        <p>入住时间：{{ hotel.checkInTime }}  退房时间：{{ hotel.checkOutTime }}</p>
      </section>

      <!-- 搜索区域 -->
      <section class="search-wrapper">

      <div class="search-box">
        <h2 class="search-title">查询可订房型</h2>

        <!-- 入住日期 -->
        <div class="search-item">
          <span class="label">入住日期</span>

          <el-date-picker
              v-model="searchForm.checkInDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择入住日期"
              style="width: 100%"
          />
        </div>


        <!-- 退房日期 -->
        <div class="search-item">
          <span class="label">退房日期</span>

          <el-date-picker
              v-model="searchForm.checkOutDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择退房日期"
              style="width: 100%"
          />
        </div>


        <!-- 房间数量 -->
        <div class="search-item">
          <span class="label">房间数量</span>

          <el-input-number
              v-model="searchForm.roomCount"
              :min="1"
              :max="10"
              style="width: 100%"
          />
        </div>


        <!-- 入住人数 -->
        <div class="search-item">
          <span class="label">入住人数</span>

          <el-input-number
              v-model="searchForm.guestCount"
              :min="1"
              :max="20"
              style="width: 100%"
          />
        </div>


        <!-- 查询按钮 -->
        <div class="search-action">
          <el-button
              type="primary"
              size="large"
              style="width: 100%"
              @click="handleSearch"
          >
            查询可订房型
          </el-button>
        </div>

      </div>

      </section>
    </div>

    <DailyDealFeature />

    <AIRecommendPanel @select="useRecommendation" />

    <!-- 房型区域 -->
    <main class="content">

      <div class="section-header">
        <div>
          <h2>
            {{ searched ? '可订房型' : '精选房型' }}
          </h2>

          <p>
            共 {{ roomTypes.length }} 种房型
          </p>
        </div>

        <el-button
            :loading="loading"
            @click="loadRoomTypes"
        >
          刷新
        </el-button>
      </div>


      <div
          v-loading="loading"
          class="room-grid"
      >

        <div
            v-for="room in roomTypes"
            :key="room.roomTypeId"
            class="room-card"
        >

          <div class="room-image">
            <img v-if="getRoomCover(room.typeCode)"
                 :src="getRoomCover(room.typeCode)"
                 :alt="`${room.typeName}房间照片`"
                 loading="lazy" />
            <span v-else>{{ room.typeCode }}</span>
          </div>


          <div class="room-body">

            <div class="room-title-row">
              <h3>{{ room.typeName }}</h3>

              <el-tag type="success">
                可预订
              </el-tag>
            </div>


            <div class="room-info">
              <span>
                {{ room.area }}㎡
              </span>

              <span>
                {{ room.bedType }}
              </span>

              <span>
                最多 {{ room.maxGuests }} 人
              </span>

              <span v-if="searched">
                剩余 {{ room.availableQty }} 间
              </span>

            </div>


            <div class="room-bottom">

              <div class="price">

                <template v-if="searched">

                  <div>
                    <strong>
                      ¥{{ room.totalPrice }}
                    </strong>

                    <span>
          / 共{{ searchForm.roomCount }}间
        </span>
                  </div>

                  <div class="price-detail">
                    每间整个入住期间 ¥{{ room.pricePerRoom }}
                  </div>

                </template>


                <template v-else-if="room.basePrice">

                  <strong>
                    ¥{{ room.basePrice }}
                  </strong>

                  <span>/晚起</span>

                </template>


                <template v-else>

      <span>
        价格以日期查询为准
      </span>

                </template>

              </div>

              <el-button
                  plain
                  @click="showRoomDetail(room.roomTypeId)"
              >房型详情</el-button>

              <el-button
                  type="primary"
                  :disabled="!searched"
                  @click="openBooking(room)"
              >
                {{ searched ? '立即预订' : '请先选择日期' }}
              </el-button>

            </div>

          </div>

        </div>

      </div>

    </main>

    <el-dialog v-model="roomDetailVisible" title="房型详情" width="min(820px, 94vw)">
      <template v-if="roomDetail">
        <h2>{{ roomDetail.roomType.typeName }}</h2>
        <p>{{ roomDetail.roomType.description || getRoomMedia(roomDetail.roomType.typeCode)?.description || '暂无房型介绍' }}</p>
        <div v-if="getRoomMedia(roomDetail.roomType.typeCode)?.highlights?.length" class="room-highlights">
          <el-tag v-for="item in getRoomMedia(roomDetail.roomType.typeCode).highlights"
                  :key="item" effect="plain">{{ item }}</el-tag>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="面积">{{ roomDetail.roomType.area }}㎡</el-descriptions-item>
          <el-descriptions-item label="床型">{{ roomDetail.roomType.bedType }}</el-descriptions-item>
          <el-descriptions-item label="床数">{{ roomDetail.roomType.bedCount }}</el-descriptions-item>
          <el-descriptions-item label="可住人数">{{ roomDetail.roomType.maxGuests }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detailPhotos.length" class="detail-images">
          <el-image v-for="photo in detailPhotos" :key="photo"
                    :src="photo" :preview-src-list="detailPhotos"
                    fit="cover" lazy />
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="checkinDialogVisible" title="每日签到领优惠券" width="min(620px, 92vw)" align-center>
      <div v-loading="checkinLoading">
        <p class="checkin-intro">连续签到 7 天，每天领取一张无门槛优惠券。第 7 天后重新从第 1 天开始，漏签也会重新开始。</p>
        <div v-if="checkinOverview" class="checkin-grid">
          <div v-for="reward in checkinOverview.rewards" :key="reward.dayNumber" class="checkin-day"
               :class="{ current: reward.dayNumber === checkinOverview.nextDay && !checkinOverview.checkedInToday }">
            <strong>第 {{ reward.dayNumber }} 天</strong>
            <span>¥{{ reward.amount }}</span>
          </div>
        </div>
        <p v-if="checkinOverview" class="checkin-status">
          {{ checkinOverview.checkedInToday ? '今天已签到，明天再来领取' : `当前连续签到 ${checkinOverview.streakDays} 天，今天可领取第 ${checkinOverview.nextDay} 天奖励` }}
        </p>
        <el-button type="primary" :loading="claimingCheckin" :disabled="!checkinOverview || checkinOverview.checkedInToday"
                   @click="claimToday">{{ checkinOverview?.checkedInToday ? '今日已签到' : '立即签到' }}</el-button>
        <h3>我的可用优惠券</h3>
        <el-empty v-if="checkinOverview && !checkinOverview.coupons.length" description="暂无可用优惠券" :image-size="70" />
        <div v-for="coupon in checkinOverview?.coupons || []" :key="coupon.id" class="coupon-row">
          <strong>¥{{ coupon.amount }} 无门槛</strong><span>{{ coupon.expireTime.replace('T', ' ').slice(0, 16) }} 到期</span>
        </div>
        <p class="checkin-hint">签到券领取后 24 小时内有效，可与自动优惠活动叠加。取消或超时关闭的未支付订单会返还尚未过期的优惠券。</p>
      </div>
    </el-dialog>

    <!-- 创建预订弹窗 -->
    <el-dialog
        v-model="bookingDialogVisible"
        title="确认预订"
        width="520px"
    >

      <template v-if="selectedRoom">

        <el-descriptions
            :column="1"
            border
            style="margin-bottom: 24px"
        >

          <el-descriptions-item label="房型">
            {{ selectedRoom.typeName }}
          </el-descriptions-item>

          <el-descriptions-item label="入住日期">
            {{ searchForm.checkInDate }}
          </el-descriptions-item>

          <el-descriptions-item label="退房日期">
            {{ searchForm.checkOutDate }}
          </el-descriptions-item>

          <el-descriptions-item label="房间数量">
            {{ searchForm.roomCount }} 间
          </el-descriptions-item>

          <el-descriptions-item label="入住人数">
            {{ searchForm.guestCount }} 人
          </el-descriptions-item>

          <el-descriptions-item label="预计房费（优惠前）">
            <strong class="dialog-price">
              ¥{{ selectedRoom.totalPrice }}
            </strong>
          </el-descriptions-item>

          <el-descriptions-item v-if="selectedDeal" label="秒杀房费">
            <strong class="dialog-price">¥{{ selectedDeal.dealPrice }}</strong>
          </el-descriptions-item>

        </el-descriptions>

        <el-form-item v-if="!selectedDeal" label="优惠券">
          <el-select v-model="bookingForm.couponId" clearable placeholder="选择一张无门槛优惠券（可叠加活动）" style="width: 100%">
            <el-option v-for="coupon in availableCoupons" :key="coupon.id"
                       :value="coupon.id"
                       :label="`¥${coupon.amount} 无门槛 · ${coupon.expireTime.replace('T', ' ').slice(0, 16)} 到期`" />
          </el-select>
        </el-form-item>
        <p v-if="selectedDeal" class="coupon-note">秒杀订单不使用优惠券或其他活动，最终价格和名额以下单结果为准。</p>
        <p v-else class="coupon-note">优惠活动自动计算，再抵扣所选优惠券；每笔订单限用一张，未用完面额不找零。最终金额以订单详情为准。</p>

        <el-form
            :model="bookingForm"
            label-width="90px"
        >

          <el-form-item label="联系人">
            <el-input
                v-model="bookingForm.contactName"
                placeholder="请输入联系人"
            />
          </el-form-item>


          <el-form-item label="联系电话">
            <el-input
                v-model="bookingForm.contactPhone"
                maxlength="11"
                placeholder="请输入手机号"
            />
          </el-form-item>

          <el-divider content-position="left">
            入住人信息
          </el-divider>

          <p class="saved-guest-hint">可以从个人资料中保存常用入住人，下次预订时直接选择；也可以继续手动填写。</p>


          <div
              v-for="(guest, index) in bookingForm.guests"
              :key="index"
              class="guest-block"
          >

            <div class="guest-title">

              入住人 {{ index + 1 }}

              <el-tag
                  v-if="index === 0"
                  type="success"
                  size="small"
              >
                主要入住人
              </el-tag>

            </div>

            <el-form-item label="常用入住人">
              <el-select v-model="guest.savedGuestId" clearable filterable
                         :loading="savedGuestLoading" placeholder="选择后自动填写，也可手动填写"
                         style="width: 100%" @change="id => applySavedGuest(guest, id, index)">
                <el-option v-for="saved in savedGuests" :key="saved.guestId"
                           :label="`${saved.guestName} · ${maskSavedPhone(saved.phone)}`"
                           :value="saved.guestId" />
              </el-select>
            </el-form-item>


            <el-form-item
                label="姓名"
                required
            >
              <el-input
                  v-model="guest.guestName"
                  placeholder="请输入入住人姓名"
              />
            </el-form-item>


            <el-form-item label="手机号">
              <el-input
                  v-model="guest.phone"
                  placeholder="请输入手机号"
              />
            </el-form-item>


            <el-form-item label="证件类型">
              <el-select
                  v-model="guest.idType"
                  style="width: 100%"
              >

                <el-option
                    label="身份证"
                    value="ID_CARD"
                />

                <el-option
                    label="护照"
                    value="PASSPORT"
                />

                <el-option
                    label="其他证件"
                    value="OTHER"
                />

              </el-select>
            </el-form-item>


            <el-form-item label="证件号码">
              <el-input
                  v-model="guest.idNumber"
                  placeholder="请输入证件号码"
              />
            </el-form-item>

          </div>

          <el-form-item label="备注">
            <el-input
                v-model="bookingForm.remark"
                type="textarea"
                :rows="3"
                placeholder="如有特殊需求可填写"
            />
          </el-form-item>

        </el-form>

      </template>


      <template #footer>

        <el-button
            @click="bookingDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
            type="primary"
            :loading="bookingSubmitting"
            @click="submitBooking"
        >
          确认预订
        </el-button>

      </template>

    </el-dialog>

  </div>
</template>


<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch
} from 'vue'

import { useRoute, useRouter } from 'vue-router'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'


import {
  getRoomTypes,
  searchAvailableRoomTypes,
  getHotelInfo,
  getRoomTypeDetail
} from '../api/room'

import { createBooking } from '../api/booking'
import { getCheckinOverview, claimCheckin } from '../api/checkin'
import { listSavedGuests } from '../api/savedGuest'
import AIRecommendPanel from '../components/AIRecommendPanel.vue'
import DailyDealFeature from '../components/DailyDealFeature.vue'
import { listDailyDeals } from '../api/dailyDeal'

import { useUserStore } from '../stores/user'
import { getRoomCover, getRoomMedia, getRoomPhotos } from '../data/roomMedia'

const searched = ref(false)


const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const roomTypes = ref([])
const hotel = ref(null)
const hotelDisplayName = computed(() => hotel.value?.hotelName?.replace(/^SmartHotel\s*/i, '').trim() || '智慧酒店')
const hotelDisplayDescription = computed(() => hotel.value?.description === 'SmartHotel 智慧酒店预订与运营管理系统演示酒店'
  ? '智慧酒店预订与运营管理系统'
  : hotel.value?.description)
const roomDetail = ref(null)
const roomDetailVisible = ref(false)
const detailPhotos = computed(() => {
  const typeCode = roomDetail.value?.roomType?.typeCode
  const localPhotos = getRoomPhotos(typeCode)
  return localPhotos.length ? localPhotos : (roomDetail.value?.images || []).map(image => image.imageUrl)
})

const showRoomDetail = async id => {
  try {
    const { data } = await getRoomTypeDetail(id)
    if (data.code !== 200) throw new Error(data.message)
    roomDetail.value = data.data
    roomDetailVisible.value = true
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '房型详情加载失败') }
}

const loadHotel = async () => {
  try {
    const { data } = await getHotelInfo()
    if (data.code === 200) hotel.value = data.data
  } catch { /* 酒店信息缺失时仍可预订 */ }
}

const bookingDialogVisible = ref(false)
const checkinDialogVisible = ref(false)
const checkinLoading = ref(false)
const claimingCheckin = ref(false)
const checkinOverview = ref(null)
const availableCoupons = computed(() => checkinOverview.value?.coupons || [])

const refreshCheckin = async () => {
  checkinLoading.value = true
  try {
    const { data } = await getCheckinOverview()
    if (data.code !== 200) throw new Error(data.message)
    checkinOverview.value = data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '签到信息加载失败')
  } finally {
    checkinLoading.value = false
  }
}

const openCheckin = () => {
  checkinDialogVisible.value = true
  refreshCheckin()
}

const claimToday = async () => {
  claimingCheckin.value = true
  try {
    const { data } = await claimCheckin()
    if (data.code !== 200) throw new Error(data.message)
    checkinOverview.value = data.data
    ElMessage.success('签到成功，优惠券已到账')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '签到失败')
    await refreshCheckin()
  } finally {
    claimingCheckin.value = false
  }
}
const savedGuests = ref([])
const savedGuestLoading = ref(false)

const bookingSubmitting = ref(false)

const selectedRoom = ref(null)
const selectedDeal = ref(null)

const searchForm = reactive({
  checkInDate: '',
  checkOutDate: '',
  roomCount: 1,
  guestCount: 1
})

const useRecommendation = async (result, recommendedRoom, shorterStay) => {
  searchForm.checkInDate = result.checkInDate
  searchForm.checkOutDate = shorterStay
    ? result.budgetAlternative?.shorterCheckOutDate
    : result.checkOutDate
  searchForm.guestCount = result.parsed.people
  searchForm.roomCount = 1
  if (!await handleSearch()) return
  const currentRoom = roomTypes.value.find(room => room.roomTypeId === recommendedRoom.roomTypeId)
  if (!currentRoom) {
    ElMessage.warning('该房型目前不可订，请重新获取推荐')
    return
  }
  openBooking(currentRoom)
}

watch(
    () => [
      searchForm.checkInDate,
      searchForm.checkOutDate,
      searchForm.roomCount,
      searchForm.guestCount
    ],

    () => {

      searched.value = false

    }
)

const bookingForm = reactive({

  contactName: '',

  contactPhone: '',

  remark: '',
  couponId: null,

  guests: []
})

const buildGuestForms = (guestCount) => {

  bookingForm.guests =
      Array.from(
          {
            length: guestCount
          },
          () => ({
            savedGuestId: null,
            guestName: '',
            phone: '',
            idType: 'ID_CARD',
            idNumber: ''
          })
      )
}

const loadSavedGuests = async () => {
  savedGuestLoading.value = true
  try {
    const { data } = await listSavedGuests()
    if (data.code !== 200) throw new Error(data.message)
    savedGuests.value = data.data || []
  } catch (error) {
    savedGuests.value = []
    ElMessage.warning(error.response?.data?.message || '常用入住人加载失败，可以手动填写')
  } finally {
    savedGuestLoading.value = false
  }
}

const maskSavedPhone = phone => phone ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : ''

const applySavedGuest = (guest, savedId, index) => {
  if (!savedId) return
  if (bookingForm.guests.some((other, otherIndex) => otherIndex !== index && other.savedGuestId === savedId)) {
    guest.savedGuestId = null
    ElMessage.warning('同一位常用入住人不能重复选择')
    return
  }
  const saved = savedGuests.value.find(item => item.guestId === savedId)
  if (!saved) return
  guest.guestName = saved.guestName
  guest.phone = saved.phone || ''
  guest.idType = saved.idType || 'ID_CARD'
  guest.idNumber = saved.idNumber || ''
  if (index === 0) {
    bookingForm.contactName = saved.guestName
    bookingForm.contactPhone = saved.phone || ''
  }
}

const loadRoomTypes = async () => {

  try {

    loading.value = true

    const response = await getRoomTypes()

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '房型加载失败'
      )
      return
    }

    roomTypes.value = result.data || []

  } catch (error) {

    ElMessage.error('无法加载房型信息')

  } finally {

    loading.value = false
  }
}


const handleSearch = async () => {

  if (
      !searchForm.checkInDate ||
      !searchForm.checkOutDate
  ) {
    ElMessage.warning('请选择入住日期和退房日期')
    return false
  }

  if (
      searchForm.checkInDate >=
      searchForm.checkOutDate
  ) {
    ElMessage.warning('退房日期必须晚于入住日期')
    return false
  }

  try {

    loading.value = true

    const response =
        await searchAvailableRoomTypes({
          checkInDate: searchForm.checkInDate,
          checkOutDate: searchForm.checkOutDate,
          roomCount: searchForm.roomCount,
          guestCount: searchForm.guestCount
        })

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '查询失败'
      )
      return false
    }

    roomTypes.value = result.data || []

    searched.value = true

    if (roomTypes.value.length === 0) {
      ElMessage.warning(
          '所选日期暂无符合条件的可订房型'
      )
    } else {
      ElMessage.success(
          `查询成功，共找到 ${roomTypes.value.length} 种可订房型`
      )
    }
    return true

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '查询可订房型失败'
    )
    return false

  } finally {

    loading.value = false
  }
}

const openBooking = (room, deal = null) => {

  if (!searched.value) {

    ElMessage.warning(
        '请先选择入住日期并查询可订房型'
    )

    return
  }


  selectedRoom.value = room
  selectedDeal.value = deal


  bookingForm.contactName =
      userStore.nickname ||
      userStore.username ||
      ''

  bookingForm.contactPhone = ''

  bookingForm.remark = ''
  bookingForm.couponId = null


  /*
   * 根据入住人数生成入住人表单
   */
  buildGuestForms(
      searchForm.guestCount
  )


  /*
   * 默认第一个入住人使用联系人姓名
   */
  if (
      bookingForm.guests.length > 0
  ) {

    bookingForm.guests[0].guestName =
        bookingForm.contactName
  }


  bookingDialogVisible.value = true
  if (!deal) refreshCheckin()
  loadSavedGuests()
}

const openDailyDeal = async id => {
  try {
    const { data } = await listDailyDeals()
    const deal = data.data?.find(item => String(item.id) === String(id))
    if (!deal || deal.remaining <= 0 || Date.now() < new Date(deal.startTime).getTime()
        || Date.now() >= new Date(deal.endTime).getTime()) {
      ElMessage.warning('该秒杀当前不可订，请查看每日优惠列表')
      return
    }
    searchForm.checkInDate = deal.checkInDate
    searchForm.checkOutDate = deal.checkOutDate
    searchForm.roomCount = 1
    searchForm.guestCount = 1
    if (!await handleSearch()) return
    const room = roomTypes.value.find(item => item.roomTypeId === deal.roomTypeId)
    if (!room) { ElMessage.warning('该秒杀房型当前无可用库存'); return }
    openBooking(room, deal)
  } catch (error) { ElMessage.error(error.response?.data?.message || '秒杀信息加载失败') }
}

const submitBooking = async () => {

  if (!bookingForm.contactName.trim()) {
    ElMessage.warning('请输入联系人姓名')
    return
  }

  if (!bookingForm.contactPhone.trim()) {
    ElMessage.warning('请输入联系电话')
    return
  }

  if (!/^1\d{10}$/.test(bookingForm.contactPhone)) {
    ElMessage.warning('请输入正确的11位手机号')
    return
  }

  try {

    bookingSubmitting.value = true

    const response = await createBooking({

      roomTypeId:
      selectedRoom.value.roomTypeId,

      roomCount:
      searchForm.roomCount,

      guestCount:
      searchForm.guestCount,

      checkInDate:
      searchForm.checkInDate,

      checkOutDate:
      searchForm.checkOutDate,

      contactName:
          bookingForm.contactName.trim(),

      contactPhone:
          bookingForm.contactPhone.trim(),

      remark:
      bookingForm.remark,

      couponId: bookingForm.couponId,
      dailyDealId: selectedDeal.value?.id || null,

      guests:
          bookingForm.guests.map(
              guest => ({
                guestName:
                    guest.guestName.trim(),

                phone:
                    guest.phone?.trim() || null,

                idType:
                    guest.idType || 'ID_CARD',

                idNumber:
                    guest.idNumber?.trim() || null
              })
          )
    })

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(
          result.message || '创建订单失败'
      )
      return
    }

    bookingDialogVisible.value = false

    ElMessage.success(result.data.paymentStatus === 'PAID'
      ? '优惠券已抵扣全部房费，预订已确认'
      : '订单创建成功，请尽快支付')

    router.push(
        `/bookings/${result.data.orderId}`
    )

  } catch (error) {

    ElMessage.error(
        error.response?.data?.message ||
        '创建订单失败'
    )

  } finally {

    bookingSubmitting.value = false
  }
}

const handleLogout = async () => {

  try {

    await ElMessageBox.confirm(
        '确定退出当前账号吗？',
        '退出登录',
        {
          confirmButtonText: '是',
          cancelButtonText: '否',
          type: 'warning'
        }
    )

    userStore.logout()

    router.push('/login')

  } catch {
    // 用户点击取消，不做处理
  }
}


onMounted(() => {
  loadRoomTypes()
  loadHotel()
  if (route.query.dailyDealId) openDailyDeal(route.query.dailyDealId)
})
</script>


<style scoped>
.hotel-search-layout {
  width: 86%;
  max-width: 1180px;
  margin: 20px auto 0;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 20px;
  align-items: stretch;
}

.hotel-info {
  min-width: 0;
  padding: 24px;
  background: white;
  border-radius: 12px;
}

.hotel-info h2 { margin-top: 0; }
.detail-images { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 10px; margin-top: 16px; }
.detail-images :deep(.el-image) { width: 100%; height: 130px; border-radius: 8px; cursor: zoom-in; }
.room-highlights { display: flex; flex-wrap: wrap; gap: 8px; margin: 12px 0 18px; }
* {
  box-sizing: border-box;
}

.page {
  min-height: 100vh;
  background: #f5f7fa;
  color: #1f2937;
}

.header {
  height: 72px;
  padding: 0 7%;

  display: flex;
  align-items: center;
  justify-content: space-between;

  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.brand-title {
  font-size: 25px;
  font-weight: 700;
}

.brand-subtitle {
  color: #909399;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 18px;

  color: #606266;
}
.coupon-note { margin: -4px 0 18px 90px; color: #667085; font-size: 13px; line-height: 1.5; }
.checkin-intro, .checkin-hint { color: #667085; line-height: 1.6; }
.checkin-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(115px, 1fr)); gap: 10px; }
.checkin-day { display: flex; flex-direction: column; gap: 8px; padding: 14px; border: 1px solid #e5e7eb; border-radius: 8px; text-align: center; }
.checkin-day.current { border-color: #e6a23c; background: #fffaf0; }
.checkin-day span { color: #d97706; font-size: 19px; font-weight: 700; }
.checkin-status { margin: 18px 0 12px; }
.coupon-row { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid #eee; }
.coupon-row span { color: #667085; }

.hero {
  height: 320px;

  display: flex;
  align-items: center;

  padding: 0 10%;

  background:
      linear-gradient(90deg, rgba(15, 30, 54, 0.82), rgba(15, 30, 54, 0.16)),
      url('/images/hotel-exterior.jpg') center 48% / cover;

  color: white;
}

.hero-content {
  max-width: 700px;
}

.hero-tag {
  margin-bottom: 14px;

  font-size: 13px;
  letter-spacing: 4px;

  opacity: 0.8;
}

.hero h1 {
  margin: 0 0 18px;

  font-size: 46px;
  line-height: 1.2;
}

.hero p {
  margin: 0;

  font-size: 18px;
  line-height: 1.8;

  opacity: 0.88;
}

.search-wrapper {
  position: relative;
  min-width: 0;
}

.search-box {
  display: grid;

  grid-template-columns: repeat(2, minmax(0, 1fr));

  gap: 20px;

  padding: 24px;

  background: white;

  border-radius: 12px;
  height: 100%;

  box-shadow:
      0 8px 30px
      rgba(0, 0, 0, 0.08);
}

.search-title { grid-column: 1 / -1; margin: 0; font-size: 24px; }

.search-item {
  display: flex;
  flex-direction: column;

  gap: 8px;
}

.label {
  font-size: 13px;
  color: #909399;
}

.search-action {
  display: flex;
  align-items: flex-end;
  grid-column: 1 / -1;
}

.content {
  width: 86%;
  max-width: 1180px;

  margin: 40px auto 60px;
}

.section-header {
  margin-bottom: 24px;

  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-header h2 {
  margin: 0 0 5px;

  font-size: 28px;
}

.section-header p {
  margin: 0;
  color: #909399;
}

.room-grid {
  display: grid;

  grid-template-columns:
    repeat(3, minmax(0, 1fr));

  gap: 24px;
}

.room-card {
  overflow: hidden;

  background: white;

  border-radius: 12px;

  transition:
      transform 0.2s,
      box-shadow 0.2s;
}

.room-card:hover {
  transform: translateY(-4px);

  box-shadow:
      0 10px 30px
      rgba(0, 0, 0, 0.08);
}

.room-image {
  height: 180px;

  display: flex;
  align-items: center;
  justify-content: center;

  background:
      linear-gradient(
          135deg,
          #dbeafe,
          #eff6ff
      );

  font-size: 28px;
  font-weight: 700;

  color: #4b6cb7;
}

.room-image img { width: 100%; height: 100%; object-fit: cover; }

.room-body {
  padding: 20px;
}

.room-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 10px;
}

.room-title-row h3 {
  margin: 0;

  font-size: 18px;
}

.room-info {
  margin-top: 16px;

  display: flex;
  flex-wrap: wrap;

  gap: 12px;

  font-size: 14px;
  color: #606266;
}

.room-bottom {
  margin-top: 24px;

  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price strong {
  font-size: 24px;
  color: #d97706;
}

.dialog-price {
  font-size: 20px;
  color: #d97706;
}

.price span {
  margin-left: 3px;

  font-size: 13px;
  color: #909399;
}

@media (max-width: 900px) {

  .hotel-search-layout { grid-template-columns: 1fr; }

  .room-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {

  .header {
    padding: 0 20px;
  }

  .user-area span {
    display: none;
  }

  .hero {
    padding: 0 24px;
  }

  .hero h1 {
    font-size: 34px;
  }

  .search-box {
    grid-template-columns: 1fr;
  }

  .room-grid {
    grid-template-columns: 1fr;
  }
}

.guest-block {
  margin-bottom: 18px;
  padding: 16px;

  border: 1px solid #ebeef5;
  border-radius: 8px;

  background: #fafafa;
}

.saved-guest-hint { margin: 0 0 14px; color: #667085; line-height: 1.6; }

.guest-title {
  margin-bottom: 14px;

  display: flex;
  align-items: center;
  gap: 10px;

  font-weight: 600;
}
</style>
