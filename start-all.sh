#!/bin/bash

echo "========================================="
echo "启动 LingQue Cloud 完整系统"
echo "========================================="

# 启动 lq-admin 后端服务
echo ""
echo "1. 启动 lq-admin 管理服务..."
cd lq-admin
./mvnw spring-boot:run &
ADMIN_PID=$!
cd ..

# 等待后端启动
echo "等待后端服务启动..."
sleep 10

# 启动 dashboard 前端
echo ""
echo "2. 启动 dashboard 前端界面..."
cd dashboard
npm run dev &
DASHBOARD_PID=$!
cd ..

echo ""
echo "========================================="
echo "系统启动完成！"
echo "========================================="
echo "管理后端: http://localhost:8080"
echo "管理前端: http://localhost:5173"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo "========================================="

# 等待用户中断
wait
