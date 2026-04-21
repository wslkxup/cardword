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

      <div v-if="activeTab === 'all'" class="tag-search" @click.stop>
        <div class="tag-search-row">
          <div class="tag-search-input-wrap">
            <svg class="tag-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              class="tag-search-input"
              type="text"
              v-model="tagQuery"
              placeholder="搜索标签..."
              @input="onTagQueryInput"
              @keydown.esc.prevent="clearTagSuggestions"
            />
          </div>
          <button v-if="activeTag" class="tag-clear-btn" @click="clearTagFilter" type="button">清除</button>
        </div>

        <div v-if="tagSuggestionsOpen" class="tag-suggestions">
          <div v-if="tagSearching" class="tag-suggestions-item muted">搜索中...</div>
          <button
            v-for="t in tagSuggestions"
            :key="t.id"
            class="tag-suggestions-item"
            type="button"
            @click="selectTag(t)"
          >
            #{{ t.name }}
          </button>
          <div v-if="!tagSearching && tagSuggestions.length === 0" class="tag-suggestions-item muted">无匹配标签</div>
        </div>

        <div v-if="activeTag" class="tag-active-hint">当前筛选：#{{ activeTag.name }}</div>

        <div v-if="!activeTag && hotTags.length && !tagSuggestionsOpen" class="hot-tags">
          <span class="hot-tags-label">热门标签：</span>
          <button
            v-for="(t, i) in hotTags"
            :key="t.id"
            class="hot-tag-btn"
            type="button"
            :style="hotTagStyle(t)"
            @click="selectTag(t)"
          >#{{ t.name }}</button>
        </div>
      </div>
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
        @click="activeTab === 'all' ? shuffle() : switchTab('all')"
        :title="activeTab === 'all' ? '换一批' : '卡片世界'"
      >
        <svg
          v-if="activeTab !== 'all'"
          class="grid-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="3" y="3" width="7" height="7" rx="2"/>
          <rect x="14" y="3" width="7" height="7" rx="2"/>
          <rect x="3" y="14" width="7" height="7" rx="2"/>
          <rect x="14" y="14" width="7" height="7" rx="2"/>
        </svg>
        <svg
          v-else
          :class="['refresh-icon', { spinning: shuffling }]"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <g class="refresh-icon-inner">
            <!-- 半圆箭头刷新图标 -->
            <path d="M4 10a7 7 0 0 1 11.5-5.2"/>
            <polyline points="16 3 16 8 11 8"/>
            <path d="M20 14a7 7 0 0 1-11.5 5.2"/>
            <polyline points="8 21 8 16 13 16"/>
          </g>
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
        @click="openPublishForm"
        title="写一张卡片"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
      <button
        v-if="!userId"
        class="nav-btn nav-btn-primary"
        @click="openLogin"
        title="进入小站"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
      </button>
      <button
        v-if="!userId"
        class="nav-btn"
        @click="openRegister"
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
        <div class="profile-info">
          <span class="profile-nickname">{{ userNickname || '用户' }}</span>
          <div class="level-info" v-if="userInfo.level !== undefined">
            <span class="level-badge">Lv.{{ userInfo.level }}</span>
            <div class="exp-bar">
              <div class="exp-progress" :style="{ width: expProgress + '%' }"></div>
            </div>
            <span class="exp-text">{{ userInfo.exp }} / {{ userInfo.nextLevelExp }} 经验</span>
          </div>
        </div>
        <button class="theme-toggle-btn" @click="toggleDark" :title="darkMode ? '切换浅色模式' : '切换深色模式'">
          <svg v-if="darkMode" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
      </div>
      <div class="my-sub-tabs">
        <div class="sub-tab-group">
          <button :class="['sub-tab', { active: mySubTab === 'cards' }]" @click="switchMySubTab('cards')">我的卡片</button>
          <button :class="['sub-tab', { active: mySubTab === 'followed' }]" @click="switchMySubTab('followed')">我追的</button>
        </div>
        <button class="btn-outline" @click="showChangePassword = true">
          修改密码
        </button>
      </div>
    </div>

    <main class="card-grid">
      <template v-if="loading">
        <div v-for="i in 6" :key="'sk'+i" class="skeleton-card">
          <div class="skeleton-header">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-nickname"></div>
          </div>
          <div class="skeleton-content">
            <div class="skeleton-line" style="width: 100%"></div>
            <div class="skeleton-line" style="width: 85%"></div>
            <div class="skeleton-line" style="width: 60%"></div>
          </div>
          <div v-if="i % 3 === 0" class="skeleton-image"></div>
          <div class="skeleton-footer">
            <div class="skeleton-time"></div>
            <div class="skeleton-actions">
              <div class="skeleton-action"></div>
              <div class="skeleton-action"></div>
              <div class="skeleton-action"></div>
            </div>
          </div>
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
          :allow-delete="activeTab === 'my' && mySubTab === 'cards'"
          :remove-on-unfollow="activeTab === 'my' && mySubTab === 'followed'"
          @liked="onLiked"
          @deleted="onDeleted"
          @followed="onFollowed"
          @tag-click="onTagClick"
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
          <div class="form-group">
            <label>确认密码</label>
            <input type="password" v-model="registerForm.confirmPwd" placeholder="请再次输入密码" />
            <span v-if="registerPwdError" class="form-error">{{ registerPwdError }}</span>
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

    <Transition name="modal">
      <div v-if="showChangePassword" class="modal" @click.self="() => { showChangePassword = false; resetChangePasswordForm() }">
        <div class="modal-content" style="max-width: 420px;">
          <h3>修改密码</h3>
          <div class="form-group">
            <label>旧密码</label>
            <input type="password" v-model="changePasswordForm.oldPwd" placeholder="请输入旧密码" />
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input type="password" v-model="changePasswordForm.newPwd" placeholder="请输入新密码" />
          </div>
          <div class="form-group">
            <label>确认新密码</label>
            <input type="password" v-model="changePasswordForm.confirmPwd" placeholder="请再次输入新密码" />
          </div>
          <div class="modal-buttons">
            <button class="btn-cancel" @click="() => { showChangePassword = false; resetChangePasswordForm() }">取消</button>
            <button class="btn-submit" :disabled="changePasswordSubmitting" @click="handleChangePassword">
              {{ changePasswordSubmitting ? '提交中...' : '确认修改' }}
            </button>
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
import { getRandomCards, getMyCards, publishCard, getLocalUserId, setLocalUserId, getLocalToken, setLocalToken, login, register, logout as apiLogout, submitFeedback, followCard, getFollowedCards, getFollowedCardIds, getAnnouncements, getLatestAnnouncement, changePassword, getUserInfo, searchTags, getCardsByTag, getHotTags, loadUploadConfig, setOnSessionExpired } from './api.js'
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
const recentCardIds = ref([])
const refreshingAll = ref(false)

