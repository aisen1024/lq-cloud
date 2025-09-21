// 页面管理器
class PageManager {
    constructor() {
        this.currentPage = null;
        this.pages = {
            dashboard: '/pages/dashboard.html',
            queue: '/pages/queue.html',
            service: '/pages/service.html',
            config: '/pages/config.html',
            mcp: '/pages/mcp.html'
        };
    }
    
    async loadPage(pageName) {
        if (this.currentPage === pageName) return;
        
        const pageUrl = this.pages[pageName];
        if (!pageUrl) {
            console.error(`Page ${pageName} not found`);
            return;
        }
        
        try {
            const response = await fetch(pageUrl);
            const html = await response.text();
            
            const contentContainer = document.getElementById('page-content');
            contentContainer.innerHTML = html;
            
            // 执行页面特定的初始化脚本
            this.initPageScript(pageName);
            
            this.currentPage = pageName;
            
            // 更新页面标题
            this.updatePageTitle(pageName);
            
        } catch (error) {
            console.error('Failed to load page:', error);
            this.showErrorPage();
        }
    }
    
    initPageScript(pageName) {
        // 动态加载页面特定的脚本
        const scriptId = `${pageName}-script`;
        const existingScript = document.getElementById(scriptId);
        
        if (existingScript) {
            existingScript.remove();
        }
        
        const script = document.createElement('script');
        script.id = scriptId;
        script.src = `/js/pages/${pageName}.js`;
        document.head.appendChild(script);
    }
    
    updatePageTitle(pageName) {
        const titles = {
            dashboard: '仪表盘',
            queue: '队列管理',
            service: '服务发现',
            config: '配置中心',
            mcp: 'MCP工具'
        };
        
        document.title = `${titles[pageName]} - AI Cloud 管理平台`;
    }
    
    showErrorPage() {
        const contentContainer = document.getElementById('page-content');
        contentContainer.innerHTML = `
            <div class="error-page">
                <i class="fas fa-exclamation-triangle"></i>
                <h3>页面加载失败</h3>
                <p>请检查网络连接或联系管理员</p>
                <button onclick="location.reload()">重新加载</button>
            </div>
        `;
    }
}

window.PageManager = PageManager;