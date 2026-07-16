# 软件开发新闻聚合 App — 设计文档

日期：2026-07-15
状态：已确认，待生成实现计划

## 1. 目标

一个个人使用的安卓 App，聚合并展示与软件开发相关的新闻、资讯、博客。App 只提供**标题 + 简介 + 链接**，点击后跳转到**外部浏览器**阅读全文。面向初级到中级软件开发者，内容有一定深度与实用性，既包含时新资讯，也包含"开发者应知"的经典内容。

- 仅在本人设备（vivo X100S PRO，Android 15）上使用，不上架应用商店。
- 强调可扩展性：可配置侧重不同的（软件开发相关）topic。

## 2. 已确认的关键决策

| 决策点 | 选择 |
|---|---|
| 内容源 | Hacker News API + 精选 RSS 源 |
| 推送方式 | 只做应用内 feed（无系统通知）；抓取层保留可插拔，便于后续加本地通知 |
| Topic 配置 | 预置开发 topic，可开关 + 调权重 |
| UI 框架 | Jetpack Compose + Material 3 |
| 视觉风格 | 极客·终端绿黑：纯黑底 + 磷光绿 `#33FF66` + 全等宽 JetBrains Mono（暗色单一主题，不跟随系统动态取色）。详见独立 UI 设计文档 |
| 导航结构 | 3 个底部 tab：`Feed / 经典 / 我的`；「我的」内含收藏、Topic 偏好、关于/后续拓展 |
| 阅读方式 | **默认 Chrome Custom Tabs 打开**；设置中可切换为「跳转外部浏览器」(`ACTION_VIEW`)；设备无 Custom Tabs 提供方时自动回退到外部浏览器 |
| 本地状态 | 只做「收藏」（不做已读/未读）；**收藏内容支持按主题（topic）检索/筛选** |
| RSS 解析 | 使用第三方库 `prof18/RssParser` |
| 架构 | 纯客户端、单 module、内部 data/domain/ui 分层、MVVM、手写 DI（不用 Hilt） |

> UI/视觉的完整规范见 **`.claude/2026-07-15-dev-news-app-ui-design.md`**（配色 token、字体、组件与逐屏线框）。

## 3. 架构总览

纯客户端，无后端。单 Gradle module，内部按 package 分层，保持清晰边界。

```
com.example.hackernews
├── data
│   ├── remote      // HnApiService(Retrofit), RssFetcher + RssParser 封装
│   ├── local       // Room: ArticleDao/ArticleEntity; DataStore: TopicConfigStore
│   ├── config      // 内置配置加载：assets/topics.json, assets/classics.json
│   └── repository  // FeedRepository —— 合并/去重/排序/缓存的唯一入口
├── domain
│   ├── model       // Article, Topic
│   └── usecase     // GetFeed, RefreshFeed, GetTopics, ToggleTopic, SetTopicWeight,
│                   //   ToggleBookmark, GetBookmarks(topicId?, query?), GetClassics
├── ui
│   ├── feed        // FeedScreen + FeedViewModel
│   ├── classics    // ClassicsScreen + ClassicsViewModel
│   ├── profile     // 「我的」：入口页 + 收藏 + 设置
│   │   ├── ProfileScreen + ProfileViewModel        // 我的入口（收藏/设置/关于 列表）
│   │   ├── BookmarksScreen + BookmarksViewModel     // 收藏列表 + 按 topic 检索
│   │   └── TopicSettingsScreen + TopicSettingsVM    // Topic 开关 + 权重
│   ├── nav         // NavHost / 底部导航（Feed / 经典 / 我的）
│   └── theme       // 终端绿黑主题（Color/Type/Theme）+ JetBrains Mono
└── di              // AppContainer（手写 DI 容器）
```

分层依赖方向：`ui → domain → data`。domain 定义模型与 usecase 接口，data 提供实现，ui 只依赖 domain 抽象与 ViewModel。

## 4. 技术选型

