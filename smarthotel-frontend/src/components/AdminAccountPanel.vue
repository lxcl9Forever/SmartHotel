<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="用户管理" name="users">
      <el-button @click="loadUsers">刷新</el-button>
      <el-table :data="users" v-loading="loading" border style="margin-top: 12px">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="账号" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                       :disabled="row.userId === myUserId" @click="toggle(row)">
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current-page="userQuery.page" v-model:page-size="userQuery.pageSize" :total="userTotal"
                     :page-sizes="[10, 20, 50]"
                     class="pager" @current-change="loadUsers" @size-change="loadUsers" />
    </el-tab-pane>
    <el-tab-pane label="员工管理" name="employees">
      <el-button type="primary" @click="showCreate = true">新增员工</el-button>
      <el-button @click="loadEmployees">刷新</el-button>
      <el-table :data="employees" v-loading="loading" border style="margin-top: 12px">
        <el-table-column prop="employeeNo" label="工号" />
        <el-table-column prop="employeeName" label="姓名" />
        <el-table-column prop="username" label="登录账号" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="position" label="职位" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link @click="toggle({ userId: row.userId, status: row.status })">
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <AppPagination v-model:current-page="employeeQuery.page" v-model:page-size="employeeQuery.pageSize"
                     :total="employeeTotal" :page-sizes="[10, 20, 50]"
                     class="pager" @current-change="loadEmployees" @size-change="loadEmployees" />
    </el-tab-pane>
  </el-tabs>
  <el-dialog v-model="showCreate" title="新增员工" width="500px">
    <el-form label-width="90px">
      <el-form-item label="登录账号"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="工号"><el-input v-model="form.employeeNo" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.employeeName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="职位"><el-input v-model="form.position" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="create">创建</el-button></template>
  </el-dialog>
  <el-dialog v-model="showEdit" title="编辑员工" width="500px">
    <el-form label-width="90px">
      <el-form-item label="姓名"><el-input v-model="editForm.employeeName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
      <el-form-item label="职位"><el-input v-model="editForm.position" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="showEdit = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="saveEdit">保存</el-button></template>
  </el-dialog>
</template>
<script setup>
import AppPagination from './AppPagination.vue'
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAdminEmployee, getAdminEmployees, getAdminUsers, setAdminUserStatus, updateAdminEmployee } from '../api/admin'
import request from '../api/request'
const tab = ref('users'), loading = ref(false), creating = ref(false), showCreate = ref(false)
const users = ref([]), employees = ref([]), userTotal = ref(0), employeeTotal = ref(0), myUserId = ref(null)
const userQuery = reactive({ page: 1, pageSize: 10 }), employeeQuery = reactive({ page: 1, pageSize: 10 })
const form = reactive({ username: '', password: '', employeeNo: '', employeeName: '', phone: '', email: '', position: '' })
const showEdit = ref(false)
const editForm = reactive({ employeeId: null, employeeName: '', phone: '', email: '', position: '' })
const openEdit = row => { Object.assign(editForm, row); showEdit.value = true }
const saveEdit = async () => {
  if (!editForm.employeeName.trim()) { ElMessage.warning('请输入姓名'); return }
  creating.value = true
  try {
    const { data } = await updateAdminEmployee(editForm.employeeId, editForm)
    if (data.code !== 200) throw new Error(data.message)
    showEdit.value = false
    ElMessage.success('员工资料已保存')
    await loadEmployees()
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '保存失败') }
  finally { creating.value = false }
}
const loadUsers = async () => {
  loading.value = true
  try {
    const { data } = await getAdminUsers(userQuery)
    if (data.code !== 200) throw new Error(data.message)
    users.value = data.data.records || []; userTotal.value = data.data.total || 0
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '用户加载失败') }
  finally { loading.value = false }
}
const loadEmployees = async () => {
  loading.value = true
  try {
    const { data } = await getAdminEmployees(employeeQuery)
    if (data.code !== 200) throw new Error(data.message)
    employees.value = data.data.records || []; employeeTotal.value = data.data.total || 0
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '员工加载失败') }
  finally { loading.value = false }
}
const toggle = async row => {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try { await ElMessageBox.confirm(`确定${next === 'ACTIVE' ? '启用' : '停用'}该账号吗？`, '账号管理') }
  catch { return }
  try {
    const { data } = await setAdminUserStatus(row.userId, next)
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('状态已更新')
    await Promise.all([loadUsers(), loadEmployees()])
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '操作失败') }
}
const create = async () => {
  if (!form.username.trim() || form.password.length < 8 || !form.employeeNo.trim() || !form.employeeName.trim()) {
    ElMessage.warning('请填写账号、至少8位密码、工号和姓名'); return
  }
  creating.value = true
  try {
    const { data } = await createAdminEmployee(form)
    if (data.code !== 200) throw new Error(data.message)
    showCreate.value = false
    Object.keys(form).forEach(key => { form[key] = '' })
    ElMessage.success('员工已创建')
    await Promise.all([loadUsers(), loadEmployees()])
  } catch (error) { ElMessage.error(error.response?.data?.message || error.message || '创建失败') }
  finally { creating.value = false }
}
watch(tab, value => { if (value === 'employees') loadEmployees() })
onMounted(async () => {
  loadUsers()
  try { const { data } = await request.get('/v1/user/me'); myUserId.value = Number(data.data.userId || data.data.username) } catch {}
})
</script>
<style scoped>.pager { justify-content: flex-end; margin-top: 15px; }</style>
