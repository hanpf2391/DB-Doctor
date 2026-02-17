#!/bin/bash
# ============================================
# DB-Doctor 启动脚本 (Linux/Mac)
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo ""
echo "============================================"
echo "  DB-Doctor MySQL 慢查询智能诊疗系统"
echo "  版本: v3.1.0"
echo "============================================"
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. 检查 Java 环境
echo -e "${BLUE}[1/5]${NC} 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 错误: 未检测到 Java 环境${NC}"
    echo ""
    echo "请安装 Java 17 或更高版本:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-17-jre"
    echo "  CentOS/RHEL:   sudo yum install java-17-openjdk"
    echo "  macOS:         brew install openjdk@17"
    echo ""
    exit 1
fi

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ 错误: Java 版本过低 (当前: $JAVA_VERSION, 需要: 17+)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java 版本:$(java -version 2>&1 | head -n 1)${NC}"
echo ""

# 2. 检查 JAR 文件
echo -e "${BLUE}[2/5]${NC} 检查程序文件..."
if [ ! -f "db-doctor.jar" ]; then
    echo -e "${RED}❌ 错误: 未找到 db-doctor.jar 文件${NC}"
    echo ""
    echo "请确保此脚本与 db-doctor.jar 在同一目录下"
    echo ""
    exit 1
fi
echo -e "${GREEN}✅ 找到程序文件: db-doctor.jar${NC}"
echo ""

# 3. 检查配置文件
echo -e "${BLUE}[3/5]${NC} 检查配置文件..."
if [ ! -f "config/application-local.yml" ]; then
    if [ -f "config/application-local.yml.template" ]; then
        echo -e "${YELLOW}⚠️  未找到配置文件，正在从模板创建...${NC}"
        cp "config/application-local.yml.template" "config/application-local.yml"
        echo -e "${GREEN}✅ 已创建配置文件: config/application-local.yml${NC}"
        echo ""
        echo -e "${YELLOW}⚠️  请根据您的环境修改配置文件中的数据库连接信息${NC}"
        echo "   配置文件位置: config/application-local.yml"
        echo ""
    else
        echo -e "${YELLOW}⚠️  警告: 未找到配置文件，将使用默认配置${NC}"
        echo "   建议创建配置文件: config/application-local.yml"
        echo ""
    fi
else
    echo -e "${GREEN}✅ 配置文件已存在${NC}"
fi
echo ""

# 4. 创建必要目录
echo -e "${BLUE}[4/5]${NC} 创建数据目录..."
mkdir -p data logs
echo -e "${GREEN}✅ 目录准备完成${NC}"
echo ""

# 5. 启动应用
echo -e "${BLUE}[5/5]${NC} 启动 DB-Doctor..."
echo ""
echo "============================================"
echo "  正在启动，请稍候..."
echo "  访问地址: http://localhost:8080"
echo "  默认账号: dbdoctor / dbdoctor"
echo "============================================"
echo ""
echo "提示: 按 Ctrl+C 可停止服务"
echo ""

# 设置 JVM 参数
JVM_OPTS="-Xms512m -Xmx1024m"

# 启动 Spring Boot 应用
java $JVM_OPTS -jar db-doctor.jar

# 如果程序异常退出
if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}❌ DB-Doctor 异常退出${NC}"
    echo ""
    echo "请查看日志文件: logs/db-doctor.log"
    echo ""
    exit 1
fi
