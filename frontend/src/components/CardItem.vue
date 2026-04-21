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
    <div class="card-header">
      <div class="card-author-wrap">
        <div class="card-avatar">
          <template v-if="card.isAnonymous === 1 && !isOwner">匿</template>
          <template v-else>{{ (card.username || '匿')[0] }}</template>
        </div>
        <span class="card-author">
          <template v-if="card.isAnonymous === 1 && !isOwner">匿名卡片</template>
          <template v-else>{{ card.username || '' }}</template>
        </span>
        <span v-if="card.isAnonymous === 1" class="anonymous-badge">匿名卡片</span>
      </div>
      <button v-if="isOwner && allowDelete" class="btn-delete" @click.stop="showConfirm = true">删除</button>
    </div>

    <div class="card-divider"></div>

    <div class="card-content">{{ card.content }}</div>

    <!-- 卡片图片 -->
    <div v-if="card.imageUrl" class="card-image" @click.stop>
      <img :src="card.imageUrl" alt="卡片图片" @click="previewImage(card.imageUrl)" />
    </div>

    <!-- 标签 chips -->
    <div v-if="card.tags && card.tags.length" class="tag-chips" @click.stop>
      <span
        v-for="t in card.tags"
        :key="t.id || t.name"
        class="tag-chip tag-clickable"
        :style="tagChipStyle(t)"
        @click.stop="$emit('tagClick', t)"
      >#{{ t.name }}</span>
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
        <button class="btn-share" @click="openShare" title="分享">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="share-icon"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/></svg>
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

    <!-- 图片预览弹窗 -->
    <Teleport to="body">
      <Transition name="confirm">
        <div v-if="previewOpen" class="image-preview-overlay" @click.self="closePreview">
          <div class="image-preview-inner" role="dialog" aria-modal="true" aria-label="图片预览">
            <img
              class="image-preview-img"
              :src="previewUrl"
              alt="预览图片"
              @load="previewLoading = false"
              @error="previewLoading = false"
            />
            <div v-if="previewLoading" class="image-preview-loading">加载中...</div>
            <button class="image-preview-close" type="button" @click="closePreview" aria-label="关闭图片预览">×</button>
          </div>
        </div>
      </Transition>
    </Teleport>

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
                <template v-else>{{ (card.username || '匿')[0] }}</template>
              </div>
              <div class="detail-info">
                <span class="detail-nickname">
                  <template v-if="card.isAnonymous === 1 && !isOwner">匿名卡片</template>
                  <template v-else>{{ card.username || '' }}</template>
                </span>
                <span v-if="card.isAnonymous === 1" class="anonymous-badge">匿名卡片</span>
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

            <!-- 标签 chips -->
            <div v-if="card.tags && card.tags.length" class="tag-chips detail-tag-chips" @click.stop>
              <span
                v-for="t in card.tags"
                :key="t.id || t.name"
                class="tag-chip tag-clickable"
                :style="tagChipStyle(t)"
                @click.stop="$emit('tagClick', t)"
              >#{{ t.name }}</span>
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

    <!-- 分享弹窗 -->
    <Teleport to="body">
      <Transition name="confirm">
        <div v-if="showShare" class="share-overlay" @click.self="showShare = false">
          <div class="share-box">
            <button class="share-close" @click="showShare = false">×</button>
            <div class="share-heading">
              <p class="share-eyebrow">Card Word Share</p>
              <h3 class="share-title">分享卡片</h3>
              <p class="share-subtitle">分享图生成成功，请长按（右键）复制，或者点击下方按钮下载分享~~</p>
            </div>
            <div class="share-preview">
              <img v-if="shareImage" :src="shareImage" alt="分享图片" />
              <div v-else class="share-generating">生成中...</div>
            </div>
            <div class="share-actions">
              <button class="share-btn share-download" :disabled="!shareImage" @click="downloadShareImage">
                下载图片
              </button>
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

