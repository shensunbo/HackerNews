# 软件开发新闻聚合 App 实施计划 Review

日期：2026-07-15

审查对象：`2026-07-15-dev-news-app-plan.md`

## 结论

计划的功能拆分和依赖顺序大体清晰，但当前版本还不能作为可逐 Task 直接执行的实现说明。实施前至少需要修复下列 P0/P1 问题，并为 Repository 编排补充自动化测试。

## P0：阻断实施

### 1. Task 13 在 assets 创建前读取配置，启动必然崩溃

- 证据：计划 L1542 在 `FeedRepository` 构造时立即执行 `configLoader.loadTopics()`；L1770 在 `Application.onCreate()` 中构造 `AppContainer`；`topics.json` 和 `classics.json` 直到 L2982-L2983 的 Task 21 才创建。
- 影响：Task 13 到 Task 20 的安装和启动验证无法通过，应用会因找不到 `topics.json` 而在启动阶段失败。
- 建议：将 Task 21 移到 Task 5 之后，或在 Task 5 同时加入最小合法 assets，再在后续任务补充正式内容。

### 2. Task 8/9 示例代码不能按原样编译

- HN：L1153-L1160 先在 `filter` 中检查 `title`/`url`，随后在另一个 lambda 中仍将 nullable 属性传给 `Article` 的非空字段。应在 `mapNotNull` 内提取 `val title = item.title ?: return@mapNotNull null` 和 `val url = item.url ?: return@mapNotNull null`。
- RSS：L1287 使用 `RssParser()`，但项目解析到的 `rssparser-android:6.0.10` 没有无参构造器。本地 AAR 检查确认应使用 `RssParserBuilder().build()`。
- 影响：Task 8/9 的构建验证会失败，后续数据层任务无法继续。

## P1：核心行为错误

### 3. 全部网络源失败时不会显示离线错误

- 证据：L1149 将 HN 列表请求异常转换为空列表；L1293 将每个 RSS 源异常转换为 `continue`；L1571 只检查顶层 `Result.isFailure`。
- 影响：两个 `fetch()` 都会以空列表成功返回，`RefreshResult.failed` 始终为 `false`，Task 22 的“刷新失败，显示缓存”验收无法通过。
- 建议：远端源返回包含成功源数、失败源数和文章列表的结构化结果；Repository 在所有实际请求均失败时设置 `failed = true`，局部失败仍缓存成功结果。

### 4. 修改 topic 的单个属性会覆盖另一个配置默认值

- 证据：L1009-L1013 在缺少某个 DataStore key 时固定补 `enabled = true`、`weight = 1.0f`；L1544-L1548 将 `TopicPref` 作为完整覆盖使用。
- 影响：例如 `backend` 默认权重为 `1.2`，只切换 enabled 后权重会被重置为 `1.0`；配置中默认关闭的 topic 只修改权重后会被意外启用。
- 建议：把覆盖字段建模为 nullable，逐字段与 assets 默认值合并；或者写入时显式使用该 topic 的真实默认值补齐另一字段。

### 5. 收藏经典文章会将其混入主 Feed

- 证据：L2319-L2326 将经典文章插入通用 `articles` 表；L1551-L1557 的 Feed 查询读取整张表，只按 enabled topic 过滤。
- 影响：经典内容会参与主 Feed，与“经典独立展示、不参与时新排序”的设计约束冲突。
- 建议：给实体增加 `origin/contentType` 并让 Feed 只查询远端资讯，或者为经典收藏使用独立存储模型。

## P2：可靠性与一致性

### 6. 缺少发布日期的 RSS 文章会在每次刷新后重新变成最新

- 证据：L1305 在 `pubDate` 解析失败时使用本次 `nowMillis`，刷新 upsert 时会覆盖旧值。
- 影响：无日期的旧文章可能长期占据排序前列。
- 建议：优先使用 item/channel 日期；均缺失时保留数据库中的首次发现时间，不在后续刷新中更新。

### 7. 测试计划没有覆盖关键编排逻辑

- `FeedRepository.refresh()`、全源失败、偏好默认值合并、经典内容隔离均无自动化测试。
- L1175 和 L1419 引用了不存在的 Task 23；当前计划实际只有 22 个 Task。
- 建议：为远端源注入 fake，为 Repository 增加成功、局部失败、全部失败、书签保留和经典隔离测试；为偏好合并增加非 1.0 默认权重及默认关闭 topic 的测试。
- Instrumented test 的单类过滤应使用 instrumentation runner 参数，避免把 JVM `Test` task 的 `--tests` 选项直接用于 `connectedDebugAndroidTest`。

### 8. UI 实现没有完全落实 UI 设计文档

- L2041-L2058 的底部导航只有文字颜色变化，没有 UI 设计要求的顶部 2dp 激活指示条。
- L2133-L2147 使用普通 Material Slider，没有实现设计文档约定的 ASCII 条形观感。
- 建议：在组件任务中明确加入激活指示条，并决定保留 ASCII 权重控件还是同步修改 UI 设计文档，避免两份规范冲突。

## 建议修订顺序

1. 前移 assets 任务并修正 HN/RSS 编译接口。
2. 重构远端抓取结果和刷新失败判定，补 Repository 测试。
3. 修正 topic 偏好逐字段覆盖与经典内容存储隔离。
4. 明确 RSS 首次发现时间策略，并补齐 UI 组件设计差异。
5. 修正不存在的 Task 23 引用和 instrumented test 命令。

## 验证说明

- 本次 review 未修改原实施计划或业务代码。
- Gradle 验证在受限沙箱中因 Gradle 文件锁/网络接口初始化失败而未能运行。
- RSS Parser 构造器结论已通过本机缓存的 `rssparser-android:6.0.10` AAR 使用 `javap` 验证。
