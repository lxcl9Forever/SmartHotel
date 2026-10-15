<template>
  <el-tag type="warning">{{ remaining > 0 ? `支付剩余 ${clock}` : '支付时间已到，请刷新订单状态' }}</el-tag>
</template>
<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
const props = defineProps({ expireTime: { type: String, required: true } })
const now = ref(Date.now())
let timer
const remaining = computed(() => Math.max(0, Math.ceil((new Date(props.expireTime.replace(' ', 'T')).getTime() - now.value) / 1000) || 0))
const clock = computed(() => `${String(Math.floor(remaining.value / 60)).padStart(2, '0')}:${String(remaining.value % 60).padStart(2, '0')}`)
onMounted(() => { timer = setInterval(() => { now.value = Date.now() }, 1000) })
onUnmounted(() => clearInterval(timer))
</script>
