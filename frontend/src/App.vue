<template>
  <div class="app">
    <!-- 动态背景装饰 -->
    <div class="bg-decoration" aria-hidden="true">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <header class="header">
      <h1>Card Word</h1>
      <p class="subtitle">
        <span class="typing-text">{{ currentPhrase }}</span>
        <span class="typing-cursor">|</span>
      </p>
    </header>

    <!-- 右上角公告铃铛 -->
    <button class="bell-btn" @click="openAnnouncements" title="公告通知">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
      <span v-if="hasNewAnnouncement" class="bell-dot"></span>
    </button>

    <!-- 右侧浮动圆形按钮栏 -->
    <nav class="side-nav">
      <button
        :class="['nav-btn', { active: activeTab === 'all' }]"
        @click="switchTab('all')"
        title="卡片世界"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
      </button>
      <button
        v-if="activeTab === 'all'"
        class="nav-btn"
        @click="shuffle"
        title="换一批"
      >
        <svg :class="['windmill-icon', { spinning: shuffling }]" viewBox="0 0 24 24" fill="currentColor" stroke="none">
          <path d="M12 12 C12 12, 8 2, 12 2 C16 2, 12 12, 12 12Z" opacity="0.9"/>
          <path d="M12 12 C12 12, 22 8, 22 12 C22 16, 12 12, 12 12Z" opacity="0.7"/>
          <path d="M12 12 C12 12, 16 22, 12 22 C8 22, 12 12, 12 12Z" opacity="0.9"/>
          <path d="M12 12 C12 12, 2 16, 2 12 C2 8, 12 12, 12 12Z" opacity="0.7"/>
          <circle cx="12" cy="12" r="2" fill="currentColor"/>
        </svg>
      </button>
      <button
        :class="['nav-btn', { active: activeTab === 'my' }]"
        @click="switchTab('my')"
        title="我的"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </button>
      <div class="nav-divider"></div>
      <button
        v-if="userId"
        class="nav-btn nav-btn-primary"
        @click="showForm = true"
        title="写一张卡片"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
      <button
        v-if="!userId"
        class="nav-btn nav-btn-primary"
        @click="showLogin = true"
        title="进入小站"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
      </button>
      <button
        v-if="!userId"
        class="nav-btn"
        @click="showRegister = true"
        title="加入小站"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
      </button>
      <button
        v-if="userId"
        class="nav-btn nav-btn-danger"
        @click="logout"
        title="退出登录"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
      </button>
      <div class="nav-divider"></div>
      <button
        class="nav-btn"
        @click="showFeedback = true"
        title="问题反馈"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><line x1="12" y1="8" x2="12" y2="12"/><circle cx="12" cy="15" r="0.5" fill="currentColor"/></svg>
      </button>
    </nav>

    <!-- 个人中心头部 -->
    <div v-if="activeTab === 'my' && userId" class="my-profile">
      <div class="profile-header">
        <div class="profile-avatar">{{ (userNickname || '?')[0] }}</div>
        <span class="profile-nickname">{{ userNickname || '用户' }}</span>
        <button class="theme-toggle-btn" @click="toggleDark" :title="darkMode ? '切换浅色模式' : '切换深色模式'">
          <svg v-if="darkMode" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
      </div>
      <div class="my-sub-tabs">
        <button :class="['sub-tab', { active: mySubTab === 'cards' }]" @click="switchMySubTab('cards')">我的卡片</button>
        <button :class="['sub-tab', { active: mySubTab === 'followed' }]" @click="switchMySubTab('followed')">我追的</button>
      </div>
    </div>

    <main class="card-grid">
      <template v-if="loading">
        <div v-for="i in 6" :key="'sk'+i" class="skeleton-card">
          <div class="skeleton-line" style="width: 35%"></div>
          <div class="skeleton-line" style="width: 100%"></div>
          <div class="skeleton-line" style="width: 85%"></div>
          <div class="skeleton-line skeleton-last" style="width: 50%"></div>
        </div>
      </template>

      <template v-if="!loading">
        <CardItem
          v-for="(card, index) in cards"
          :key="card.id"
          :card="card"
          :user-id="userId"
          :index="index"
          :initial-followed="followedSet.has(card.id)"
          @liked="onLiked"
          @deleted="onDeleted"
          @followed="onFollowed"
        />
      </template>

      <div v-if="!loading && cards.length === 0" class="empty-state">
        <svg class="empty-illustration" width="140" height="140" viewBox="0 0 140 140" fill="none">
          <rect x="30" y="25" width="80" height="55" rx="10" fill="var(--color-accent-light)" stroke="var(--color-accent)" stroke-width="1.5" transform="rotate(-5 70 52)"/>
          <rect x="30" y="35" width="80" height="55" rx="10" fill="var(--color-bg-card)" stroke="var(--color-border-strong)" stroke-width="1.5" transform="rotate(3 70 62)"/>
          <line x1="45" y1="55" x2="85" y2="55" stroke="var(--color-border-strong)" stroke-width="2.5" stroke-linecap="round"/>
          <line x1="45" y1="65" x2="75" y2="65" stroke="var(--color-border-strong)" stroke-width="2.5" stroke-linecap="round"/>
          <line x1="45" y1="75" x2="65" y2="75" stroke="var(--color-border-strong)" stroke-width="2.5" stroke-linecap="round"/>
          <circle cx="100" cy="30" r="3" fill="#ffd166"/>
          <circle cx="110" cy="22" r="2" fill="#ffd166" opacity="0.6"/>
          <circle cx="108" cy="35" r="1.5" fill="#ffd166" opacity="0.4"/>
        </svg>
        <p v-if="activeTab === 'all'">还没有卡片，来发布第一张吧</p>
        <p v-else-if="!userId">请先登录查看你的卡片</p>
        <p v-else-if="mySubTab === 'followed'">还没有追任何卡片，去首页逛逛吧</p>
        <p v-else>你还没有发布过卡片</p>
      </div>
    </main>

    <div class="load-more" v-if="activeTab === 'my' && hasMore && userId">
      <button class="btn-more" @click="loadMore">加载更多</button>
    </div>

    <!-- Toast 通知 -->
    <TransitionGroup name="toast" tag="div" class="toast-container">
      <div v-for="t in toasts" :key="t.id" :class="['toast', `toast-${t.type}`]">
        <span class="toast-icon">{{ { success: '✓', error: '✕', warning: '!', info: 'i' }[t.type] }}</span>
        <span class="toast-message">{{ t.message }}</span>
      </div>
    </TransitionGroup>

    <Transition name="modal">
      <CardForm v-if="showForm" @close="showForm = false" @published="onPublished" />
    </Transition>

    <Transition name="modal">
      <div v-if="showLogin" class="modal" @click.self="showLogin = false">
        <div class="modal-content">
          <h3>欢迎回来</h3>
          <p class="modal-desc">看看大家最近都在说些什么吧</p>
          <div class="form-group">
            <label>用户名</label>
            <input type="text" v-model="loginForm.nickname" placeholder="请输入用户名" />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input type="password" v-model="loginForm.pwd" placeholder="请输入密码" />
          </div>
          <div class="modal-buttons">
            <button class="btn-cancel" @click="showLogin = false">取消</button>
            <button class="btn-submit" @click="handleLogin">进入小站</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div v-if="showRegister" class="modal" @click.self="showRegister = false">
        <div class="modal-content register-modal">
          <h3>加入小站</h3>
          <p class="modal-desc">用一个账号，开始记录你的故事</p>
          <div class="form-group">
            <label>用户名</label>
            <input type="text" v-model="registerForm.nickname" placeholder="请输入用户名" @input="registerError = ''" />
            <span v-if="registerError" class="form-error">{{ registerError }}</span>
            <span v-else class="form-hint">支持中英文，建议使用昵称，让分享更轻松自在</span>
          </div>
          <div class="form-group">
            <label>密码</label>
            <input type="password" v-model="registerForm.pwd" placeholder="请输入密码" />
          </div>
          <div class="modal-buttons">
            <button class="btn-cancel" @click="showRegister = false">取消</button>
            <button class="btn-submit" @click="handleRegister">加入小站</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div v-if="showFeedback" class="modal" @click.self="showFeedback = false">
        <div class="modal-content" style="max-width: 480px;">
          <h3>问题反馈</h3>
          <div class="form-group">
            <label>问题标题</label>
            <input type="text" v-model="feedbackForm.title" placeholder="请简要描述问题" maxlength="100" />
            <span class="char-hint">{{ feedbackForm.title.length }}/100</span>
          </div>
          <div class="form-group">
            <label>问题描述</label>
            <textarea v-model="feedbackForm.content" placeholder="请详细描述你遇到的问题..." maxlength="500" rows="5" class="feedback-textarea"></textarea>
            <span class="char-hint">{{ feedbackForm.content.length }}/500</span>
          </div>
          <div class="modal-buttons">
            <button class="btn-cancel" @click="showFeedback = false">取消</button>
            <button class="btn-submit" :disabled="!feedbackForm.title.trim() || !feedbackForm.content.trim() || feedbackSubmitting" @click="handleFeedback">
              {{ feedbackSubmitting ? '提交中...' : '提交' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="modal">
      <div v-if="showAnnouncements" class="modal" @click.self="showAnnouncements = false">
        <div class="modal-content announcement-modal">
          <h3>公告</h3>
          <div v-if="announcements.length === 0" class="announcement-empty">暂无公告</div>
          <div v-else class="announcement-list">
            <div v-for="a in announcements" :key="a.id" class="announcement-item">
              <div class="announcement-title">{{ a.title }}</div>
              <div class="announcement-content">{{ a.content }}</div>
              <div class="announcement-time">{{ formatTime(a.createdAt) }}</div>
            </div>
          </div>
          <div class="modal-buttons">
            <button class="btn-cancel" @click="showAnnouncements = false">关闭</button>
          </div>
        </div>
      </div>
    </Transition>

    <footer class="footer">
      <svg class="footer-wave" viewBox="0 0 1440 60" preserveAspectRatio="none">
        <path d="M0,20 C240,50 480,0 720,25 C960,50 1200,10 1440,30 L1440,60 L0,60Z" fill="var(--color-accent)" opacity="0.06"/>
      </svg>
      <p>Made with ♥ by likexin</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, provide } from 'vue'
import { getRandomCards, getMyCards, publishCard, getLocalUserId, setLocalUserId, login, register, submitFeedback, followCard, getFollowedCards, getAnnouncements, getLatestAnnouncement } from './api.js'
import CardItem from './components/CardItem.vue'
import CardForm from './components/CardForm.vue'

const cards = ref([])
const showForm = ref(false)
const showLogin = ref(false)
const showRegister = ref(false)
const activeTab = ref('all')
const page = ref(1)
const hasMore = ref(true)
const userId = ref(getLocalUserId())
const userNickname = ref(localStorage.getItem('cardword_nickname') || '')
const loading = ref(true)
const shuffling = ref(false)
const darkMode = ref(localStorage.getItem('cardword_dark') === '1')
const mySubTab = ref('cards')
const followedSet = ref(new Set())

const loginForm = ref({ nickname: '', pwd: '' })
const registerForm = ref({ nickname: '', pwd: '' })
const showFeedback = ref(false)
const feedbackForm = ref({ title: '', content: '' })
const feedbackSubmitting = ref(false)
const showAnnouncements = ref(false)
const announcements = ref([])
const hasNewAnnouncement = ref(false)

// ========== Toast 通知系统 ==========
const toasts = ref([])

function showToast(message, type = 'info') {
  const id = Date.now() + Math.random()
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3000)
}

