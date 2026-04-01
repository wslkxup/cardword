<template>
  <div
    class="card-item"
    :class="{ 'card-deleting': deleting }"
    ref="cardRef"
    :style="{ animationDelay: `${index * 0.08}s` }"
    @mousemove="handleMouseMove"
    @mouseleave="handleMouseLeave"
    @click="onCardClick"
  >
    <div class="card-accent" :style="{ background: accentGradient }"></div>

    <div class="card-header">
      <div class="card-author-wrap">
        <div class="card-avatar">
          <template v-if="card.isAnonymous === 1 && !isOwner">匿</template>
          <template v-else>{{ (card.user?.nickname || '匿')[0] }}</template>
        </div>
        <span class="card-author">
          <template v-if="card.isAnonymous === 1 && !isOwner">匿名卡片</template>
          <template v-else>{{ card.user?.nickname || '匿名用户' }}</template>
        </span>
        <span v-if="card.isAnonymous === 1 && isOwner" class="anonymous-badge">匿名卡片</span>
      </div>
      <button v-if="isOwner" class="btn-delete" @click="showConfirm = true">删除</button>
    </div>

    <div class="card-content">{{ card.content }}</div>

    <!-- 卡片图片 -->
    <div v-if="card.imageUrl" class="card-image" @click.stop>
      <img :src="card.imageUrl" alt="卡片图片" @click="previewImage(card.imageUrl)" />
    </div>

    <div class="card-footer">
      <span class="card-time">{{ formatTime(card.createdAt) }}</span>
      <div class="card-actions">
        <button class="btn-like" @click="handleLike" title="点赞">
          <span class="like-icon" :class="{ 'like-burst': justLiked }">❤</span>
          <span class="like-count" :class="{ 'count-pop': countAnimating }">{{ card.likesCount }}</span>
        </button>
        <button class="btn-comment" @click="toggleComments" title="评论">
          💬 {{ card.commentCount || 0 }}
        </button>
        <button class="btn-follow" :class="{ followed }" @click="handleFollow" :title="followed ? '取消追' : '追'">
          <span class="follow-icon" :class="{ 'follow-burst': justFollowed }">{{ followed ? '★' : '☆' }}</span>
          <span class="follow-pop" v-if="justFollowed">追！</span>
        </button>
      </div>
    </div>

    <Transition name="comment-slide">
      <CommentList
        v-if="showComments"
        :card-id="card.id"
        :user-id="userId"
        :card="card"
        @commented="onCommented"
      />
    </Transition>

    <!-- 卡片详情弹窗 -->
    <Teleport to="body">
      <Transition name="confirm">
        <div v-if="showDetail" class="detail-overlay" @click.self="showDetail = false">
          <div class="detail-box">
            <!-- 关闭按钮 -->
            <button class="detail-close" @click="showDetail = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>

            <!-- 发布者信息 -->
            <div class="detail-header">
              <div class="detail-avatar">
                <template v-if="card.isAnonymous === 1 && !isOwner">匿</template>
                <template v-else>{{ (card.user?.nickname || '匿')[0] }}</template>
              </div>
              <div class="detail-info">
                <span class="detail-nickname">
                  <template v-if="card.isAnonymous === 1 && !isOwner">匿名用户</template>
                  <template v-else>{{ card.user?.nickname || '匿名用户' }}</template>
                </span>
                <span v-if="card.isAnonymous === 1 && isOwner" class="anonymous-badge">匿名卡片</span>
                <span class="detail-time">{{ formatFullTime(card.createdAt) }}</span>
              </div>
            </div>

            <div class="detail-divider"></div>

            <!-- 卡片内容 -->
            <div class="detail-content">{{ card.content }}</div>

            <!-- 详情图片 -->
            <div v-if="card.imageUrl" class="detail-image" @click.stop>
              <img :src="card.imageUrl" alt="卡片图片" @click="previewImage(card.imageUrl)" />
            </div>

            <div class="detail-divider"></div>

            <!-- 评论列表 -->
            <div class="detail-comments">
              <p class="detail-comments-title">评论 ({{ card.commentCount || 0 }})</p>
              <CommentList
                :card-id="card.id"
                :user-id="userId"
                :card="card"
                @commented="onCommented"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 删除确认弹窗 - Teleport 到 body 确保全屏遮罩 -->
    <Teleport to="body">
      <Transition name="confirm">
        <div v-if="showConfirm" class="confirm-overlay" @click.self="showConfirm = false">
          <div class="confirm-box">
            <div class="confirm-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            </div>
            <p class="confirm-title">确定删除这张卡片吗？</p>
            <p class="confirm-desc">删除后不可恢复</p>
            <div class="confirm-actions">
              <button class="confirm-cancel" @click="showConfirm = false">取消</button>
              <button class="confirm-delete" @click="handleDelete">删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, inject, nextTick, onUnmounted } from 'vue'
