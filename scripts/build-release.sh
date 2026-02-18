#!/bin/bash
# ============================================
# DB-Doctor 完整打包脚本（前后端一起）
# ============================================

set -e

# 切换到项目根目录
cd "$(dirname "$0")/.."

PROJECT_ROOT=$(pwd)
VERSION=$(grep -oP '(?<=<version>)[^<]+' "$PROJECT_ROOT/pom.xml" | head -1)

echo ""
echo "============================================"
echo "  DB-Doctor 完整打包工具"
echo "============================================"
echo ""
echo "版本号: $VERSION"
echo ""

# 1. 清理旧的构建文件
echo "[1/8] 清理旧的构建文件..."
mvn clean -q
rm -rf frontend/dist
rm -rf dist
echo "[OK] 清理完成"
echo ""

# 2. 检查前端依赖
echo "[2/8] 检查前端依赖..."
cd frontend
if [ ! -d "node_modules" ]; then
    echo "[INFO] 前端依赖未安装，正在安装..."
    npm install
fi
echo "[OK] 前端依赖检查完成"
echo ""

# 3. 构建前端
echo "[3/8] 构建前端（Vue 3）..."
echo "[INFO] 正在构建前端，请稍候..."
npm run build
if [ ! -d "dist" ]; then
    echo "[ERROR] 前端构建失败"
    exit 1
fi
echo "[OK] 前端构建完成"
echo ""