provide('showToast', showToast)

// ========== 打字机效果 ==========
const phrases = ['说出你的心声，让世界倾听', '想法无界限，言语无限可能', '用文字表达真实的自己', '分享你的生活趣事，激发共鸣', '让每个人的故事都有舞台']
const currentPhrase = ref('')
let phraseIdx = 0
let charIdx = 0
let isDeleting = false
let typingTimer = null

function typeStep() {
  const phrase = phrases[phraseIdx]

  if (!isDeleting) {
    currentPhrase.value = phrase.slice(0, charIdx + 1)
    charIdx++
    if (charIdx >= phrase.length) {
      isDeleting = true
      typingTimer = setTimeout(typeStep, 2000)
      return
    }
    typingTimer = setTimeout(typeStep, 120)
  } else {
    charIdx--
    currentPhrase.value = phrase.slice(0, charIdx)
    if (charIdx <= 0) {
      isDeleting = false
      phraseIdx = (phraseIdx + 1) % phrases.length
      typingTimer = setTimeout(typeStep, 400)
      return
    }
    typingTimer = setTimeout(typeStep, 50)
  }
}

// ========== 撒花效果 ==========
function launchConfetti() {
  const colors = ['#4361ee', '#7c3aed', '#f72585', '#ffd166', '#06d6a0', '#4cc9f0']
  const container = document.createElement('div')
  container.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:9999;overflow:hidden'
  document.body.appendChild(container)

  for (let i = 0; i < 60; i++) {
    const p = document.createElement('div')
    const size = Math.random() * 8 + 4
    p.style.cssText = `
      position:absolute;
      width:${size}px;height:${size}px;
      background:${colors[Math.floor(Math.random() * colors.length)]};
      border-radius:${Math.random() > 0.5 ? '50%' : '2px'};
      left:${Math.random() * 100}%;
      top:-10px;
    `
    container.appendChild(p)
    p.animate([
      { transform: 'translateY(0) translateX(0) rotate(0deg)', opacity: 1 },
      { transform: `translateY(${window.innerHeight + 50}px) translateX(${(Math.random() - 0.5) * 200}px) rotate(${Math.random() * 720}deg)`, opacity: 0 }
    ], {
      duration: Math.random() * 2000 + 2000,
      delay: Math.random() * 500,
      easing: 'cubic-bezier(0.25, 0.46, 0.45, 0.94)',
      fill: 'forwards'
    })
  }

  setTimeout(() => container.remove(), 4000)
}

