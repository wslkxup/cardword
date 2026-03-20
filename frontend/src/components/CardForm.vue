<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="form-box">
      <h3>发布卡片</h3>
      <textarea
        v-model="content"
        maxlength="200"
        placeholder="写下你的想法..."
        rows="4"
      ></textarea>
      <div class="form-meta">
        <span class="char-count">{{ content.length }}/200</span>
      </div>
      <div class="form-actions">
        <button class="btn-cancel" @click="$emit('close')">取消</button>
        <button class="btn-submit" :disabled="!content.trim()" @click="submit">发布</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { publishCard, getLocalUserId } from '../api.js'

const emit = defineEmits(['close', 'published'])

const content = ref('')

async function submit() {
  if (!content.value.trim()) return
  const userId = getLocalUserId()
  const res = await publishCard(content.value.trim(), '', userId)
  emit('published', res.data)
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: var(--color-modal-backdrop);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.form-box {
  background: var(--color-modal-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 28px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.form-box h3 {
  margin-bottom: 16px;
  font-size: 18px;
  color: var(--color-text);
}

textarea {
  width: 100%;
  border: 1px solid var(--color-input-border);
  border-radius: 10px;
  padding: 12px;
  font-size: 15px;
  resize: none;
  outline: none;
  font-family: inherit;
  background: var(--color-bg-card);
  color: var(--color-text);
  transition: border-color 0.2s, box-shadow 0.2s;
}

textarea:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
}

.form-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 8px;
}

.char-count {
  font-size: 12px;
  color: var(--color-text-muted);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel {
  background: transparent;
  border: 1px solid var(--color-input-border);
  color: var(--color-text-secondary);
  padding: 9px 18px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: var(--color-accent-light);
}

.btn-submit {
  background: var(--color-accent);
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-submit:hover {
  background: var(--color-accent-hover);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
