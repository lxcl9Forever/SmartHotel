import request from './request'

export const getWorkbenchStats = () => request.get('/v1/staff/workbench/stats')
export const getStayHistory = params => request.get('/v1/staff/workbench/history', { params })
export const searchStaffRooms = params => request.get('/v1/staff/rooms', { params })

// 查询前台订单
export function getStaffOrders(status) {
    return request({
        url: '/v1/staff/orders',
        method: 'get',
        params: {
            status
        }
    })
}

export const markOrderNoShow = orderId => request.post(`/v1/staff/orders/${orderId}/no-show`)
export const markOverdueNoShows = () => request.post('/v1/staff/orders/overdue/no-show')

// 查询订单可分配房间
export function getAvailableRooms(orderId) {
    return request({
        url: `/v1/staff/check-ins/orders/${orderId}/available-rooms`,
        method: 'get'
    })
}

// 办理入住
export function checkIn(data) {
    return request({
        url: '/v1/staff/check-ins',
        method: 'post',
        data
    })
}

// 办理退房
export function checkOut(data) {
    return request({
        url: '/v1/staff/check-outs',
        method: 'post',
        data
    })
}

export function completeOrder(orderId){

    return request({

        url:`/v1/staff/orders/${orderId}/complete`,

        method:'post'

    })

}

// 按状态查询实体房间
export function getStaffRooms(status) {
    return request({
        url: '/v1/staff/rooms',
        method: 'get',
        params: {
            status
        }
    })
}

// 开始清洁
export function startCleaning(roomId) {
    return request({
        url: `/v1/staff/rooms/${roomId}/start-cleaning`,
        method: 'post'
    })
}

// 完成清洁
export function finishCleaning(roomId) {
    return request({
        url: `/v1/staff/rooms/${roomId}/finish-cleaning`,
        method: 'post'
    })
}

// 查询订单入住人
export function getOrderGuests(orderId) {
    return request({
        url: `/v1/staff/orders/${orderId}/guests`,
        method: 'get'
    })
}

// 房间进入维修状态
export function startMaintenance(roomId, reason) {
    return request({
        url: `/v1/staff/rooms/${roomId}/maintenance`,
        method: 'post',
        data: {
            reason
        }
    })
}


// 房间停用
export function setOutOfService(roomId, reason) {
    return request({
        url: `/v1/staff/rooms/${roomId}/out-of-service`,
        method: 'post',
        data: {
            reason
        }
    })
}


// 维修 / 停用房恢复为空闲
export function restoreRoom(roomId, reason) {
    return request({
        url: `/v1/staff/rooms/${roomId}/restore`,
        method: 'post',
        data: {
            reason
        }
    })
}

export function previewStaffExtend(data){

    return request({
        url:'/v1/staff/stay/preview',
        method:'post',
        data
    })

}


export function extendStaff(data){

    return request({
        url:'/v1/staff/stay/extend',
        method:'post',
        data
    })

}

export function getCheckedOutOrders(){

    return request({

        url:'/v1/staff/orders',

        method:'get',

        params:{
            status:'CHECKED_OUT'
        }

    })

}
