// 系统监控仪表板
const Dashboard = {
    // 初始化仪表板
    init() {
        this.initSystemMetrics();
        this.initAlertSystem();
        this.startRealTimeUpdates();
    },

    // 系统指标图表
    initSystemMetrics() {
        // CPU使用率图表
        this.cpuChart = new Chart(
            document.getElementById('cpuChart'),
            {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'CPU使用率 (%)',
                        data: [],
                        borderColor: '#FF6384',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 100
                        }
                    }
                }
            }
        );

        // 内存使用率图表
        this.memoryChart = new Chart(
            document.getElementById('memoryChart'),
            {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: '内存使用率 (%)',
                        data: [],
                        borderColor: '#36A2EB',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 100
                        }
                    }
                }
            }
        );
    },

    // 告警系统
    initAlertSystem() {
        this.alerts = [];
        this.alertContainer = document.getElementById('alertContainer');
    },

    // 添加告警
    addAlert(type, message, details = {}) {
        const alert = {
            id: Date.now(),
            type, // 'info', 'warning', 'error', 'success'
            message,
            details,
            timestamp: new Date(),
            acknowledged: false
        };
        
        this.alerts.unshift(alert);
        this.renderAlert(alert);
        
        // 自动清理旧告警
        if (this.alerts.length > 100) {
            this.alerts = this.alerts.slice(0, 100);
        }
    },

    // 渲染告警
    renderAlert(alert) {
        const alertElement = document.createElement('div');
        alertElement.className = `alert alert-${alert.type}`;
        alertElement.innerHTML = `
            <div class="alert-header">
                <span class="alert-type">${alert.type.toUpperCase()}</span>
                <span class="alert-time">${alert.timestamp.toLocaleTimeString()}</span>
                <button class="alert-close" onclick="Dashboard.acknowledgeAlert('${alert.id}')">&times;</button>
            </div>
            <div class="alert-message">${alert.message}</div>
        `;
        
        this.alertContainer.insertBefore(alertElement, this.alertContainer.firstChild);
    },

    // 确认告警
    acknowledgeAlert(alertId) {
        const alert = this.alerts.find(a => a.id == alertId);
        if (alert) {
            alert.acknowledged = true;
            const alertElement = document.querySelector(`[data-alert-id="${alertId}"]`);
            if (alertElement) {
                alertElement.remove();
            }
        }
    }
};