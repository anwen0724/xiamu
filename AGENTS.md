# Repository Guidelines

## 项目结构与模块组织

核心后端代码位于 `src/main/java/com/anwen/xiamubackend`，按职责划分为 `controller`、`service`、`service/impl`、`mapper`、`model`、`config`、`utils`、`exception`。配置文件位于 `src/main/resources/application.yml`。后端测试代码位于 `src/test/java`。项目文档位于 `docs/`，数据库脚本位于 `sql/`，Whisper 相关脚本位于 `scripts/`，本地运行文件存储在 `data/`。

前端工程位于 `frontend/`，当前基于 `Vue 3 + TypeScript + Vite`，主要代码位于 `frontend/src`，按 `pages`、`components`、`layouts`、`router`、`api`、`queries`、`stores`、`styles` 组织。前端构建产物位于 `frontend/dist`。

涉及模型调用、Prompt、AI 结果生成的能力，统一放在 `src/main/java/com/anwen/xiamubackend/ai` 包下，与传统业务 `service` 包分开维护。不要将 AI 调用逻辑直接混入 `VideoService` 这类传统业务服务中。

## 开发前必读

开始实现、修改接口或调整处理流程前，优先阅读 `docs/` 下的项目文档，尤其是：

- `docs/文档维护规范.md`
- 当前正在开发模块对应的模块文档

如需确认产品边界、MVP 历史范围或阶段演进，再补充参考：

- `docs/02-MVP开发说明.md`
- `docs/03-MVP开发任务拆解.md`

当前阶段判断：

- 后端已完成 MVP 定义范围内的核心闭环：注册登录、视频上传、异步处理、摘要、章节、笔记、Dashboard、失败重试与基础测试均已具备最小可用实现
- 前端已完成首页、登录、注册、Dashboard、统一视频详情页与结果页内嵌笔记的主链路
- 当前开发重点不再是“补齐 MVP 是否可用”，而是体验优化、首页与详情页收口、AI 结果质量提升、状态流稳定性和工程可维护性

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

- `.\\mvnw.cmd spring-boot:run`：启动后端服务，默认地址为 `http://localhost:8456/api`
- `.\\mvnw.cmd test`：运行后端 JUnit 5 测试
- `.\\mvnw.cmd clean package`：打包生成可运行 jar
- `cd frontend && npm run dev`：启动前端开发服务，默认地址为 `http://localhost:5173`
- `cd frontend && npm run build`：执行前端类型检查并构建生产包
- `cd frontend && npm run test:unit`：运行前端单元测试
- `python scripts/whisper_openai.py --help`：检查 Whisper 脚本是否可执行

本地运行前需要准备 MySQL，并确认 `application.yml` 中数据库配置可用。涉及 Java 21 特性时，构建与运行环境应明确使用 JDK 21。前端开发环境应使用 `frontend/package.json` 中声明的 Node 版本范围。

## 编码风格与命名约定

遵循现有 Java 风格：4 空格缩进，类名使用 `PascalCase`，方法和字段使用 `camelCase`，包名全小写。Controller 保持轻量，业务逻辑放在 Service，文件处理与 FFmpeg 能力放在 `utils`。优先沿用现有 Lombok 写法，例如 `@RequiredArgsConstructor`、`@Slf4j`。

前端遵循现有 Vue 代码组织方式：页面逻辑放在 `pages`，可复用界面片段放在 `components`，接口封装放在 `api`，服务端状态查询优先放在 `queries`。不要把页面级数据请求和复杂状态散落到多个无关组件中。

AI 能力命名应尽量直观，例如 `AiSummaryService`、`AiChapterService`。传统业务服务继续使用 `VideoService`、`UserService` 这类命名。

## 文档与语言约定

与用户沟通实现方案、修改汇报、文档更新说明时，默认使用中文。新增项目文档默认使用中文，除非用户另有要求。

更新已有文档时遵循“最小改动原则”：

- 优先保持原有结构、顺序和表述风格
- 只修改本次实现真正影响到的条目
- 除非用户明确要求，不要整段重写已有内容

每次实现完成后，优先更新对应模块文档中的“进度”或“迭代记录”；如影响 MVP 范围、当前迭代阶段或总体进度，再同步更新总控文档。

## 测试要求

后端测试基于 `spring-boot-starter-test` 和 JUnit 5。测试类命名采用 `*Tests`，路径应与生产代码包结构保持一致。新增核心功能时，至少补充对应的服务层或接口层测试，不要只依赖 `contextLoads()`。

前端改动至少应做与改动范围匹配的验证：

- 页面结构或交互调整后，至少执行一次 `cd frontend && npm run build`
- 如涉及状态管理、查询逻辑或公共组件，优先补充或更新前端单元测试
- 不要只在浏览器手工查看而完全跳过构建校验

## 提交与合并请求约定

现有提交历史主要采用版本前缀加简短说明的形式，例如 `v1.4 ...`。提交信息应保持简洁、聚焦单一改动。提交 PR 时应说明：变更内容、影响模块、配置或数据库变更、手动验证方式；接口行为变化时补充请求或响应示例。

## 安全与配置提示

不要提交真实数据库密码、API Key 或 `data/` 下生成的运行文件，也不要提交前端构建产物或本地调试缓存。`application.yml` 仅作为本地默认配置，敏感信息应通过环境变量或环境隔离方式覆盖。涉及上传、转写、摘要、章节等处理链路时，要同步说明新增的 Python、FFmpeg、Spring AI 或其他系统依赖。
