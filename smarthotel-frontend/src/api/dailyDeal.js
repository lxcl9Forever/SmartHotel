import request from './request'

export const listDailyDeals = () => request.get('/v1/daily-deals')
export const listAdminDailyDeals = () => request.get('/v1/admin/daily-deals')
export const createDailyDeal = data => request.post('/v1/admin/daily-deals', data)
export const updateDailyDeal = (id, data) => request.put(`/v1/admin/daily-deals/${id}`, data)
export const setDailyDealEnabled = (id, enabled) =>
  request.put(`/v1/admin/daily-deals/${id}/enabled`, null, { params: { enabled } })
export const deleteDailyDeal = id => request.delete(`/v1/admin/daily-deals/${id}`)