// ========== 暗黑模式 ==========
if (darkMode.value) {
  document.documentElement.setAttribute('data-theme', 'dark')
}

function toggleDark() {
  darkMode.value = !darkMode.value
  document.documentElement.setAttribute('data-theme', darkMode.value ? 'dark' : 'light')
  localStorage.setItem('cardword_dark', darkMode.value ? '1' : '0')
}

// ========== 数据加载 ==========
async function loadCards() {
  loading.value = true
  const res = await getRandomCards()
  cards.value = res.data
  loading.value = false
}

async function loadMyCards() {
  if (!userId.value) {
    cards.value = []
    hasMore.value = false
    loading.value = false
    return
  }
  loading.value = true
  page.value = 1
  const res = await getMyCards(userId.value, page.value, 10)
  cards.value = res.data.records
  hasMore.value = page.value < res.data.pages
  loading.value = false
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'all') loadCards()
  else {
    mySubTab.value = 'cards'
    loadMyCards()
  }
}

function switchMySubTab(sub) {
  mySubTab.value = sub
  if (sub === 'cards') {
    loadMyCards()
  } else {
    loadFollowedCards()
  }
}

function shuffle() {
  shuffling.value = true
  loadCards()
  setTimeout(() => { shuffling.value = false }, 1000)
}

