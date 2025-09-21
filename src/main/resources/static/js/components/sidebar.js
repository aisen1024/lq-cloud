// 侧边栏组件
class Sidebar {
    constructor() {
        this.currentPage = 'dashboard';
        this.init();
    }
    
    init() {
        this.render();
        this.bindEvents();
    }
    
    render() {
        const sidebarHtml = `
            <div class="sidebar">
                <div class="sidebar-header">
                    <div class="logo">
                        <i class="fas fa-brain"></i>
                        <span>AI Cloud</span>
                    </div>
                </div>
                <nav class="sidebar-nav">
                    <div class="nav-item ${this.currentPage === 'dashboard' ? 'active' : ''}" data-page="dashboard">
                        <i class="fas fa-tachometer-alt"></i>
                        <span>仪表盘</span>
                    </div>
                    <div class="nav-item ${this.currentPage === 'queue' ? 'active' : ''}" data-page="queue">
                        <i class="fas fa-list"></i>
                        <span>队列管理</span>
                    </div>
                    <div class="nav-item ${this.currentPage === 'service' ? 'active' : ''}" data-page="service">
                        <i class="fas fa-network-wired"></i>
                        <span>服务发现</span>
                    </div>
                    <div class="nav-item ${this.currentPage === 'config' ? 'active' : ''}" data-page="config">
                        <i class="fas fa-cogs"></i>
                        <span>配置中心</span>
                    </div>
                    <div class="nav-item ${this.currentPage === 'mcp' ? 'active' : ''}" data-page="mcp">
                        <i class="fas fa-tools"></i>
                        <span>MCP工具</span>
                    </div>
                </nav>
                <div class="sidebar-footer">
                    <div class="user-info">
                        <i class="fas fa-user-circle"></i>
                        <span>管理员</span>
                    </div>
                    <button class="logout-btn" onclick="logout()">
                        <i class="fas fa-sign-out-alt"></i>
                    </button>
                </div>
            </div>
        `;
        
        document.getElementById('sidebar-container').innerHTML = sidebarHtml;
    }
    
    bindEvents() {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const page = e.currentTarget.dataset.page;
                this.switchPage(page);
            });
        });
    }
    
    switchPage(page) {
        this.currentPage = page;
        this.updateActiveState();
        
        // 使用webview加载页面内容
        if (window.pageManager) {
            window.pageManager.loadPageInWebview(page);
        }
    }
    
    updateActiveState() {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        const activeItem = document.querySelector(`[data-page="${this.currentPage}"]`);
        if (activeItem) {
            activeItem.classList.add('active');
        }
    }
}

// 页面管理器
class PageManager {
    constructor() {
        this.webviewContainer = null;
        this.currentWebview = null;
        this.init();
    }
    
    init() {
        this.webviewContainer = document.getElementById('webview-container');
        if (!this.webviewContainer) {
            console.error('Webview container not found');
            return;
        }
    }
    
    loadPageInWebview(page) {
        // 清除当前webview
        if (this.currentWebview) {
            this.currentWebview.remove();
        }
        
        // 创建新的iframe作为webview
        const iframe = document.createElement('iframe');
        iframe.style.width = '100%';
        iframe.style.height = '100%';
        iframe.style.border = 'none';
        iframe.style.borderRadius = '10px';
        
        // 根据页面类型加载对应的HTML文件
        const pageUrls = {
            'dashboard': 'dashboard.html',
            'queue': 'modules/queue.html',
            'service': 'modules/service.html',
            'config': 'modules/config.html',
            'mcp': 'modules/mcp.html'
        };
        
        const url = pageUrls[page] || 'dashboard.html';
        iframe.src = url;
        
        // 添加加载事件监听
        iframe.onload = () => {
            console.log(`页面 ${page} 加载完成`);
        };
        
        iframe.onerror = () => {
            console.error(`页面 ${page} 加载失败`);
            this.showErrorPage(page);
        };
        
        this.webviewContainer.appendChild(iframe);
        this.currentWebview = iframe;
        
        // 更新页面标题
        this.updatePageTitle(page);
    }
    
    showErrorPage(page) {
        const errorHtml = `
            <div style="padding: 40px; text-align: center; color: #ff6b6b;">
                <i class="fas fa-exclamation-triangle" style="font-size: 3rem; margin-bottom: 20px;"></i>
                <h2>页面加载失败</h2>
                <p>无法加载 ${page} 页面，请检查文件是否存在。</p>
                <button onclick="window.pageManager.loadPageInWebview('dashboard')" 
                        style="margin-top: 20px; padding: 10px 20px; background: #00d4ff; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    返回仪表盘
                </button>
            </div>
        `;
        
        if (this.currentWebview) {
            this.currentWebview.remove();
        }
        
        const errorDiv = document.createElement('div');
        errorDiv.innerHTML = errorHtml;
        errorDiv.style.width = '100%';
        errorDiv.style.height = '100%';
        errorDiv.style.background = 'rgba(26, 26, 26, 0.8)';
        errorDiv.style.borderRadius = '10px';
        
        this.webviewContainer.appendChild(errorDiv);
        this.currentWebview = errorDiv;
    }
    
    updatePageTitle(page) {
        const titles = {
            'dashboard': '仪表盘',
            'queue': '队列管理',
            'service': '服务发现',
            'config': '配置中心',
            'mcp': 'MCP工具'
        };
        
        const titleElement = document.getElementById('page-title');
        if (titleElement) {
            titleElement.textContent = titles[page] || page;
        }
    }
}

// 全局函数
function switchModule(module) {
    if (window.sidebar) {
        window.sidebar.switchPage(module);
    }
}

function logout() {
    if (confirm('确定要退出登录吗？')) {
        localStorage.removeItem('token');
        window.location.href = '/login.html';
    }
}

window.Sidebar = Sidebar;
window.PageManager = PageManager;