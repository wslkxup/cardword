<template>
  <div class="comment-section">
    <div class="comment-list" ref="commentListRef">
      <TransitionGroup name="comment-pop">
      <div v-for="c in comments" :key="c.id" class="comment-item">
        <div class="comment-avatar">
          <template v-if="isAnonymousCard && c.userId === props.card?.userId">匿</template>
          <template v-else>{{ (c.nickname || '?')[0] }}</template>
        </div>
        <div class="comment-bubble">
          <div class="comment-bubble-header">
            <span class="comment-author">
              <template v-if="isAnonymousCard && c.userId === props.card?.userId">匿名卡片作者</template>
              <template v-else>{{ c.nickname }}</template>
            </span>
            <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
          </div>
          <p v-if="c.replyToNickname" class="comment-reply-tag">回复 @{{ c.replyToNickname }}</p>
          <p class="comment-text">{{ c.content }}</p>
          <button v-if="props.userId" class="btn-reply" @click="setReply(c)">回复</button>
        </div>
      </div>
      </TransitionGroup>
      <p v-if="comments.length === 0" class="no-comment">暂无评论，来抢沙发</p>
    </div>
    <div v-if="props.userId" class="comment-input">
      <div v-if="replyTo" class="reply-hint">
        <span>
          <template v-if="isAnonymousCard && replyTo.userId === props.card?.userId">回复 @匿名卡片作者</template>
          <template v-else>回复 @{{ replyTo.nickname }}</template>
        </span>
        <button class="btn-cancel-reply" @click="cancelReply">取消</button>
      </div>
      <div class="input-row">
        <input v-model="content" :placeholder="replyTo ? (isAnonymousCard && replyTo.userId === props.card?.userId ? '回复 @匿名卡片作者...' : `回复 @${replyTo.nickname}...`) : '写评论...'" class="input-content" @keyup.enter="submit" />
        <button class="btn-send" :class="{ 'btn-sent': sent }" :disabled="!content.trim() || sending" @click="submit">
          {{ sent ? '✓' : sending ? '...' : '发送' }}
        </button>
      </div>
    </div>
    <p v-else class="no-comment">请先登录后再评论</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { getComments, addComment } from '../api.js'

const props = defineProps({ cardId: Number, userId: Number, card: Object })
const emit = defineEmits(['commented'])

const comments = ref([])
const content = ref('')
const sending = ref(false)
const sent = ref(false)
const commentListRef = ref(null)
const replyTo = ref(null)

const isAnonymousCard = computed(() => props.card?.isAnonymous === 1)

function setReply(comment) {
  replyTo.value = { id: comment.id, nickname: comment.nickname, userId: comment.userId }
}

function cancelReply() {
  replyTo.value = null
}

async function loadComments() {
  const res = await getComments(props.cardId)
  comments.value = res
}

async function submit() {
  if (!content.value.trim() || sending.value) return
  sending.value = true
  try {
    const parentId = replyTo.value ? replyTo.value.id : null
    const res = await addComment(props.cardId, content.value.trim(), '', parentId)
    comments.value.push(res)
    content.value = ''
    replyTo.value = null
    emit('commented')
    sent.value = true
    setTimeout(() => { sent.value = false }, 1200)
    await nextTick()
    const list = commentListRef.value
    if (list) list.scrollTop = list.scrollHeight
  } finally {
    sending.value = false
  }
}

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

onMounted(loadComments)
</script>

<style scoped>
.comment-section {
  margin-top: 14px;
  border-top: 1px solid var(--color-border);
  padding-top: 14px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-accent-light);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-bubble {
  flex: 1;
  background: var(--color-comment-bg);
  border-radius: 12px;
  padding: 8px 12px;
  min-width: 0;
}

.comment-bubble-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  gap: 8px;
}

.comment-author {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-accent);
}

.comment-time {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.comment-text {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.5;
  word-break: break-word;
}

.comment-reply-tag {
  font-size: 12px;
  color: var(--color-accent);
  margin-bottom: 2px;
}

.btn-reply {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 0 0;
  transition: color 0.2s;
}

.btn-reply:hover {
  color: var(--color-accent);
}

.no-comment {
  color: var(--color-text-muted);
  font-size: 13px;
  padding: 8px 0;
}

.comment-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.input-row {
  display: flex;
  gap: 8px;
}

.reply-hint {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--color-accent);
  background: var(--color-accent-light);
  padding: 4px 10px;
  border-radius: 8px;
}

.btn-cancel-reply {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
}

.btn-cancel-reply:hover {
  color: var(--color-text);
}

.input-content {
  flex: 1;
  border: 1px solid var(--color-input-border);
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
  background: var(--color-bg-card);
  color: var(--color-text);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-content:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
}

.btn-send {
  background: var(--color-accent);
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
  white-space: nowrap;
}

.btn-send:hover {
  background: var(--color-accent-hover);
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-sent {
  background: #06d6a0 !important;
  transform: scale(1.05);
}

/* TransitionGroup animations for new comments */
.comment-pop-enter-from {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}

.comment-pop-enter-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.comment-pop-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.comment-pop-move {
  transition: transform 0.3s ease;
}
</style>