// ========== 标签搜索/筛选 ==========
const tagQuery = ref('')
const tagSuggestions = ref([])
const tagSearching = ref(false)
const activeTag = ref(null)
const tagSuggestionsOpen = ref(false)
const hotTags = ref([])
let tagDebounceTimer = null

async function loadHotTags() {
  try {
    const res = await getHotTags(5)
    hotTags.value = Array.isArray(res) ? res : []
  } catch {
    hotTags.value = []
  }
}

function clearTagSuggestions() {
  tagSuggestionsOpen.value = false
  tagSuggestions.value = []
}

async function fetchTagSuggestions(q) {
  const keyword = (q || '').trim()
  if (!keyword) {
    clearTagSuggestions()
    return
  }

  tagSearching.value = true
  tagSuggestionsOpen.value = true
  try {
    const res = await searchTags(keyword, 10)
    tagSuggestions.value = Array.isArray(res) ? res : []
  } catch {
    tagSuggestions.value = []
  } finally {
    tagSearching.value = false
  }
}

function onTagQueryInput() {
  if (tagDebounceTimer) clearTimeout(tagDebounceTimer)
  const q = tagQuery.value
  tagDebounceTimer = setTimeout(() => fetchTagSuggestions(q), 250)
}

async function selectTag(tag) {
  activeTag.value = tag
  tagQuery.value = tag?.name ? `#${tag.name}` : ''
  clearTagSuggestions()

  loading.value = true
  try {
    const res = await getCardsByTag(tag.id, 1, 10)
    cards.value = res?.records || []
    hasMore.value = res ? (1 < res.pages) : false
    if (userId.value) {
      try {
        const followedIds = await getFollowedCardIds()
        followedSet.value = new Set(followedIds)
      } catch {}
    }
  } finally {
    loading.value = false
  }
}