import { likeCard, deleteCard, followCard } from '../api.js'
import CommentList from './CommentList.vue'

const props = defineProps({
  card: Object,
  userId: Number,
  index: { type: Number, default: 0 },
  initialFollowed: { type: Boolean, default: false }
})

const emit = defineEmits(['liked', 'deleted', 'followed'])
const showToast = inject('showToast')

const justLiked = ref(false)
const countAnimating = ref(false)
const followed = ref(props.initialFollowed)
const justFollowed = ref(false)
const showComments = ref(false)
const showConfirm = ref(false)
const showDetail = ref(false)
const deleting = ref(false)
const cardRef = ref(null)

watch(() => props.initialFollowed, (val) => {
  followed.value = val
})

const isOwner = computed(() => props.userId && props.card.userId === props.userId)

const accentGradient = computed(() => {
  const gradients = [
    'linear-gradient(135deg, #4361ee, #7c3aed)',
    'linear-gradient(135deg, #f72585, #b5179e)',
    'linear-gradient(135deg, #4cc9f0, #4361ee)',
    'linear-gradient(135deg, #06d6a0, #118ab2)',
    'linear-gradient(135deg, #ffd166, #ef476f)',
    'linear-gradient(135deg, #7c3aed, #f72585)',
    'linear-gradient(135deg, #118ab2, #073b4c)',
    'linear-gradient(135deg, #ef476f, #ffd166)',
  ]
  return gradients[props.card.id % gradients.length]
})

// ========== 点击卡片查看详情 ==========
function onCardClick(e) {
  // 点击按钮/链接/输入框/评论区域时不弹详情
  if (e.target.closest('button, a, input, .comment-section')) return
  showDetail.value = true
}

// ========== 3D 倾斜 ==========
function handleMouseMove(e) {
  const card = e.currentTarget
  const rect = card.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const rotateX = ((y - centerY) / centerY) * -6
  const rotateY = ((x - centerX) / centerX) * 6
  card.style.transition = 'box-shadow 0.3s ease'
  card.style.transform = `perspective(800px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-4px)`
}

function handleMouseLeave(e) {
  const card = e.currentTarget
  card.style.transition = 'transform 0.4s ease, box-shadow 0.3s ease'
  card.style.transform = ''
}

function toggleComments() {
  if (!props.userId) {
    showToast('请先登录再评论', 'warning')
    return
  }
  showComments.value = !showComments.value
}

watch(() => props.userId, (val) => {
  if (!val) showComments.value = false
})

// 点击卡片外部时自动收起评论区
function onClickOutside(e) {
  if (cardRef.value && !cardRef.value.contains(e.target)) {
    showComments.value = false
  }
}

