import request from './request'

export function recommendRooms(data) {
  return request({ url: '/v1/customer/ai/recommend', method: 'post', data, timeout: 30000 })
}
