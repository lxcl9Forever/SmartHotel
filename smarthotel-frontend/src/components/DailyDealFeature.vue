<template>
  <section class="feature" @click="router.push('/daily-deals')">
    <div class="heading">
      <div>
        <h2>每日优惠</h2>
        <p>限时抢房，秒杀价不与优惠券或其他活动叠加</p>
      </div>
      <el-button type="primary" @click.stop="router.push('/daily-deals')">查看全部优惠</el-button>
    </div>
    <div v-if="featured" class="offer">
      <img v-if="getRoomCover(featured.typeCode)" :src="getRoomCover(featured.typeCode)" :alt="featured.roomTypeName" />
      <div class="offer-copy">
        <el-tag type="danger">限时秒杀 · {{ Number(featured.discountRate) * 10 }} 折</el-tag>
        <h3>{{ featured.roomTypeName }}</h3>
        <p>{{ featured.checkInDate }} 入住 · {{ featured.checkOutDate }} 退房</p>
        <p>剩余 {{ featured.remaining }} 间 · {{ featured.startTime.replace('T', ' ').slice(0, 16) }} 开抢</p>
      </div>
      <div class="prices">
        <del>¥{{ featured.originalPrice }}</del>
        <strong>¥{{ featured.dealPrice }}</strong>
        <span>/ 间</span>
      </div>
    </div>
    <el-empty v-else description="暂无每日优惠，稍后再来看看" :image-size="70" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listDailyDeals } from '../api/dailyDeal'
import { getRoomCover } from '../data/roomMedia'

const router = useRouter()
const featured = ref(null)
onMounted(async () => {
  try {
    const { data } = await listDailyDeals()
    if (data.code === 200) {
      const offers = data.data || []
      featured.value = offers.find(offer => offer.remaining > 0
        && Date.now() >= new Date(offer.startTime).getTime())
        || offers.find(offer => offer.remaining > 0)
        || offers[0] || null
    }
  } catch { featured.value = null }
})
</script>

<style scoped>
.feature { width: 86%; max-width: 1180px; margin: 24px auto 0; padding: 24px; background: white; border-radius: 12px; cursor: pointer; box-shadow: 0 8px 30px #0000000c; }
.heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.heading h2 { margin: 0; }
.heading p { margin: 8px 0 0; color: #667085; }
.offer { display: flex; align-items: center; gap: 22px; margin-top: 20px; }
.offer img { width: 180px; height: 120px; object-fit: cover; border-radius: 10px; }
.offer-copy { flex: 1; }
.offer-copy h3 { margin: 8px 0; }
.offer-copy p { margin: 6px 0; color: #667085; }
.prices { text-align: right; white-space: nowrap; }
.prices del { display: block; color: #98a2b3; }
.prices strong { color: #e53935; font-size: 30px; }
.prices span { color: #667085; }
@media (max-width: 700px) { .heading, .offer { align-items: flex-start; flex-direction: column; } .offer img { width: 100%; } .prices { text-align: left; } }
</style>
