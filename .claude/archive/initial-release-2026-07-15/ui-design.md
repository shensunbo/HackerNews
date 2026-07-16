# UI 设计文档 — 软件开发新闻聚合 App

日期：2026-07-15
配套：`.claude/2026-07-15-dev-news-app-design.md`（总体设计）
主题方向：**极客 · 终端绿黑**（已确认）

## 1. 设计语言与原则

整体观感 = 一个"活的终端"：纯黑背景、磷光绿、全等宽字体、命令行母题（提示符 `>`、闪烁光标 `█`、ASCII 分隔线、`[tag]` 方括号标签）。

原则：
- **单一暗色主题**，不跟随系统、不使用 Material You 动态取色。
- **可读优先于炫技**：正文用"柔和绿白" `#C8FFD4` 而非纯绿，长文阅读不刺眼；纯磷光绿 `#33FF66` 只用于强调、标题、激活态。
- **信息密度偏高**但留白克制，符合开发者审美。
- 母题点到为止（光标、提示符、braille spinner），不堆砌 CRT 扫描线等过度装饰。

## 2. 配色 Token（dark-only）

| Token | 值 | 用途 |
|---|---|---|
| `bg` | `#000000` | 全局背景 |
| `surface` | `#0A0F0A` | 卡片/列表项/导航栏（微微抬升） |
| `surfaceElevated` | `#10160F` | 弹层、选中项底色 |
| `border` | `#16351C` | 分隔线、边框、chip 描边 |
| `primary` | `#33FF66` | 磷光绿：标题强调、激活态、光标、收藏★ |
| `primaryBright` | `#66FF99` | 高亮/按压态 |
| `primaryDim` | `#3FB950` | 次级绿：来源、时间、topic 标签 |
| `textPrimary` | `#C8FFD4` | 正文/列表标题（柔和绿白，主阅读色） |
| `textSecondary` | `#5FB374` | 简介摘要、辅助文字 |
| `accent` | `#FFB000` | 琥珀：HN 分数 `▲`、错误横幅强调 |
| `error` | `#FF5555` | 错误/失败 |
| `disabled` | `#2A3A2E` | 禁用态（关闭的 topic 等） |

对比度：`textPrimary`/`textSecondary` 对 `#000` 均达 WCAG AA；纯 `primary` 仅用于短文本与图标。

## 3. 字体排版（全等宽 JetBrains Mono）

字体文件打包进 `res/font/`（JetBrains Mono Regular/Medium/Bold，OFL 许可）。全 App 统一等宽。

| 角色 | 字号/字重 | 颜色 | 备注 |
|---|---|---|---|
| AppBar 标题（命令行） | 16sp / Bold | `primary` | 如 `> dev_feed --sort=hot`，尾随闪烁 `█` |
| 分区标题 | 13sp / Bold，大写、字距+ | `primaryDim` | 如 `MUST_READ` |
| 列表标题 | 16sp / Medium | `textPrimary` | 最多 2 行省略 |
| 简介摘要 | 13sp / Regular | `textSecondary` | 最多 2 行省略 |
| 元信息（来源/时间/topic） | 12sp / Regular | `primaryDim` | |
| Tag / Chip | 11sp / Medium | `primary` | `[backend]` 方括号样式 |
| 数字/分数 | 12sp / Medium | `accent` | `▲128` |

## 4. 通用组件规范

- **TerminalAppBar**：左对齐命令行式标题 + 闪烁块状光标 `█`（1s 周期）。二级页左侧 `‹` 返回。无阴影，底部一条 `border` 线。
- **ArticleRow**（Feed/经典/收藏复用）：
  ```
  [topic] · 来源名                         ← 元信息行 primaryDim
  文章标题（最多两行）                      ← textPrimary
  简介摘要 摘要 摘要……（最多两行）          ← textSecondary
  2h ago · ▲128                     ☆      ← 时间 · HN分数(accent) · 收藏星(右)
  ```
  项间用 1px `border` 分隔（而非卡片阴影）。整行可点击 → `ACTION_VIEW` 跳外部浏览器；右侧星独立点击切换收藏。
- **TopicTag**：`[id]` 方括号文本，`primary` 色，不填充。
- **BookmarkStar**：激活 `★`(primary) / 未激活 `☆`(primaryDim)。点击有短促缩放反馈。
- **TerminalBottomBar**：3 项 `FEED / 经典 / 我的`，等宽标签。激活项 `primary` + 顶部 2px 绿色指示条；非激活 `primaryDim`。背景 `surface`。
- **TopicChipRow**（收藏检索用）：水平滚动 chip，首个为 `全部`。选中 = 填充 `primary`、文字黑；未选 = `border` 描边、文字 `primaryDim`。
- **SearchField**：占位文本 `grep> …`，等宽，聚焦时下边框变 `primary`。
- **WeightSlider**：ASCII 条形观感 `▊▊▊▊▊▁▁▁` + 数值（如 `1.4`）。范围 0–2，步进 0.1。topic 关闭时置灰禁用。
- **StatusBanner**：顶部细横幅，失败用 `accent`：`! fetch failed — 显示缓存 [重试]`。
- **BrailleSpinner**：`⠋⠙⠹⠸⠼⠴⠦⠧` 循环 + 文案 `fetching feeds…`，用于刷新/加载。
- **EmptyState**：居中 `~ 还没有内容，下拉刷新 ~` / `$ no bookmarks yet`，`textSecondary`。

## 5. 逐屏线框

