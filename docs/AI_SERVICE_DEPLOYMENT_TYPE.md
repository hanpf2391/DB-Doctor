# AI 服务实例部署类型使用指南

## 概述

从 v3.1.0 开始，AI 服务实例支持两种部署类型：
- **本地部署（Local）**：如 Ollama、LM Studio 等本地运行的模型
- **云端API（Cloud）**：如 OpenAI、DeepSeek、Anthropic 等在线 API 服务

## 主要区别

| 特性 | 本地部署 | 云端API |
|------|---------|---------|
| API密钥 | ❌ 不需要 | ✅ 必需 |
| 网络要求 | 本地网络 | 互联网连接 |
| 响应速度 | 较快（取决于本地硬件） | 较慢（受网络影响） |
| 成本 | 免费（需硬件投入） | 按使用量付费 |
| 数据隐私 | 数据不出本地 | 数据上传到云端 |
| 超时时间 | 推荐 120 秒 | 推荐 60 秒 |
| 典型场景 | 开发测试、隐私敏感场景 | 生产环境、需要高性能模型 |

## 配置步骤

### 1. 本地部署配置（以 Ollama 为例）

**前提条件**：已安装并启动 Ollama 服务

```bash
# 安装 Ollama（如果还未安装）
# 访问 https://ollama.ai 下载安装

# 启动 Ollama 服务
ollama serve

# 拉取模型
ollama pull qwen
ollama pull llama2
```

**配置步骤**：
1. 登录 DB-Doctor 系统
2. 进入「设置」→「AI服务实例管理」
3. 点击「新增AI服务实例」
4. 填写配置信息：
   - **部署类型**：选择「本地部署」
   - **实例名称**：例如「Ollama-Qwen」
   - **AI提供商**：选择「Ollama」
   - **API地址**：`http://localhost:11434`
   - **API密钥**：（无需填写）
   - **模型名称**：`qwen` 或其他已安装的模型
   - **温度参数**：0.7
   - **最大Tokens**：4096
   - **超时时间**：120 秒
5. 点击「创建」

**验证配置**：
```bash
# 测试 Ollama 服务是否正常运行
curl http://localhost:11434/api/tags
```

### 2. 云端API配置（以 OpenAI 为例）

**前提条件**：已获取 API Key

**配置步骤**：
1. 登录 DB-Doctor 系统
2. 进入「设置」→「AI服务实例管理」
3. 点击「新增AI服务实例」
4. 填写配置信息：
   - **部署类型**：选择「云端API」
   - **实例名称**：例如「OpenAI-GPT4」
   - **AI提供商**：选择「OpenAI」
   - **API地址**：`https://api.openai.com/v1`
   - **API密钥**：`sk-proj-xxxxxxxxxxxxxxxx`
   - **模型名称**：`gpt-4-turbo-preview`
   - **温度参数**：0.7
   - **最大Tokens**：4096
   - **超时时间**：60 秒
5. 点击「创建」

**其他云端服务配置示例**：

#### DeepSeek
- **API地址**：`https://api.deepseek.com/v1`
- **模型名称**：`deepseek-coder`
- **部署类型**：云端API

#### Anthropic Claude
- **API地址**：`https://api.anthropic.com/v1`
- **模型名称**：`claude-3-opus-20240229`
- **部署类型**：云端API

## 配置模板

### 模板1：本地 Ollama（推荐用于开发环境）
```yaml
实例名称: Ollama-Qwen
部署类型: 本地部署
AI提供商: Ollama
API地址: http://localhost:11434
API密钥: （留空）
模型名称: qwen
温度参数: 0.7
最大Tokens: 4096
超时时间: 120
```

### 模板2：云端 OpenAI（推荐用于生产环境）
```yaml
实例名称: OpenAI-GPT4
部署类型: 云端API
AI提供商: OpenAI
API地址: https://api.openai.com/v1
API密钥: sk-proj-xxxxx
模型名称: gpt-4-turbo-preview
温度参数: 0.7
最大Tokens: 4096
超时时间: 60
```

### 模板3：云端 DeepSeek（高性价比）
```yaml
实例名称: DeepSeek-Coder
部署类型: 云端API
AI提供商: DeepSeek
API地址: https://api.deepseek.com/v1
API密钥: sk-xxxxx
模型名称: deepseek-coder
温度参数: 0.7
最大Tokens: 4096
超时时间: 60
```

## 常见问题

### Q1: 切换部署类型后原有配置会丢失吗？
**A**: 不会。切换部署类型时：
- 切换到本地部署：自动清空 API 密钥，调整超时时间为 120 秒
- 切换到云端API：保留所有配置，需要填写 API 密钥

### Q2: 本地部署和云端API可以同时使用吗？
**A**: 可以。系统支持创建多个实例，您可以同时配置本地和云端实例，根据需要选择使用。

### Q3: 如何验证配置是否正确？
**A**:
1. 本地部署：确保服务正常运行，访问 API 地址测试
2. 云端API：确保 API 密钥有效，网络连接正常

### Q4: 推荐使用哪种部署类型？
**A**:
- **开发/测试环境**：推荐本地部署（Ollama），成本低，响应快
- **生产环境**：推荐云端API（OpenAI GPT-4），性能强，稳定性好
- **隐私敏感场景**：必须使用本地部署，数据不出本地

### Q5: 本地部署需要什么硬件配置？
**A**: 推荐配置：
- **CPU**：4 核心以上
- **内存**：16GB 以上（运行 7B 模型）
- **GPU**：可选，有 GPU 可大幅提升推理速度
- **存储**：至少 20GB 可用空间

## 数据库迁移

如果您是从旧版本升级，需要执行数据库迁移：

```sql
-- 执行迁移脚本
source migration_add_deployment_type.sql;
```

迁移脚本会自动：
1. 添加 `deployment_type` 字段
2. 将 Ollama 实例标记为本地部署
3. 将其他实例标记为云端API

## 技术实现

### 后端实体
```java
public enum DeploymentType {
    LOCAL("local", "本地部署"),
    CLOUD("cloud", "云端API");

    private final String value;
    private final String label;
}
```

### 前端动态表单
```typescript
// 根据部署类型动态显示/隐藏字段
<el-form-item v-if="form.deploymentType === 'cloud'" label="API密钥">
  <!-- 云端API需要密钥 -->
</el-form-item>

<el-form-item v-else label="认证说明">
  <!-- 本地部署无需密钥 -->
</el-form-item>
```

## 版本历史

- **v3.1.0** (2024-02-04): 新增部署类型区分功能
- 支持「本地部署」和「云端API」两种类型
- 根据部署类型动态调整配置项和默认值

## 相关文档

- [AI服务实例管理](./AI_SERVICE_INSTANCE_MANAGEMENT.md)
- [数据库迁移指南](./DATABASE_MIGRATION.md)
- [配置管理最佳实践](./CONFIGURATION_BEST_PRACTICES.md)
