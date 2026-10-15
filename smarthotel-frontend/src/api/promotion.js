import request from './request'

export const listPromotions = () => request.get('/v1/admin/promotion/list')
export const createPromotion = data => request.post('/v1/admin/promotion', data)
export const updatePromotion = (id, data) => request.put(`/v1/admin/promotion/${id}`, data)
export const deletePromotion = id => request.delete(`/v1/admin/promotion/${id}`)
export const setPromotionEnabled = (id, enabled) =>
  request.put(`/v1/admin/promotion/${id}/enabled`, null, { params: { enabled } })
