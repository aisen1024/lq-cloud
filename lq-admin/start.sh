#!/bin/bash

echo "========================================="
echo "启动 LingQue Admin 管理服务"
echo "========================================="

# 检查 Java 版本
java -version

echo ""
echo "正在启动服务..."
echo ""

# 启动 Spring Boot 应用
./mvnw spring-boot:run
