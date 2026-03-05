# 智看视频后端 - Claude Code 开发指南

**智看视频** - 长视频快速理解工具。帮助用户快速理解长视频内容，AI 智能提炼重点，实现知识沉淀与高效学习。后端提供视频处理、内容分析、笔记管理、知识库等核心功能的 API 服务。

## 项目信息

- **项目名称**: xiamu-backend（智看视频后端）
- **包名**: `com.anwen.xiamubackend`
- **主类**: `XiamuBackendApplication`
- **端口**: 8456
- **Context Path**: `/api`

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.11 | Web 框架 |
| Java | 21 | JDK 版本 |
| MySQL | - | 数据库 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| Lombok | 1.18.34 | 代码简化 |
| Hutool | 5.8.26 | 工具库 |
| Knife4j | 4.4.0 | API 文档 |

## 代码风格与规范

### 1. Lombok 注解使用

- 实体类使用 `@Data`
- 枚举类使用 `@Getter`
- 需要日志的类使用 `@Slf4j`

### 2. 统一响应格式

所有 Controller 方法统一返回 `BaseResponse<T>`：

```java
// 成功响应
return ResultUtils.success(data);

// 失败响应
return ResultUtils.error(ErrorCode.PARAMS_ERROR);
return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "具体错误信息");
```

### 3. 统一异常处理

- 使用 `ThrowUtils.throwIf()` 进行参数校验和条件判断
- 抛出业务异常使用 `new BusinessException(ErrorCode.XXX)`
- 错误码定义在 `ErrorCode` 枚举中

### 4. MyBatis-Plus 配置

- 已配置分页插件（PaginationInnerInterceptor）
- 逻辑删除字段：`isDelete`（删除值 1，未删除值 0）
- 驼峰转下划线：**关闭**（`map-underscore-to-camel-case: false`）
- SQL 日志：开启 StdOutImpl 输出

### 5. 注释规范

- 使用中文注释
- 公共方法添加 JavaDoc 注释
- 复杂业务逻辑添加行内注释

## 项目结构

```
src/main/java/com/anwen/xiamubackend/
├── common/           # 通用类
│   ├── BaseResponse.java
│   ├── DeleteRequest.java
│   ├── PageRequest.java
│   └── ResultUtils.java
├── config/           # 配置类
│   ├── CorsConfig.java
│   └── MyBatisPlusConfig.java
├── exception/        # 异常处理
│   ├── BusinessException.java
│   ├── ErrorCode.java
│   ├── GlobalExceptionHandler.java
│   └── ThrowUtils.java
├── mapper/           # MyBatis Mapper
├── model/            # 实体类（待添加）
├── service/          # Service 层（待添加）
└── controller/       # Controller 层（待添加）
```

## 重要提醒

1. **包路径更新**: 项目已从 `org.anwen` 迁移到 `com.anwen`，所有新代码使用 `com.anwen.xiamubackend` 包名

2. **Knife4j 配置**: `application.yml` 中的包扫描路径需要更新为 `com.anwen.xiamubackend.controller`

3. **数据库**: 数据库名为 `xiamu`，连接信息见 `application.yml`

4. **API 文档**: 启动后访问 `http://localhost:8456/api/doc.html` 查看 Knife4j 文档

## ⚠️ 操作约束与禁制

### 严禁行为（执行前必须请求用户确认）

| 操作 | 说明 |
|------|------|
| **删除文件** | 严禁私自删除任何项目文件，包括 Java 源文件、配置文件、资源文件等。执行删除操作前**必须**先向用户确认 |
| **删除目录** | 严禁私自删除项目目录，执行前**必须**请求用户许可 |
| **强制 git 操作** | 严禁执行 `git reset --hard`、`git push --force` 等可能造成数据丢失的操作，执行前**必须**确认 |
| **删除数据库/表** | 严禁执行 `DROP DATABASE`、`DROP TABLE` 等 SQL 命令 |
| **修改 pom.xml 依赖版本** | 未经确认不得修改依赖版本，可能导致兼容性问题 |
| **修改配置文件的敏感信息** | 未经确认不得修改数据库密码、API Key 等敏感信息 |

### 谨慎操作（建议先说明）

| 操作 | 建议 |
|------|------|
| 重构代码结构 | 先说明重构方案，获得确认后再执行 |
| 大规模代码替换 | 先说明替换范围和影响，获得确认后再执行 |
| 修改异常处理逻辑 | 可能影响错误信息的展示，先确认 |
| 修改数据库表结构 | 先说明变更内容，确认后再执行 SQL |

### 安全原则

1. **不可逆操作 = 必须确认**：任何无法轻易撤销的操作，必须先向用户说明并等待确认
2. **修改前先读取**：修改文件前先读取文件内容，确认当前状态
3. **删除前先检查**：删除文件前先用 `Glob` 或 `Read` 确认文件是否存在和内容
4. **git 操作谨慎**：推送代码、创建分支、合并代码前先确认当前分支状态

## 功能模块（参考 PRD）

- 用户模块：注册/登录、个人信息管理
- 视频处理模块：上传、URL导入、音频提取、ASR转文字、AI分析
- 内容展示模块：快速摘要、章节划分、视频播放器
- 笔记模块：创建、编辑、管理笔记
- 知识库模块：知识卡片、分类、检索

## 待完善

- 添加实体类（User、Video、Chapter、Note 等）
- 添加 Mapper 接口
- 实现 Service 和 Controller 层
- 补充数据库表结构（见 `docs/PRD.md` 第 6.3 节）