watch(showComments, (val) => {
  if (val) {
    nextTick(() => document.addEventListener('click', onClickOutside))
  } else {
    document.removeEventListener('click', onClickOutside)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

async function handleLike() {
  if (justLiked.value) return
  const res = await likeCard(props.card.id)
  justLiked.value = true
  countAnimating.value = true
  setTimeout(() => { justLiked.value = false }, 600)
  setTimeout(() => { countAnimating.value = false }, 400)
  emit('liked', props.card.id, res.data.likesCount)
}

async function handleFollow() {
  if (!props.userId) {
    showToast('请先登录', 'warning')
    return
  }
  const res = await followCard(props.card.id, props.userId)
  followed.value = res.followed
  if (followed.value) {
    justFollowed.value = true
    setTimeout(() => { justFollowed.value = false }, 800)
    showToast('已追！将持续关注该卡片', 'success')
  } else {
    showToast('已取消追', 'info')
  }
  emit('followed', props.card.id, followed.value)
}

async function handleDelete() {
  showConfirm.value = false
  const res = await deleteCard(props.card.id, props.userId)
  if (res.success) {
    deleting.value = true
    setTimeout(() => emit('deleted', props.card.id), 500)
  } else {
    showToast(res.error || '删除失败', 'error')
  }
}

function onCommented() {
  props.card.commentCount = (props.card.commentCount || 0) + 1
}

// ========== 相对时间 ==========
function formatTime(dt) {
  if (!dt) return ''
  const now = new Date()
  const date = new Date(dt)
  const diffSec = Math.floor((now - date) / 1000)
  if (diffSec < 60) return '刚刚'
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay}天前`
  return date.toLocaleString('zh-CN', {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatFullTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ========== 图片预览 ==========
function previewImage(url) {
  // 创建全屏图片预览
  const overlay = document.createElement('div')
  overlay.className = 'image-preview-overlay'
  overlay.innerHTML = `
    <div class="image-preview-inner" @click.self="closePreview()">
      <img src="${url}" />
      <button class="image-preview-close" @click="closePreview()">×</button>
    </div>
  `
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    cursor: zoom-out;
  `
  
  const inner = overlay.querySelector('.image-preview-inner')
  inner.style.cssText = `
    position: relative;
    max-width: 90%;
    max-height: 90%;
  `
  
  const img = overlay.querySelector('img')
  img.style.cssText = `
    max-width: 100%;
    max-height: 90vh;
    object-fit: contain;
    cursor: zoom-out;
  `
  
  const closeBtn = overlay.querySelector('.image-preview-close')
  closeBtn.style.cssText = `
    position: absolute;
    top: -40px;
    right: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    color: #fff;
    border: none;
    font-size: 24px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
  `
  
  function closePreview() {
    overlay.remove()
  }
  
  closeBtn.onclick = closePreview
  overlay.onclick = closePreview
  
  document.body.appendChild(overlay)
}
</script>

<style scoped>
.card-item {
  background: linear-gradient(145deg, var(--color-bg-card) 0%, var(--color-bg-card-end) 100%);
  border-radius: 16px;
  padding: 24px;
  padding-top: 27px;
  position: relative;
  border: 1px solid var(--color-border);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 12px 24px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.3s ease;
  overflow: hidden;
  break-inside: avoid;
  margin-bottom: 20px;
  animation: cardEnter 0.5s ease backwards;
  will-change: transform;
  cursor: pointer;
}


@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.card-item:hover {
  box-shadow:
    0 4px 8px rgba(0, 0, 0, 0.06),
    0 12px 28px rgba(67, 97, 238, 0.1),
    0 20px 40px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.card-author-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.card-author {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: 0.5px;
}

.detail-nickname {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: 0.5px;
}

.btn-delete {
  background: none;
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete:hover {
  background: var(--color-danger);
  color: #fff;
}

.card-content {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 16px;
  word-break: break-word;
  color: var(--color-text-content);
}

.card-image {
  margin-bottom: 16px;
  border-radius: 12px;
  overflow: hidden;
  cursor: zoom-in;
}

.card-image img {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.card-image:hover img {
  transform: scale(1.02);
}

.detail-image {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  cursor: zoom-in;
}

.detail-image img {
  width: 100%;
  max-height: 500px;
  object-fit: cover;
  display: block;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--color-text-muted);
  padding-top: 14px;
  border-top: 1px solid var(--color-border);
}

.card-actions {
  display: flex;
  gap: 8px;
}

.btn-like, .btn-comment, .btn-follow {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 5px 10px;
  border-radius: 20px;
  transition: all 0.2s ease;
  position: relative;
}

.btn-like:hover, .btn-comment:hover, .btn-follow:hover {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.btn-like {
  color: var(--color-like);
}

.btn-like:hover {
  background: var(--color-danger-light);
  color: var(--color-like);
}

.btn-follow.followed {
  color: #ffc107;
  background: transparent;
}

/* ========== 追按钮动画 ========== */
.follow-icon {
  display: inline-block;
  font-size: 16px;
  -webkit-text-stroke: 0.5px currentColor;
}

.btn-follow.followed .follow-icon {
  -webkit-text-stroke: 0;
}

.follow-burst {
  animation: starBurst 0.5s ease;
}

@keyframes starBurst {
  0% { transform: scale(1); }
  25% { transform: scale(1.6) rotate(15deg); }
  50% { transform: scale(0.9) rotate(-5deg); }
  75% { transform: scale(1.2); }
  100% { transform: scale(1) rotate(0); }
}

.follow-pop {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  color: #ffc107;
  font-size: 14px;
  font-weight: bold;
  white-space: nowrap;
  pointer-events: none;
  animation: followPopUp 0.8s ease forwards;
}

@keyframes followPopUp {
  0% { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
  100% { opacity: 0; transform: translateX(-50%) translateY(-28px) scale(1.3); }
}

/* ========== 点赞动画 ========== */
.like-icon {
  display: inline-block;
  position: relative;
  font-size: 16px;
}

.like-burst {
  animation: heartBeat 0.5s ease;
}

.like-burst::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 24px;
  height: 24px;
  margin: -12px 0 0 -12px;
  border-radius: 50%;
  border: 2px solid var(--color-like);
  animation: burstRing 0.6s ease forwards;
  pointer-events: none;
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  20% { transform: scale(1.5); }
  40% { transform: scale(0.9); }
  60% { transform: scale(1.25); }
  80% { transform: scale(0.95); }
  100% { transform: scale(1); }
}

@keyframes burstRing {
  0% { transform: scale(0.3); opacity: 1; }
  100% { transform: scale(2.5); opacity: 0; }
}

/* ========== 数字滚动 ========== */
.like-count {
  display: inline-block;
}

.count-pop {
  animation: countRoll 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes countRoll {
  0% { transform: translateY(0); opacity: 1; }
  30% { transform: translateY(-8px); opacity: 0; }
  31% { transform: translateY(8px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}

/* ========== 删除动画 ========== */
.card-deleting {
  animation: cardDelete 0.5s ease forwards !important;
  pointer-events: none;
}

@keyframes cardDelete {
  0% { transform: scale(1); opacity: 1; filter: none; }
  40% { transform: scale(0.95); opacity: 0.7; filter: grayscale(0.5); }
  100% { transform: scale(0.8) translateY(-20px); opacity: 0; filter: grayscale(1); }
}

/* ========== 匿名卡片水印 ========== */
.anonymous-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
  font-size: 11px;
  border-radius: 4px;
  border: 1px solid rgba(107, 114, 128, 0.2);
  font-weight: 500;
}

/* ========== 评论展开过渡动画 ========== */
.comment-slide-enter-active,
.comment-slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.comment-slide-enter-from,
.comment-slide-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.comment-slide-enter-to,
.comment-slide-leave-from {
  opacity: 1;
  max-height: 1000px;
  transform: translateY(0);
}
</style>
