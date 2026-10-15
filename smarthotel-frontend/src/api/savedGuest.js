import request from './request'

export const listSavedGuests = () => request.get('/v1/customer/saved-guests')
export const createSavedGuest = data => request.post('/v1/customer/saved-guests', data)
export const updateSavedGuest = (id, data) => request.put(`/v1/customer/saved-guests/${id}`, data)
export const deleteSavedGuest = id => request.delete(`/v1/customer/saved-guests/${id}`)
