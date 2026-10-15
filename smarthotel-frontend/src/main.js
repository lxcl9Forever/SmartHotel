import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
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
app.use(ElementPlus)

app.mount('#app')
