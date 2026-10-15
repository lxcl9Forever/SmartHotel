import request from './request'

export const getAdminUsers = params => request.get('/v1/admin/users', { params })
export const setAdminUserStatus = (id, status) => request.post(`/v1/admin/users/${id}/status`, null, { params: { status } })
export const getAdminEmployees = params => request.get('/v1/admin/employees', { params })
export const createAdminEmployee = data => request.post('/v1/admin/employees', data)
export const updateAdminEmployee = (id, data) => request.put(`/v1/admin/employees/${id}`, data)
export const getAdminPayments = params => request.get('/v1/admin/payments', { params })

// 管理员查询全部房型
export function getAdminRoomTypes() {
    return request({
        url: '/v1/admin/room-types',
        method: 'get'
    })
}

// 管理员查询实体客房
export function getAdminRooms(params) {
    return request({
        url: '/v1/admin/rooms',
        method: 'get',
        params
    })
}
// 查询每日房量
export function getAdminInventories(params) {
    return request({
        url: '/v1/admin/inventories',
        method: 'get',
        params
    })
}

// 查询每日房价
export function getAdminPrices(params) {
    return request({
        url: '/v1/admin/prices',
        method: 'get',
        params
    })
}

// 修改每日房量
export function updateAdminInventory(data) {
    return request({
        url: '/v1/admin/inventories',
        method: 'put',
        data
    })
}

// 修改每日房价
export function updateAdminPrice(data) {
    return request({
        url: '/v1/admin/prices',
        method: 'put',
        data
    })
}

// 重新生成日期区间经营统计
export function refreshStatisticsRange(params) {
    return request({
        url: '/v1/admin/statistics/refresh-range',
        method: 'post',
        params
    })
}


// 查询每日经营统计
export function getDailyStatistics(params) {
    return request({
        url: '/v1/admin/statistics/daily',
        method: 'get',
        params
    })
}

export function getAdminOrders(params) {
    return request({
        url: '/v1/admin/orders',
        method: 'get',
        params
    })
}

export function getAdminOrderDetail(orderId) {
    return request({
        url: `/v1/admin/orders/${orderId}`,
        method: 'get'
    })
}

export function getAdminRefunds(params) {
    return request.get('/v1/admin/refund/list', { params })
}

export function approveAdminRefund(refundId) {
    return request.post(`/v1/admin/refund/${refundId}/approve`)
}

export function rejectAdminRefund(refundId) {
    return request.post(`/v1/admin/refund/${refundId}/reject`)
}
