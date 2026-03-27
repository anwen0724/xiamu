# Repository Guidelines

## 1. Scope
- 本文件提供仓库级高优先级协作约束。
- 详细规范采用渐进式披露，不在本文件中展开长篇说明；遇到具体技术栈、模块或流程时，先根据本文档提供的入口继续阅读对应文档。
- 若子目录未来新增更近一级 `AGENTS.md`，优先遵循更近一级规则。

## 2. Universal Rules

### 2.1 Collaboration Workflow
- 开始实现前，先给出本轮实现方案。
- 方案至少包含：本轮目标、改动范围、涉及文件、完成标准。
- 在得到确认前，不直接开始大范围代码修改。

### 2.2 Change Control
- 优先小步修改，避免一次性大范围重写。
- 有参考图、参考站或参考实现时，先抽取硬约束，再开始实现。
- 已被明确否定的设计点、方案或方向，不得在后续轮次擅自加回。

### 2.3 Reporting
- 每次代码修改完成后，先向用户汇报本次改动，再进入下一步。
- 汇报至少包含：修改了哪些文件、实现了什么、影响了什么、是否做过验证。
- 只有在用户确认后，再继续下一个功能或下一个阶段。

### 2.4 Verification
- 修改后至少做与改动范围匹配的验证。
- 评审不只看代码，还要看真实运行结果。
- 如项目已接入构建、测试、lint、type-check，应按改动范围执行相应校验。

### 2.5 Documentation
- 文档更新遵循最小改动原则。
- 优先更新对应模块文档，再视情况更新总控文档。
- 小规模重构、命名调整或连续微调，不单独新增零散文档。

### 2.6 Safety
- 不覆盖用户未明确要求回退的已有修改。
- 不执行未获确认的高风险或破坏性操作。
- 不提交真实数据库密码、API Key、令牌或本地运行产物。

### 2.7 Definition of Done
- 代码实现完成。
- 与改动范围匹配的验证完成。
- 修改结果已汇报。
- 相关文档已按需要同步。

## 3. Stack Rules

### 3.1 Frontend
- 前端基于 `Vue 3 + TypeScript + Vite`。
- 页面、组件、布局、状态、接口分层明确：页面逻辑在 `pages`，可复用界面放 `components`，布局放 `layouts`，接口封装放 `api`，服务端状态查询优先放 `queries`。
- 页面级改版先确认布局骨架和硬约束；大改按“小 patch”推进，先骨架，再关键区域，再样式收口，最后验证。
- 页面评审以真实页面为准，不只看代码；登录后页面优先在真实登录态下验证。
- 页面文案默认收短，宁少勿多；高频页面优先“少文字、强结构”。
- 详细规范见：
  - `docs/前端协作约定.md`
  - `docs/Vue前端项目开发约束.md`

### 3.2 Backend
- 后端基于 `Java + Spring Boot + Spring AI`。
- Controller 保持轻量，业务逻辑放在 Service，文件处理与 FFmpeg 能力放在 `utils`。
- 涉及模型调用、Prompt、AI 结果生成的能力，统一放在 `src/main/java/com/anwen/xiamubackend/ai` 包下，不要直接混入 `VideoService` 这类传统业务服务中。
- 遵循现有 Java 风格：4 空格缩进，类名 `PascalCase`，方法和字段 `camelCase`，包名全小写，优先沿用现有 Lombok 写法。

### 3.3 Data / API
- 数据结构、接口行为、状态流转发生变化时，需同步文档。
- 数据库变更、接口变更、契约变更需明确记录。
- 涉及上传、转写、摘要、章节等处理链路时，要同步说明新增的 Python、FFmpeg、Spring AI 或其他系统依赖。

### 3.4 Testing
- 后端新增核心功能时，至少补充对应的服务层或接口层测试，不要只依赖 `contextLoads()`。
- 前端页面结构或交互调整后，至少执行一次 `cd frontend && npm run build`。
- 如前端改动涉及状态管理、查询逻辑或公共组件，优先补充或更新前端单元测试。
- 不要只在浏览器手工查看而完全跳过构建校验。

