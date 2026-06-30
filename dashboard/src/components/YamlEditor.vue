<template>
  <div class="yaml-editor-container">
    <div class="editor-toolbar">
      <span class="editor-label">{{ label }}</span>
      <div class="editor-actions">
        <button @click="formatContent" class="editor-btn" title="格式化">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 7h16M4 12h16M4 17h16"/>
          </svg>
          格式化
        </button>
        <button @click="copyContent" class="editor-btn" title="复制">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
          复制
        </button>
      </div>
    </div>
    <div class="editor-wrapper">
      <div class="line-numbers">
        <div v-for="n in lineCount" :key="n" class="line-number">{{ n }}</div>
      </div>
      <textarea
        ref="editorRef"
        v-model="content"
        :readonly="readonly"
        :placeholder="placeholder"
        class="yaml-editor"
        @input="handleInput"
        @scroll="syncScroll"
        @keydown.tab.prevent="handleTab"
        spellcheck="false"
      ></textarea>
    </div>
    <div v-if="error" class="editor-error">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  readonly: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: '请输入配置内容...'
  },
  label: {
    type: String,
    default: '配置内容'
  },
  validateYaml: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'error'])

const editorRef = ref(null)
const content = ref(props.modelValue)
const error = ref('')

const lineCount = computed(() => {
  return content.value.split('\n').length
})

watch(() => props.modelValue, (newVal) => {
  if (newVal !== content.value) {
    content.value = newVal
  }
})

watch(content, (newVal) => {
  emit('update:modelValue', newVal)
  if (props.validateYaml) {
    validateYamlSyntax(newVal)
  }
})

const handleInput = () => {
  // 内容已通过 v-model 更新
}

const handleTab = (e) => {
  const textarea = editorRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  
  // 插入两个空格
  content.value = content.value.substring(0, start) + '  ' + content.value.substring(end)
  
  // 恢复光标位置
  nextTick(() => {
    textarea.selectionStart = textarea.selectionEnd = start + 2
  })
}

const syncScroll = (e) => {
  const lineNumbers = e.target.previousElementSibling
  if (lineNumbers) {
    lineNumbers.scrollTop = e.target.scrollTop
  }
}

const formatContent = () => {
  try {
    // 简单的 YAML 格式化
    const lines = content.value.split('\n')
    const formatted = lines.map(line => {
      // 移除行尾空格
      return line.trimEnd()
    }).join('\n')
    
    content.value = formatted
    error.value = ''
  } catch (e) {
    error.value = '格式化失败: ' + e.message
  }
}

const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(content.value)
    // 可以添加一个提示
    const btn = event.target.closest('.editor-btn')
    const originalText = btn.textContent
    btn.textContent = '已复制!'
    setTimeout(() => {
      btn.textContent = originalText
    }, 2000)
  } catch (e) {
    error.value = '复制失败'
  }
}

const validateYamlSyntax = (text) => {
  if (!text.trim()) {
    error.value = ''
    emit('error', '')
    return
  }
  
  try {
    // 简单的 YAML 语法检查
    const lines = text.split('\n')
    let inMultiline = false
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i]
      const trimmed = line.trim()
      
      // 跳过空行和注释
      if (!trimmed || trimmed.startsWith('#')) continue
      
      // 检查缩进（必须是偶数个空格）
      const indent = line.match(/^ */)[0].length
      if (indent % 2 !== 0) {
        error.value = `第 ${i + 1} 行: 缩进必须是偶数个空格`
        emit('error', error.value)
        return
      }
      
      // 检查键值对格式
      if (trimmed.includes(':') && !inMultiline) {
        const parts = trimmed.split(':')
        if (parts.length < 2) {
          error.value = `第 ${i + 1} 行: 键值对格式错误`
          emit('error', error.value)
          return
        }
      }
      
      // 检查多行字符串
      if (trimmed.endsWith('|') || trimmed.endsWith('>')) {
        inMultiline = true
      } else if (inMultiline && indent === 0) {
        inMultiline = false
      }
    }
    
    error.value = ''
    emit('error', '')
  } catch (e) {
    error.value = 'YAML 语法错误: ' + e.message
    emit('error', error.value)
  }
}
</script>

<style scoped>
.yaml-editor-container {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f7fafc;
  border-bottom: 1px solid #e2e8f0;
}

.editor-label {
  font-weight: 500;
  color: #2d3748;
  font-size: 0.875rem;
}

.editor-actions {
  display: flex;
  gap: 0.5rem;
}

.editor-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-size: 0.875rem;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s;
}

.editor-btn:hover {
  background: #edf2f7;
  border-color: #cbd5e0;
}

.editor-wrapper {
  display: flex;
  position: relative;
  background: #f7fafc;
}

.line-numbers {
  padding: 1rem 0.5rem;
  background: #edf2f7;
  color: #a0aec0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
  font-size: 13px;
  line-height: 1.5;
  text-align: right;
  user-select: none;
  overflow: hidden;
  min-width: 3rem;
  border-right: 1px solid #e2e8f0;
}

.line-number {
  height: 19.5px;
}

.yaml-editor {
  flex: 1;
  padding: 1rem;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
  font-size: 13px;
  line-height: 1.5;
  border: none;
  outline: none;
  resize: vertical;
  min-height: 400px;
  background: transparent;
  color: #2d3748;
  tab-size: 2;
}

.yaml-editor:focus {
  background: #fff;
}

.yaml-editor::placeholder {
  color: #a0aec0;
}

.editor-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #fff5f5;
  border-top: 1px solid #feb2b2;
  color: #c53030;
  font-size: 0.875rem;
}

.editor-error svg {
  flex-shrink: 0;
}
</style>
