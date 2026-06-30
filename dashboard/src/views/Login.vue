<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="login-title">LingQue Admin</h1>
      <p class="login-subtitle">管理和治理平台</p>
      
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            required
            autocomplete="username"
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            required
            autocomplete="current-password"
          />
        </div>
        
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        
        <button type="submit" class="login-button" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      
      <div class="login-info">
        <p>默认账号：admin</p>
        <p>默认密码：admin123</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const username = ref('admin')
    const password = ref('admin123')
    const loading = ref(false)
    const errorMessage = ref('')

    const handleLogin = async () => {
      loading.value = true
      errorMessage.value = ''

      try {
        const response = await api.auth.login(username.value, password.value)
        
        if (response.data.success) {
          // 保存 token 和用户信息
          localStorage.setItem('token', response.data.token)
          localStorage.setItem('username', response.data.username)
          
          // 跳转到首页
          router.push('/')
        } else {
          errorMessage.value = response.data.message || '登录失败'
        }
      } catch (error) {
        console.error('登录错误:', error)
        errorMessage.value = '登录失败，请检查网络连接'
      } finally {
        loading.value = false
      }
    }

    return {
      username,
      password,
      loading,
      errorMessage,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-title {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  background: #fee;
  color: #c33;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 15px;
  font-size: 14px;
  text-align: center;
}

.login-button {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.login-button:hover:not(:disabled) {
  opacity: 0.9;
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-info {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 13px;
}

.login-info p {
  margin: 5px 0;
}
</style>
