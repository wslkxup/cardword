import axios from 'axios'

/**
 * 创建 axios 实例，统一配置基础路径和超时时间
 * baseURL 设为 '/api'，前端开发时由 Vite 代理转发到后端服务
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 从 localStorage 中读取当前登录用户的 ID
 * 用户登录成功后会将 userId 存储到 localStorage，这样刷新页面后仍能保持登录状态
 *
 * @returns {number|null} 用户ID（整数），未登录时返回 null
 */
export function getLocalUserId() {
  let userId = localStorage.getItem('cardword_user_id')
  if (!userId) {
    userId = null
  }
  return userId ? parseInt(userId) : null
}

/**
 * 将用户ID保存到 localStorage，在登录/注册成功后调用
 *
 * @param {number} userId - 要保存的用户ID
 */
export function setLocalUserId(userId) {
  localStorage.setItem('cardword_user_id', userId.toString())
}

// ==================== 卡片相关接口 ====================

/**
 * 分页获取卡片列表
 */
export function getCards(page = 1, size = 10) {
  return api.get('/cards', { params: { page, size } })
}

/**
 * 随机获取卡片列表（用于首页"换一批"功能）
 *
 * @param {number} size - 获取数量，默认 10
 */
export function getRandomCards(size = 10) {
  return api.get('/cards/random', { params: { size } })
}

/**
 * 获取指定用户发布的卡片列表（"我的"标签页）
 *
 * @param {number} userId - 用户ID
 * @param {number} page   - 页码
 * @param {number} size   - 每页条数
 */
export function getMyCards(userId, page = 1, size = 10) {
  return api.get('/cards/my', { params: { userId, page, size } })
}

/**
 * 发布新卡片
 *
 * @param {string} content  - 卡片文本内容
 * @param {string} nickname - 发布者昵称（未登录时后端会使用匿名用户）
 * @param {number} userId   - 当前登录用户 ID
 * @param {string} imageUrl - 图片 URL（可选）
 */
export function publishCard(content, nickname, userId, imageUrl = null) {
  return api.post('/cards', { content, nickname, userId, imageUrl })
}

/**
 * 上传图片
 *
 * @param {File} file - 要上传的图片文件
 * @returns {Promise<object>} 成功返回 {url: string}，失败返回 {error: "..."}
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/cards/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => res.data)
}

/**
 * 给卡片点赞
 *
 * @param {number} id - 卡片ID
 */
export function likeCard(id) {
  return api.post(`/cards/${id}/like`)
}

/**
 * 删除卡片（仅限本人操作）
 * 后端会校验 userId 是否为卡片的发布者，防止越权删除
 *
 * @param {number} cardId - 要删除的卡片ID
 * @param {number} userId - 当前登录用户ID，用于后端权限校验
 * @returns {Promise<object>} 成功返回 {success: true}，失败返回 {error: "..."}
 */
export function deleteCard(cardId, userId) {
  return api.delete(`/cards/${cardId}`, { params: { userId } })
    .then(res => res.data)
}

/**
 * 追/取消追卡片（toggle）
 */
export function followCard(cardId, userId) {
  return api.post(`/cards/${cardId}/follow`, { userId })
    .then(res => res.data)
}

/**
 * 获取用户追的卡片列表
 */
export function getFollowedCards(userId, page = 1, size = 10) {
  return api.get('/cards/followed', { params: { userId, page, size } })
}

// ==================== 评论相关接口 ====================

/**
 * 获取指定卡片下的所有评论
 *
 * @param {number} cardId - 卡片ID
 * @returns {Promise<Array>} 评论列表
 */
export function getComments(cardId) {
  return api.get(`/cards/${cardId}/comments`)
    .then(res => res.data)
}

/**
 * 发表评论（需要登录）
 * 自动从 localStorage 获取当前用户ID附加到请求体中
 *
 * @param {number} cardId  - 评论所属的卡片ID
 * @param {string} content - 评论内容
 * @param {string} nickname - 昵称（目前未使用，后端通过 userId 获取）
 * @returns {Promise<object>} 创建好的评论对象
 */
export function addComment(cardId, content, nickname, parentId) {
  const userId = getLocalUserId()
  return api.post(`/cards/${cardId}/comments`, { content, nickname, userId, parentId })
    .then(res => res.data)
}

// ==================== 用户认证接口 ====================

/**
 * 用户登录
 *
 * @param {string} nickname - 用户名
 * @param {string} pwd      - 密码
 * @returns {Promise<object>} 成功返回 {userId: number}，失败返回 {error: "..."}
 */
export function login(nickname, pwd) {
  return api.post('/users/login', { nickname, pwd })
    .then(res => res.data)
}

/**
 * 用户注册
 *
 * @param {string} nickname - 用户名（不能为空，不能与已有用户重复）
 * @param {string} pwd      - 密码（不能为空）
 * @returns {Promise<object>} 成功返回 {userId: number}，失败返回 {error: "..."}
 */
export function register(nickname, pwd) {
  return api.post('/users/register', { nickname, pwd })
    .then(res => res.data)
}

// ==================== 公告相关接口 ====================

/**
 * 获取公告列表（按时间倒序）
 */
export function getAnnouncements() {
  return api.get('/announcements').then(res => res.data)
}

/**
 * 查询是否有新公告
 *
 * @param {string} since - ISO 时间字符串，查询该时间之后是否有新公告
 */
export function getLatestAnnouncement(since) {
  const params = since ? { since } : {}
  return api.get('/announcements/latest', { params }).then(res => res.data)
}

// ==================== 问题反馈接口 ====================

/**
 * 提交问题反馈
 *
 * @param {string} title   - 问题标题
 * @param {string} content - 问题描述
 * @returns {Promise<object>} 成功返回 {success: true}，失败返回 {error: "..."}
 */
export function submitFeedback(title, content) {
  return api.post('/feedback', { title, content })
    .then(res => res.data)
}
