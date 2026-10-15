<template>
  <div class="page">
    <el-card class="card" v-loading="loading">
      <template #header><b>个人资料</b></template>
      <el-form label-width="90px">
        <el-form-item label="账号">{{ profile.username }}</el-form-item>
        <el-form-item label="姓名"><el-input v-model="profile.name" maxlength="50" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profile.phone" maxlength="11" /></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
        <el-button @click="router.push('/home')">返回首页</el-button>
      </el-form>
      <el-divider>身份证绑定</el-divider>
      <p class="hint">绑定本人身份证后，可用身份证号和当前手机号找回密码。身份证号绑定后不能自行修改，也不会在此页显示。</p>
      <p v-if="profile.idCardBound">已绑定身份证</p>
      <el-form v-else label-width="90px">
        <el-form-item label="身份证号"><el-input v-model="idCard.idNumber" maxlength="18" autocomplete="off" /></el-form-item>
        <el-form-item label="当前密码"><el-input v-model="idCard.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-button :loading="binding" @click="bindIdentity">绑定身份证</el-button>
      </el-form>
      <el-divider>修改密码</el-divider>
      <el-form label-width="90px">
        <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
        <el-button :loading="changing" @click="change">修改密码</el-button>
      </el-form>
    </el-card>

    <el-card class="card saved-card" v-loading="savedGuestLoading">
      <template #header>
        <div class="card-header">
          <b>常用入住人</b>
          <el-button type="primary" :disabled="savedGuests.length >= 20" @click="openGuestForm()">新增入住人</el-button>
        </div>
      </template>
      <p class="hint">可保存常用入住人的信息，预订时直接选择。仅当前顾客账号可查看和管理，最多保存20位。</p>
      <el-empty v-if="!savedGuestLoading && !savedGuests.length" description="还没有常用入住人" />
      <el-table v-else :data="pagedGuests" style="width: 100%">
        <el-table-column prop="guestName" label="姓名" min-width="100" />
        <el-table-column label="手机号" min-width="140">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="证件" min-width="180">
          <template #default="{ row }">{{ idTypeLabel(row.idType) }} · {{ maskId(row.idNumber) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGuestForm(row)">编辑</el-button>
            <el-button link type="danger" @click="removeGuest(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current-page="guestPage" v-model:page-size="guestPageSize" :total="savedGuests.length" />
    </el-card>

    <el-dialog v-model="guestDialogVisible" :title="guestForm.guestId ? '编辑常用入住人' : '新增常用入住人'" width="min(480px, 94vw)">
      <el-button v-if="!guestForm.guestId" link type="primary" class="use-profile" @click="fillFromProfile">填入我的姓名和手机号</el-button>
      <el-form label-width="90px">
        <el-form-item label="姓名" required><el-input v-model="guestForm.guestName" maxlength="50" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="guestForm.phone" maxlength="11" /></el-form-item>
        <el-form-item label="证件类型">
          <el-select v-model="guestForm.idType" style="width: 100%">
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="护照" value="PASSPORT" />
            <el-option label="其他证件" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="证件号码"><el-input v-model="guestForm.idNumber" maxlength="50" placeholder="可选；预订时也可补充" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="guestDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="guestSaving" @click="saveGuest">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppPagination from '../components/AppPagination.vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bindIdCard, changePassword, getProfile, updateProfile } from '../api/auth'
import { createSavedGuest, deleteSavedGuest, listSavedGuests, updateSavedGuest } from '../api/savedGuest'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const profile = reactive({ username: '', name: '', phone: '', idCardBound: false })
const password = reactive({ oldPassword: '', newPassword: '' })
const idCard = reactive({ idNumber: '', currentPassword: '' })
const loading = ref(false), saving = ref(false), changing = ref(false), binding = ref(false)
const savedGuests = ref([])
const guestPage = ref(1)
const guestPageSize = ref(10)
const pagedGuests = computed(() => savedGuests.value.slice((guestPage.value - 1) * guestPageSize.value, guestPage.value * guestPageSize.value))
const savedGuestLoading = ref(false), guestSaving = ref(false), guestDialogVisible = ref(false)
const guestForm = reactive({ guestId: null, guestName: '', phone: '', idType: 'ID_CARD', idNumber: '' })

