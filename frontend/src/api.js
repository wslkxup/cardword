import axios from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: 10000
})

// 每次请求自动带上 token
api.interceptors.request.use(config => {
  const token = getLocalToken()
  if (token) {
    config.headers['Authorization'] = token
  }
  return config
})

export function getLocalToken() {
  return localStorage.getItem('cardword_token') || null
}

export function setLocalToken(token) {
  localStorage.setItem('cardword_token', token)
}

export function getLocalUserId() {
  const userId = localStorage.getItem('cardword_user_id')
  return userId ? parseInt(userId) : null
}

export function setLocalUserId(userId) {
  localStorage.setItem('cardword_user_id', userId.toString())
}

// ==================== 卡片相关接口 ====================

export function getCards(page = 1, size = 10) {
  return api.get('/cards', { params: { page, size } })
}

export function getRandomCards(size = 10, excludeIds = []) {
  const params = { size }
  if (excludeIds.length > 0) {
    params.excludeIds = excludeIds.join(',')
  }
  return api.get('/cards/random', { params }).then(res => res.data)
}

export function getMyCards(page = 1, size = 10) {
  return api.get('/cards/my', { params: { page, size } })
}

export function publishCard(content, nickname, imageUrl = null, isAnonymous = 0) {
  return api.post('/cards', { content, nickname, imageUrl, isAnonymous })
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/cards/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data)
}

export function likeCard(id) {
  return api.post(`/cards/${id}/like`)
}

export function deleteCard(cardId) {
  return api.delete(`/cards/${cardId}`).then(res => res.data)
}

export function followCard(cardId) {
  return api.post(`/cards/${cardId}/follow`).then(res => res.data)
}

export function getFollowedCards(page = 1, size = 10) {
  return api.get('/cards/followed', { params: { page, size } })
}

export function getFollowedCardIds() {
  return api.get('/cards/followed-ids').then(res => res.data)
}

// ==================== 评论相关接口 ====================

export function getComments(cardId) {
  return api.get(`/cards/${cardId}/comments`).then(res => res.data)
}

export function addComment(cardId, content, nickname, parentId) {
  return api.post(`/cards/${cardId}/comments`, { content, nickname, parentId })
    .then(res => res.data)
}

// ==================== 用户认证接口 ====================

export function login(nickname, pwd) {
  return api.post('/users/login', { nickname, pwd }).then(res => res.data)
}

export function register(nickname, pwd) {
  return api.post('/users/register', { nickname, pwd }).then(res => res.data)
}

export function logout() {
  return api.post('/users/logout').then(res => res.data)
}

export function changePassword(oldPwd, newPwd) {
  return api.post('/users/changePassword', { oldPwd, newPwd }).then(res => res.data)
}

export function getUserInfo() {
  return api.get('/users/info').then(res => res.data)
}

// ==================== 公告相关接口 ====================

export function getAnnouncements() {
  return api.get('/announcements').then(res => res.data)
}

export function getLatestAnnouncement(since) {
  const params = since ? { since } : {}
  return api.get('/announcements/latest', { params }).then(res => res.data)
}

// ==================== 问题反馈接口 ====================

export function submitFeedback(title, content, userId = 0) {
  return api.post('/feedback', { title, content, userId }).then(res => res.data)
}