- **UI**：Jetpack Compose、Material 3、Navigation-Compose、Coil（图片，若 RSS 带缩略图）。
- **主题/字体**：自定义固定暗色配色（终端绿黑，不用 Material You 动态取色）；等宽字体 JetBrains Mono 打包进 `res/font`。详见 UI 设计文档。
- **打开链接**：`androidx.browser`（Custom Tabs），并封装一个 `LinkOpener`，按偏好在 Custom Tabs 与外部浏览器间选择、并处理回退。
- **网络**：Retrofit + OkHttp + kotlinx.serialization（Hacker News 返回 JSON）。
- **RSS 解析**：`com.prof18.rssparser:rssparser`（Kotlin 协程友好）。
- **异步**：Kotlin Coroutines + Flow。
- **持久化**：Room（文章缓存 + 收藏），DataStore(Preferences)（topic 开关与权重、阅读方式偏好）。
- **DI**：手写 `AppContainer`（在 Application 中构造，向下传递），不引入 Hilt/kapt。
- **测试**：JUnit4 + kotlinx-coroutines-test（单元）、Room instrumented 测试（DAO）。

## 5. 内容源与配置驱动（可扩展性核心）

### 5.1 topics.json（`app/src/main/assets/topics.json`）
每个 topic 一条记录，App 首次启动把默认配置读入 DataStore，之后以 DataStore 为准（用户改的开关/权重持久化）。

```json
{
  "topics": [
    {
      "id": "backend",
      "name": "后端 & 架构",
      "enabled": true,
      "weight": 1.0,
      "feeds": [
        "https://martinfowler.com/feed.atom",
        "http://highscalability.com/rss.xml"
      ],
      "keywords": ["scalability", "microservice", "database", "system design", "architecture"]
    }
  ]
}
```

预置 topic（面向初/中级开发者）：
- `后端 & 架构`
- `前端 & Web`
- `编程语言 & CS 基础`
- `AI 工程`
- `DevOps & 云`
- `职业成长 & 工程实践`

具体 feed 列表在实现阶段确定，从公认高质量的开发博客/资讯中选取（例如 Martin Fowler、High Scalability、Julia Evans、ByteByteGo、Android Developers Blog、InfoQ 等）。要求每个 URL 是有效可访问的 RSS/Atom。

### 5.2 Hacker News
- 使用官方 Firebase API：`topstories` / `beststories` 取 id 列表，再取 item 详情。
- HN item 只有 title + url（无简介）。用各 topic 的 `keywords` 对 title 做匹配：命中则打上该 topic 标签并纳入 feed；未命中任何 keyword 的 HN 条目默认丢弃（避免噪音）。
- 简介缺失的 HN 条目，简介字段留空或显示来源域名。

### 5.3 经典必读（`app/src/main/assets/classics.json`）
手工精选、长期有效的"开发者应知"文章清单（标题 + 链接 + 一句话简介 + 归属 topic）。作为独立「经典」Tab 展示，不参与时新排序。示例条目：Twelve-Factor App、Latency Numbers Every Programmer Should Know、经典工程实践文章等（具体清单实现阶段敲定）。

## 6. 数据模型

```kotlin
// domain/model
data class Article(
    val id: String,          // 稳定 id：URL 的规范化 hash
    val title: String,
    val url: String,
    val summary: String,     // RSS 简介；HN 可能为空
    val source: String,      // 来源名，如 "Martin Fowler" / "Hacker News"
    val topicIds: List<String>,
    val publishedAt: Long,   // epoch millis；缺失则取抓取时间
    val isBookmarked: Boolean,
    val score: Int? = null   // HN 分数，可空
)

data class Topic(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val weight: Float,       // 0.0–2.0，默认 1.0
    val feeds: List<String>,
    val keywords: List<String>
)
```

Room 侧 `ArticleEntity` 持久化文章缓存与 `isBookmarked`。`topicIds` 以逗号分隔字符串存储（数据量小，收藏按 topic 检索用 `LIKE` 或在内存中过滤即可，无需关联表）。收藏项即使不再出现在新抓取结果中也需保留。

## 7. 数据流

