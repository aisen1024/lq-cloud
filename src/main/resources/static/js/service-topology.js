// 服务拓扑图可视化
const ServiceTopology = {
    // 初始化服务拓扑图
    initTopologyGraph() {
        const container = document.getElementById('serviceTopology');
        const data = {
            nodes: new vis.DataSet([]),
            edges: new vis.DataSet([])
        };
        
        const options = {
            nodes: {
                shape: 'dot',
                size: 30,
                font: {
                    size: 12,
                    color: '#000000'
                },
                borderWidth: 2
            },
            edges: {
                width: 2,
                color: { inherit: 'from' },
                smooth: {
                    type: 'continuous'
                }
            },
            physics: {
                stabilization: { iterations: 100 }
            }
        };
        
        this.network = new vis.Network(container, data, options);
        this.loadServiceTopology();
    },

    // 加载服务拓扑数据
    async loadServiceTopology() {
        try {
            const response = await axios.get('/api/console/services');
            if (response.data.success) {
                this.updateTopologyData(response.data.data);
            }
        } catch (error) {
            console.error('加载服务拓扑失败:', error);
        }
    },

    // 更新拓扑数据
    updateTopologyData(services) {
        const nodes = services.map(service => ({
            id: service.instanceId,
            label: service.serviceName,
            color: this.getServiceColor(service.status),
            title: `${service.serviceName}\n${service.host}:${service.port}\n状态: ${service.status}`
        }));
        
        this.network.setData({ nodes, edges: [] });
    },

    // 获取服务状态颜色
    getServiceColor(status) {
        const colors = {
            'up': '#4CAF50',
            'down': '#F44336',
            'starting': '#FF9800',
            'stopping': '#9E9E9E'
        };
        return colors[status] || '#9E9E9E';
    }
};