### 5.1 Feed
```
┌───────────────────────────────┐
│ > dev_feed --sort=hot        █ │
├───────────────────────────────┤
│ [backend] · High Scalability   │
│ Scaling to 100M users          │
│ how they sharded the db and... │
│ 2h ago · ▲128            ☆     │
│ ───────────────────────────── │
│ [ai] · Hacker News             │
│ A practical guide to RAG       │
│ ...                       ★     │
├───────────────────────────────┤
│   FEED        经典       我的   │
└───────────────────────────────┘
```
下拉刷新触发 `RefreshFeed`；顶部可显示 BrailleSpinner / StatusBanner。

### 5.2 经典
```
┌───────────────────────────────┐
│ > classics --must_read       █ │
├───────────────────────────────┤
│ [architecture]                 │
│ The Twelve-Factor App          │
│ 云原生应用的 12 条准则          │
│ 12factor.net             ☆     │
│ ───────────────────────────── │
│ [performance]                  │
│ Latency Numbers Every Progr... │
│ ...                            │
└───────────────────────────────┘
```
静态清单（`classics.json`），不参与时新排序；同样支持收藏与跳转。

### 5.3 我的（Profile 入口）
```
┌───────────────────────────────┐
│ > whoami                     █ │
├───────────────────────────────┤
│  ★  收藏               (23) ›  │
│  #  Topic 偏好               › │
│  ⇱  阅读方式     Custom Tabs › │
│  ?  关于                     › │
│                                │
│  ── 后续拓展预留区 ──           │
├───────────────────────────────┤
│   FEED        经典      [我的]  │
└───────────────────────────────┘
```

### 5.4 收藏（支持按主题检索）
```
┌───────────────────────────────┐
│ ‹ 收藏                         │
│ grep> ______________________   │
│ [全部][backend][ai][frontend]… │   ← 水平滚动 topic chip
├───────────────────────────────┤
│ [backend] · High Scalability   │
│ Scaling to 100M users     ★    │
│ ───────────────────────────── │
│ [backend] · Martin Fowler      │
│ ...                       ★    │
└───────────────────────────────┘
```
交互：选中某 topic chip → 只显示归属该 topic 的收藏；`grep>` 文本框叠加过滤（匹配标题/来源，等宽占位符呼应终端 grep）。二者可同时生效。空结果显示 `$ no match`。

### 5.6 阅读方式（我的 → 阅读方式）
一个二选一偏好：`Custom Tabs（默认）` / `外部浏览器`。终端风单选：
```
┌───────────────────────────────┐
│ ‹ 阅读方式                     │
├───────────────────────────────┤
│ (•) Custom Tabs  应用内浮层打开 │
│ ( ) 外部浏览器   跳转 Chrome 等 │
│ ─ 设备无 Custom Tabs 时自动回退 │
└───────────────────────────────┘
```
选择写入 DataStore，`LinkOpener` 据此决定打开方式。

### 5.5 Topic 偏好
```
┌───────────────────────────────┐
│ ‹ Topic 偏好                   │
├───────────────────────────────┤
│ 后端 & 架构             [ on ] │
│ weight ▊▊▊▊▊▊▁▁  1.4           │
│ ───────────────────────────── │
│ 前端 & Web              [off]  │
│ weight ░░░░░░░░  --（禁用）    │
│ ───────────────────────────── │
│ AI 工程                 [ on ] │
│ weight ▊▊▊▊▁▁▁▁  1.0           │
└───────────────────────────────┘
```
开关 = 终端风 `[ on ]/[off]` 文本切换（或极简 Switch 着绿色）。改动即时写入 DataStore 并影响 Feed。

## 6. 交互与动效母题

- **闪烁光标** `█`：AppBar 标题尾部，1s 周期，营造"终端就绪"。
- **收藏切换**：★/☆ 切换 + 130ms 缩放回弹。
- **刷新**：BrailleSpinner 循环；完成后 spinner 消失，若失败转 StatusBanner。
- **导航切换**：底部指示条滑动/淡入；无花哨转场。
- **列表进入**：可选轻微逐项淡入（≤120ms），克制。
- 无 CRT 扫描线、无打字机逐字等重装饰（保持流畅、省电、耐用）。

## 7. 无障碍与可读性

- 正文用 `textPrimary #C8FFD4`，长文不用纯绿；关键对比度达 AA。
- 触控目标 ≥ 48dp；星标点击区域独立且足够大。
- 正文 ≥ 13sp；支持系统字体缩放（sp 单位）。
- 颜色不作为唯一信息载体：topic 用 `[文字]`、收藏用 `★/☆` 形状区分，非仅靠颜色。

## 8. Compose 落地映射

- `ui/theme/Color.kt`：定义上述 token 为 `val`。
- `ui/theme/Type.kt`：`FontFamily(JetBrains Mono …)` + 各 `TextStyle`。
- `ui/theme/Theme.kt`：`darkColorScheme(primary=…, background=bg, surface=surface, onBackground=textPrimary, error=error…)`，`dynamicColor = false`，状态栏/导航栏着色为 `bg`。
- 复用 composable：`TerminalAppBar`、`ArticleRow`、`TopicTag`、`BookmarkStar`、`TerminalBottomBar`、`TopicChipRow`、`SearchField`、`WeightSlider`、`StatusBanner`、`BrailleSpinner`、`EmptyState`。
- 这些组件在 Feed / 经典 / 收藏 三处复用，保证观感统一。

## 9. 明确不做（UI 范围）

- 浅色主题 / 主题切换（终端风固定暗色）。
- 自建应用内 WebView 阅读器（默认用 Custom Tabs 打开，可切换为外部浏览器；不自研阅读器）。
- 复杂动效（扫描线、逐字打字机、粒子等）。
- 自定义字体切换（固定 JetBrains Mono）。
