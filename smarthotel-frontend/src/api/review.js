import request from './request'

export const getMyReview = orderId => request.get(`/v1/customer/reviews/orders/${orderId}`)
export const createReview = (orderId, data) => request.post(`/v1/customer/reviews/orders/${orderId}`, data)
export const getAdminReviews = params => request.get('/v1/admin/reviews', { params })
export const hideReview = reviewId => request.post(`/v1/admin/reviews/${reviewId}/hide`)
export const showReview = reviewId => request.post(`/v1/admin/reviews/${reviewId}/show`)