1. 进入 Feed 或下拉刷新 → `RefreshFeed` usecase → `FeedRepository.refresh()`。
2. Repository 并发抓取所有 **enabled** topic 的 RSS 源 + HN，解析为 `Article`。
3. 按规范化 URL **去重**（同一条被多源命中时合并 topicIds）。
4. 写入 Room（保留已有收藏状态，不覆盖 `isBookmarked`）。
5. UI 观察 Room 的 `Flow<List<Article>>`，按 **topic 权重 × 时效衰减 × 来源** 排序后展示。
6. 排序公式（初版，可调）：`rankScore = maxTopicWeight × recencyFactor(publishedAt)`，HN 条目额外叠加 `score` 的归一化加权。
7. 列表项 UI：标题 + 来源 + topic 标签 + 简介摘要 + 相对时间 + 收藏图标。
8. 点击条目 → 依「阅读方式」偏好打开原文：默认用 **Custom Tabs**（`androidx.browser`）打开；偏好设为「外部浏览器」或设备无 Custom Tabs 提供方时，回退到 `Intent(ACTION_VIEW, url)` 跳转外部浏览器。

## 8. 界面（底部导航 3 个 Tab）

- **Feed**：主信息流，下拉刷新，按排序展示 enabled topic 的聚合内容；每项可收藏。
- **经典**：`classics.json` 的精选清单。
- **我的（Profile）**：入口页，列出以下项，均可在此新增后续拓展：
  - **收藏**：用户收藏的文章列表。**支持按主题检索/筛选** —— 顶部一排 topic 筛选 chip（含"全部"），选中某 topic 只显示归属该 topic 的收藏；可叠加关键字文本搜索（匹配标题/来源）。
  - **Topic 偏好**：列出所有预置 topic，每个可开关、拖动权重滑块（0–2，默认 1）。改动写入 DataStore，立即影响 Feed 排序/过滤。
  - **阅读方式**：切换「Custom Tabs（默认）/ 外部浏览器」，写入 DataStore。
  - **关于**：版本、数据来源说明等（占位，便于后续拓展）。

视觉规范（终端绿黑极客风、字体、组件、逐屏线框）见独立文档 **`.claude/2026-07-15-dev-news-app-ui-design.md`**。

## 9. 错误处理

- 单个 RSS 源抓取/解析失败：记录并跳过该源，不影响其他源与整体刷新（局部失败不致命）。
- 全部源失败 / 无网络：Feed 展示缓存内容 + 顶部提示"刷新失败，显示缓存"。首次启动无缓存时显示空态与重试按钮。
- HN API 失败：同样降级，仅影响 HN 部分。
- 解析异常、字段缺失：以合理默认值兜底（如 publishedAt 缺失取抓取时间），不崩溃。
- 所有失败都要显式暴露给用户或日志，不静默吞掉。

## 10. 测试策略

- **单元测试**（JUnit + coroutines-test）：
  - Repository 的合并 / 去重（URL 规范化） / 排序逻辑。
  - RSS 解析封装对样例 feed 的解析。
  - HN keyword 匹配与打标签逻辑。
  - 局部失败降级（部分源抛异常时整体仍返回）。
- **Instrumented 测试**：Room DAO 的增删查、收藏状态保留。
- **手动验证**：关键路径（刷新、打开链接的两种方式 Custom Tabs/外部浏览器、收藏、切换 topic/权重）在 vivo X100S PRO 上真机 verify（设备已连 ADB）。

## 11. 明确不做（YAGNI / 超出本期）

- 系统通知 / FCM 推送（抓取层保留可插拔，后续可加本地通知）。
- 后端服务。
- 已读/未读追踪。
- 自建应用内 WebView 阅读器（用 Custom Tabs 打开，不自研阅读器）。
- 用户在设置页自定义添加 RSS 源（当前通过改 `topics.json` 扩展；后续可做）。
- 账号 / 云同步。

## 12. 可扩展性说明（对应 spec 要求）

- 新增 topic 或源：编辑 `assets/topics.json`。
- 新增经典文章：编辑 `assets/classics.json`。
- 新增内容源类型（如未来加某 API）：在 `data/remote` 增加 fetcher，并在 `FeedRepository` 汇入，其余层不受影响。
- 加本地定时抓取 + 通知：新增一个 WorkManager worker 调用 `RefreshFeed`，UI 与数据层不变。
