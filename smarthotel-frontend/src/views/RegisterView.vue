<template>
  <div class="page">
    <el-card class="card">
      <h2>注册顾客账号</h2>
      <el-form label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" maxlength="50" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" maxlength="50" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="11" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">注册</el-button>
        <el-button @click="router.push('/login')">返回登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', name: '', phone: '', password: '' })
const submit = async () => {
  if (form.username.trim().length < 3 || !form.name.trim() || !/^1\d{10}$/.test(form.phone) || form.password.length < 8) {
    ElMessage.warning('请填写账号、姓名、11位手机号和至少8位密码')
    return
  }
  loading.value = true
  try {
    const { data } = await register(form)
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '注册失败')
  } finally { loading.value = false }
}
</script>
<style scoped>
.page { min-height: 100vh; display: grid; place-items: center; background: #f5f7fa; }
.card { width: min(460px, 95vw); }
</style>
