// 队列管理增强功能
const QueueManagement = {
    // 实时队列监控
    startRealTimeMonitoring() {
        this.monitoringInterval = setInterval(async () => {
            try {
                const response = await axios.get('/api/console/queues');
                if (response.data.success) {
                    this.updateQueueData(response.data.data);
                }
            } catch (error) {
                console.error('队列监控更新失败:', error);
            }
        }, 5000); // 每5秒更新一次
    },

    // 队列性能图表
    initQueueCharts() {
        const chartOptions = {
            responsive: true,
            plugins: {
                title: {
                    display: true,
                    text: '队列性能监控'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        };
        
        // 消息处理速率图表
        this.messageRateChart = new Chart(
            document.getElementById('messageRateChart'),
            {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: '消息处理速率',
                        data: [],
                        borderColor: 'rgb(75, 192, 192)',
                        tension: 0.1
                    }]
                },
                options: chartOptions
            }
        );
    },

    // 批量队列操作
    async batchQueueOperation(operation, queueIds) {
        const results = [];
        for (const queueId of queueIds) {
            try {
                const response = await axios.post(`/api/console/queues/${queueId}/${operation}`);
                results.push({ queueId, success: response.data.success });
            } catch (error) {
                results.push({ queueId, success: false, error: error.message });
            }
        }
        return results;
    }
};