async function loadMore() {
  page.value++
  if (mySubTab.value === 'followed') {
    const res = await getFollowedCards(userId.value, page.value, 10)
    cards.value = [...cards.value, ...res.data.records]
    hasMore.value = page.value < res.data.pages
  } else {
    const res = await getMyCards(userId.value, page.value, 10)
    cards.value = [...cards.value, ...res.data.records]
    hasMore.value = page.value < res.data.pages
  }
}

async function onPublished(card) {
  if (card.user && card.user.id) {
    userId.value = card.user.id
    setLocalUserId(card.user.id)
  }
  if (activeTab.value === 'my') {
    loadMyCards()
  } else {
    cards.value.unshift(card)
  }
  showForm.value = false
  launchConfetti()
  showToast('发布成功！', 'success')
}

async function handleLogin() {
  const res = await login(loginForm.value.nickname, loginForm.value.pwd)
  if (res.userId) {
    userId.value = res.userId
    setLocalUserId(res.userId)
    userNickname.value = loginForm.value.nickname
    localStorage.setItem('cardword_nickname', loginForm.value.nickname)
    showLogin.value = false
    showToast(`${loginForm.value.nickname}，欢迎回来！`, 'success')
    if (activeTab.value === 'my') loadMyCards()
  } else {
    showToast(res.error || '登录失败', 'error')
  }
}

const registerError = ref('')
async function handleRegister() {
  registerError.value = ''
  const res = await register(registerForm.value.nickname, registerForm.value.pwd)
  if (res.userId) {
    userId.value = res.userId
    setLocalUserId(res.userId)
    userNickname.value = registerForm.value.nickname
    localStorage.setItem('cardword_nickname', registerForm.value.nickname)
    showRegister.value = false
    showToast('注册成功！', 'success')
  } else if (res.error === '用户名已存在') {
    registerError.value = '用户名已存在，请换一个试试'
  } else {
    showToast(res.error || '注册失败', 'error')
  }
}

async function handleFeedback() {
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    const res = await submitFeedback(feedbackForm.value.title.trim(), feedbackForm.value.content.trim())
    if (res.success) {
      showFeedback.value = false
      feedbackForm.value = { title: '', content: '' }
      showToast('提交成功，工程师尽快处理中', 'success')
    } else {
      showToast(res.error || '提交失败', 'error')
    }
  } catch {
    showToast('网络异常，请稍后重试', 'error')
  } finally {
    feedbackSubmitting.value = false
  }
}

// ========== 公告功能 ==========
async function checkNewAnnouncement() {
  try {
    const lastRead = localStorage.getItem('cardword_announcement_read')
    const res = await getLatestAnnouncement(lastRead || undefined)
    hasNewAnnouncement.value = res.hasNew
  } catch {}
}

