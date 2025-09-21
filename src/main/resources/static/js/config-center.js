// 配置中心管理功能
const ConfigCenter = {
    // 配置编辑器
    initConfigEditor() {
        this.editor = ace.edit('configEditor');
        this.editor.setTheme('ace/theme/github');
        this.editor.session.setMode('ace/mode/yaml');
        
        // 配置语法高亮和自动完成
        this.editor.setOptions({
            enableBasicAutocompletion: true,
            enableSnippets: true,
            enableLiveAutocompletion: true
        });
    },

    // 配置版本管理
    async getConfigHistory(configId) {
        try {
            const response = await axios.get(`/api/console/configs/${configId}/history`);
            if (response.data.success) {
                this.displayConfigHistory(response.data.data);
            }
        } catch (error) {
            console.error('获取配置历史失败:', error);
        }
    },

    // 配置差异对比
    compareConfigs(config1, config2) {
        const diffEditor = ace.edit('configDiffEditor');
        diffEditor.setTheme('ace/theme/github');
        
        // 使用 ace-diff 插件显示配置差异
        const aceDiff = new AceDiff({
            element: '#configDiffEditor',
            left: {
                content: config1,
                mode: 'ace/mode/yaml'
            },
            right: {
                content: config2,
                mode: 'ace/mode/yaml'
            }
        });
    },

    // 配置验证
    async validateConfig(configData) {
        try {
            const response = await axios.post('/api/console/configs/validate', {
                config: configData
            });
            return response.data;
        } catch (error) {
            console.error('配置验证失败:', error);
            return { success: false, message: error.message };
        }
    }
};