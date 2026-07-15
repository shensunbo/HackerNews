# Dev News App 执行状态

更新时间：2026-07-15 21:39 CST

分支：`main`

当前 HEAD：`af37378 feat: add reading mode and about screens`

## 结论

- Task 12–19 已实现、验证并按逻辑任务提交。
- Task 21 的正式 assets 已根据 plan review 的 P0 结论提前到 Task 13 之前完成并提交。
- Task 20 的导航生产代码已经写入工作区，但尚未通过新增的 Compose UI 仪器测试，因此尚未提交。
- Task 22 的完整真机端到端验收尚未开始。
- 当前没有执行 `git push`。

计划文件中的 checkbox 不能代表真实进度；本状态以 Git 历史、现有源码和测试结果为准。

## 已完成并提交

| 范围 | 提交 | 主要内容 |
| --- | --- | --- |
| Plan review P0/P1/P2 适用修复 | `4d542f9` | 保留 topic 配置默认值；HN nullable 安全；使用正确的 RSS parser builder；结构化远端抓取结果；全源失败可上报；补 Repository/偏好回归测试 |
| Task 12 | `dbfed85` | `LinkOpener`，Custom Tabs 包选择与外部浏览器回退 |
| Task 21（提前） | `76223bb` | `topics.json`、`classics.json`；14 个 RSS 地址中 13 个验证为 HTTP 200，超时的 Hugging Face feed 已替换为 Google AI feed |
| Task 13 | `394d9f9` | 手写 `AppContainer`、`Application` 接线、ViewModel factory；安装冷启动无崩溃 |
| Task 14 | `a12f571` | 终端风格复用组件、相对时间、ASCII 权重控件、48dp 触控目标、底栏 2dp 激活指示条 |
| Task 15 | `959b564` | Feed 页面与 ViewModel，刷新、错误横幅、收藏交互 |
| Task 16 | `bfdea3d` | Classics 页面；`FEED`/`CLASSIC` 来源隔离；Room v2 migration；无发布日期 RSS 保留首次发现时间 |
| Task 17 | `dc5b381` | Profile 入口、可搜索且可按 topic 筛选的收藏页 |
| Task 18 | `46c4b88` | Topic 启用与权重设置页面，权重范围约束 |
| Task 19 | `af37378` | 阅读方式设置与关于页面 |

Task 1–11 已在开始本轮执行前存在于 Git 历史中。每个已完成的 Task 12–19 均运行了相应 focused tests 和 `./gradlew :app:assembleDebug`；Task 13 还完成了安装及冷启动检查。

## Task 20 当前工作区

以下改动属于正在进行的 Task 20，尚未提交：

- `app/src/main/java/com/example/hackernews/ui/nav/AppNav.kt`
  - 3 个底部路由：Feed、经典、我的。
  - 4 个 Profile 子路由：收藏、Topic、阅读方式、关于。
  - 子页面隐藏底部导航并支持返回。
  - 增加可注入的 `startDestination`，用于从 Profile 路由开始做隔离测试。
- `app/src/main/java/com/example/hackernews/MainActivity.kt`
  - 从占位页面切换为 `HackerNewsTheme { AppNav() }`。
- `app/src/androidTest/java/com/example/hackernews/ui/nav/AppNavTest.kt`
  - 验证从 Profile 进入收藏页后显示 `> bookmarks`，且底部 `FEED` 不再存在。
- `app/src/main/java/com/example/hackernews/ui/components/Atoms.kt`
  - 增加 `LocalTerminalAnimationsEnabled`，允许测试使用静态光标和 spinner 帧。
- `app/src/androidTest/java/com/example/hackernews/ui/profile/ProfileScreenIdleTest.kt`
  - 用于隔离 Compose 页面与装饰动画的诊断测试。

其中动画开关和 `ProfileScreenIdleTest` 是排障期间加入的诊断性改动；在根因确认后应决定保留为可测试/减弱动态效果能力，或删除诊断代码，不能直接视为 Task 20 已完成内容。

## 当前阻塞问题：Compose UI 仪器测试不结束

