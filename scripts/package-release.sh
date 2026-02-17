#!/bin/bash
# ============================================
# DB-Doctor 发布打包脚本
# ============================================
# 功能: 自动打包并生成发布包
# 使用: ./scripts/package-release.sh [版本号]
# 示例: ./scripts/package-release.sh 3.1.0
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo ""
echo "============================================"
echo "  DB-Doctor 发布打包工具"
echo "============================================"
echo ""

# 解析参数
VERSION=${1:-$(grep -oP '(?<=<version>)[^<]+' "$PROJECT_ROOT/pom.xml" | head -1)}
echo -e "${BLUE}版本号:${NC} $VERSION"
echo ""

# 1. 清理旧的构建文件
echo -e "${BLUE}[1/7]${NC} 清理旧的构建文件..."
cd "$PROJECT_ROOT"
mvn clean -q
echo -e "${GREEN}✅ 清理完成${NC}"
echo ""

# 2. 编译并打包
echo -e "${BLUE}[2/7]${NC} 编译并打包 JAR..."
mvn package -DskipTests -q
if [ ! -f "target/db-doctor-${VERSION}.jar" ]; then
    echo -e "${RED}❌ 打包失败: 未找到 JAR 文件${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 打包完成: target/db-doctor-${VERSION}.jar${NC}"
echo ""

# 3. 创建发布目录
echo -e "${BLUE}[3/7]${NC} 创建发布目录..."
RELEASE_DIR="DB-Doctor-v${VERSION}"
DIST_DIR="dist"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/$RELEASE_DIR"
echo -e "${GREEN}✅ 创建目录: $DIST_DIR/$RELEASE_DIR${NC}"
echo ""

# 4. 复制文件
echo -e "${BLUE}[4/7]${NC} 复制文件到发布目录..."

# 复制 JAR 文件
cp "target/db-doctor-${VERSION}.jar" "$DIST_DIR/$RELEASE_DIR/db-doctor.jar"
echo -e "${GREEN}  ✅ db-doctor.jar${NC}"

# 生成 SHA256 校验文件
cd "$DIST_DIR/$RELEASE_DIR"
sha256sum db-doctor.jar > db-doctor.jar.sha256
echo -e "${GREEN}  ✅ db-doctor.jar.sha256${NC}"
cd - > /dev/null

# 创建配置目录
mkdir -p "$DIST_DIR/$RELEASE_DIR/config"
cp "$PROJECT_ROOT/src/main/resources/application-local.yml.template" "$DIST_DIR/$RELEASE_DIR/config/" 2>/dev/null || true
echo -e "${GREEN}  ✅ config/application-local.yml.template${NC}"

# 创建脚本目录
mkdir -p "$DIST_DIR/$RELEASE_DIR/scripts"
cp "$PROJECT_ROOT/scripts/start.sh" "$DIST_DIR/$RELEASE_DIR/scripts/" 2>/dev/null || true
cp "$PROJECT_ROOT/scripts/start.bat" "$DIST_DIR/$RELEASE_DIR/scripts/" 2>/dev/null || true
echo -e "${GREEN}  ✅ scripts/start.sh${NC}"
echo -e "${GREEN}  ✅ scripts/start.bat${NC}"

# 复制文档
mkdir -p "$DIST_DIR/$RELEASE_DIR/docs"
cp "$PROJECT_ROOT/README.md" "$DIST_DIR/$RELEASE_DIR/" 2>/dev/null || true
cp "$PROJECT_ROOT/LICENSE" "$DIST_DIR/$RELEASE_DIR/" 2>/dev/null || true
echo -e "${GREEN}  ✅ README.md${NC}"
echo ""

# 5. 创建快速开始文档
echo -e "${BLUE}[5/7]${NC} 生成快速开始文档..."
cat > "$DIST_DIR/$RELEASE_DIR/QUICKSTART.md" << 'EOF'
# DB-Doctor 快速开始指南

## 系统要求

- Java 17 或更高版本
- 操作系统: Windows / Linux / macOS
- 内存: 至少 2GB 可用内存
- 磁盘: 至少 500MB 可用空间

## 快速启动

### Windows 用户

1. 双击运行 `scripts/start.bat`
2. 等待启动完成
3. 浏览器访问: http://localhost:8080
4. 使用默认账号登录:
   - 用户名: `dbdoctor`
   - 密码: `dbdoctor`

### Linux/Mac 用户

```bash
# 1. 赋予执行权限
chmod +x scripts/*.sh

# 2. 运行启动脚本
./scripts/start.sh

# 3. 浏览器访问
open http://localhost:8080
```

## 配置说明

首次使用需要配置数据库连接信息:

1. 复制配置文件模板:
   ```bash
   cp config/application-local.yml.template config/application-local.yml
   ```

2. 编辑配置文件，填写数据库连接信息

3. 重启应用

## 常见问题

### Q: 提示"未找到 Java"
A: 请安装 Java 17 或更高版本

### Q: 端口 8080 被占用
A: 修改 `application.yml` 中的 `server.port` 配置

### Q: 如何查看日志
A: 查看 `logs/db-doctor.log` 文件

## 技术支持

- GitHub Issues: https://github.com/hanpf2391/DB-Doctor/issues
- 文档: 查看项目 README.md
EOF
echo -e "${GREEN}✅ QUICKSTART.md${NC}"
echo ""

# 6. 打包压缩文件
echo -e "${BLUE}[6/7]${NC} 创建压缩包..."
cd "$DIST_DIR"
# tar.gz for Linux/Mac
tar -czf "${RELEASE_DIR}.tar.gz" "$RELEASE_DIR"
echo -e "${GREEN}  ✅ ${RELEASE_DIR}.tar.gz${NC}"

# zip for Windows
zip -qr "${RELEASE_DIR}.zip" "$RELEASE_DIR"
echo -e "${GREEN}  ✅ ${RELEASE_DIR}.zip${NC}"
cd - > /dev/null
echo ""

# 7. 显示结果
echo -e "${BLUE}[7/7]${NC} 打包完成！"
echo ""
echo "============================================"
echo "  发布包位置"
echo "============================================"
echo ""
echo "📦 Linux/Mac: "
echo "   $DIST_DIR/${RELEASE_DIR}.tar.gz"
echo ""
echo "📦 Windows: "
echo "   $DIST_DIR/${RELEASE_DIR}.zip"
echo ""
echo "📁 目录: "
echo "   $DIST_DIR/$RELEASE_DIR/"
echo ""
echo "============================================"
echo "  文件校验"
echo "============================================"
echo ""
SHA256=$(sha256sum "$DIST_DIR/${RELEASE_DIR}.tar.gz" | awk '{print $1}')
echo "SHA256 (.tar.gz): $SHA256"
echo ""
echo "============================================"
echo "  下一步"
echo "============================================"
echo ""
echo "1. 测试发布包:"
echo "   cd $DIST_DIR/$RELEASE_DIR"
echo "   ./scripts/start.sh"
echo ""
echo "2. 上传到 GitHub Release:"
echo "   访问: https://github.com/hanpf2391/DB-Doctor/releases/new"
echo ""
echo ""
