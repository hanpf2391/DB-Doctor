# ============================================
# DB-Doctor Multi-Stage Dockerfile
# ============================================

# Stage 1: 构建前端
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

# 复制前端 package.json 和源码
COPY frontend/package*.json ./
COPY frontend/ ./

# 安装依赖并构建
RUN npm ci --legacy-peer-deps
RUN npm run build

# Stage 2: 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 复制 pom.xml 和源码
COPY pom.xml .
COPY src ./src

# 从前端构建阶段复制静态资源
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# 构建 JAR（跳过测试以加快构建）
RUN mvn clean package -DskipTests -B

# Stage 3: 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache tzdata

# 设置时区
ENV TZ=Asia/Shanghai

WORKDIR /app

# 从后端构建阶段复制 JAR 文件
COPY --from=backend-builder /app/target/db-doctor-*.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data /app/logs

# 暴露端口
EXPOSE 8080

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