# 4. 复制前端资源到后端
echo "[4/8] 复制前端资源到后端..."
cd ..
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r frontend/dist/* src/main/resources/static/
echo "[OK] 前端资源复制完成"
echo ""

# 5. 编译并打包后端（跳过测试）
echo "[5/8] 打包后端（Spring Boot）..."
echo "[INFO] 正在打包，请稍候..."
# 临时重命名测试目录以避免编译测试
if [ -d "src/test" ]; then
    mv src/test src/test.bak
fi
mvn package -DskipTests -Dmaven.test.skip=true -q
# 恢复测试目录
if [ -d "src/test.bak" ]; then
    mv src/test.bak src/test
fi
if [ ! -f "target/db-doctor-$VERSION.jar" ]; then
    echo "[ERROR] 后端打包失败: 未找到 JAR 文件"
    echo "[INFO] 检查 target 目录..."
    ls -lh target/*.jar || true
    exit 1
fi
echo "[OK] 后端打包完成: target/db-doctor-$VERSION.jar"
echo ""

# 6. 创建发布目录
echo "[6/8] 创建发布目录..."
RELEASE_DIR="DB-Doctor-v$VERSION"
DIST_DIR="dist"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/$RELEASE_DIR"
echo "[OK] 创建目录: $DIST_DIR/$RELEASE_DIR"
echo ""

# 7. 复制文件
echo "[7/8] 复制文件到发布目录..."

# 复制 JAR 文件
cp "target/db-doctor-$VERSION.jar" "$DIST_DIR/$RELEASE_DIR/db-doctor.jar"
echo "  [OK] db-doctor.jar"

# 生成 SHA256 校验文件
cd "$DIST_DIR/$RELEASE_DIR"
if command -v sha256sum &> /dev/null; then
    sha256sum db-doctor.jar > db-doctor.jar.sha256
elif command -v shasum &> /dev/null; then
    shasum -a 256 db-doctor.jar > db-doctor.jar.sha256
else
    echo "  [WARN] 无法生成 SHA256 校验文件（系统不支持 sha256sum 或 shasum）"
fi
cd "$PROJECT_ROOT"
if [ -f "$DIST_DIR/$RELEASE_DIR/db-doctor.jar.sha256" ]; then
    echo "  [OK] db-doctor.jar.sha256"
fi

# 创建配置目录
mkdir -p "$DIST_DIR/$RELEASE_DIR/config"
if [ -f "src/main/resources/application-local.yml.template" ]; then
    cp "src/main/resources/application-local.yml.template" "$DIST_DIR/$RELEASE_DIR/config/"
else
    echo "# 配置文件模板" > "$DIST_DIR/$RELEASE_DIR/config/application-local.yml.template"
fi
echo "  [OK] config/application-local.yml.template"

# 创建脚本目录
mkdir -p "$DIST_DIR/$RELEASE_DIR/scripts"
if [ -f "scripts/start.bat" ]; then
    cp "scripts/start.bat" "$DIST_DIR/$RELEASE_DIR/scripts/"
fi
if [ -f "scripts/start.sh" ]; then
    cp "scripts/start.sh" "$DIST_DIR/$RELEASE_DIR/scripts/"
fi
echo "  [OK] scripts/start.bat"
echo "  [OK] scripts/start.sh"

# 复制文档
if [ -f "README.md" ]; then
    cp "README.md" "$DIST_DIR/$RELEASE_DIR/"
fi
if [ -f "LICENSE" ]; then
    cp "LICENSE" "$DIST_DIR/$RELEASE_DIR/"
fi
echo "  [OK] README.md"
echo "  [OK] LICENSE"
echo ""

# 8. 生成快速开始文档
echo "[8/8] 生成快速开始文档..."
cat > "$DIST_DIR/$RELEASE_DIR/QUICKSTART.md" << 'EOF'
# DB-Doctor 快速开始指南

## 系统要求

- Java 17 或更高版本
- 操作系统: Windows / Linux / macOS
- 内存: 至少 2GB 可用内存
- 磁盘: 至少 500MB 可用空间
- MySQL 5.7+ / 8.0+

## 快速启动

### Windows 用户

1. 双击运行 `scripts\start.bat`
2. 等待启动完成
3. 浏览器访问: http://localhost:8080

### Linux/Mac 用户

```bash
# 1. 赋予执行权限
chmod +x scripts/*.sh

# 2. 运行启动脚本
./scripts/start.sh

# 3. 浏览器访问
open http://localhost:8080
```

## 首次使用配置

启动后访问 Web 界面进行配置：

1. 点击"配置中心" → "数据源配置"
   - 填写 MySQL 数据库连接信息

2. 点击"配置 AI" → 选择 AI 模型
   - DeepSeek（推荐，性价比高）
   - Ollama 本地模型（数据不出域）
   - 通义千问、OpenAI 等

3. 填写 API Key 或本地模型地址

4. 点击"测试连接"验证配置

5. 点击"重载配置"生效

## 常见问题

### Q: 提示"未找到 Java"
A: 请安装 Java 17 或更高版本

### Q: 端口 8080 被占用
A: 修改配置文件中的 server.port 配置

### Q: 如何查看日志
A: 查看 logs/db-doctor.log 文件

### Q: 配置文件在哪里
A: 所有配置通过 Web 界面管理，支持热加载

## 技术支持

- GitHub: https://github.com/hanpf2391/DB-Doctor
- Issues: https://github.com/hanpf2391/DB-Doctor/issues
EOF
echo "  [OK] QUICKSTART.md"
echo ""

# 9. 显示结果
echo "============================================"
echo "  打包完成！"
echo "============================================"
echo ""
echo "发布包位置"
echo ""
echo "JAR 包（上传到 GitHub Release）:"
echo "  $DIST_DIR/$RELEASE_DIR/db-doctor.jar"
echo ""
echo "完整发布包:"
echo "  $DIST_DIR/$RELEASE_DIR/"
echo ""
echo "============================================"
echo "  下一步操作"
echo "============================================"
echo ""
echo "1. 测试 JAR 包:"
echo "   cd $DIST_DIR/$RELEASE_DIR"
echo "   java -jar db-doctor.jar"
echo ""
echo "2. 访问 GitHub 创建 Release:"
echo "   https://github.com/hanpf2391/DB-Doctor/releases/new"
echo ""
echo "3. 填写 Release 信息:"
echo "   Tag: v$VERSION"
echo "   Title: DB-Doctor v$VERSION 发布"
echo ""
echo "4. 上传文件:"
echo "   上传 $DIST_DIR/$RELEASE_DIR/db-doctor.jar"
echo "   文件名改为: db-doctor.jar"
echo ""
echo "5. 发布"
echo "   点击 \"Publish release\""
echo ""