function clearTagFilter() {
  activeTag.value = null
  tagQuery.value = ''
  clearTagSuggestions()
  loadCards()
}

function onTagClick(tag) {
  activeTab.value = 'all'
  selectTag(tag)
}

function hashString(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = ((h << 5) - h) + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

const tagPalette = [
  { color: '#4361ee', bg: 'rgba(67, 97, 238, 0.10)', border: 'rgba(67, 97, 238, 0.25)' },
  { color: '#7c3aed', bg: 'rgba(124, 58, 237, 0.10)', border: 'rgba(124, 58, 237, 0.25)' },
  { color: '#f72585', bg: 'rgba(247, 37, 133, 0.10)', border: 'rgba(247, 37, 133, 0.25)' },
  { color: '#118ab2', bg: 'rgba(17, 138, 178, 0.10)', border: 'rgba(17, 138, 178, 0.25)' },
  { color: '#06d6a0', bg: 'rgba(6, 214, 160, 0.10)', border: 'rgba(6, 214, 160, 0.25)' },
  { color: '#ef476f', bg: 'rgba(239, 71, 111, 0.10)', border: 'rgba(239, 71, 111, 0.25)' },
  { color: '#ffd166', bg: 'rgba(255, 209, 102, 0.14)', border: 'rgba(255, 209, 102, 0.35)' },
]

function getTagPaletteItem(tag) {
  const rawId = tag && tag.id != null ? Number(tag.id) : null
  const idx = Number.isFinite(rawId)
    ? (Math.abs(rawId) % tagPalette.length)
    : (hashString(String(tag?.name || '')) % tagPalette.length)

  return tagPalette[idx]
}

function hotTagStyle(tag) {
  const p = getTagPaletteItem(tag)
  return {
    color: p.color,
    background: p.bg,
    border: `1px solid ${p.border}`
  }
}

const loginForm = ref({ nickname: '', pwd: '' })
const registerForm = ref({ nickname: '', pwd: '', confirmPwd: '' })
const showFeedback = ref(false)
const feedbackForm = ref({ title: '', content: '' })
const feedbackSubmitting = ref(false)
const showAnnouncements = ref(false)
const announcements = ref([])
const hasNewAnnouncement = ref(false)
const showChangePassword = ref(false)
const changePasswordForm = ref({ oldPwd: '', newPwd: '', confirmPwd: '' })
const changePasswordSubmitting = ref(false)
const userInfo = ref({})
const expProgress = ref(0)

function resetChangePasswordForm() {
  changePasswordForm.value = { oldPwd: '', newPwd: '', confirmPwd: '' }
}

async function handleChangePassword() {
  if (changePasswordSubmitting.value) return
  if (!changePasswordForm.value.oldPwd || !changePasswordForm.value.newPwd || !changePasswordForm.value.confirmPwd) {
    showToast('请填写完整信息', 'warning')
    return
  }
  if (changePasswordForm.value.newPwd !== changePasswordForm.value.confirmPwd) {
    showToast('两次输入的新密码不一致', 'error')
    return
  }
  changePasswordSubmitting.value = true
  try {
    const res = await changePassword(changePasswordForm.value.oldPwd, changePasswordForm.value.newPwd)
    if (res.success) {
      showToast('密码修改成功', 'success')
      showChangePassword.value = false
      resetChangePasswordForm()
    } else {
      showToast(res.error || '修改失败', 'error')
    }
  } catch (err) {
    showToast(err?.response?.data?.error || '网络异常，请稍后重试', 'error')
  } finally {
    changePasswordSubmitting.value = false
  }
}

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
const phrases = ['一张卡片，一段心事', '说出你的心声，让世界倾听', '想法无界限，言语无限可能', '分享你的生活趣事，激发共鸣', '让每个人的故事都有舞台']
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
function syncRecentCardIds(cardsList) {
  const nextIds = cardsList.map(card => card.id).filter(id => id != null)
  if (nextIds.length === 0) return

  const merged = [...nextIds, ...recentCardIds.value.filter(id => !nextIds.includes(id))]
  recentCardIds.value = merged.slice(0, 40)
  localStorage.setItem('cardword_recent_card_ids', JSON.stringify(recentCardIds.value))
}

async function loadCards() {
  loading.value = true
  try {
    const excludeIds = recentCardIds.value.slice(0, 30)
    const res = await getRandomCards(10, excludeIds)
    const { cards: nextCards, resetBlacklist } = res
    if (resetBlacklist) {
      recentCardIds.value = []
      localStorage.removeItem('cardword_recent_card_ids')
    }
    cards.value = nextCards
    syncRecentCardIds(cards.value)
    // 如果用户已登录，加载收藏状态
    if (userId.value) {
      try {
        const followedIds = await getFollowedCardIds()
        followedSet.value = new Set(followedIds)
      } catch (err) {
        console.error('加载收藏状态失败', err)
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadMyCards() {
  if (!userId.value) {
    cards.value = []
    hasMore.value = false
    loading.value = false
    return
  }
  loading.value = true
  try {
    page.value = 1
    const res = await getMyCards(page.value, 10)
    cards.value = res.records
    hasMore.value = page.value < res.pages
    try {
      const followedIds = await getFollowedCardIds()
      followedSet.value = new Set(followedIds)
    } catch (err) {
      console.error('加载收藏状态失败', err)
    }
  } catch (err) {
    if (err?.response?.status !== 401) {
      console.error('加载我的卡片失败', err)
    }
    cards.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
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
  if (refreshingAll.value) return
  refreshingAll.value = true
  shuffling.value = true
  const startedAt = Date.now()

  const p = (activeTab.value === 'all' && activeTag.value)
    ? selectTag(activeTag.value)
    : loadCards()

  Promise.resolve(p)
    .finally(() => {
      const elapsed = Date.now() - startedAt
      const minDuration = 700 // 确保动画至少展示一小段时间
      const remain = Math.max(0, minDuration - elapsed)

      setTimeout(() => {
        shuffling.value = false
        refreshingAll.value = false
      }, remain)
    })
}

async function loadMore() {
  try {
    page.value++
    if (mySubTab.value === 'followed') {
      const res = await getFollowedCards(page.value, 10)
      cards.value = [...cards.value, ...res.records]
      hasMore.value = page.value < res.pages
    } else {
      const res = await getMyCards(page.value, 10)
      cards.value = [...cards.value, ...res.records]
      hasMore.value = page.value < res.pages
    }
  } catch (err) {
    page.value--
    if (err?.response?.status !== 401) {
      showToast('加载失败，请重试', 'error')
    }
  }
}

async function onPublished(card) {
  if (!userId.value) {
    userNickname.value = card.username || ''
    localStorage.setItem('cardword_nickname', userNickname.value)
  }
  if (!userId.value && card.user && card.user.id) {
    userId.value = card.user.id
    setLocalUserId(card.user.id)
  }
  if (activeTab.value === 'my') {
    if (mySubTab.value === 'followed') {
      mySubTab.value = 'cards'
      await loadMyCards()
    } else {
      await loadMyCards()
    }
  } else {
    cards.value.unshift(card)
  }
  showForm.value = false
  launchConfetti()
  showToast('发布成功！', 'success')
}

async function handleLogin() {
  try {
    const res = await login(loginForm.value.nickname, loginForm.value.pwd)
    if (res.token) {
      setLocalToken(res.token)
      setLocalUserId(res.userId)
      userId.value = res.userId
      userNickname.value = loginForm.value.nickname
      localStorage.setItem('cardword_nickname', loginForm.value.nickname)
      // 清空密码
      loginForm.value.pwd = ''
      showLogin.value = false
      showToast(`${loginForm.value.nickname}，欢迎回来！`, 'success')
      setTimeout(() => window.location.reload(), 500)
    } else {
      showToast(res.error || '登录失败', 'error')
    }
  } catch (err) {
    showToast(err?.response?.data?.error || err?.message || '网络异常，请稍后重试', 'error')
  }
}

// 加载用户信息（经验值和等级）
async function loadUserInfo() {
  if (!userId.value) return
  try {
    const info = await getUserInfo()
    if (info.id) {
      userInfo.value = info
      // 计算进度条百分比：使用总经验值除以下一级所需总经验值
      const totalExp = info.exp || 0
      const nextLevelExp = info.nextLevelExp || 1
      expProgress.value = Math.min(100, Math.max(0, (totalExp / nextLevelExp) * 100))
    }
  } catch (err) {
    console.error('加载用户信息失败:', err)
  }
}

async function openPublishForm() {
  try {
    await getUserInfo()
    showForm.value = true
  } catch (err) {
    if (err?.response?.status === 401) return
    showForm.value = true
  }
}

function openLogin() {
  // 清空密码
  loginForm.value.pwd = ''
  showLogin.value = true
}

function openRegister() {
  // 清空密码
  registerForm.value.pwd = ''
  registerForm.value.confirmPwd = ''
  registerError.value = ''
  registerPwdError.value = ''
  showRegister.value = true
}

const registerError = ref('')
const registerPwdError = ref('')
async function handleRegister() {
  registerError.value = ''
  registerPwdError.value = ''

  // 检查两次密码是否一致
  if (registerForm.value.pwd !== registerForm.value.confirmPwd) {
    registerPwdError.value = '两次输入的密码不一致'
    return
  }

  try {
    const res = await register(registerForm.value.nickname, registerForm.value.pwd)
    if (res.userId) {
      // 清空密码
      registerForm.value.pwd = ''
      registerForm.value.confirmPwd = ''
      showRegister.value = false
      loginForm.value.nickname = registerForm.value.nickname
      loginForm.value.pwd = ''
      showLogin.value = true
      showToast('注册成功，请登录', 'success')
    } else if (res.error === '用户名已存在') {
      registerError.value = '用户名已存在，请换一个试试'
    } else {
      showToast(res.error || '注册失败', 'error')
    }
  } catch (err) {
    showToast(err?.response?.data?.error || err?.message || '网络异常，请稍后重试', 'error')
  }
}

async function handleFeedback() {
  if (feedbackSubmitting.value) return
  feedbackSubmitting.value = true
  try {
    const userId = getLocalUserId() || 0
    const res = await submitFeedback(feedbackForm.value.title.trim(), feedbackForm.value.content.trim(), userId)
    if (res.success) {
      showFeedback.value = false
      feedbackForm.value = { title: '', content: '' }
      showToast('提交成功，工程师尽快处理中', 'success')
    } else {
      showToast(res.error || '提交失败', 'error')
    }
  } catch (err) {
    if (err?.response?.status !== 401) {
      showToast('网络异常，请稍后重试', 'error')
    }
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
  // 调用后端注销接口，删除服务端 session
  apiLogout().catch(() => {
    // 忽略网络错误，前端照常清理本地状态
  })

  // 清除本地登录状态和 token
  userId.value = null
  userNickname.value = ''
  localStorage.removeItem('cardword_user_id')
  localStorage.removeItem('cardword_nickname')
  localStorage.removeItem('cardword_token')

  showChangePassword.value = false
  resetChangePasswordForm()
  showToast('已退出登录', 'info')
  setTimeout(() => window.location.reload(), 500)
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
  followedSet.value = new Set(followedSet.value)

  if (isFollowed) {
    followedSet.value.add(cardId)
  } else {
    followedSet.value.delete(cardId)
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
  try {
    page.value = 1
    const res = await getFollowedCards(page.value, 10)
    cards.value = res.records
    hasMore.value = page.value < res.pages
    followedSet.value = new Set(res.records.map(c => c.id))
  } catch (err) {
    if (err?.response?.status !== 401) {
      console.error('加载追更卡片失败', err)
    }
    cards.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setOnSessionExpired(() => {
    userId.value = null
    userNickname.value = ''
    showForm.value = false
    showChangePassword.value = false
    showToast('登录已过期，请重新登录', 'warning')
    setTimeout(() => {
      showLogin.value = true
    }, 800)
  })

  const storedRecent = localStorage.getItem('cardword_recent_card_ids')
  if (storedRecent) {
    try {
      recentCardIds.value = JSON.parse(storedRecent)
    } catch {
      recentCardIds.value = []
    }
  }
  loadCards()
  loadHotTags()
  loadUploadConfig()
  typeStep()
  checkNewAnnouncement()
  loadUserInfo()
})

onUnmounted(() => {
  clearTimeout(typingTimer)
})
</script>

<style>
/* ========== CSS 变量主题系统 ========== */
:root {
  --color-bg: #f5f5fb;
  --color-bg-card: #ffffff;
  --color-bg-card-end: #faf7ff;
  --color-text: #222222;
  --color-text-secondary: #666666;
  --color-text-muted: #aaaaaa;
  --color-text-content: #2c3e50;
  --color-accent: #6c5ce7;
  --color-accent-hover: #5b4bd6;
  --color-accent-light: #efe9ff;
  --color-danger: #ff6b81;
  --color-danger-light: #ffe8ec;
  --color-like: #ff4757;
  --color-border: #f1edf8;
  --color-border-strong: #ded7f0;
  --color-input-border: #d6d0ea;
  --color-nav-bg: #ffffff;
  --color-modal-backdrop: rgba(15, 12, 41, 0.25);
  --color-modal-bg: rgba(255, 255, 255, 0.9);
  --color-skeleton: #e7e1f7;
  --color-skeleton-shine: #f7f3ff;
  --color-comment-bg: #f8f5ff;
}

[data-theme="dark"] {
  --color-bg: #0e0e1c;
  --color-bg-card: #181828;
  --color-bg-card-end: #1e1e32;
  --color-text: #dcdcf0;
  --color-text-secondary: #9898b8;
  --color-text-muted: #5a5a7a;
  --color-text-content: #c8c8e8;
  --color-accent: #9d97ff;
  --color-accent-hover: #8a84ff;
  --color-accent-light: #1f1f3a;
  --color-danger: #ff7b94;
  --color-danger-light: #2b1621;
  --color-like: #ff7b94;
  --color-border: #2a2a45;
  --color-border-strong: #383860;
  --color-input-border: #383860;
  --color-nav-bg: #141422;
  --color-modal-backdrop: rgba(0, 0, 0, 0.6);
  --color-modal-bg: rgba(24, 24, 40, 0.92);
  --color-skeleton: #26264a;
  --color-skeleton-shine: #32325a;
  --color-comment-bg: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] body {
  background: linear-gradient(135deg, #0e0e1c 0%, #0a0a18 50%, #110818 100%);
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
  background: linear-gradient(135deg, #f0edf8 0%, #f5f5fb 40%, #fdf2f5 70%, #f5f5fb 100%);
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
.bg-circle { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.40; transition: opacity 0.5s ease; }
[data-theme="dark"] .bg-circle { opacity: 0.07; }
.bg-circle-1 { width: 500px; height: 500px; background: #4361ee; top: -150px; right: -100px; animation: bgFloat1 25s ease-in-out infinite; }
.bg-circle-2 { width: 400px; height: 400px; background: #7c3aed; bottom: -100px; left: -100px; animation: bgFloat2 20s ease-in-out infinite; }
.bg-circle-3 { width: 300px; height: 300px; background: #f72585; top: 40%; left: 50%; animation: bgFloat3 22s ease-in-out infinite; }
@keyframes bgFloat1 { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(-50px, 50px) scale(1.1); } 66% { transform: translate(30px, -30px) scale(0.95); } }
@keyframes bgFloat2 { 0%, 100% { transform: translate(0, 0) scale(1); } 33% { transform: translate(40px, -40px) scale(1.05); } 66% { transform: translate(-30px, 20px) scale(0.9); } }
@keyframes bgFloat3 { 0%, 100% { transform: translate(0, 0) scale(1); } 50% { transform: translate(-60px, -40px) scale(1.1); } }

/* ========== 标题 ========== */
.header { text-align: center; margin-bottom: 32px; position: relative; z-index: 1; }

/* ========== 标签搜索框（胶囊风） ========== */
.tag-search {
  margin-top: 18px;
  display: inline-block;
  width: min(520px, 92vw);
  text-align: left;
}
.tag-search-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tag-search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  border: 1.5px solid var(--color-input-border);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 26px rgba(15, 12, 41, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
[data-theme="dark"] .tag-search-input-wrap {
  background: rgba(17, 19, 34, 0.72);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.35);
}
.tag-search-input-wrap:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 4px rgba(108, 92, 231, 0.14), 0 12px 30px rgba(15, 12, 41, 0.10);
  transform: translateY(-1px);
}
.tag-search-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.tag-search-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: var(--color-text);
  font-size: 14px;
  padding: 0;
}
.tag-search-input::placeholder {
  color: var(--color-text-muted);
}
.tag-clear-btn {
  border: 1.5px solid var(--color-border-strong);
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-text-secondary);
  padding: 10px 14px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 10px 26px rgba(15, 12, 41, 0.06);
}
[data-theme="dark"] .tag-clear-btn {
  background: rgba(17, 19, 34, 0.72);
}
.tag-clear-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  transform: translateY(-1px);
}
.tag-suggestions {
  margin-top: 10px;
  border-radius: 16px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
  box-shadow: 0 18px 48px rgba(15, 12, 41, 0.12);
  overflow: hidden;
}
.tag-suggestions-item {
  width: 100%;
  text-align: left;
  padding: 10px 14px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text);
  font-size: 14px;
}
.tag-suggestions-item:hover {
  background: var(--color-accent-light);
  color: var(--color-accent);
}
.tag-suggestions-item.muted {
  cursor: default;
  color: var(--color-text-muted);
}
.tag-active-hint {
  margin-top: 10px;
  font-size: 12px;
  color: var(--color-text-muted);
  padding-left: 6px;
}
.hot-tags-wrap {
  margin-top: 12px;
  margin-left: 2px;
  padding: 12px 13px 14px;
  border-radius: 18px;
  border: 1px solid rgba(124, 58, 237, 0.18);
  background:
    radial-gradient(120% 110% at 0% 0%, rgba(99, 102, 241, 0.20) 0%, rgba(99, 102, 241, 0) 55%),
    radial-gradient(120% 110% at 100% 100%, rgba(244, 114, 182, 0.20) 0%, rgba(244, 114, 182, 0) 58%),
    linear-gradient(138deg, rgba(255, 255, 255, 0.78) 0%, rgba(248, 250, 255, 0.86) 48%, rgba(255, 245, 250, 0.72) 100%);
  box-shadow: 0 10px 26px rgba(99, 102, 241, 0.10);
}
[data-theme="dark"] .hot-tags-wrap {
  border-color: rgba(168, 85, 247, 0.24);
  background:
    radial-gradient(120% 110% at 0% 0%, rgba(99, 102, 241, 0.26) 0%, rgba(99, 102, 241, 0) 58%),
    radial-gradient(120% 110% at 100% 100%, rgba(236, 72, 153, 0.22) 0%, rgba(236, 72, 153, 0) 60%),
    linear-gradient(140deg, rgba(26, 27, 49, 0.92) 0%, rgba(22, 20, 40, 0.90) 100%);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.34);
}
.hot-tags-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 11px;
}
.hot-tags-label {
  font-family: 'Pacifico', 'LXGW WenKai Screen', cursive;
  font-size: 15px;
  font-weight: 400;
  color: var(--color-accent);
  letter-spacing: 1px;
  flex-shrink: 0;
}
.hot-tags-desc {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hot-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 12px;
  max-height: 32px;
  overflow: hidden;
}
.hot-tag-btn {
  display: inline-flex;
  align-items: center;
  border: none;
  background: var(--tag-bg, rgba(107, 114, 128, 0.16));
  color: var(--tag-color, #4b5563);
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  white-space: nowrap;
  user-select: none;
}
.hot-tag-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.hot-tag-btn:active {
  transform: translateY(0);
}
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
.nav-btn { width: 48px; height: 48px; border-radius: 50%; border: none; background: rgba(255, 255, 255, 0.96); color: var(--color-text-secondary); cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 24px rgba(15, 12, 41, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.8); transition: all 0.22s cubic-bezier(0.34, 1.56, 0.64, 1); position: relative; backdrop-filter: blur(10px); }
.nav-btn svg { width: 20px; height: 20px; }
.nav-btn:hover { transform: translateY(-2px) scale(1.04); box-shadow: 0 14px 32px rgba(15, 12, 41, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.9); color: var(--color-accent); }
.nav-btn.active { background: var(--color-accent); color: #fff; box-shadow: 0 16px 34px rgba(108, 92, 231, 0.35); }
/* 换一批刷新图标与动画 */
.grid-icon {
  transition: transform 0.18s ease;
}

.nav-btn:hover .grid-icon {
  transform: translateY(-1px);
}

.refresh-icon {
  transform-origin: 12px 12px;
}

.refresh-icon-inner {
  transform-origin: 12px 12px;
}

/* 点击瞬间轻微缩放一下，反馈“已点击” */
.nav-btn:active .refresh-icon-inner {
  transform: scale(0.9);
}

/* 正在刷新时旋转动画，由 shuffling 控制 */
.refresh-icon.spinning .refresh-icon-inner {
  animation: refresh-spin 1.1s cubic-bezier(0.33, 1, 0.68, 1) infinite;
}

@keyframes refresh-spin {
  0%   { transform: rotate(0deg)   scale(0.96); }
  50%  { transform: rotate(300deg) scale(1.05); }
  100% { transform: rotate(360deg) scale(1); }
}
.nav-btn-primary { background: var(--color-accent); color: #fff; }
.nav-btn-primary:hover { background: var(--color-accent-hover); color: #fff; box-shadow: 0 4px 12px rgba(212,165,116,0.3), 0 8px 24px rgba(212,165,116,0.15); }
.nav-btn-danger { color: #ccc; }
.nav-btn-danger:hover { background: var(--color-danger); color: #fff; box-shadow: 0 4px 12px rgba(224,122,106,0.3), 0 8px 24px rgba(224,122,106,0.15); }
.nav-divider { width: 24px; height: 1px; background: var(--color-border-strong); margin: 2px 0; }
.nav-btn::after { content: attr(title); position: absolute; right: 58px; top: 50%; transform: translateY(-50%); background: #333; color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 12px; white-space: nowrap; opacity: 0; pointer-events: none; transition: opacity 0.2s ease; }
.nav-btn:hover::after { opacity: 1; }

/* ========== 瀑布流 ========== */
.card-grid {
  column-count: 2;
  column-gap: 20px;
  column-fill: balance;
  position: relative;
  z-index: 1;
}

.card-grid > * {
  width: 100%;
}

/* ========== 骨架屏 ========== */
.skeleton-card {
  background: var(--color-bg-card);
  border-radius: 16px;
  padding: 24px;
  padding-top: 27px;
  margin-bottom: 20px;
  break-inside: avoid;
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.skeleton-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--color-skeleton);
  opacity: 0.3;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.skeleton-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-skeleton);
  animation: shimmer 1.5s ease infinite;
}

.skeleton-nickname {
  height: 14px;
  width: 60px;
  border-radius: 7px;
  background: var(--color-skeleton);
  animation: shimmer 1.5s ease infinite;
}

.skeleton-content {
  margin-bottom: 16px;
}

.skeleton-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, var(--color-skeleton) 25%, var(--color-skeleton-shine) 50%, var(--color-skeleton) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease infinite;
  margin-bottom: 12px;
}

.skeleton-line:last-child { margin-bottom: 0; }

.skeleton-image {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  background: var(--color-skeleton);
  margin-bottom: 16px;
  animation: shimmer 1.5s ease infinite;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid var(--color-border);
}

.skeleton-time {
  height: 12px;
  width: 50px;
  border-radius: 6px;
  background: var(--color-skeleton);
  animation: shimmer 1.5s ease infinite;
}

.skeleton-actions {
  display: flex;
  gap: 8px;
}

.skeleton-action {
  width: 32px;
  height: 22px;
  border-radius: 11px;
  background: var(--color-skeleton);
  animation: shimmer 1.5s ease infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}


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
.toast-error { border-left-color: #ff6b81; }
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
.toast-error .toast-icon { background: #ff6b81; }
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
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1500;
  padding: 20px;
  overflow-y: auto;
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
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  margin: auto;
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

.profile-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-nickname {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
}

.level-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.level-badge {
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  color: #fff;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.exp-bar {
  flex: 1;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.exp-progress {
  height: 100%;
  background: linear-gradient(90deg, #6c5ce7, #a29bfe);
  border-radius: 4px;
  transition: width 0.3s ease;
  min-width: 2px;
}

.exp-text {
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
  min-width: 100px;
  text-align: right;
}

.theme-toggle-btn {
  margin-left: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1.5px solid var(--color-border);
  background: var(--color-bg-card);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
  gap: 6px;
  font-size: 13px;
}

.theme-toggle-btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.theme-toggle-btn:hover {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.25);
}


.my-sub-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 2px solid var(--color-border);
}

.sub-tab-group {
  display: flex;
  gap: 0;
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

.my-actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}


.btn-outline {
  padding: 6px 18px;
  border-radius: 999px;
  border: 1.5px solid var(--color-border-strong);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline:hover {
  color: var(--color-text);
  border-color: var(--color-accent);
}


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
