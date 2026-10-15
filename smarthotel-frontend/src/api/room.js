import request from './request'

export const getHotelInfo = () => request.get('/v1/hotel')
export const getRoomTypeDetail = id => request.get(`/v1/hotel/room-types/${id}`)

export function getRoomTypes() {
    return request({
        url: '/v1/room-types',
        method: 'get'
    })
}
export function searchAvailableRoomTypes(params) {
    return request({
        url: '/v1/room-types/available',
        method: 'get',
        params
    })
}
