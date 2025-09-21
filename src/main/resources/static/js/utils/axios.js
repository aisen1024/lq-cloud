// Axios 独立封装
class ApiClient {
    constructor() {
        this.instance = axios.create({
            baseURL: '/api/console',
            timeout: 10000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        this.setupInterceptors();
    }
    
    setupInterceptors() {
        // 请求拦截器
        this.instance.interceptors.request.use(
            config => {
                const token = localStorage.getItem('token');
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
            },
            error => Promise.reject(error)
        );
        
        // 响应拦截器
        this.instance.interceptors.response.use(
            response => response.data,
            error => {
                if (error.response?.status === 401) {
                    localStorage.removeItem('token');
                    window.location.href = '/login.html';
                }
                return Promise.reject(error);
            }
        );
    }
    
    // API 方法
    async getOverview() {
        return this.instance.get('/overview');
    }
    
    async getQueueStatistics() {
        return this.instance.get('/queues/statistics');
    }
    
    async getServiceStatistics() {
        return this.instance.get('/services/statistics');
    }
    
    async getSystemMetrics() {
        return this.instance.get('/system/metrics');
    }
    
    async getQueues() {
        return this.instance.get('/queues');
    }
    
    async createQueue(data) {
        return this.instance.post('/queues', data);
    }
    
    async deleteQueue(id) {
        return this.instance.delete(`/queues/${id}`);
    }
    
    async getServices() {
        return this.instance.get('/services');
    }
    
    async getConfigs() {
        return this.instance.get('/configs');
    }
    
    async createConfig(data) {
        return this.instance.post('/configs', data);
    }
    
    async deleteConfig(id) {
        return this.instance.delete(`/configs/${id}`);
    }
    
    async getMcpTools() {
        return this.instance.get('/mcp-tools');
    }
}

// 全局实例
window.apiClient = new ApiClient();