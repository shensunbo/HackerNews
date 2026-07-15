# CLAUDE.md

个人用软件开发新闻聚合 Android App（不上架，仅在 vivo X100S PRO / Android 15 使用）。

## 项目文档（改代码前先读）

- 总体设计：`.claude/2026-07-15-dev-news-app-design.md`
- UI 设计（终端绿黑规范）：`.claude/2026-07-15-dev-news-app-ui-design.md`
- 实施计划（22 个 TDD task）：`.claude/2026-07-15-dev-news-app-plan.md`
- 原始需求：`.claude/spec.md`

实施时用 `superpowers:executing-plans` 或 `superpowers:subagent-driven-development` 按 plan 逐 task 执行。

## 是什么

聚合 Hacker News + 精选 RSS 的软件开发资讯，按 topic 权重排序，只给标题+简介+链接，点击跳外部阅读。面向初/中级开发者。纯客户端，无后端、无推送、无账号。

## 架构

- 单 module，package 分层 `data / domain / ui`，依赖方向 `ui → domain → data`。
- MVVM + Repository + Coroutines/Flow；手写 DI（`di/AppContainer`，**不用 Hilt**）。
- `FeedRepository` 是面向 UI 的唯一领域入口（不单列 usecase 层，YAGNI）。
- Room 缓存文章+收藏；DataStore 存 topic 开关/权重、阅读方式。
- 内容源/topic/经典清单由 `app/src/main/assets/*.json` 配置驱动——扩展即改 JSON。

## 关键约定

- 包名 `com.example.hackernews`；minSdk 35 / targetSdk 36 / compileSdk 36；JDK 17；Kotlin 官方风格。
- UI 全 Jetpack Compose + Material 3；**固定暗色终端绿黑主题，`dynamicColor = false`，无浅色主题**。
- 全等宽 JetBrains Mono；正文色 `#C8FFD4`，磷光绿 `#33FF66` 仅用于强调/激活/图标（配色 token 见 UI 文档）。
- 阅读方式默认 Custom Tabs，可切外部浏览器，无 provider 自动回退 `ACTION_VIEW`。
- 只做收藏（不做已读/未读）；收藏支持按 topic 检索。
- 文章 `id` = 规范化 URL 的 SHA-256 前 16 hex；按 URL 去重合并 topicIds。
- 局部失败不致命：单个源抓取/解析失败静默跳过，不影响整体刷新。

## 开发流程

- TDD：先写失败测试再实现；DRY / YAGNI；每个 task 独立提交。
- 提交信息用英文 conventional commits（`feat:` / `test:` / `chore:` …）。
- 提交遵循 iMotion 规范时另见全局约定；本项目默认简洁 conventional commits。

## 常用命令

```bash
./gradlew :app:assembleDebug                 # 构建
./gradlew :app:testDebugUnitTest             # 单元测试
./gradlew :app:connectedDebugAndroidTest     # instrumented 测试（需连设备）
./gradlew :app:installDebug                  # 装到设备
adb shell am start -n com.example.hackernews/.MainActivity
```

## 环境提醒

- 删除文件用 `trash`（Ubuntu：`trash-put`），**不要用 `rm`**（见全局 CLAUDE.md）。
- 设备已通过 ADB 连接（`adb devices` 可见），可直接装机验证。
