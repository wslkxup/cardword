<template>
  <div class="overlay" @click.self="$emit('close')">
    <div class="form-box">
      <h3>写一张卡片</h3>
      <textarea
        v-model="content"
        maxlength="200"
        placeholder="勇敢说出你的心声..."
        rows="4"
      ></textarea>

      <!-- 标签输入 -->
      <div class="tag-input-wrap">
        <input
          class="tag-input"
          v-model="tagInput"
          maxlength="200"
          placeholder="添加标签（按回车添加，最多 5 个）"
          @keydown.enter.prevent="addTagsFromInput"
          @input="onTagInputChange"
          @blur="onTagBlur"
          @focus="onTagFocus"
        />
        <div v-if="tagSuggestionsOpen && tagSuggestions.length" class="tag-suggestions">
          <button
            v-for="s in tagSuggestions"
            :key="s.id"
            type="button"
            class="tag-suggestions-item"
            @mousedown.prevent="applySuggestion(s)"
          >
            #{{ s.name }}
            <span v-if="s.useCount" class="tag-suggestions-count">{{ s.useCount }}</span>
          </button>
        </div>
      </div>
      <div v-if="tags.length" class="tag-chips">
        <span v-for="t in tags" :key="t" class="tag-chip">
          #{{ t }}
          <button type="button" class="tag-remove" @click="removeTag(t)">×</button>
        </span>
      </div>
      <div class="tag-hint">单个标签最长 12 字符</div>

      <!-- 图片预览 -->
      <div v-if="imageUrl" class="image-preview">
        <img :src="imageUrl" alt="预览图片" />
        <button class="remove-image-btn" @click="removeImage" title="删除图片">×</button>
      </div>

      <div class="form-meta">
        <span class="char-count">{{ content.length }}/200</span>
      </div>

      <!-- 图片上传按钮 -->
      <div class="image-actions">
        <label class="btn-upload" v-if="!imageUrl">
          <input
            type="file"
            :accept="uploadConfig.accept"
            @change="onFileChange"
            hidden
          />
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          <span>添加图片</span>
        </label>
      </div>

      <!-- 匿名卡片选项 -->
      <div class="anonymous-toggle">
        <label class="switch-label">
          <span class="switch-text">匿名卡片</span>
          <div class="switch">
            <input type="checkbox" v-model="isAnonymous" />
            <span class="slider"></span>
          </div>
        </label>
        <span class="anonymous-hint">发布后显示为"匿名卡片"，但本人可见</span>
      </div>

      <div class="form-actions">
        <button class="btn-cancel" @click="$emit('close')">取消</button>
        <button class="btn-submit" :disabled="(!content.trim() && !imageUrl) || uploading" @click="submit">写好了，发出去</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  uploadConfig,
  publishCard,
  uploadImage,
  searchTags
} from '../api.js'

const emit = defineEmits(['close', 'published'])

const content = ref('')
const imageUrl = ref(null)
const isAnonymous = ref(false)
const uploading = ref(false)

const tagInput = ref('')
const tags = ref([])
const tagSuggestions = ref([])
const tagSuggestionsOpen = ref(false)
let tagDebounceTimer = null

function onTagInputChange() {
  const q = tagInput.value.trim()
  if (tagDebounceTimer) clearTimeout(tagDebounceTimer)
  if (!q) {
    tagSuggestions.value = []
    tagSuggestionsOpen.value = false
    return
  }
  tagDebounceTimer = setTimeout(async () => {
    try {
      const res = await searchTags(q, 6)
      tagSuggestions.value = Array.isArray(res) ? res : []
      tagSuggestionsOpen.value = tagSuggestions.value.length > 0
    } catch {
      tagSuggestions.value = []
      tagSuggestionsOpen.value = false
    }
  }, 200)
}

function onTagFocus() {
  if (tagInput.value.trim() && tagSuggestions.value.length) {
    tagSuggestionsOpen.value = true
  }
}

function onTagBlur() {
  setTimeout(() => {
    tagSuggestionsOpen.value = false
  }, 150)
  addTagsFromInput()
}

function applySuggestion(s) {
  if (tags.value.length >= 5) return
  const name = s.name.length > 12 ? s.name.slice(0, 12) : s.name
  if (!tags.value.includes(name)) tags.value.push(name)
  tagInput.value = ''
  tagSuggestions.value = []
  tagSuggestionsOpen.value = false
}

