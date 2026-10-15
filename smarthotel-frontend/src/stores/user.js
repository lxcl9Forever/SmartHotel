import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: sessionStorage.getItem('token') || '',
        username: sessionStorage.getItem('username') || '',
        nickname: sessionStorage.getItem('nickname') || '',
        role: sessionStorage.getItem('role') || ''
    }),

    actions: {
        setLoginInfo(data) {
            this.token = data.token
            this.username = data.username
            this.nickname = data.nickname
            this.role = data.role

            sessionStorage.setItem('token', data.token)
            sessionStorage.setItem('username', data.username)
            sessionStorage.setItem('nickname', data.nickname || '')
            sessionStorage.setItem('role', data.role)
        },

        logout() {
            this.token = ''
            this.username = ''
            this.nickname = ''
            this.role = ''

            sessionStorage.removeItem('token')
            sessionStorage.removeItem('username')
            sessionStorage.removeItem('nickname')
            sessionStorage.removeItem('role')
        }
    }
})
