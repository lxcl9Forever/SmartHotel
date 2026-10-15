import request from './request'

export function login(data) {
    return request({
        url: '/v1/auth/login',
        method: 'post',
        data
    })
}

export const register = data => request.post('/v1/auth/register', data)
export const getCaptcha = () => request.get('/v1/auth/captcha')
export const resetPassword = data => request.post('/v1/auth/reset-password', data)
export const getProfile = () => request.get('/v1/customer/profile')
export const updateProfile = data => request.put('/v1/customer/profile', data)
export const changePassword = data => request.put('/v1/customer/profile/password', data)
export const bindIdCard = data => request.post('/v1/customer/profile/id-card', data)