function hashString(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = ((h << 5) - h) + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

function tagChipStyle(tag) {
  const palette = [
    { color: '#4361ee', bg: 'rgba(67, 97, 238, 0.10)', border: 'rgba(67, 97, 238, 0.25)' },
    { color: '#7c3aed', bg: 'rgba(124, 58, 237, 0.10)', border: 'rgba(124, 58, 237, 0.25)' },
    { color: '#f72585', bg: 'rgba(247, 37, 133, 0.10)', border: 'rgba(247, 37, 133, 0.25)' },
    { color: '#118ab2', bg: 'rgba(17, 138, 178, 0.10)', border: 'rgba(17, 138, 178, 0.25)' },
    { color: '#06d6a0', bg: 'rgba(6, 214, 160, 0.10)', border: 'rgba(6, 214, 160, 0.25)' },
    { color: '#ef476f', bg: 'rgba(239, 71, 111, 0.10)', border: 'rgba(239, 71, 111, 0.25)' },
    { color: '#ffd166', bg: 'rgba(255, 209, 102, 0.14)', border: 'rgba(255, 209, 102, 0.35)' },
  ]

  const rawId = tag && tag.id != null ? Number(tag.id) : null
  const idx = Number.isFinite(rawId)
    ? (Math.abs(rawId) % palette.length)
    : (hashString(String(tag?.name || '')) % palette.length)

  const p = palette[idx]
  return {
    color: p.color,
    background: p.bg,
    border: `1px solid ${p.border}`
  }
}

const props = defineProps({
  card: Object,
  userId: Number,
  index: { type: Number, default: 0 },
  initialFollowed: { type: Boolean, default: false },
  allowDelete: { type: Boolean, default: true },
  removeOnUnfollow: { type: Boolean, default: false }
})

const emit = defineEmits(['liked', 'deleted', 'followed', 'tagClick'])
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

const previewOpen = ref(false)
const previewUrl = ref('')
const previewLoading = ref(false)

const showShare = ref(false)
const shareImage = ref(null)
const shareCopying = ref(false)

watch(() => props.initialFollowed, (val) => {
  followed.value = val
})

const isOwner = computed(() => {
  if (props.card && props.card.isOwner !== undefined && props.card.isOwner !== null) {
    return !!props.card.isOwner
  }
  return props.userId && props.card.userId === props.userId
})

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

function onKeydown(e) {
  if (e.key === 'Escape') {
    if (previewOpen.value) closePreview()
    else if (showShare.value) showShare.value = false
    else if (showDetail.value) showDetail.value = false
    else if (showConfirm.value) showConfirm.value = false
  }
}

defineOptions({
  inheritAttrs: false
})

watch([previewOpen, showDetail, showConfirm, showShare], ([p, d, c, s]) => {
  const anyOpen = p || d || c || s
  if (anyOpen) {
    document.addEventListener('keydown', onKeydown)
    document.body.style.overflow = 'hidden'
  } else {
    document.removeEventListener('keydown', onKeydown)
    document.body.style.overflow = ''
  }
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})

async function handleLike() {
  if (justLiked.value) return
  try {
    const res = await likeCard(props.card.id)
    justLiked.value = true
    countAnimating.value = true
    setTimeout(() => { justLiked.value = false }, 600)
    setTimeout(() => { countAnimating.value = false }, 400)
    emit('liked', props.card.id, res.data.likesCount)
  } catch (err) {
    if (err?.response?.status !== 401) {
      showToast('点赞失败', 'error')
    }
  }
}

async function handleFollow() {
  if (!props.userId) {
    showToast('请先登录', 'warning')
    return
  }
  if (deleting.value) return

  try {
    const res = await followCard(props.card.id, props.userId)
    followed.value = res.followed
    if (followed.value) {
      justFollowed.value = true
      setTimeout(() => { justFollowed.value = false }, 800)
      showToast('已追！将持续关注该卡片', 'success')
      emit('followed', props.card.id, true)
      return
    }

    showToast('已取消追', 'info')
    if (props.removeOnUnfollow) {
      deleting.value = true
      setTimeout(() => emit('followed', props.card.id, false), 500)
      return
    }

    emit('followed', props.card.id, false)
  } catch (err) {
    if (err?.response?.status !== 401) {
      showToast('操作失败', 'error')
    }
  }
}

async function handleDelete() {
  showConfirm.value = false
  try {
    const res = await deleteCard(props.card.id, props.userId)
    if (res.success) {
      deleting.value = true
      setTimeout(() => emit('deleted', props.card.id), 500)
    } else {
      showToast(res.error || '删除失败', 'error')
    }
  } catch (err) {
    if (err?.response?.status !== 401) {
      showToast('删除失败', 'error')
    }
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
  previewUrl.value = url || ''
  previewLoading.value = true
  previewOpen.value = true
}

function closePreview() {
  previewOpen.value = false
  previewUrl.value = ''
  previewLoading.value = false
}

// ========== 分享功能 ==========
const SHARE_ACCENT = '#6c5ce7'
const SHARE_ACCENT_LIGHT = '#efe9ff'
const SHARE_FONT_SERIF = '"ZCOOL Happy", "ZCOOL XiaoWei", "LXGW WenKai Screen", "Noto Serif SC", Georgia, serif'
const SHARE_FONT_SANS = '"PingFang SC", "Microsoft YaHei", sans-serif'

function wrapText(ctx, text, maxWidth) {
  const lines = []
  let line = ''
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    if (ch === '\n') {
      lines.push(line)
      line = ''
      continue
    }
    const test = line + ch
    if (ctx.measureText(test).width >= maxWidth) {
      if (line) lines.push(line)
      line = ch
    } else {
      line = test
    }
  }
  if (line) lines.push(line)
  return lines
}

function fillRoundRect(ctx, x, y, w, h, r, color) {
  ctx.save()
  roundRect(ctx, x, y, w, h, r)
  ctx.fillStyle = color
  ctx.fill()
  ctx.restore()
}

async function generateShareImage() {
  const W = 600
  const dpr = 2
  const canvas = document.createElement('canvas')
  canvas.width = W * dpr
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)

  const bgGrad = ctx.createLinearGradient(0, 0, W, 800)
  bgGrad.addColorStop(0, '#e0d8f0')
  bgGrad.addColorStop(0.4, '#e8e4f4')
  bgGrad.addColorStop(0.7, '#f0e4ec')
  bgGrad.addColorStop(1, '#e8e4f4')

  const outerMargin = 20
  const outerX = outerMargin
  const outerY = outerMargin
  const outerW = W - outerMargin * 2
  const outerH = 20

  const innerMargin = 24
  const cardX = innerMargin
  const cardY = outerMargin + 20
  const cardW = W - innerMargin * 2
  const px = cardX + 22

  const authorName = props.card.isAnonymous === 1 ? '匿名卡片' : (props.card.username || '匿名')

  ctx.font = `22px ${SHARE_FONT_SERIF}`
  const contentLines = wrapText(ctx, props.card.content || '', W - px * 2)
  const lineHeight = 36
  const contentH = contentLines.length * lineHeight

  let tagH = 0
  if (props.card.tags && props.card.tags.length) {
    ctx.font = `13px ${SHARE_FONT_SANS}`
    let tx = px
    for (const t of props.card.tags.slice(0, 5)) {
      const label = `#${t.name}`
      const tw = ctx.measureText(label).width + 18
      if (tx + tw > W - px) break
      tx += tw + 6
    }
    tagH = 34
  }

  let imgH = 0
  const MAX_IMG_HEIGHT = 400
  if (props.card.imageUrl) {
    try {
      const img = await loadImage(props.card.imageUrl)
      const drawW = W - px * 2
      imgH = img.height * (drawW / img.width)
      if (imgH > MAX_IMG_HEIGHT) {
        imgH = MAX_IMG_HEIGHT
      }
    } catch {
      imgH = 0
    }
  }

  const authorSectionH = 80
  const topDividerH = 1
  const padding = 24
  const contentBottomPadding = 20
  const dateSectionH = 30
  const bottomDividerH = 1
  const footerSectionH = 40
  const footerBottomPadding = 48

  const neededH = authorSectionH + topDividerH + padding + contentH + (imgH > 0 ? imgH + 24 : 0) + (tagH > 0 ? tagH + 24 : 0) + dateSectionH + bottomDividerH + footerSectionH + contentBottomPadding + footerBottomPadding
  const cardH = neededH
  const H = cardY + cardH + outerMargin + 20

  canvas.height = H * dpr
  ctx.scale(dpr, dpr)

  ctx.fillStyle = bgGrad
  ctx.fillRect(0, 0, W, H)

  ctx.save()
  roundRect(ctx, outerX, outerY, outerW, outerH + cardH + outerMargin, 20)
  ctx.clip()
  ctx.fillStyle = bgGrad
  ctx.fillRect(outerX, outerY, outerW, outerH + cardH + outerMargin)
  ctx.restore()

  ctx.save()
  roundRect(ctx, cardX, cardY, cardW, cardH, 20)
  ctx.clip()
  const cardGrad = ctx.createLinearGradient(cardX, cardY, cardX, cardY + cardH)
  cardGrad.addColorStop(0, 'rgba(255, 255, 255, 0.62)')
  cardGrad.addColorStop(1, 'rgba(255, 255, 255, 0.38)')
  ctx.fillStyle = cardGrad
  ctx.fillRect(cardX, cardY, cardW, cardH)
  ctx.restore()

  let y = cardY + 16

  fillRoundRect(ctx, px, y + 10, 44, 44, 22, 'rgba(108, 92, 231, 0.15)')
  ctx.fillStyle = '#6c5ce7'
  ctx.font = `bold 22px ${SHARE_FONT_SANS}`
  ctx.textAlign = 'center'
  ctx.fillText(authorName[0], px + 22, y + 38)
  ctx.textAlign = 'left'

  ctx.fillStyle = '#2D2A26'
  ctx.font = `bold 18px ${SHARE_FONT_SANS}`
  ctx.fillText(authorName, px + 56, y + 28)

  ctx.fillStyle = '#aaa5a0'
  ctx.font = `12px ${SHARE_FONT_SANS}`
  ctx.fillText('Card Word', px + 56, y + 46)

  y += authorSectionH

  ctx.strokeStyle = 'rgba(108, 92, 231, 0.15)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(px, y)
  ctx.lineTo(W - px, y)
  ctx.stroke()

  y += padding

  ctx.fillStyle = '#2c3e50'
  ctx.font = `22px ${SHARE_FONT_SERIF}`
  for (const line of contentLines) {
    ctx.fillText(line, px, y)
    y += lineHeight
  }

  if (props.card.imageUrl && imgH > 0) {
    try {
      const img = await loadImage(props.card.imageUrl)
      const drawW = W - px * 2
      const drawH = imgH
      fillRoundRect(ctx, px, y, drawW, drawH, 12, '#e8e2f5')
      ctx.save()
      roundRect(ctx, px, y, drawW, drawH, 12)
      ctx.clip()
      const scale = drawW / img.width
      const scaledImgH = img.height * scale
      if (scaledImgH > drawH) {
        const srcY = (scaledImgH - drawH) / 2 / scale
        const srcH = drawH / scale
        ctx.drawImage(img, 0, srcY, img.width, srcH, px, y, drawW, drawH)
      } else {
        const offsetY = y + (drawH - scaledImgH) / 2
        ctx.drawImage(img, px, offsetY, drawW, scaledImgH)
      }
      ctx.restore()
      y += drawH + 24
    } catch {
    }
  }

  if (props.card.tags && props.card.tags.length) {
    ctx.font = `13px ${SHARE_FONT_SANS}`
    let tx = px
    for (const t of props.card.tags.slice(0, 5)) {
      const style = tagChipStyle(t)
      const label = `#${t.name}`
      const tw = ctx.measureText(label).width + 18
      if (tx + tw > W - px) break
      fillRoundRect(ctx, tx, y, tw, 24, 12, style.background)
      ctx.fillStyle = style.color
      ctx.fillText(label, tx + 9, y + 16)
      tx += tw + 6
    }
    y += tagH + 24
  }

  const cardTime = formatTime(props.card.createdAt)
  if (cardTime) {
    ctx.fillStyle = '#b8b4b0'
    ctx.font = `14px ${SHARE_FONT_SANS}`
    ctx.fillText(cardTime, px, y + 18)
    y += dateSectionH
  }

  y += 12

  ctx.strokeStyle = 'rgba(108, 92, 231, 0.15)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(px, y)
  ctx.lineTo(W - px, y)
  ctx.stroke()

  y += 28

  ctx.fillStyle = '#6c5ce7'
  ctx.font = `bold 14px ${SHARE_FONT_SANS}`
  ctx.fillText('Card Word', px, y + 16)

  ctx.fillStyle = '#aaa5a0'
  ctx.font = `12px ${SHARE_FONT_SANS}`
  ctx.fillText('一张卡片，一段心事', px + 80, y + 16)

  ctx.textAlign = 'right'
  ctx.fillStyle = '#c8c4c0'
  ctx.font = `11px ${SHARE_FONT_SANS}`
  ctx.fillText('www.cardword.online', W - px, y + 16)
  ctx.textAlign = 'left'

  return canvas.toDataURL('image/png')
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

async function openShare() {
  showShare.value = true
  shareImage.value = null
  try {
    shareImage.value = await generateShareImage()
  } catch (err) {
    console.error('生成分享图片失败', err)
    showToast(err?.message ? `生成分享图片失败：${err.message}` : '生成分享图片失败', 'error')
  }
}

async function copyShareImage() {
  if (!shareImage.value) return
  try {
    const img = new Image()
    img.src = shareImage.value
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
    })
    const canvas = document.createElement('canvas')
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    canvas.toBlob(async (blob) => {
      if (!blob) {
        showToast('复制失败，请尝试保存图片', 'error')
        return
      }
      try {
        await navigator.clipboard.write([
          new ClipboardItem({ [blob.type]: blob })
        ])
        showToast('图片已复制到剪贴板', 'success')
      } catch (e) {
        console.error('剪贴板写入失败:', e)
        showToast('复制失败，请尝试保存图片', 'error')
      }
    }, 'image/png')
  } catch (err) {
    console.error('复制失败:', err)
    showToast('复制失败，请尝试保存图片', 'error')
  }
}