function addTagsFromInput() {
  const value = tagInput.value.trim()
  if (!value) return
  if (tags.value.length >= 5) {
    tagInput.value = ''
    return
  }

  const v = value.length > 12 ? value.slice(0, 12) : value
  if (!tags.value.includes(v)) tags.value.push(v)
  tagInput.value = ''
}

function removeTag(t) {
  tags.value = tags.value.filter(x => x !== t)
}

async function isStaticImage(file) {
  const header = new Uint8Array(await file.slice(0, 16).arrayBuffer())
  const headerText = String.fromCharCode(...header)
  if (headerText.startsWith('GIF87a') || headerText.startsWith('GIF89a')) {
    return false
  }

  try {
    const bitmap = await createImageBitmap(file)
    bitmap.close()
    return true
  } catch {
    return false
  }
}

async function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > uploadConfig.maxSizeBytes) {
    alert(`图片大小不能超过 ${uploadConfig.maxSizeDisplay}`)
    e.target.value = ''
    return
  }

  if (!uploadConfig.allowedTypes.includes(file.type)) {
    alert('只允许上传 JPG、PNG、WEBP 格式的静态图片')
    e.target.value = ''
    return
  }

  if (!await isStaticImage(file)) {
    alert('不支持上传动态图片或非法图片文件')
    e.target.value = ''
    return
  }

  uploading.value = true
  try {
    const res = await uploadImage(file)
    if (res.error) {
      alert(res.error)
    } else {
      imageUrl.value = res.url
    }
  } catch (err) {
    alert('上传失败：' + err.message)
  } finally {
    uploading.value = false
    e.target.value = '' // 清空 input，允许重复选择同一文件
  }
}

function removeImage() {
  imageUrl.value = null
}

async function submit() {
  addTagsFromInput()
  if (!content.value.trim() && !imageUrl.value) return

  try {
    const res = await publishCard(
      content.value.trim(),
      '',
      imageUrl.value,
      isAnonymous.value ? 1 : 0,
      tags.value
    )
    emit('published', res.data)
  } catch (err) {
    if (err?.response?.status === 401) return
    alert('发布失败：' + (err?.response?.data?.error || err?.message || '未知错误'))
  }
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

.tag-input-wrap {
  position: relative;
  margin-top: 10px;
}

.tag-input {
  width: 100%;
  border: 1px solid var(--color-input-border);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  font-family: inherit;
  background: var(--color-bg-card);
  color: var(--color-text);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.tag-input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(67, 97, 238, 0.1);
}

.tag-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--color-modal-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  margin-top: 4px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 10;
  overflow: hidden;
}

.tag-suggestions-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text);
  font-size: 14px;
  text-align: left;
  transition: background 0.15s;
}

.tag-suggestions-item:hover {
  background: var(--color-accent-light);
  color: var(--color-accent);
}

.tag-suggestions-count {
  font-size: 11px;
  color: var(--color-text-muted);
  background: rgba(107, 114, 128, 0.1);
  padding: 1px 6px;
  border-radius: 999px;
}

.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(67, 97, 238, 0.08);
  border: 1px solid rgba(67, 97, 238, 0.18);
  color: var(--color-accent);
  font-size: 12px;
  line-height: 1;
}

.tag-remove {
  border: none;
  background: transparent;
  color: currentColor;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
}

.tag-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
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

.image-preview {
  position: relative;
  margin-top: 12px;
  border-radius: 10px;
  overflow: hidden;
  max-width: 100%;
}

.image-preview img {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  display: block;
}

.remove-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.remove-image-btn:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: scale(1.1);
}

.image-actions {
  margin-top: 12px;
}

.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-accent-light);
  border: 1px dashed var(--color-accent);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-accent);
  transition: all 0.2s;
}

.btn-upload svg {
  width: 18px;
  height: 18px;
}

.btn-upload:hover {
  background: var(--color-accent);
  color: #fff;
}

.anonymous-toggle {
  margin-top: 12px;
}

.switch-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text);
}

.switch-text {
  font-weight: 500;
}

.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--color-accent);
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.anonymous-hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