async function openAnnouncements() {
  showAnnouncements.value = true
  try {
    const data = await getAnnouncements()
    announcements.value = data
    if (data.length > 0) {
      localStorage.setItem('cardword_announcement_read', data[0].createdAt)
    }
    hasNewAnnouncement.value = false
  } catch {}
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function logout() {
  userId.value = null
  userNickname.value = ''
  localStorage.removeItem('cardword_user_id')
  localStorage.removeItem('cardword_nickname')
  showToast('已退出登录', 'info')
  if (activeTab.value === 'my') loadMyCards()
}

function onLiked(id, newCount) {
  const card = cards.value.find(c => c.id === id)
  if (card) card.likesCount = newCount
}

function onDeleted(cardId) {
  cards.value = cards.value.filter(c => c.id !== cardId)
  showToast('卡片已删除', 'info')
}

function onFollowed(cardId, isFollowed) {
  if (isFollowed) {
    followedSet.value.add(cardId)
  } else {
    followedSet.value.delete(cardId)
    // 如果在"我追的"标签页取消追，从列表移除
    if (activeTab.value === 'my' && mySubTab.value === 'followed') {
      cards.value = cards.value.filter(c => c.id !== cardId)
    }
  }
}

async function loadFollowedCards() {
  if (!userId.value) {
    cards.value = []
    hasMore.value = false
    loading.value = false
    return
  }
  loading.value = true
  page.value = 1
  const res = await getFollowedCards(userId.value, page.value, 10)
  cards.value = res.data.records
  hasMore.value = page.value < res.data.pages
  // 标记所有为已追
  followedSet.value = new Set(res.data.records.map(c => c.id))
  loading.value = false
}

onMounted(() => {
  loadCards()
  typeStep()
  checkNewAnnouncement()
})

onUnmounted(() => {
  clearTimeout(typingTimer)
})
</script>

<style>
/* ========== CSS 变量主题系统 ========== */
:root {
  --color-bg: #f0f2f5;
  --color-bg-card: #ffffff;
  --color-bg-card-end: #fafbfc;
  --color-text: #333333;
  --color-text-secondary: #666666;
  --color-text-muted: #aaaaaa;
  --color-text-content: #2c3e50;
  --color-accent: #4361ee;
  --color-accent-hover: #3a56d4;
  --color-accent-light: #eef0ff;
  --color-danger: #ff4757;
  --color-danger-light: #fff0f0;
  --color-like: #e74c3c;
  --color-border: #f0f0f0;
  --color-border-strong: #e0e0e0;
  --color-input-border: #dddddd;
  --color-nav-bg: #ffffff;
  --color-modal-backdrop: rgba(0, 0, 0, 0.25);
  --color-modal-bg: rgba(255, 255, 255, 0.88);
  --color-skeleton: #e9ecef;
  --color-skeleton-shine: #f8f9fa;
  --color-comment-bg: #f8f9fa;
}

[data-theme="dark"] {
  --color-bg: #0f0f1a;
  --color-bg-card: #1a1a2e;
  --color-bg-card-end: #16213e;
  --color-text: #e0e0e0;
  --color-text-secondary: #a0a0b0;
  --color-text-muted: #666680;
  --color-text-content: #d0d0e0;
  --color-accent: #5a7ff0;
  --color-accent-hover: #4a6fe0;
  --color-accent-light: #1e2040;
  --color-danger: #ff6b7a;
  --color-danger-light: #2a1a1e;
  --color-like: #ff6b6b;
  --color-border: #2a2a4a;
  --color-border-strong: #3a3a5a;
  --color-input-border: #3a3a5a;
  --color-nav-bg: #1a1a2e;
  --color-modal-backdrop: rgba(0, 0, 0, 0.5);
  --color-modal-bg: rgba(26, 26, 46, 0.88);
  --color-skeleton: #2a2a4a;
  --color-skeleton-shine: #3a3a5a;
  --color-comment-bg: #16213e;
}

/* ========== 自定义滚动条 ========== */
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--color-border-strong); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--color-text-muted); }

/* ========== 基础 ========== */
* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'LXGW WenKai Screen', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: var(--color-bg);
  color: var(--color-text);
  transition: background 0.4s ease, color 0.4s ease;
}

.app {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 16px;
  padding-right: 80px;
  position: relative;
  min-height: 100vh;
}

