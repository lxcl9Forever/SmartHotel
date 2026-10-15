<template>
  <div class="page">
    <el-card class="card">
      <h1>找回密码</h1>
      <p class="hint">请填写顾客账号已绑定的身份证号和手机号。尚未绑定身份证且无法登录的账号，需联系管理员核实身份。</p>
      <el-form label-width="95px" @submit.prevent="submit">
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" autocomplete="tel" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idNumber" maxlength="18" autocomplete="off" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-button type="primary" :loading="saving" class="submit" native-type="submit">设置新密码</el-button>
        <el-button link @click="router.push('/login')">返回登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword } from '../api/auth'

const router = useRouter()
const saving = ref(false)
const confirmPassword = ref('')
const form = reactive({ phone: '', idNumber: '', newPassword: '' })

const submit = async () => {
  if (!/^1\d{10}$/.test(form.phone) || !/^\d{17}[\dXx]$/.test(form.idNumber)) {
    ElMessage.warning('请输入正确的手机号和18位身份证号')
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 72 || form.newPassword !== confirmPassword.value) {
    ElMessage.warning('新密码需为8到72位，且两次输入一致')
    return
  }
  saving.value = true
  try {
    const { data } = await resetPassword({ ...form, idNumber: form.idNumber.toUpperCase() })
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('密码已重置，请使用新密码登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '找回密码失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; background: #f5f7fa; }
.card { width: min(460px, 100%); }
h1 { text-align: center; margin-bottom: 20px; }
.hint { color: #667085; line-height: 1.6; margin-bottom: 24px; }
.submit { width: 100%; margin-bottom: 12px; }
</style>