### 现象

在已连接的 vivo `V2324HA`（API 35）上运行以下 focused test 时，Gradle 长时间停留在 `Tests 0/1 completed`：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.hackernews.ui.nav.AppNavTest \
  --offline
```

把测试缩小为只渲染 `ProfileScreen` 后仍可复现。最近一次诊断命令为：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.hackernews.ui.profile.ProfileScreenIdleTest \
  --offline
```

最终结果：`BUILD FAILED`，单测运行约 296.868 秒，报告中的 failure body 为空；Gradle 汇总为 `Instrumentation run failed due to Process crashed`。报告位于：

- `app/build/reports/androidTests/connected/debug/index.html`
- `app/build/outputs/androidTest-results/connected/debug/TEST-V2324HA - 15-_app-.xml`

### 已排除或弱化的假设

1. **不是 Feed 初始化网络刷新导致**：测试起始路由已改为 Profile，不会构造 Feed 页面路径，问题仍存在。
2. **不是装饰性永久动画的唯一原因**：最初怀疑 `BlinkingCursor`/`BrailleSpinner` 的永久 `LaunchedEffect` 使 Compose 无法进入 idle；加入静态动画开关后，单独的 Profile 测试仍然挂起。
3. **设备不是锁屏状态**：`dumpsys window policy` 显示屏幕已唤醒、keyguard 未显示。

### 现有证据

- 测试日志显示 `ProfileScreenIdleTest` 已开始，并注册了 Compose `EspressoLink` idling resource，随后没有正常结束记录。
- 挂起期间 `dumpsys activity` 显示 instrumentation 仍处于 active，目标进程为 `com.example.hackernews`。
- 当时 Activity 列表中没有自动出现测试使用的 `androidx.activity.ComponentActivity`。
- 手动执行 `adb shell am start -W -n com.example.hackernews/androidx.activity.ComponentActivity` 可在约 91 ms 内成功启动该 Activity。
- 因此当前更可能是测试宿主 Activity 的自动启动/生命周期同步，或 Compose/Espresso 与该 vivo 真机环境之间的同步问题；证据尚不足以断言最终根因。

### 建议的下一步排查

1. 用最小 `ActivityScenarioRule<ComponentActivity>` 测试确认问题发生在 ActivityScenario 启动，还是 Compose `setContent`/idle 等待。
2. 对照运行一个不使用 Compose 的最小 instrumented Activity 测试。
3. 为测试声明项目内专用 `TestActivity`，改用 `createAndroidComposeRule<TestActivity>()`，避免依赖库 manifest 中的通用 `ComponentActivity` 声明。
4. 若专用 Activity 仍挂起，收集 ActivityTaskManager/ActivityScenario 的系统日志，并在 emulator 或另一台设备上交叉验证，以区分代码问题与 vivo 系统策略/测试栈兼容问题。
5. 根因解决后重新运行 `AppNavTest`，再执行 Task 20 的 `assembleDebug`、安装、目视导航验证与提交。

## 其他未提交改动

以下文件在 Task 20 开发期间已存在并持续保留，未纳入之前各 Task 提交：

- `.claude/2026-07-15-dev-news-app-plan.md`
- `app/build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`

内容主要是 AGP 9 说明、packaging excludes、移除未使用的 Material extended icons，以及 Gradle 缓存/内存配置。这些改动未被回退，也尚未判定应与哪个逻辑任务一起提交；后续提交 Task 20 时必须继续避免误带，除非先明确确认其归属。

## 剩余工作

1. 解决或明确隔离 Compose UI 仪器测试阻塞。
2. 完成 Task 20 focused test、`assembleDebug`、装机导航目视验证，并创建单独 conventional commit。
3. 执行 Task 22：
   - 全部 JVM unit tests；
   - 全部 connected tests；
   - debug build 与安装；
   - HN/RSS 聚合、Custom Tabs/外部浏览器、收藏与搜索、Topic 开关与权重、Classics、离线缓存错误横幅的真机端到端检查。
4. 复核 staged diff，确保不包含归属未确认的工作区改动；不自动 push。
