import request from './request'

export const getCheckinOverview = () => request.get('/v1/customer/checkin')
export const claimCheckin = () => request.post('/v1/customer/checkin')
export const getCheckinRewards = () => request.get('/v1/admin/checkin/rewards')
export const updateCheckinReward = (day, amount) => request.put(`/v1/admin/checkin/rewards/${day}`, { amount })
