<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>智慧酒店</h1>

      <p class="subtitle">
        智慧酒店预订与运营管理系统
      </p>

      <el-form :model="form" label-width="70px">

        <el-form-item label="账号">
          <el-input
              v-model="form.username"
              placeholder="请输入用户名"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
          />
        </el-form-item>

        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" maxlength="5" placeholder="输入图片验证码" @keyup.enter="handleLogin" />
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" title="点击刷新" @click="loadCaptcha" />
            <el-button v-else link @click="loadCaptcha">刷新</el-button>
          </div>
        </el-form-item>

        <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
        >
          登录
        </el-button>

        <div class="account-links">
          <el-button link type="primary" @click="router.push('/register')">注册顾客账号</el-button>
          <el-button link type="primary" @click="router.push('/forgot-password')">找回密码</el-button>
        </div>

      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getCaptcha, login } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const captchaImage = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})

const loadCaptcha = async () => {
  try {
    const { data } = await getCaptcha()
    if (data.code !== 200) throw new Error(data.message)
    form.captchaId = data.data.captchaId
    form.captchaCode = ''
    captchaImage.value = data.data.image
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '验证码加载失败')
  }
}

onMounted(loadCaptcha)

const handleLogin = async () => {

  if (!form.username || !form.password || !form.captchaCode) {
    ElMessage.warning('请输入账号、密码和验证码')
    return
  }

  try {

    loading.value = true

    const response = await login(form)

    const result = response.data

    if (result.code !== 200) {
      ElMessage.error(result.message || '登录失败')
      await loadCaptcha()
      return
    }

    userStore.setLoginInfo(result.data)

    ElMessage.success('登录成功')

    if (result.data.role === 'ADMIN') {

      router.push('/admin')

    } else if (result.data.role === 'STAFF') {

      router.push('/staff')

    } else {

      router.push('/home')
    }

  } catch (error) {

    const message =
        error.response?.data?.message || '无法连接服务器'

    ElMessage.error(message)
    await loadCaptcha()

  } finally {

    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  box-sizing: border-box;
  padding: 16px;

  display: flex;
  justify-content: center;
  align-items: center;

  background: #f5f7fa;
}

.login-card {
  width: min(420px, 100%);
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 5px;
}

.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 30px;
}

.login-tip {
  color: #667085;
  font-size: 13px;
  text-align: center;
  margin-bottom: 20px;
}
.captcha-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.captcha-row img { width: 150px; height: 48px; cursor: pointer; border-radius: 4px; }
.account-links { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; }
.account-links :deep(.el-button + .el-button) { margin-left: 0; }
</style>