function downloadShareImage() {
  if (!shareImage.value) return
  try {
    const a = document.createElement('a')
    a.href = shareImage.value
    a.download = `cardword-${props.card.id}.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    showToast('图片下载成功', 'success')
  } catch (err) {
    console.error('下载失败:', err)
    showToast('下载失败，请重试', 'error')
  }
}
</script>

<style scoped>
.card-item {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.38) 0%, rgba(255, 255, 255, 0.20) 100%);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 22px 22px 20px;
  position: relative;
  border: 1px solid rgba(108, 92, 231, 0.18);
  box-shadow:
    0 8px 32px rgba(108, 92, 231, 0.10),
    0 2px 8px rgba(0, 0, 0, 0.04);
  transition:
    transform 0.28s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.28s ease,
    border-color 0.25s ease,
    background 0.28s ease;
  overflow: hidden;
  break-inside: avoid;
  margin-bottom: 20px;
  animation: cardEnter 0.5s ease backwards;
  will-change: transform;
  cursor: pointer;
}

.card-item:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow:
    0 20px 40px rgba(108, 92, 231, 0.18),
    0 8px 16px rgba(0, 0, 0, 0.06);
  border-color: rgba(108, 92, 231, 0.40);
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
  font-weight: 600;
  color: var(--color-accent);
  letter-spacing: 0.8px;
  text-shadow: 0 1px 8px rgba(108, 92, 231, 0.15);
}

.detail-nickname {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-accent);
  letter-spacing: 0.8px;
  text-shadow: 0 1px 8px rgba(108, 92, 231, 0.15);
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

.card-divider {
  height: 1px;
  background: rgba(108, 92, 231, 0.15);
  margin: 12px 0;
}

.card-content {
  font-size: 15.5px;
  line-height: 1.9;
  margin-bottom: 10px;
  word-break: break-word;
  color: var(--color-text-content);
  font-family: "ZCOOL Happy", "ZCOOL XiaoWei", "LXGW WenKai Screen", "Noto Serif SC", Georgia, serif;
  letter-spacing: 0.4px;
}

.detail-content {
  font-size: 16.5px;
  line-height: 1.9;
  word-break: break-word;
  color: var(--color-text-content);
  font-family: "ZCOOL Happy", "ZCOOL XiaoWei", "LXGW WenKai Screen", "Noto Serif SC", Georgia, serif;
  letter-spacing: 0.4px;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.detail-tag-chips {
  margin-top: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  line-height: 1;
  user-select: none;
  letter-spacing: 0.3px;
  font-weight: 500;
}

.tag-clickable {
  cursor: pointer;
  transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.15s;
}

.tag-clickable:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 10px rgba(108, 92, 231, 0.18);
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
  font-size: 11.5px;
  color: var(--color-text-muted);
  padding-top: 10px;
  margin-top: 4px;
  border-top: 1px solid rgba(108, 92, 231, 0.15);
}

.card-time {
  font-size: 11.5px;
  letter-spacing: 0.4px;
  color: var(--color-text-muted);
}

.card-actions {
  display: flex;
  gap: 4px;
}

.btn-like, .btn-comment, .btn-follow {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 11.5px;
  color: var(--color-text-secondary);
  padding: 3px 10px;
  border-radius: 999px;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  display: flex;
  align-items: center;
  gap: 2px;
}

.btn-like:hover, .btn-comment:hover, .btn-follow:hover {
  background: rgba(108, 92, 231, 0.10);
  color: var(--color-accent);
  transform: scale(1.06);
}

.btn-like {
  color: var(--color-like);
}

.btn-like:hover {
  background: rgba(239, 71, 111, 0.10);
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


/* ========== 图片预览弹窗 ========== */
.image-preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.86);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2200;
  padding: 20px;
  cursor: zoom-out;
}

.image-preview-inner {
  position: relative;
  max-width: min(92vw, 920px);
  max-height: 90vh;
  cursor: default;
}

.image-preview-img {
  max-width: 100%;
  max-height: 90vh;
  display: block;
  border-radius: 12px;
  object-fit: contain;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.4);
}

.image-preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  letter-spacing: 2px;
  pointer-events: none;
}

.image-preview-close {
  position: absolute;
  top: -44px;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, transform 0.2s ease;
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

/* ========== 深色模式卡片 ========== */
[data-theme="dark"] .card-item {
  background: linear-gradient(160deg, rgba(30, 30, 52, 0.55) 0%, rgba(24, 24, 40, 0.40) 100%);
  border: 1px solid rgba(108, 92, 231, 0.22);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.35),
    0 2px 8px rgba(0, 0, 0, 0.15);
}

[data-theme="dark"] .card-divider {
  background: rgba(108, 92, 231, 0.18);
}

[data-theme="dark"] :deep(.comment-bubble) {
  background: rgba(30, 30, 52, 0.65);
  border: 1px solid rgba(108, 92, 231, 0.20);
}

[data-theme="dark"] :deep(.input-content) {
  background: rgba(30, 30, 52, 0.65);
  border: 1px solid rgba(108, 92, 231, 0.20);
  color: var(--color-text);
}

.comment-slide-enter-to,
.comment-slide-leave-from {
  opacity: 1;
  max-height: 1000px;
  transform: translateY(0);
}

/* ========== 分享按钮 ========== */
.btn-share {
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

.btn-share:hover {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.share-icon {
  width: 16px;
  height: 16px;
  display: inline-block;
  vertical-align: -2px;
}

/* ========== 分享弹窗 ========== */
.share-overlay {
  position: fixed;
  inset: 0;
  background: var(--color-modal-backdrop);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
  padding: 20px;
}

.share-box {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(250, 250, 255, 0.92));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 24px;
  padding: 30px;
  width: 90%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow:
    0 24px 70px rgba(15, 23, 42, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  position: relative;
}

.share-heading {
  margin-bottom: 18px;
}

.share-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--color-accent);
}

.share-close {
  position: absolute;
  top: 14px;
  right: 16px;
  background: rgba(148, 163, 184, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.18);
  font-size: 22px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1;
  padding: 6px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
}

.share-close:hover {
  color: var(--color-text);
  background: rgba(108, 92, 231, 0.12);
  border-color: rgba(108, 92, 231, 0.2);
}

.share-title {
  font-size: 22px;
  color: var(--color-text);
  margin-bottom: 8px;
}

.share-subtitle {
  margin: 0;
  color: var(--color-accent);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.7;
}

.share-preview {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.86), rgba(244, 247, 255, 0.92));
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  padding: 10px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.share-preview img {
  width: 100%;
  display: block;
  border-radius: 16px;
  box-shadow: 0 14px 30px rgba(100, 116, 139, 0.16);
}

.share-generating {
  padding: 48px 24px;
  color: var(--color-text-muted);
  font-size: 14px;
  letter-spacing: 0.08em;
}

.share-actions {
  display: flex;
  gap: 12px;
}

.share-btn {
  flex: 1;
  border: none;
  border-radius: 14px;
  padding: 13px 0;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.share-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.share-copy {
  background: linear-gradient(135deg, var(--color-accent), var(--color-accent-hover));
  color: #fff;
  box-shadow: 0 12px 24px rgba(108, 92, 231, 0.22);
}

.share-copy:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(108, 92, 231, 0.28);
}

.share-download {
  background: rgba(108, 92, 231, 0.08);
  color: var(--color-accent);
  border: 1px solid rgba(108, 92, 231, 0.16);
}

.share-download:hover:not(:disabled) {
  background: rgba(108, 92, 231, 0.14);
  transform: translateY(-1px);
}

</style>
