import request from './request'

export function createBooking(data) {
    return request({
        url: '/v1/bookings',
        method: 'post',
        data
    })
}

export function getBookingDetail(orderId) {
    return request({
        url: `/v1/bookings/${orderId}`,
        method: 'get'
    })
}

export function getMyBookings() {
    return request({
        url: '/v1/bookings/my',
        method: 'get'
    })
}

export function cancelBooking(orderId) {
    return request({
        url: `/v1/bookings/${orderId}/cancel`,
        method: 'post'
    })
}

export function mockPayBooking(orderId) {
    return request({
        url: `/v1/bookings/${orderId}/mock-pay`,
        method: 'post'
    })
}

export function createAlipayPage(orderId) {
    return request({
        url: '/v1/payment/create',
        method: 'post',
        params: { orderId }
    })
}

export function queryAlipayPayment(orderId) {
    return request({
        url: '/v1/payment/query',
        method: 'post',
        params: { orderId }
    })
}

export function previewExtendStay(data){

    return request({
        url:'/v1/customer/stay/preview',
        method:'post',
        data
    })

}


export function extendStay(data){

    return request({
        url:'/v1/customer/stay/extend',
        method:'post',
        data
    })

}
