// 仪表盘页面逻辑
class DashboardPage {
    constructor() {
        this.init();
    }
    
    async init() {
        try {
            await this.loadOverviewData();
            this.initCharts();
        } catch (error) {
            console.error('仪表盘初始化失败:', error);
        }
    }
    
    async loadOverviewData() {
        try {
            const data = await apiClient.getOverview();
            
            document.getElementById('queue-count').textContent = data.queueCount || 0;
            document.getElementById('service-count').textContent = data.serviceCount || 0;
            document.getElementById('config-count').textContent = data.configCount || 0;
            document.getElementById('tool-count').textContent = data.toolCount || 0;
            
        } catch (error) {
            console.error('加载概览数据失败:', error);
        }
    }
    
    initCharts() {
        // 这里可以集成 Chart.js 或其他图表库
        this.initPerformanceChart();
        this.initServiceStatusChart();
    }
    
    initPerformanceChart() {
        const container = document.getElementById('performance-chart');
        container.innerHTML = `
            <div style="display: flex; align-items: center; justify-content: center; height: 200px; color: var(--text-secondary);">
                <i class="fas fa-chart-line" style="font-size: 48px; margin-right: 15px;"></i>
                <span>性能图表加载中...</span>
            </div>
        `;
    }
    
    initServiceStatusChart() {
        const container = document.getElementById('service-status-chart');
        container.innerHTML = `
            <div style="display: flex; align-items: center; justify-content: center; height: 200px; color: var(--text-secondary);">
                <i class="fas fa-chart-pie" style="font-size: 48px; margin-right: 15px;"></i>
                <span>状态图表加载中...</span>
            </div>
        `;
    }
}

// 页面加载时初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new DashboardPage();
    });
} else {
    new DashboardPage();
}