const loadSavedGuests = async () => {
  savedGuestLoading.value = true
  try {
    const { data } = await listSavedGuests()
    if (data.code !== 200) throw new Error(data.message)
    savedGuests.value = data.data || []
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '常用入住人加载失败') }
  finally { savedGuestLoading.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getProfile()
    if (data.code !== 200) throw new Error(data.message)
    Object.assign(profile, data.data)
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '资料加载失败') }
  finally { loading.value = false }
  await loadSavedGuests()
})

const save = async () => {
  if (!profile.name.trim() || !/^1\d{10}$/.test(profile.phone)) { ElMessage.warning('请填写姓名和11位手机号'); return }
  saving.value = true
  try {
    const { data } = await updateProfile({ name: profile.name.trim(), phone: profile.phone })
    if (data.code !== 200) throw new Error(data.message)
    Object.assign(profile, data.data)
    userStore.nickname = profile.name
    sessionStorage.setItem('nickname', profile.name)
    ElMessage.success('资料已保存')
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '保存失败') }
  finally { saving.value = false }
}

const change = async () => {
  if (!password.oldPassword || password.newPassword.length < 8) { ElMessage.warning('新密码至少8位'); return }
  changing.value = true
  try {
    const { data } = await changePassword(password)
    if (data.code !== 200) throw new Error(data.message)
    password.oldPassword = ''; password.newPassword = ''
    ElMessage.success('密码已修改')
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '修改失败') }
  finally { changing.value = false }
}

const bindIdentity = async () => {
  if (!/^\d{17}[\dXx]$/.test(idCard.idNumber) || !idCard.currentPassword) {
    ElMessage.warning('请输入18位身份证号和当前密码')
    return
  }
  binding.value = true
  try {
    const { data } = await bindIdCard({ idNumber: idCard.idNumber.toUpperCase(), currentPassword: idCard.currentPassword })
    if (data.code !== 200) throw new Error(data.message)
    idCard.idNumber = ''
    idCard.currentPassword = ''
    profile.idCardBound = true
    ElMessage.success('身份证已绑定')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

const openGuestForm = (guest = null) => {
  Object.assign(guestForm, {
    guestId: guest?.guestId ?? null,
    guestName: guest?.guestName ?? '',
    phone: guest?.phone ?? '',
    idType: guest?.idType ?? 'ID_CARD',
    idNumber: guest?.idNumber ?? ''
  })
  guestDialogVisible.value = true
}

const fillFromProfile = () => {
  guestForm.guestName = profile.name || ''
  guestForm.phone = profile.phone || ''
}

const saveGuest = async () => {
  const guestName = guestForm.guestName.trim()
  const phone = guestForm.phone.trim()
  if (!guestName || !/^1\d{10}$/.test(phone)) { ElMessage.warning('请填写入住人姓名和11位手机号'); return }
  guestSaving.value = true
  try {
    const payload = { guestName, phone, idType: guestForm.idType, idNumber: guestForm.idNumber.trim() || null }
    const { data } = guestForm.guestId
      ? await updateSavedGuest(guestForm.guestId, payload)
      : await createSavedGuest(payload)
    if (data.code !== 200) throw new Error(data.message)
    guestDialogVisible.value = false
    ElMessage.success('常用入住人已保存')
    await loadSavedGuests()
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '保存常用入住人失败') }
  finally { guestSaving.value = false }
}

const removeGuest = async guest => {
  try {
    await ElMessageBox.confirm(`确定删除“${guest.guestName}”吗？`, '删除常用入住人', { type: 'warning' })
    const { data } = await deleteSavedGuest(guest.guestId)
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('已删除')
    await loadSavedGuests()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.response?.data?.message || error.message || '删除失败')
  }
}

const maskPhone = phone => phone ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : '未填写'
const maskId = id => id ? (id.length > 8 ? `${id.slice(0, 4)}****${id.slice(-4)}` : '****') : '未填写'
const idTypeLabel = type => ({ ID_CARD: '身份证', PASSPORT: '护照', OTHER: '其他证件' })[type] || '证件'
</script>

<style scoped>
.page { min-height: 100vh; padding: 40px 16px; background: #f5f7fa; }
.card { max-width: 760px; margin: 0 auto; }
.saved-card { margin-top: 24px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #667085; line-height: 1.6; }
.use-profile { margin-bottom: 12px; }
</style>