### 3.5 Scripts / Tooling
- Whisper 相关脚本位于 `scripts/`，运行前需确认 Python 环境和依赖可用。
- 前端开发环境应使用 `frontend/package.json` 中声明的 Node 版本范围。
- 涉及 Java 21 特性时，构建与运行环境应明确使用 JDK 21。

## 4. Project Rules

### 4.1 Summary
- 项目为音视频学习处理平台，后端负责上传、转写、摘要、章节、笔记与状态流，前端负责首页、认证、Dashboard 与统一视频详情页等主链路。
- 当前阶段不再是补齐 MVP 是否可用，而是体验优化、首页与详情页收口、AI 结果质量提升、状态流稳定性、主动学习能力试点和工程可维护性。
- 如果代码实现与文档不一致，先确认是文档过期还是实现偏离，再继续修改。

### 4.2 Repository Map
- 后端核心代码：`src/main/java/com/anwen/xiamubackend`
- 后端配置：`src/main/resources/application.yml`
- 后端测试：`src/test/java`
- 前端工程：`frontend/`
- 项目文档：`docs/`
- 数据库脚本：`sql/`
- Whisper 脚本：`scripts/`
- 本地运行文件：`data/`

### 4.3 Read Before Work
- 开始实现、修改接口或调整处理流程前，优先阅读：
  - `docs/00-PRD.md`
  - `docs/文档维护规范.md`
  - 当前正在开发模块对应的模块文档
- 如需确认产品边界、MVP 历史范围或阶段演进，再补充阅读：
  - `docs/01-实现前决策清单.md`
  - `docs/02-MVP开发说明.md`
  - `docs/03-MVP开发任务拆解.md`
- 如需确认当前版本、下一阶段与中长期规划，再补充阅读：
  - `docs/08-产品路线与版本分层规划.md`
  - `docs/09-主动学习能力规划.md`
  - `docs/10-商业化与企业版方向草案.md`

### 4.4 Current Priorities
- 首页与统一视频详情页继续收口。
- AI 结果质量提升，尤其是摘要、章节与长视频处理质量。
- 状态流、SSE 推送与整体工程可维护性继续稳定。

### 4.5 Boundaries
- AI 能力与传统业务服务分开维护，不混写。
- 前端页面级数据请求和复杂状态不要散落到多个无关组件中。
- 前端详情页、Dashboard 等页面收口时，优先遵循已确认的前端协作约定和真实页面验证流程。

### 4.6 Commands
- 后端启动：`.\\mvnw.cmd spring-boot:run`
- 后端测试：`.\\mvnw.cmd test`
- 后端打包：`.\\mvnw.cmd clean package`
- 前端开发：`cd frontend && npm run dev`
- 前端构建：`cd frontend && npm run build`
- 前端单测：`cd frontend && npm run test:unit`
- Whisper 脚本检查：`python scripts/whisper_openai.py --help`

### 4.7 Local Notes
- 本地运行前需要准备 MySQL，并确认 `application.yml` 中数据库配置可用。
- `application.yml` 仅作为本地默认配置，敏感信息应通过环境变量或环境隔离方式覆盖。
- 不要提交前端构建产物、本地调试缓存或 `data/` 下的运行文件。

## 5. Extended References
- 前端协作：`docs/前端协作约定.md`
- Vue 前端规范：`docs/Vue前端项目开发约束.md`
- 文档维护：`docs/文档维护规范.md`
- 前端模块文档：`docs/07-前端模块开发文档.md`
- 视频处理模块文档：`docs/04-视频处理模块开发文档.md`
- 项目总控与规划：
  - `docs/00-PRD.md`
  - `docs/02-MVP开发说明.md`
  - `docs/03-MVP开发任务拆解.md`
  - `docs/08-产品路线与版本分层规划.md`

## 6. Subtree Overrides
- 当前仓库暂以根 `AGENTS.md` 作为统一规则入口。
- 若未来 `frontend/`、`scripts/` 或其他子系统形成明显不同的稳定规则，再在对应子目录新增 `AGENTS.md`。
- 子目录规则只写差异部分，避免重复整份根规则。
