<template>
  <nav v-if="total > 0" class="app-pagination" aria-label="分页导航">
    <span class="total">共 {{ total }} 条</span>
    <select :value="pageSize" aria-label="每页条数" @change="changeSize">
      <option v-for="size in pageSizes" :key="size" :value="size">{{ size }} 条/页</option>
    </select>
    <button type="button" :disabled="page <= 1" @click="go(1)">首页</button>
    <button type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
    <button v-for="number in visiblePages" :key="number" type="button"
            :class="{ active: number === page }" :aria-current="number === page ? 'page' : undefined"
            @click="go(number)">{{ number }}</button>
    <button type="button" :disabled="page >= pageCount" @click="go(page + 1)">下一页</button>
    <button type="button" :disabled="page >= pageCount" @click="go(pageCount)">末页</button>
    <label>跳至 <input v-model="jumpPage" type="number" min="1" :max="pageCount" aria-label="跳转页码" @keydown.enter="jump" /> 页</label>
    <button type="button" @click="jump">跳转</button>
  </nav>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  pageSizes: { type: Array, default: () => [10, 20, 50] }
})
const emit = defineEmits(['update:currentPage', 'update:pageSize', 'current-change', 'size-change'])
const jumpPage = ref('')
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const page = computed(() => Math.min(Math.max(1, props.currentPage), pageCount.value))
const visiblePages = computed(() => {
  const start = Math.max(1, Math.min(page.value - 2, pageCount.value - 4))
  return Array.from({ length: Math.min(5, pageCount.value - start + 1) }, (_, index) => start + index)
})

function go(value) {
  const requested = Number(value)
  if (!Number.isFinite(requested)) return
  const next = Math.min(Math.max(1, Math.trunc(requested)), pageCount.value)
  if (next === props.currentPage) return
  emit('update:currentPage', next)
  emit('current-change', next)
}
function changeSize(event) {
  const next = Number(event.target.value)
  if (next === props.pageSize) return
  emit('update:pageSize', next)
  emit('update:currentPage', 1)
  emit('size-change', next)
}
function jump() {
  if (jumpPage.value === '') return
  go(jumpPage.value)
  jumpPage.value = ''
}
watch(pageCount, () => {
  if (props.currentPage > pageCount.value) go(pageCount.value)
})
</script>

<style scoped>
.app-pagination { display: flex; justify-content: flex-end; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 18px; color: #475467; }
.total { margin-right: 6px; white-space: nowrap; }
button, select, input { box-sizing: border-box; height: 36px; border: 1px solid #e1e5eb; border-radius: 4px; background: #fff; color: #344054; font: inherit; }
button { min-width: 36px; padding: 0 10px; cursor: pointer; }
button:hover:not(:disabled) { border-color: #409eff; color: #409eff; }
button.active, button.active:hover { border-color: #409eff; background: #409eff; color: #fff; }
button:disabled { color: #aeb6c2; background: #f5f6f8; cursor: not-allowed; }
select { padding: 0 28px 0 10px; }
label { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
input { width: 58px; padding: 0 5px; text-align: center; }
input::-webkit-inner-spin-button, input::-webkit-outer-spin-button { appearance: none; margin: 0; }
</style>
