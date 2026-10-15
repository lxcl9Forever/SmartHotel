import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

// Remove credentials left by older versions that shared one login across every tab.
for (const key of ['token', 'username', 'nickname', 'role']) {
  localStorage.removeItem(key)
}

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: {
    ...zhCn,
    el: {
      ...zhCn.el,
      messagebox: { ...zhCn.el.messagebox, confirm: '是', cancel: '否' },
      popconfirm: { ...zhCn.el.popconfirm, confirmButtonText: '是', cancelButtonText: '否' }
    }
  }
})

app.mount('#app')
