# Repository Guidelines

## 项目结构与模块组织

核心后端代码位于 `src/main/java/com/anwen/xiamubackend`，按职责划分为 `controller`、`service`、`service/impl`、`mapper`、`model`、`config`、`utils`、`exception`。配置文件位于 `src/main/resources/application.yml`。测试代码位于 `src/test/java`。项目文档位于 `docs/`，数据库脚本位于 `sql/`，Whisper 相关脚本位于 `scripts/`，本地运行文件存储在 `data/`。

涉及模型调用、Prompt、AI 结果生成的能力，统一放在 `src/main/java/com/anwen/xiamubackend/ai` 包下，与传统业务 `service` 包分开维护。不要将 AI 调用逻辑直接混入 `VideoService` 这类传统业务服务中。

## 开发前必读

开始实现、修改接口或调整处理流程前，优先阅读 `docs/` 下的项目文档，尤其是：

- `docs/文档维护规范.md`
- `docs/02-MVP开发说明.md`
- `docs/03-MVP开发任务拆解.md`
- 当前正在开发模块对应的模块文档

如果代码实现与文档不一致，先确认是文档过期还是实现偏离，再继续修改。

## 实现协作约定

开始实现某个功能前，先给出本轮实现方案，至少说明以下内容：

- 本轮目标
- 改动范围
- 涉及文件
- 完成标准

在得到确认前，不直接开始大范围代码修改。

每次代码修改完成后，先向用户汇报本次改动，再进入下一步。汇报至少包含：

- 修改了哪些文件
- 实现了什么
- 影响了什么
- 是否做过验证

只有在用户确认后，再继续下一个功能或下一个阶段。

## 构建、测试与运行命令

- `.\mvnw.cmd spring-boot:run`：启动后端服务，默认地址为 `http://localhost:8456/api`
- `.\mvnw.cmd test`：运行 JUnit 5 测试
- `.\mvnw.cmd clean package`：打包生成可运行 jar
- `python scripts/whisper_openai.py --help`：检查 Whisper 脚本是否可执行

本地运行前需要准备 MySQL，并确认 `application.yml` 中数据库配置可用。涉及 Java 21 特性时，构建与运行环境应明确使用 JDK 21。

## 编码风格与命名约定

遵循现有 Java 风格：4 空格缩进，类名使用 `PascalCase`，方法和字段使用 `camelCase`，包名全小写。Controller 保持轻量，业务逻辑放在 Service，文件处理与 FFmpeg 能力放在 `utils`。优先沿用现有 Lombok 写法，例如 `@RequiredArgsConstructor`、`@Slf4j`。

AI 能力命名应尽量直观，例如 `AiSummaryService`、`AiChapterService`。传统业务服务继续使用 `VideoService`、`UserService` 这类命名。

## 文档与语言约定

与用户沟通实现方案、修改汇报、文档更新说明时，默认使用中文。新增项目文档默认使用中文，除非用户另有要求。

更新已有文档时遵循“最小改动原则”：

- 优先保持原有结构、顺序和表述风格
- 只修改本次实现真正影响到的条目
- 除非用户明确要求，不要整段重写已有内容

每次实现完成后，优先更新对应模块文档中的“进度”或“迭代记录”；如影响 MVP 范围、阶段或总体进度，再同步更新总控文档。

## 测试要求

测试基于 `spring-boot-starter-test` 和 JUnit 5。测试类命名采用 `*Tests`，路径应与生产代码包结构保持一致。新增核心功能时，至少补充对应的服务层或接口层测试，不要只依赖 `contextLoads()`。

## 提交与合并请求约定

现有提交历史主要采用版本前缀加简短说明的形式，例如 `v1.4 ...`。提交信息应保持简洁、聚焦单一改动。提交 PR 时应说明：变更内容、影响模块、配置或数据库变更、手动验证方式；接口行为变化时补充请求或响应示例。

## 安全与配置提示

不要提交真实数据库密码、API Key 或 `data/` 下生成的运行文件。`application.yml` 仅作为本地默认配置，敏感信息应通过环境变量或环境隔离方式覆盖。涉及上传、转写、摘要、章节等处理链路时，要同步说明新增的 Python、FFmpeg、Spring AI 或其他系统依赖。