/* ========== 动态背景 ========== */
.bg-decoration { position: fixed; inset: 0; pointer-events: none; z-index: 0; overflow: hidden; }
.bg-circle { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.15; transition: opacity 0.5s ease; }
[data-theme="dark"] .bg-circle { opacity: 0.07; }
.bg-circle-1 { width: 500px; height: 500px; background: #4361ee; top: -150px; right: -100px; animation: bgFloat1 25s ease-in-out infinite; }
.bg-circle-2 { width: 400px; height: 400px; background: #7c3aed; bottom: -100px; left: -100px; animation: bgFloat2 20s ease-in-out infinite; }
.bg-circle-3 { width: 300px; height: 300px; background: #f72585; top: 40%; left: 50%; animation: bgFloat3 22s ease-in-out infinite; }
@keyframes bgFloat1 { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(-50px, 50px) scale(1.1); } 66% { transform: translate(30px, -30px) scale(0.95); } }
@keyframes bgFloat2 { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(40px, -40px) scale(1.05); } 66% { transform: translate(-30px, 20px) scale(0.9); } }
@keyframes bgFloat3 { 0%, 100% { transform: translate(0, 0) scale(1); } 50% { transform: translate(-60px, -40px) scale(1.1); } }

/* ========== 标题 ========== */
.header { text-align: center; margin-bottom: 32px; position: relative; z-index: 1; }
.header h1 {
  font-family: 'Pacifico', cursive;
  font-size: 42px;
  font-weight: 400;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #4361ee 0%, #7c3aed 50%, #f72585 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 2px 4px rgba(67, 97, 238, 0.2));
  display: inline-block;
}

.subtitle {
  color: var(--color-text-muted);
  margin: 6px 0 0;
  font-size: 14px;
  letter-spacing: 4px;
  min-height: 22px;
}

.typing-cursor {
  color: var(--color-accent);
  font-weight: 100;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* ========== 右侧浮动导航 ========== */
.side-nav { position: fixed; right: 20px; top: 50%; transform: translateY(-50%); display: flex; flex-direction: column; align-items: center; gap: 12px; z-index: 100; }
.nav-btn { width: 48px; height: 48px; border-radius: 50%; border: none; background: var(--color-nav-bg); color: var(--color-text-secondary); cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 8px rgba(0,0,0,0.08), 0 4px 16px rgba(0,0,0,0.04); transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1); position: relative; }
.nav-btn svg { width: 20px; height: 20px; }
.nav-btn:hover { transform: scale(1.12); box-shadow: 0 4px 12px rgba(0,0,0,0.12), 0 8px 24px rgba(0,0,0,0.06); color: var(--color-accent); }
.nav-btn.active { background: var(--color-accent); color: #fff; box-shadow: 0 4px 12px rgba(67,97,238,0.3), 0 8px 24px rgba(67,97,238,0.15); }
.windmill-icon.spinning { animation: windmill-spin 1s linear; }
@keyframes windmill-spin { from { transform: rotate(0deg); } to { transform: rotate(720deg); } }
.nav-btn-primary { background: var(--color-accent); color: #fff; }
.nav-btn-primary:hover { background: var(--color-accent-hover); color: #fff; box-shadow: 0 4px 12px rgba(67,97,238,0.3), 0 8px 24px rgba(67,97,238,0.15); }
.nav-btn-danger { color: #ccc; }
.nav-btn-danger:hover { background: var(--color-danger); color: #fff; box-shadow: 0 4px 12px rgba(255,71,87,0.3), 0 8px 24px rgba(255,71,87,0.15); }
.nav-divider { width: 24px; height: 1px; background: var(--color-border-strong); margin: 2px 0; }
.nav-btn::after { content: attr(title); position: absolute; right: 58px; top: 50%; transform: translateY(-50%); background: #333; color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 12px; white-space: nowrap; opacity: 0; pointer-events: none; transition: opacity 0.2s ease; }
.nav-btn:hover::after { opacity: 1; }

/* ========== 瀑布流 ========== */
.card-grid { column-count: 2; column-gap: 20px; position: relative; z-index: 1; }

/* ========== 骨架屏 ========== */
.skeleton-card { background: var(--color-bg-card); border-radius: 16px; padding: 24px; margin-bottom: 20px; break-inside: avoid; }
.skeleton-line { height: 14px; border-radius: 7px; background: linear-gradient(90deg, var(--color-skeleton) 25%, var(--color-skeleton-shine) 50%, var(--color-skeleton) 75%); background-size: 200% 100%; animation: shimmer 1.5s ease infinite; margin-bottom: 12px; }
.skeleton-last { margin-bottom: 0; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* ========== 空状态 ========== */
.empty-state { column-span: all; text-align: center; padding: 60px 20px; color: var(--color-text-muted); }
.empty-illustration { margin-bottom: 20px; opacity: 0.7; }
.empty-state p { font-size: 15px; }

/* ========== 加载更多 ========== */
.load-more { text-align: center; margin-top: 24px; position: relative; z-index: 1; }
.btn-more { background: none; border: 1px solid var(--color-accent); color: var(--color-accent); padding: 8px 20px; border-radius: 16px; cursor: pointer; font-size: 14px; transition: all 0.2s; }
.btn-more:hover { background: var(--color-accent); color: #fff; }

/* ========== Toast 通知 ========== */
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-radius: 12px;
  background: var(--color-bg-card);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
  border-left: 4px solid;
  min-width: 220px;
  font-size: 14px;
  color: var(--color-text);
}

.toast-success { border-left-color: #06d6a0; }
.toast-error { border-left-color: #ff4757; }
.toast-warning { border-left-color: #ffd166; }
.toast-info { border-left-color: #4361ee; }

.toast-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}

.toast-success .toast-icon { background: #06d6a0; }
.toast-error .toast-icon { background: #ff4757; }
.toast-warning .toast-icon { background: #ffd166; color: #333; }
.toast-info .toast-icon { background: #4361ee; }

.toast-enter-active { transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from { transform: translateX(120px); opacity: 0; }
.toast-leave-to { transform: translateX(120px); opacity: 0; }

/* ========== 毛玻璃弹窗 ========== */
.modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: var(--color-modal-backdrop); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: var(--color-modal-bg); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1px solid var(--color-border); padding: 28px; border-radius: 16px; width: 90%; max-width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal-content h3 { margin-bottom: 6px; color: var(--color-text); font-size: 18px; }
.modal-desc { margin-bottom: 20px; color: var(--color-text-secondary); font-size: 13px; }
.form-group { margin-bottom: 16px; position: relative; padding-bottom: 2px; }
.form-group label { display: block; margin-bottom: 8px; color: var(--color-text-secondary); font-size: 14px; }
.form-group input { width: 100%; padding: 10px 12px; border: 1px solid var(--color-input-border); border-radius: 10px; font-size: 14px; transition: border-color 0.2s, box-shadow 0.2s; background: var(--color-bg-card); color: var(--color-text); }
.form-group input:focus { outline: none; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(67,97,238,0.1); }
.form-hint { display: block; margin-top: 6px; font-size: 12px; color: var(--color-text-muted); }
.form-error { display: block; margin-top: 6px; font-size: 12px; color: #e74c3c; }
.register-modal { min-width: 360px; }
.char-hint { position: absolute; right: 0; bottom: -18px; font-size: 12px; color: var(--color-text-muted); }
.feedback-textarea { width: 100%; border: 1px solid var(--color-input-border); border-radius: 10px; padding: 10px 12px; font-size: 14px; font-family: inherit; resize: none; background: var(--color-bg-card); color: var(--color-text); transition: border-color 0.2s, box-shadow 0.2s; }
.feedback-textarea:focus { outline: none; border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(67,97,238,0.1); }
.modal-buttons { display: flex; gap: 10px; justify-content: flex-end; margin-top: 24px; }
.btn-cancel, .btn-submit { padding: 9px 18px; border-radius: 10px; font-size: 14px; cursor: pointer; transition: all 0.2s; }
.btn-cancel { background: transparent; color: var(--color-text-secondary); border: 1px solid var(--color-input-border); }
.btn-cancel:hover { background: var(--color-accent-light); }
.btn-submit { background: var(--color-accent); color: #fff; border: none; }
.btn-submit:hover { background: var(--color-accent-hover); }

/* ========== 弹窗过渡 ========== */
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-active .modal-content, .modal-enter-active .form-box, .modal-leave-active .modal-content, .modal-leave-active .form-box { transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal-content, .modal-enter-from .form-box, .modal-leave-to .modal-content, .modal-leave-to .form-box { transform: scale(0.85) translateY(20px); opacity: 0; }

/* ========== 页脚 ========== */
.footer { margin-top: 60px; text-align: center; position: relative; z-index: 1; }
.footer-wave { width: 100%; height: 40px; display: block; }
.footer p { padding: 16px; color: var(--color-text-muted); font-size: 13px; letter-spacing: 1px; }

/* ========== 删除确认弹窗（全局，因为 Teleport 到 body） ========== */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.confirm-box {
  background: var(--color-modal-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  padding: 28px 32px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  max-width: 320px;
  width: 85%;
}

.confirm-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-danger-light);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.confirm-icon svg {
  width: 26px;
  height: 26px;
  stroke: var(--color-danger);
}

.confirm-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}

.confirm-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 24px;
}

.confirm-actions {
  display: flex;
  gap: 10px;
}

.confirm-cancel, .confirm-delete {
  flex: 1;
  padding: 10px 0;
  border-radius: 12px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-cancel {
  background: transparent;
  border: 1px solid var(--color-input-border);
  color: var(--color-text-secondary);
}

.confirm-cancel:hover {
  background: var(--color-accent-light);
}

.confirm-delete {
  background: var(--color-danger);
  border: none;
  color: #fff;
}

.confirm-delete:hover {
  background: #e63e50;
}

.confirm-enter-active,
.confirm-leave-active {
  transition: opacity 0.25s ease;
}

.confirm-enter-active .confirm-box,
.confirm-leave-active .confirm-box {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease;
}

.confirm-enter-from,
.confirm-leave-to {
  opacity: 0;
}

.confirm-enter-from .confirm-box,
.confirm-leave-to .confirm-box {
  transform: scale(0.85);
  opacity: 0;
}

/* transition 也应用于详情弹窗 */
.confirm-enter-active .detail-box,
.confirm-leave-active .detail-box {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.25s ease;
}

.confirm-enter-from .detail-box,
.confirm-leave-to .detail-box {
  transform: scale(0.9) translateY(20px);
  opacity: 0;
}

/* ========== 卡片详情弹窗 ========== */
.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1500;
  padding: 20px;
}

.detail-box {
  background: var(--color-modal-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  padding: 28px;
  width: 100%;
  max-width: 520px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
}

.detail-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--color-accent-light);
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 1;
}

.detail-close svg {
  width: 18px;
  height: 18px;
}

.detail-close:hover {
  background: var(--color-danger);
  color: #fff;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-right: 40px;
}

.detail-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.detail-nickname {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.detail-time {
  font-size: 13px;
  color: var(--color-text-muted);
}

.detail-divider {
  height: 1px;
  background: var(--color-border);
  margin: 20px 0;
}

.detail-content {
  font-size: 17px;
  line-height: 1.9;
  color: var(--color-text-content);
  word-break: break-word;
}

.detail-comments {
  min-height: 60px;
}

.detail-comments-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

/* ========== 个人中心 ========== */
.my-profile {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  background: linear-gradient(145deg, var(--color-bg-card) 0%, var(--color-bg-card-end) 100%);
  border-radius: 20px;
  padding: 28px;
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.profile-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  flex-shrink: 0;
}

.profile-nickname {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.theme-toggle-btn {
  margin-left: auto;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
  background: var(--color-card-bg);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.theme-toggle-btn svg {
  width: 20px;
  height: 20px;
}

.theme-toggle-btn:hover {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
}

.my-sub-tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--color-border);
}

.sub-tab {
  background: none;
  border: none;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.sub-tab:hover {
  color: var(--color-text);
}

.sub-tab.active {
  color: var(--color-accent);
}

.sub-tab.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 2px;
  background: var(--color-accent);
  border-radius: 1px;
}

/* ========== 右上角公告铃铛 ========== */
.bell-btn {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 200;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.bell-btn svg {
  width: 24px;
  height: 24px;
  transition: transform 0.3s ease;
}

.bell-btn:hover {
  transform: scale(1.15);
  color: var(--color-accent);
}

.bell-btn:hover svg {
  animation: bellSwing 0.6s ease-in-out;
}

@keyframes bellSwing {
  0% { transform: rotate(0deg); }
  20% { transform: rotate(15deg); }
  40% { transform: rotate(-12deg); }
  60% { transform: rotate(8deg); }
  80% { transform: rotate(-5deg); }
  100% { transform: rotate(0deg); }
}

.bell-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 10px;
  height: 10px;
  background: #ff4757;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(255, 71, 87, 0.5);
  animation: bellPulse 1.5s ease-in-out infinite;
}

@keyframes bellPulse {
  0% { box-shadow: 0 0 0 0 rgba(255, 71, 87, 0.5); }
  70% { box-shadow: 0 0 0 8px rgba(255, 71, 87, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 71, 87, 0); }
}

/* ========== 公告弹窗 ========== */
.announcement-modal {
  max-width: 520px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}

.announcement-list {
  overflow-y: auto;
  flex: 1;
  max-height: 50vh;
}

.announcement-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}

.announcement-item:last-child {
  border-bottom: none;
}

.announcement-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
}

.announcement-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.announcement-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.announcement-empty {
  text-align: center;
  padding: 40px 0;
  color: var(--color-text-muted);
  font-size: 14px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .app { padding-right: 16px; padding-bottom: 80px; }
  .card-grid { column-count: 1; }
  .side-nav { position: fixed; right: auto; left: 50%; top: auto; bottom: 16px; transform: translateX(-50%); flex-direction: row; background: var(--color-nav-bg); padding: 8px 16px; border-radius: 28px; box-shadow: 0 4px 20px rgba(0,0,0,0.1), 0 8px 32px rgba(0,0,0,0.05); }
  .nav-btn { width: 42px; height: 42px; box-shadow: none; }
  .nav-btn:hover, .nav-btn.active { box-shadow: none; }
  .nav-divider { width: 1px; height: 24px; margin: 0 2px; }
  .nav-btn::after { display: none; }
  .toast-container { right: 10px; left: 10px; top: 72px; }
  .toast { min-width: auto; }
  .bell-btn { top: 14px; right: 16px; width: 40px; height: 40px; }
  .bell-btn svg { width: 20px; height: 20px; }
}
</style>
