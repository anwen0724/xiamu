# AGENTS 模板使用说明

## 1. 文档定位

- [AGENTS模板.md](C:/all/work/study/xiamu/xiamu-backend/docs/AGENTS%E6%A8%A1%E6%9D%BF.md) 是可直接复制到新仓库根目录作为 `AGENTS.md` 使用的骨架
- 本文档只说明如何使用这份骨架，不提供新的模板内容

## 2. 使用步骤

1. 将 [AGENTS模板.md](C:/all/work/study/xiamu/xiamu-backend/docs/AGENTS%E6%A8%A1%E6%9D%BF.md) 复制到新仓库根目录并命名为 `AGENTS.md`
2. 先填写 `Project Rules`，补齐项目简介、仓库结构、必读文档、命令和本地环境说明
3. 再填写 `Stack Rules`，只保留当前项目实际使用的技术栈段落
4. 将详细规范移到 `docs/` 下的专门文档，并在 `Extended References` 中补路径
5. 最后检查是否需要在子目录新增更近一级 `AGENTS.md`

## 3. 填写原则

- 根 `AGENTS.md` 只保留高频、稳定、可执行的规则
- 不在根 `AGENTS.md` 中展开长篇实现细节
- 详细规范通过文档入口继续阅读，遵循渐进式披露
- 通用规则写在 `Universal Rules`
- 技术栈通用规则写在 `Stack Rules`
- 当前仓库自己的内容只写在 `Project Rules`

## 4. 详细规范如何拆分

推荐将细则拆到 `docs/` 中，例如：

- `docs/<前端规范文档>.md`
- `docs/<后端规范文档>.md`
- `docs/<数据库或接口规范文档>.md`
- `docs/<文档维护规范>.md`

根 `AGENTS.md` 只保留一句高层规则，再提供对应文档入口。

## 5. 什么时候拆子目录 AGENTS.md

以下情况建议在子目录新增 `AGENTS.md`：

- 前后端位于同一仓库且开发规则明显不同
- 某个子系统有独立架构边界或独立工作流
- 某个目录存在稳定且明显不同于仓库根规则的约束

写法要求：

- 子目录 `AGENTS.md` 只写差异部分
- 不重复整份根规则
- 更近一级规则优先

## 6. 推荐落地顺序

1. 先落仓库根 `AGENTS.md`
2. 再补充 `docs/` 下的详细规范文档
3. 再按需要增加子目录 `AGENTS.md`
4. 随项目演进持续收口规则，但避免把根文件写成长篇百科
