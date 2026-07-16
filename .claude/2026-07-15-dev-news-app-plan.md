# 软件开发新闻聚合 App 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个纯客户端安卓 App，聚合 Hacker News + 精选 RSS 的软件开发资讯，按 topic 权重排序展示，支持收藏与外部/Custom Tabs 阅读，终端绿黑极客风。

**Architecture:** 单 Gradle module，内部按 `data / domain / ui` package 分层，MVVM + Repository + Coroutines/Flow。手写 DI 容器（不用 Hilt）。Room 缓存文章与收藏，DataStore 存 topic 偏好与阅读方式。UI 用 Jetpack Compose + Material 3，固定终端绿黑暗色主题。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation-Compose、Retrofit + OkHttp + kotlinx.serialization、prof18/RssParser、Room、DataStore(Preferences)、androidx.browser(Custom Tabs)、Coil、Coroutines/Flow、JUnit4 + kotlinx-coroutines-test + Robolectric/Room instrumented。

配套设计文档：
- 总体设计：`.claude/2026-07-15-dev-news-app-design.md`
- UI 设计：`.claude/2026-07-15-dev-news-app-ui-design.md`

## Global Constraints

- 包名 / namespace：`com.example.hackernews`（沿用现有 scaffold）。
- minSdk = 35，targetSdk = 36，compileSdk = 36，AGP 9.1.1，Kotlin 官方代码风格。
- 单一暗色主题；`dynamicColor = false`；不做浅色主题。
- 阅读方式默认 Custom Tabs，可切外部浏览器，无 provider 时自动回退到 `ACTION_VIEW`。
- 只做收藏，不做已读/未读。收藏支持按 topic 检索。
- 无后端、无 FCM、无系统通知（抓取层保留可插拔）。
- 内容源、topic、经典清单全部由 `assets/*.json` 配置驱动，改配置即可扩展。
- 每个 topic 的 keyword 用于给 HN 条目打标签/过滤；未命中任何 enabled topic keyword 的 HN 条目丢弃。
- 文章 `id` = 规范化 URL 的稳定 hash；按 URL 去重。
- 局部失败不致命：单个源抓取/解析失败跳过，不影响整体刷新。
- 正文色 `#C8FFD4`，磷光绿 `#33FF66` 只用于强调/激活/图标（见 UI 文档配色 token）。
- TDD：每个 task 先写失败测试再实现；频繁提交；DRY / YAGNI。
- 提交信息用英文 conventional commits（如 `feat:`, `test:`, `chore:`）。

---

## 文件结构总览（决定分解边界）

```
app/src/main/java/com/example/hackernews/
├── HackerNewsApp.kt                      // Application，构造 AppContainer
├── MainActivity.kt                       // 单 Activity，承载 Compose NavHost
├── di/
│   └── AppContainer.kt                   // 手写 DI：构造 DB/网络/repository/usecase
├── data/
│   ├── config/
│   │   ├── ConfigModels.kt               // TopicConfig/ClassicItem 的 @Serializable DTO
│   │   └── AssetConfigLoader.kt          // 读取 assets/topics.json、classics.json
│   ├── remote/
│   │   ├── HnApiService.kt               // Retrofit 接口
│   │   ├── HnModels.kt                   // HnItem DTO
│   │   ├── HnRemoteSource.kt             // 拉 top/best + 详情 + keyword 打标签
│   │   ├── RssRemoteSource.kt            // 用 RssParser 抓取解析 RSS → Article
│   │   └── ArticleMapper.kt             // DTO → domain Article、URL 规范化/hash
│   ├── local/
│   │   ├── ArticleEntity.kt              // Room 实体
│   │   ├── ArticleDao.kt                 // 查询：feed 流、收藏、按 topic 过滤、写入
│   │   ├── AppDatabase.kt                // RoomDatabase
│   │   └── PreferencesStore.kt           // DataStore：topic 开关/权重、阅读方式
│   └── repository/
│       ├── FeedRepository.kt             // 合并/去重/排序/缓存的唯一入口
│       └── FeedRanker.kt                 // 纯函数排序逻辑（可单测）
├── domain/
│   ├── model/
│   │   ├── Article.kt
│   │   ├── Topic.kt
│   │   └── ReadingMode.kt
│   └── usecase/                          // 薄封装，转调 repository
│       ├── GetFeed.kt / RefreshFeed.kt
│       ├── GetTopics.kt / ToggleTopic.kt / SetTopicWeight.kt
│       ├── ToggleBookmark.kt / GetBookmarks.kt
│       ├── GetClassics.kt
│       └── ReadingModeUseCases.kt        // Get/SetReadingMode
├── ui/
│   ├── theme/  Color.kt / Type.kt / Theme.kt
│   ├── components/                       // 复用 composable（见 UI 文档 §4）
│   │   ├── TerminalAppBar.kt / TerminalBottomBar.kt
│   │   ├── ArticleRow.kt / TopicTag.kt / BookmarkStar.kt
│   │   ├── TopicChipRow.kt / SearchField.kt / WeightSlider.kt
│   │   └── StatusBanner.kt / BrailleSpinner.kt / EmptyState.kt
│   ├── util/LinkOpener.kt                // Custom Tabs / 外部浏览器
│   ├── nav/AppNav.kt                     // NavHost + 底部导航（Feed/经典/我的）
│   ├── feed/       FeedScreen.kt / FeedViewModel.kt
│   ├── classics/   ClassicsScreen.kt / ClassicsViewModel.kt
│   └── profile/
│       ├── ProfileScreen.kt
│       ├── bookmarks/ BookmarksScreen.kt / BookmarksViewModel.kt
│       ├── topics/    TopicSettingsScreen.kt / TopicSettingsViewModel.kt
│       └── reading/   ReadingModeScreen.kt / ReadingModeViewModel.kt
└── (assets) app/src/main/assets/topics.json, classics.json
```

依赖方向：`ui → domain → data`。domain 定义模型；data 实现；ui 只依赖 domain 与 ViewModel。

---

## 任务清单（共 22 个，按依赖顺序，每个 TDD + 独立提交）

### Task 1: Gradle 依赖与启用 Compose

搭建整个工程的构建地基：Compose / serialization 插件、KSP、以及全部依赖到版本目录。这是纯配置任务，"测试"= 依赖同步 + 空构建成功。

> **AGP 9 内置 Kotlin**：本工程用 AGP 9.1.1 自带的 Kotlin 支持，**不引入独立 `kotlin-android` 插件**；`gradle.properties` 保留 `android.disallowKotlinSourceSets=false` 让 KSP 注册生成源。构建加速项（缓存/配置缓存/并行、堆）已在 `gradle.properties` 配好，本 task 只关注依赖与插件。

**Files:**
- Modify: `gradle/libs.versions.toml`（补充 versions/libraries/plugins）
- Modify: `app/build.gradle.kts`（加插件、Compose、依赖、packaging）
- Modify: `build.gradle.kts`（根，声明插件 apply false）

**Interfaces:**
- Produces: 可用的 Compose 构建环境、Room KSP、以下依赖别名供后续 task 引用。

- [ ] **Step 1: 在 `gradle/libs.versions.toml` 的 `[versions]` 补充**

```toml
kotlin = "2.2.10"
ksp = "2.2.10-2.0.2"
coreKtx = "1.18.0"
lifecycle = "2.9.0"
activityCompose = "1.11.0"
composeBom = "2025.09.00"
navigationCompose = "2.9.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"
coroutines = "1.9.0"
rssparser = "6.0.10"
room = "2.7.0"
datastore = "1.1.1"
browser = "1.8.0"
coil = "2.7.0"
coroutinesTest = "1.9.0"
```

- [ ] **Step 2: 在 `[libraries]` 补充**

```toml
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
rssparser = { group = "com.prof18.rssparser", name = "rssparser", version.ref = "rssparser" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
```

- [ ] **Step 3: 在 `[plugins]` 补充**

```toml
# 注意：AGP 9 内置 Kotlin，不声明 kotlin-android 插件
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4: 根 `build.gradle.kts` 声明插件**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 5: 重写 `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.hackernews"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hackernews"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/*.version",
            )
        }
    }
}
// 注：AGP 9 内置 Kotlin，jvmTarget 随 compileOptions 对齐 17；
// 若构建告警 jvmTarget 不一致，再按 AGP 9 内置 Kotlin DSL 显式设为 17（勿用独立 kotlin-android 插件）。

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.rssparser)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
```

- [ ] **Step 6: 同步并空构建**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL（此时还没业务代码，仅验证依赖解析与插件配置）。若报 AGP/Kotlin/Compose 版本兼容错误，按报错把 `kotlin`/`ksp`/`composeBom` 提升到与 AGP 9.1.1 匹配的稳定版后重试。

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts gradle.properties
git commit -m "chore: add Compose, Room, Retrofit, RSS and KSP dependencies"
```

---

### Task 2: 终端绿黑主题 + App/Activity 骨架

落地 UI 文档 §2/§3/§8 的配色 token、JetBrains Mono 字体与暗色 Material3 主题，并让 App 能起一个空 Compose 屏。视觉类难以纯单测，用 `@Preview` + 真机目视验证。

**Files:**
- Create: `app/src/main/res/font/`（放入 `jetbrains_mono_regular.ttf`、`jetbrains_mono_medium.ttf`、`jetbrains_mono_bold.ttf`，OFL 许可）
- Create: `app/src/main/java/com/example/hackernews/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/hackernews/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/hackernews/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/hackernews/HackerNewsApp.kt`
- Create: `app/src/main/java/com/example/hackernews/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`（`android:name`、Activity、INTERNET 权限）

**Interfaces:**
- Produces: `TerminalColors`（object，暴露 `Bg/Surface/SurfaceElevated/Border/Primary/PrimaryBright/PrimaryDim/TextPrimary/TextSecondary/Accent/Error/Disabled`）、`AppTypography`、`@Composable HackerNewsTheme(content)`。

- [ ] **Step 1: `Color.kt` — 配色 token（对应 UI 文档 §2）**

```kotlin
package com.example.hackernews.ui.theme

import androidx.compose.ui.graphics.Color

object TerminalColors {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF0A0F0A)
    val SurfaceElevated = Color(0xFF10160F)
    val Border = Color(0xFF16351C)
    val Primary = Color(0xFF33FF66)
    val PrimaryBright = Color(0xFF66FF99)
    val PrimaryDim = Color(0xFF3FB950)
    val TextPrimary = Color(0xFFC8FFD4)
    val TextSecondary = Color(0xFF5FB374)
    val Accent = Color(0xFFFFB000)
    val Error = Color(0xFFFF5555)
    val Disabled = Color(0xFF2A3A2E)
}
```

- [ ] **Step 2: `Type.kt` — JetBrains Mono 排版（对应 UI 文档 §3）**

```kotlin
package com.example.hackernews.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hackernews.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    bodyMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
```

- [ ] **Step 3: `Theme.kt` — 固定暗色 scheme（`dynamicColor = false`）**

```kotlin
package com.example.hackernews.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalScheme = darkColorScheme(
    primary = TerminalColors.Primary,
    onPrimary = TerminalColors.Bg,
    background = TerminalColors.Bg,
    onBackground = TerminalColors.TextPrimary,
    surface = TerminalColors.Surface,
    onSurface = TerminalColors.TextPrimary,
    surfaceVariant = TerminalColors.SurfaceElevated,
    outline = TerminalColors.Border,
    error = TerminalColors.Error,
)

@Composable
fun HackerNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TerminalScheme, typography = AppTypography, content = content)
}
```

- [ ] **Step 4: `HackerNewsApp.kt`（Application，先留空容器占位，Task 13 填充）**

```kotlin
package com.example.hackernews

import android.app.Application

class HackerNewsApp : Application()
```

- [ ] **Step 5: `MainActivity.kt`（先渲染占位屏，验证主题）**

```kotlin
package com.example.hackernews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.hackernews.ui.theme.HackerNewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HackerNewsTheme { Placeholder() } }
    }
}

@Composable
private fun Placeholder() {
    Scaffold(Modifier.fillMaxSize()) { p -> Text("> hackernews ready █", Modifier.padding(p).padding(16.dp)) }
}
```
（注：补 `import androidx.compose.ui.unit.dp`。）

- [ ] **Step 6: `AndroidManifest.xml` — 权限、Application、Activity**

在 `<manifest>` 内、`<application>` 前加：
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
`<application android:name=".HackerNewsApp" ...>`，并确保 `MainActivity` 为 LAUNCHER。

- [ ] **Step 7: 构建 + 装机目视**

Run: `./gradlew :app:installDebug && adb shell am start -n com.example.hackernews/.MainActivity`
Expected: 手机上黑底绿字显示 `> hackernews ready █`，字体为等宽。

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/hackernews app/src/main/res/font app/src/main/AndroidManifest.xml
git commit -m "feat: add terminal green theme, JetBrains Mono and app skeleton"
```

---

### Task 3: domain 模型

定义纯 Kotlin 领域模型，被所有层引用。无逻辑，折叠进后续 task 的编译验证即可，单独成 task 便于锁定类型契约。

**Files:**
- Create: `domain/model/Article.kt`, `domain/model/Topic.kt`, `domain/model/ReadingMode.kt`

**Interfaces:**
- Produces: `Article`, `Topic`, `ReadingMode`（供全工程使用，字段与总体设计 §6 一致）。

- [ ] **Step 1: 写模型**

```kotlin
// domain/model/Article.kt
package com.example.hackernews.domain.model

data class Article(
    val id: String,              // 规范化 URL 的稳定 hash
    val title: String,
    val url: String,
    val summary: String,         // RSS 简介；HN 可能为空
    val source: String,          // 来源名
    val topicIds: List<String>,
    val publishedAt: Long,       // epoch millis
    val isBookmarked: Boolean = false,
    val score: Int? = null,      // HN 分数
)
```
```kotlin
// domain/model/Topic.kt
package com.example.hackernews.domain.model

data class Topic(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val weight: Float,           // 0.0–2.0，默认 1.0
    val feeds: List<String>,
    val keywords: List<String>,
)
```
```kotlin
// domain/model/ReadingMode.kt
package com.example.hackernews.domain.model

enum class ReadingMode { CUSTOM_TABS, EXTERNAL_BROWSER }
```

- [ ] **Step 2: 编译**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/hackernews/domain/model
git commit -m "feat: add Article, Topic and ReadingMode domain models"
```

---

### Task 4: URL 规范化与文章 id（纯函数，TDD）

去重的正确性取决于 URL 规范化与稳定 id。纯函数，最适合 TDD。

**Files:**
- Create: `data/remote/ArticleMapper.kt`
- Test: `app/src/test/java/com/example/hackernews/data/remote/ArticleMapperTest.kt`

**Interfaces:**
- Produces:
  - `fun normalizeUrl(url: String): String` — 小写 scheme/host、去 `#fragment`、去追踪参数（`utm_*`、`ref`、`fbclid`）、去末尾 `/`。
  - `fun articleIdFor(url: String): String` — 对 `normalizeUrl` 结果做 SHA-256 取前 16 hex。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArticleMapperTest {
    @Test fun normalize_stripsFragmentTrailingSlashAndTracking() {
        assertEquals(
            "https://ex.com/post",
            normalizeUrl("HTTPS://Ex.com/post/?utm_source=hn&ref=x#top")
        )
    }
    @Test fun normalize_keepsMeaningfulQuery() {
        assertEquals("https://ex.com/p?id=42", normalizeUrl("https://ex.com/p?id=42"))
    }
    @Test fun id_isStableAcrossEquivalentUrls() {
        assertEquals(articleIdFor("https://ex.com/a#x"), articleIdFor("https://ex.com/a/"))
    }
    @Test fun id_differsForDifferentUrls() {
        assertNotEquals(articleIdFor("https://ex.com/a"), articleIdFor("https://ex.com/b"))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ArticleMapperTest*"`
Expected: FAIL（未解析引用 `normalizeUrl`/`articleIdFor`）。

- [ ] **Step 3: 实现**

```kotlin
package com.example.hackernews.data.remote

import java.security.MessageDigest

private val TRACKING_PARAMS = setOf("ref", "fbclid", "gclid")

fun normalizeUrl(url: String): String {
    val trimmed = url.trim()
    val hashIdx = trimmed.indexOf('#')
    val noFragment = if (hashIdx >= 0) trimmed.substring(0, hashIdx) else trimmed
    val qIdx = noFragment.indexOf('?')
    val base = if (qIdx >= 0) noFragment.substring(0, qIdx) else noFragment
    val query = if (qIdx >= 0) noFragment.substring(qIdx + 1) else ""

    val schemeSplit = base.split("://", limit = 2)
    val normalizedBase = if (schemeSplit.size == 2) {
        val rest = schemeSplit[1]
        val slash = rest.indexOf('/')
        val host = (if (slash >= 0) rest.substring(0, slash) else rest).lowercase()
        val path = if (slash >= 0) rest.substring(slash) else ""
        "${schemeSplit[0].lowercase()}://$host$path"
    } else base
    val trimmedBase = normalizedBase.trimEnd('/')

    val keptParams = query.split('&')
        .filter { it.isNotEmpty() }
        .filter { p ->
            val k = p.substringBefore('=')
            k.isNotEmpty() && !k.startsWith("utm_") && k !in TRACKING_PARAMS
        }
    return if (keptParams.isEmpty()) trimmedBase else "$trimmedBase?${keptParams.joinToString("&")}"
}

fun articleIdFor(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(normalizeUrl(url).toByteArray())
    return digest.take(8).joinToString("") { "%02x".format(it) }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ArticleMapperTest*"`
Expected: PASS（4/4）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/remote/ArticleMapper.kt app/src/test/java/com/example/hackernews/data/remote/ArticleMapperTest.kt
git commit -m "feat: add URL normalization and stable article id"
```

---

### Task 5: 配置模型 + assets 加载器（TDD）

配置驱动是可扩展性的核心。解析 `topics.json`/`classics.json` 是纯逻辑，用 JVM 单测（把 JSON 作为字符串喂入，assets 读取单独薄封装）。

**Files:**
- Create: `data/config/ConfigModels.kt`
- Create: `data/config/AssetConfigLoader.kt`
- Test: `app/src/test/java/com/example/hackernews/data/config/ConfigParsingTest.kt`

**Interfaces:**
- Consumes: `Topic`（Task 3）。
- Produces:
  - `@Serializable class TopicsConfig(val topics: List<TopicDto>)`；`TopicDto(id,name,enabled,weight,feeds,keywords)`。
  - `@Serializable class ClassicItem(val title,url,summary,topicId)`；`@Serializable class ClassicsConfig(val items: List<ClassicItem>)`。
  - `fun parseTopics(json: String): List<Topic>`、`fun parseClassics(json: String): List<ClassicItem>`（用共享 `Json { ignoreUnknownKeys = true }`）。
  - `class AssetConfigLoader(context)`，方法 `loadTopics(): List<Topic>`、`loadClassics(): List<ClassicItem>`（读 `assets/*.json` 后转调上面纯函数）。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParsingTest {
    @Test fun parsesTopicsWithDefaults() {
        val json = """
          {"topics":[{"id":"backend","name":"后端 & 架构","enabled":true,"weight":1.0,
          "feeds":["https://martinfowler.com/feed.atom"],"keywords":["database","architecture"]}]}
        """.trimIndent()
        val topics = parseTopics(json)
        assertEquals(1, topics.size)
        assertEquals("backend", topics[0].id)
        assertEquals(1.0f, topics[0].weight)
        assertTrue(topics[0].enabled)
        assertEquals(listOf("database", "architecture"), topics[0].keywords)
    }

    @Test fun parsesClassics() {
        val json = """
          {"items":[{"title":"The Twelve-Factor App","url":"https://12factor.net",
          "summary":"云原生 12 准则","topicId":"backend"}]}
        """.trimIndent()
        val items = parseClassics(json)
        assertEquals(1, items.size)
        assertEquals("https://12factor.net", items[0].url)
        assertEquals("backend", items[0].topicId)
    }

    @Test fun ignoresUnknownKeys() {
        val json = """{"topics":[{"id":"x","name":"X","enabled":false,"weight":0.5,
          "feeds":[],"keywords":[],"future":"ignored"}],"meta":"ignored"}"""
        assertEquals("x", parseTopics(json)[0].id)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConfigParsingTest*"`
Expected: FAIL（未解析 `parseTopics`/`parseClassics`）。

- [ ] **Step 3: 实现 `ConfigModels.kt`**

```kotlin
package com.example.hackernews.data.config

import com.example.hackernews.domain.model.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TopicDto(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val weight: Float = 1.0f,
    val feeds: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

@Serializable data class TopicsConfig(val topics: List<TopicDto> = emptyList())

@Serializable
data class ClassicItem(
    val title: String,
    val url: String,
    val summary: String = "",
    val topicId: String = "",
)

@Serializable data class ClassicsConfig(val items: List<ClassicItem> = emptyList())

private val configJson = Json { ignoreUnknownKeys = true }

fun parseTopics(json: String): List<Topic> =
    configJson.decodeFromString<TopicsConfig>(json).topics.map {
        Topic(it.id, it.name, it.enabled, it.weight, it.feeds, it.keywords)
    }

fun parseClassics(json: String): List<ClassicItem> =
    configJson.decodeFromString<ClassicsConfig>(json).items
```

- [ ] **Step 4: 实现 `AssetConfigLoader.kt`（读 assets 的薄封装）**

```kotlin
package com.example.hackernews.data.config

import android.content.Context
import com.example.hackernews.domain.model.Topic

class AssetConfigLoader(private val context: Context) {
    fun loadTopics(): List<Topic> = parseTopics(readAsset("topics.json"))
    fun loadClassics(): List<ClassicItem> = parseClassics(readAsset("classics.json"))
    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ConfigParsingTest*"`
Expected: PASS（3/3）。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/config app/src/test/java/com/example/hackernews/data/config
git commit -m "feat: add config models and asset config loader"
```

---

### Task 6: Room 层（Entity + Dao + Database）

文章缓存与收藏。`topicIds` 存逗号分隔字符串（数据量小）。收藏刷新时不被覆盖是关键不变量，用 instrumented DAO 测试固定。

**Files:**
- Create: `data/local/ArticleEntity.kt`, `data/local/ArticleDao.kt`, `data/local/AppDatabase.kt`
- Test: `app/src/androidTest/java/com/example/hackernews/data/local/ArticleDaoTest.kt`

**Interfaces:**
- Consumes: —
- Produces:
  - `ArticleEntity(id PK, title, url, summary, source, topicIds:String, publishedAt, isBookmarked, score:Int?)`。
  - `ArticleDao`：
    - `fun feedStream(): Flow<List<ArticleEntity>>`（按 `publishedAt DESC`，排序细调在 Ranker）
    - `fun bookmarksStream(): Flow<List<ArticleEntity>>`（`WHERE isBookmarked = 1`）
    - `suspend fun upsertPreservingBookmark(items: List<ArticleEntity>)`（新数据不覆盖已有 `isBookmarked`）
    - `suspend fun setBookmarked(id: String, value: Boolean)`
    - `suspend fun getById(id: String): ArticleEntity?`
  - `AppDatabase.articleDao()`；`AppDatabase.build(context): AppDatabase`。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ArticleDao

    private fun entity(id: String, bm: Boolean = false) =
        ArticleEntity(id, "t-$id", "https://e.com/$id", "s", "src", "backend", 1000L, bm, null)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        dao = db.articleDao()
    }
    @After fun close() = db.close()

    @Test fun upsertThenBookmark_preservedOnReUpsert() = runBlocking {
        dao.upsertPreservingBookmark(listOf(entity("a")))
        dao.setBookmarked("a", true)
        dao.upsertPreservingBookmark(listOf(entity("a")))          // 重新抓取同一条
        assertTrue(dao.getById("a")!!.isBookmarked)                 // 收藏未被覆盖
    }
    @Test fun bookmarksStream_onlyBookmarked() = runBlocking {
        dao.upsertPreservingBookmark(listOf(entity("a"), entity("b")))
        dao.setBookmarked("b", true)
        val list = dao.bookmarksStream().first()
        assertEquals(1, list.size)
        assertEquals("b", list[0].id)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*ArticleDaoTest*"`（需设备已连）
Expected: FAIL / 编译错误（类未定义）。

- [ ] **Step 3: 实现 `ArticleEntity.kt`**

```kotlin
package com.example.hackernews.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val summary: String,
    val source: String,
    val topicIds: String,        // 逗号分隔
    val publishedAt: Long,
    val isBookmarked: Boolean,
    val score: Int?,
)
```

- [ ] **Step 4: 实现 `ArticleDao.kt`**

```kotlin
package com.example.hackernews.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun feedStream(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun bookmarksStream(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getById(id: String): ArticleEntity?

    @Query("UPDATE articles SET isBookmarked = :value WHERE id = :id")
    suspend fun setBookmarked(id: String, value: Boolean)

    @Query("SELECT isBookmarked FROM articles WHERE id = :id")
    suspend fun bookmarkFlag(id: String): Boolean?

    @Query("""INSERT OR REPLACE INTO articles
        (id,title,url,summary,source,topicIds,publishedAt,isBookmarked,score)
        VALUES (:id,:title,:url,:summary,:source,:topicIds,:publishedAt,:isBookmarked,:score)""")
    suspend fun insertRaw(
        id: String, title: String, url: String, summary: String, source: String,
        topicIds: String, publishedAt: Long, isBookmarked: Boolean, score: Int?,
    )

    @Transaction
    suspend fun upsertPreservingBookmark(items: List<ArticleEntity>) {
        for (e in items) {
            val kept = bookmarkFlag(e.id) ?: false
            insertRaw(e.id, e.title, e.url, e.summary, e.source, e.topicIds,
                e.publishedAt, kept, e.score)
        }
    }
}
```

- [ ] **Step 5: 实现 `AppDatabase.kt`**

```kotlin
package com.example.hackernews.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "hackernews.db").build()
    }
}
```

- [ ] **Step 6: 跑测试确认通过**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*ArticleDaoTest*"`
Expected: PASS（2/2）。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/local app/src/androidTest/java/com/example/hackernews/data/local
git commit -m "feat: add Room article cache with bookmark-preserving upsert"
```

---

### Task 7: DataStore 偏好（topic 开关/权重 + 阅读方式）

用户对 topic 的开关/权重覆盖与阅读方式偏好。存 DataStore(Preferences)。用 instrumented 测试（DataStore 需 Android context）。

**Files:**
- Create: `data/local/PreferencesStore.kt`
- Test: `app/src/androidTest/java/com/example/hackernews/data/local/PreferencesStoreTest.kt`

**Interfaces:**
- Consumes: `ReadingMode`（Task 3）。
- Produces:
  - `class PreferencesStore(context)`：
    - `data class TopicPref(val enabled: Boolean, val weight: Float)`
    - `fun topicPrefs(): Flow<Map<String, TopicPref>>`（topicId → 覆盖值；缺省则用配置默认）
    - `suspend fun setEnabled(topicId: String, enabled: Boolean)`
    - `suspend fun setWeight(topicId: String, weight: Float)`
    - `fun readingMode(): Flow<ReadingMode>`（默认 `CUSTOM_TABS`）
    - `suspend fun setReadingMode(mode: ReadingMode)`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesStoreTest {
    private val store = PreferencesStore(ApplicationProvider.getApplicationContext())

    @Test fun readingMode_defaultsToCustomTabs() = runBlocking {
        assertEquals(ReadingMode.CUSTOM_TABS, store.readingMode().first())
    }
    @Test fun readingMode_persists() = runBlocking {
        store.setReadingMode(ReadingMode.EXTERNAL_BROWSER)
        assertEquals(ReadingMode.EXTERNAL_BROWSER, store.readingMode().first())
    }
    @Test fun topicPref_roundTrip() = runBlocking {
        store.setEnabled("ai", false)
        store.setWeight("ai", 1.5f)
        val pref = store.topicPrefs().first()["ai"]!!
        assertEquals(false, pref.enabled)
        assertEquals(1.5f, pref.weight)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*PreferencesStoreTest*"`
Expected: FAIL（类未定义）。

- [ ] **Step 3: 实现**

```kotlin
package com.example.hackernews.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class PreferencesStore(private val context: Context) {

    data class TopicPref(val enabled: Boolean, val weight: Float)

    private val readingModeKey = stringPreferencesKey("reading_mode")
    private fun enabledKey(id: String) = booleanPreferencesKey("topic_enabled_$id")
    private fun weightKey(id: String) = floatPreferencesKey("topic_weight_$id")

    fun readingMode(): Flow<ReadingMode> = context.dataStore.data.map { p ->
        when (p[readingModeKey]) {
            ReadingMode.EXTERNAL_BROWSER.name -> ReadingMode.EXTERNAL_BROWSER
            else -> ReadingMode.CUSTOM_TABS
        }
    }
    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[readingModeKey] = mode.name }
    }

    fun topicPrefs(): Flow<Map<String, TopicPref>> = context.dataStore.data.map { p ->
        val ids = p.asMap().keys
            .mapNotNull { k ->
                when {
                    k.name.startsWith("topic_enabled_") -> k.name.removePrefix("topic_enabled_")
                    k.name.startsWith("topic_weight_") -> k.name.removePrefix("topic_weight_")
                    else -> null
                }
            }.toSet()
        ids.associateWith { id ->
            TopicPref(
                enabled = p[enabledKey(id)] ?: true,
                weight = p[weightKey(id)] ?: 1.0f,
            )
        }
    }
    suspend fun setEnabled(topicId: String, enabled: Boolean) {
        context.dataStore.edit { it[enabledKey(topicId)] = enabled }
    }
    suspend fun setWeight(topicId: String, weight: Float) {
        context.dataStore.edit { it[weightKey(topicId)] = weight }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*PreferencesStoreTest*"`
Expected: PASS（3/3）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/local/PreferencesStore.kt app/src/androidTest/java/com/example/hackernews/data/local/PreferencesStoreTest.kt
git commit -m "feat: add DataStore preferences for topics and reading mode"
```

---

### Task 8: Hacker News 源（Retrofit + keyword 打标签）

拉 HN best stories，按各 enabled topic 的 keyword 给标题打标签，未命中任何 topic 的丢弃。keyword 匹配是纯函数，用单测锁定；网络封装薄。

**Files:**
- Create: `data/remote/HnModels.kt`, `data/remote/HnApiService.kt`, `data/remote/KeywordMatcher.kt`, `data/remote/HnRemoteSource.kt`
- Test: `app/src/test/java/com/example/hackernews/data/remote/KeywordMatcherTest.kt`

**Interfaces:**
- Consumes: `Topic`（Task 3）、`articleIdFor`（Task 4）。
- Produces:
  - `fun matchTopics(text: String, topics: List<Topic>): List<String>`（命中任一 keyword 即归属该 topic；大小写不敏感；子串匹配）。
  - `HnApiService`（Retrofit）：`suspend fun bestStoryIds(): List<Long>`、`suspend fun item(id): HnItem?`。
  - `class HnRemoteSource(api, limit=60)`：`suspend fun fetch(enabledTopics: List<Topic>): List<Article>`。

- [ ] **Step 1: 写失败测试（纯函数 matchTopics）**

```kotlin
package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    private val backend = Topic("backend", "B", true, 1f, emptyList(), listOf("database", "kubernetes"))
    private val ai = Topic("ai", "A", true, 1f, emptyList(), listOf("LLM", "rag"))

    @Test fun matches_caseInsensitiveSubstring() {
        assertEquals(listOf("backend"), matchTopics("Scaling our DataBase layer", listOf(backend, ai)))
    }
    @Test fun matches_multipleTopics() {
        val r = matchTopics("Kubernetes for LLM serving", listOf(backend, ai))
        assertTrue(r.containsAll(listOf("backend", "ai")))
    }
    @Test fun matches_noneReturnsEmpty() {
        assertTrue(matchTopics("A poem about spring", listOf(backend, ai)).isEmpty())
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*KeywordMatcherTest*"`
Expected: FAIL（`matchTopics` 未定义）。

- [ ] **Step 3: 实现 `KeywordMatcher.kt`**

```kotlin
package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Topic

fun matchTopics(text: String, topics: List<Topic>): List<String> {
    val lower = text.lowercase()
    return topics.filter { t ->
        t.keywords.any { kw -> kw.isNotBlank() && lower.contains(kw.lowercase()) }
    }.map { it.id }
}
```

- [ ] **Step 4: 实现 `HnModels.kt` 与 `HnApiService.kt`**

```kotlin
// HnModels.kt
package com.example.hackernews.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class HnItem(
    val id: Long = 0,
    val title: String? = null,
    val url: String? = null,
    val score: Int? = null,
    val time: Long? = null,      // 秒
    val type: String? = null,
)
```
```kotlin
// HnApiService.kt
package com.example.hackernews.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface HnApiService {
    @GET("v0/beststories.json") suspend fun bestStoryIds(): List<Long>
    @GET("v0/item/{id}.json") suspend fun item(@Path("id") id: Long): HnItem?
}
```

- [ ] **Step 5: 实现 `HnRemoteSource.kt`**

```kotlin
package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HnRemoteSource(
    private val api: HnApiService,
    private val limit: Int = 60,
) {
    suspend fun fetch(enabledTopics: List<Topic>): List<Article> = coroutineScope {
        if (enabledTopics.isEmpty()) return@coroutineScope emptyList()
        val ids = runCatching { api.bestStoryIds() }.getOrElse { emptyList() }.take(limit)
        ids.map { id -> async { runCatching { api.item(id) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
            .filter { it.type == "story" && !it.url.isNullOrBlank() && !it.title.isNullOrBlank() }
            .mapNotNull { item ->
                val topics = matchTopics(item.title!!, enabledTopics)
                if (topics.isEmpty()) null
                else Article(
                    id = articleIdFor(item.url!!),
                    title = item.title,
                    url = item.url,
                    summary = "",
                    source = "Hacker News",
                    topicIds = topics,
                    publishedAt = (item.time ?: 0L) * 1000L,
                    score = item.score,
                )
            }
    }
}
```

- [ ] **Step 6: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*KeywordMatcherTest*"`
Expected: PASS（3/3）。（`HnRemoteSource` 的网络路径在 Task 23 真机联调验证。）

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/remote/HnModels.kt app/src/main/java/com/example/hackernews/data/remote/HnApiService.kt app/src/main/java/com/example/hackernews/data/remote/KeywordMatcher.kt app/src/main/java/com/example/hackernews/data/remote/HnRemoteSource.kt app/src/test/java/com/example/hackernews/data/remote/KeywordMatcherTest.kt
git commit -m "feat: add Hacker News source with keyword topic tagging"
```

---

### Task 9: RSS 源（日期解析 + HTML 清洗 + RssParser 抓取）

按 topic 的 feeds 抓取 RSS，每条归属该 topic。日期解析与简介 HTML 清洗是易错纯逻辑，用单测锁定；网络抓取薄封装、真机验证。

**Files:**
- Create: `data/remote/RssFormat.kt`（`parseRssDateMillis`、`stripHtml`、`hostOf`）
- Create: `data/remote/RssRemoteSource.kt`
- Test: `app/src/test/java/com/example/hackernews/data/remote/RssFormatTest.kt`

**Interfaces:**
- Consumes: `Topic`（Task 3）、`articleIdFor`（Task 4）。
- Produces:
  - `fun parseRssDateMillis(raw: String?): Long?`（支持 RFC-1123 与 ISO-8601；失败返回 null）。
  - `fun stripHtml(input: String?): String`（去标签、解常见实体、压缩空白）。
  - `fun hostOf(url: String): String`。
  - `class RssRemoteSource(parser)`：`suspend fun fetch(topics: List<Topic>, nowMillis: Long): List<Article>`。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RssFormatTest {
    @Test fun parsesRfc1123() {
        assertEquals(1727874000000L, parseRssDateMillis("Wed, 02 Oct 2024 13:00:00 GMT"))
    }
    @Test fun parsesIso8601() {
        assertEquals(1727874000000L, parseRssDateMillis("2024-10-02T13:00:00Z"))
    }
    @Test fun invalidDateReturnsNull() {
        assertNull(parseRssDateMillis("not a date"))
        assertNull(parseRssDateMillis(null))
    }
    @Test fun stripHtml_removesTagsAndEntities() {
        assertEquals("A & B link", stripHtml("<p>A &amp; B <a href='x'>link</a></p>"))
    }
    @Test fun stripHtml_blankInput() {
        assertEquals("", stripHtml(null))
    }
    @Test fun hostOf_extractsHost() {
        assertEquals("martinfowler.com", hostOf("https://martinfowler.com/feed.atom"))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*RssFormatTest*"`
Expected: FAIL（函数未定义）。

- [ ] **Step 3: 实现 `RssFormat.kt`**

```kotlin
package com.example.hackernews.data.remote

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun parseRssDateMillis(raw: String?): Long? {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return null
    return runCatching {
        ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.recoverCatching {
        OffsetDateTime.parse(s).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(s).toEpochMilli()
    }.getOrNull()
}

fun stripHtml(input: String?): String {
    if (input.isNullOrBlank()) return ""
    return input
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&#39;", "'").replace("&quot;", "\"")
        .replace(Regex("\\s+"), " ").trim()
}

fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull().orEmpty()
```

- [ ] **Step 4: 实现 `RssRemoteSource.kt`**

```kotlin
package com.example.hackernews.data.remote

import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import com.prof18.rssparser.RssParser

// 注：方法/字段名对应 RssParser 6.x（getRssChannel/items/link/description/pubDate）。
// 若依赖版本不同导致编译错误，按该版本 API 调整字段名。
class RssRemoteSource(
    private val parser: RssParser = RssParser(),
) {
    suspend fun fetch(topics: List<Topic>, nowMillis: Long): List<Article> {
        val out = mutableListOf<Article>()
        for (topic in topics) {
            for (feed in topic.feeds) {
                val channel = runCatching { parser.getRssChannel(feed) }.getOrNull() ?: continue
                val source = channel.title?.trim().takeUnless { it.isNullOrBlank() } ?: hostOf(feed)
                for (item in channel.items) {
                    val link = item.link?.trim()
                    if (link.isNullOrBlank()) continue
                    out += Article(
                        id = articleIdFor(link),
                        title = item.title?.trim().takeUnless { it.isNullOrBlank() } ?: link,
                        url = link,
                        summary = stripHtml(item.description),
                        source = source,
                        topicIds = listOf(topic.id),
                        publishedAt = parseRssDateMillis(item.pubDate) ?: nowMillis,
                        score = null,
                    )
                }
            }
        }
        return out
    }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*RssFormatTest*"`
Expected: PASS（6/6）。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/remote/RssFormat.kt app/src/main/java/com/example/hackernews/data/remote/RssRemoteSource.kt app/src/test/java/com/example/hackernews/data/remote/RssFormatTest.kt
git commit -m "feat: add RSS source with date parsing and HTML cleanup"
```

---

### Task 10: FeedRanker 排序（纯函数，TDD）

Feed 展示顺序 = topic 权重 × 时效衰减 + HN 分数微加权。纯函数，`now` 作为参数注入（可确定性测试）。

**Files:**
- Create: `data/repository/FeedRanker.kt`
- Test: `app/src/test/java/com/example/hackernews/data/repository/FeedRankerTest.kt`

**Interfaces:**
- Consumes: `Article`（Task 3）。
- Produces:
  - `fun rankScore(a: Article, topicWeights: Map<String, Float>, nowMillis: Long): Double`
  - `fun rankFeed(list: List<Article>, topicWeights: Map<String, Float>, nowMillis: Long): List<Article>`（按分数降序）。

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedRankerTest {
    private val now = 1_000_000_000_000L
    private fun art(id: String, topic: String, ageHours: Long, score: Int? = null) =
        Article(id, id, "https://e/$id", "", "src", listOf(topic),
            now - ageHours * 3_600_000L, false, score)

    @Test fun higherWeightRanksFirst_sameAge() {
        val a = art("a", "backend", 1); val b = art("b", "frontend", 1)
        val out = rankFeed(listOf(b, a), mapOf("backend" to 2.0f, "frontend" to 1.0f), now)
        assertEquals("a", out.first().id)
    }
    @Test fun newerRanksFirst_sameWeight() {
        val old = art("old", "backend", 100); val fresh = art("fresh", "backend", 1)
        val out = rankFeed(listOf(old, fresh), mapOf("backend" to 1.0f), now)
        assertEquals("fresh", out.first().id)
    }
    @Test fun hnScoreBreaksTie() {
        val hot = art("hot", "backend", 1, score = 300); val cold = art("cold", "backend", 1, score = 0)
        val out = rankFeed(listOf(cold, hot), mapOf("backend" to 1.0f), now)
        assertEquals("hot", out.first().id)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedRankerTest*"`
Expected: FAIL（函数未定义）。

- [ ] **Step 3: 实现**

```kotlin
package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import kotlin.math.min
import kotlin.math.pow

fun rankScore(a: Article, topicWeights: Map<String, Float>, nowMillis: Long): Double {
    val weight = a.topicIds.maxOfOrNull { (topicWeights[it] ?: 1.0f).toDouble() } ?: 1.0
    val ageHours = ((nowMillis - a.publishedAt).coerceAtLeast(0L)) / 3_600_000.0
    val recency = 0.5.pow(ageHours / 24.0)           // 半衰期 24 小时
    val scoreBoost = a.score?.let { min(it / 300.0, 1.0) * 0.25 } ?: 0.0
    return weight * recency + scoreBoost
}

fun rankFeed(list: List<Article>, topicWeights: Map<String, Float>, nowMillis: Long): List<Article> =
    list.sortedByDescending { rankScore(it, topicWeights, nowMillis) }
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedRankerTest*"`
Expected: PASS（3/3）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/repository/FeedRanker.kt app/src/test/java/com/example/hackernews/data/repository/FeedRankerTest.kt
git commit -m "feat: add feed ranking by topic weight, recency and HN score"
```

---

### Task 11: FeedRepository（合并/去重/缓存 + 实体映射）

汇聚配置、偏好、HN、RSS、Ranker、Room 的唯一入口。去重合并是核心正确性逻辑，抽成纯函数 `mergeArticles` 单测；仓库编排在设备联调（Task 23）验证。

**Files:**
- Create: `data/local/EntityMapping.kt`（`Article.toEntity()` / `ArticleEntity.toArticle()`）
- Create: `data/repository/ArticleMerge.kt`（`mergeArticles`）
- Create: `data/repository/FeedRepository.kt`
- Test: `app/src/test/java/com/example/hackernews/data/repository/ArticleMergeTest.kt`

**Interfaces:**
- Consumes: `ArticleDao`(T6)、`PreferencesStore`(T7)、`AssetConfigLoader`(T5)、`HnRemoteSource`(T8)、`RssRemoteSource`(T9)、`rankFeed`(T10)。
- Produces:
  - `fun mergeArticles(list: List<Article>): List<Article>`（按 id 合并：union topicIds、取非空 summary、max publishedAt、max score）。
  - `data class RefreshResult(val count: Int, val failed: Boolean)`
  - `class FeedRepository(...)`：`topicsStream(): Flow<List<Topic>>`、`feedStream(): Flow<List<Article>>`、`suspend fun refresh(): RefreshResult`、`bookmarksStream(topicId: String?, query: String): Flow<List<Article>>`、`suspend fun toggleBookmark(id: String)`。

- [ ] **Step 1: 写失败测试（mergeArticles）**

```kotlin
package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleMergeTest {
    private fun art(topics: List<String>, summary: String = "", score: Int? = null, pub: Long = 0) =
        Article("same", "T", "https://e/x", summary, "src", topics, pub, false, score)

    @Test fun mergesSameIdUnioningTopicsAndKeepingSummary() {
        val a = art(listOf("backend"), summary = "", score = 10, pub = 100)
        val b = art(listOf("ai"), summary = "详细简介", score = null, pub = 200)
        val out = mergeArticles(listOf(a, b))
        assertEquals(1, out.size)
        assertEquals(setOf("backend", "ai"), out[0].topicIds.toSet())
        assertEquals("详细简介", out[0].summary)
        assertEquals(200L, out[0].publishedAt)
        assertEquals(10, out[0].score)
    }
    @Test fun keepsDistinctIds() {
        val a = Article("a", "A", "u", "", "s", listOf("x"), 0, false, null)
        val b = Article("b", "B", "u", "", "s", listOf("x"), 0, false, null)
        assertEquals(2, mergeArticles(listOf(a, b)).size)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ArticleMergeTest*"`
Expected: FAIL（`mergeArticles` 未定义）。

- [ ] **Step 3: 实现 `ArticleMerge.kt`**

```kotlin
package com.example.hackernews.data.repository

import com.example.hackernews.domain.model.Article

fun mergeArticles(list: List<Article>): List<Article> =
    list.groupBy { it.id }.map { (_, group) ->
        group.reduce { acc, x ->
            acc.copy(
                title = acc.title.ifBlank { x.title },
                summary = acc.summary.ifBlank { x.summary },
                topicIds = (acc.topicIds + x.topicIds).distinct(),
                publishedAt = maxOf(acc.publishedAt, x.publishedAt),
                score = listOfNotNull(acc.score, x.score).maxOrNull(),
            )
        }
    }
```

- [ ] **Step 4: 实现 `EntityMapping.kt`**

```kotlin
package com.example.hackernews.data.local

import com.example.hackernews.domain.model.Article

fun Article.toEntity() = ArticleEntity(
    id = id, title = title, url = url, summary = summary, source = source,
    topicIds = topicIds.joinToString(","), publishedAt = publishedAt,
    isBookmarked = isBookmarked, score = score,
)

fun ArticleEntity.toArticle() = Article(
    id = id, title = title, url = url, summary = summary, source = source,
    topicIds = topicIds.split(",").filter { it.isNotBlank() },
    publishedAt = publishedAt, isBookmarked = isBookmarked, score = score,
)
```

- [ ] **Step 5: 实现 `FeedRepository.kt`**

```kotlin
package com.example.hackernews.data.repository

import com.example.hackernews.data.config.AssetConfigLoader
import com.example.hackernews.data.local.ArticleDao
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.local.toArticle
import com.example.hackernews.data.local.toEntity
import com.example.hackernews.data.remote.HnRemoteSource
import com.example.hackernews.data.remote.RssRemoteSource
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RefreshResult(val count: Int, val failed: Boolean)

class FeedRepository(
    private val dao: ArticleDao,
    private val prefs: PreferencesStore,
    configLoader: AssetConfigLoader,
    private val hn: HnRemoteSource,
    private val rss: RssRemoteSource,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val baseTopics: List<Topic> = configLoader.loadTopics()

    fun topicsStream(): Flow<List<Topic>> = prefs.topicPrefs().map { overrides ->
        baseTopics.map { t ->
            val o = overrides[t.id]
            t.copy(enabled = o?.enabled ?: t.enabled, weight = o?.weight ?: t.weight)
        }
    }

    fun feedStream(): Flow<List<Article>> =
        combine(dao.feedStream(), topicsStream()) { entities, topics ->
            val enabledIds = topics.filter { it.enabled }.map { it.id }.toSet()
            val weights = topics.associate { it.id to it.weight }
            val articles = entities.map { it.toArticle() }
                .filter { a -> a.topicIds.any(enabledIds::contains) }
            rankFeed(articles, weights, now())
        }

    suspend fun refresh(): RefreshResult {
        val enabled = topicsStream().first().filter { it.enabled }
        if (enabled.isEmpty()) return RefreshResult(0, failed = false)
        val nowMs = now()
        val (hnR, rssR) = coroutineScope {
            val h = async { runCatching { hn.fetch(enabled) } }
            val r = async { runCatching { rss.fetch(enabled, nowMs) } }
            h.await() to r.await()
        }
        val merged = mergeArticles(hnR.getOrElse { emptyList() } + rssR.getOrElse { emptyList() })
        dao.upsertPreservingBookmark(merged.map { it.toEntity() })
        return RefreshResult(merged.size, failed = hnR.isFailure && rssR.isFailure)
    }

    fun bookmarksStream(topicId: String?, query: String): Flow<List<Article>> =
        dao.bookmarksStream().map { list ->
            list.map { it.toArticle() }
                .filter { topicId == null || it.topicIds.contains(topicId) }
                .filter { q ->
                    query.isBlank() || q.title.contains(query, true) || q.source.contains(query, true)
                }
        }

    suspend fun toggleBookmark(id: String) {
        val current = dao.getById(id)?.isBookmarked ?: false
        dao.setBookmarked(id, !current)
    }
}
```

- [ ] **Step 6: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ArticleMergeTest*"`
Expected: PASS（2/2）。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/local/EntityMapping.kt app/src/main/java/com/example/hackernews/data/repository app/src/test/java/com/example/hackernews/data/repository/ArticleMergeTest.kt
git commit -m "feat: add FeedRepository with merge, dedup, ranking and cache"
```

---

> **分层精简说明（YAGNI）**：ViewModel 直接依赖 `FeedRepository` + `PreferencesStore`；不单列只做转调的 usecase 类。`FeedRepository` 即面向 UI 的领域 API。这是对总体设计 §3 usecase 层的有意精简。

### Task 12: LinkOpener（Custom Tabs / 外部浏览器）

按阅读方式偏好打开链接。打开决策抽成纯函数单测；实际 Intent 启动在设备验证。

**Files:**
- Create: `ui/util/LinkOpener.kt`
- Test: `app/src/test/java/com/example/hackernews/ui/util/LinkOpenerDecisionTest.kt`

**Interfaces:**
- Consumes: `ReadingMode`（Task 3）。
- Produces:
  - `fun shouldUseCustomTabs(mode: ReadingMode, providerAvailable: Boolean): Boolean`
  - `object LinkOpener { fun open(context, url: String, mode: ReadingMode) }`

- [ ] **Step 1: 写失败测试**

```kotlin
package com.example.hackernews.ui.util

import com.example.hackernews.domain.model.ReadingMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkOpenerDecisionTest {
    @Test fun customTabsWhenPreferredAndAvailable() {
        assertTrue(shouldUseCustomTabs(ReadingMode.CUSTOM_TABS, providerAvailable = true))
    }
    @Test fun fallbackToExternalWhenNoProvider() {
        assertFalse(shouldUseCustomTabs(ReadingMode.CUSTOM_TABS, providerAvailable = false))
    }
    @Test fun externalWhenPreferred() {
        assertFalse(shouldUseCustomTabs(ReadingMode.EXTERNAL_BROWSER, providerAvailable = true))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*LinkOpenerDecisionTest*"`
Expected: FAIL（函数未定义）。

- [ ] **Step 3: 实现 `LinkOpener.kt`**

```kotlin
package com.example.hackernews.ui.util

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.example.hackernews.domain.model.ReadingMode

fun shouldUseCustomTabs(mode: ReadingMode, providerAvailable: Boolean): Boolean =
    mode == ReadingMode.CUSTOM_TABS && providerAvailable

object LinkOpener {
    fun open(context: Context, url: String, mode: ReadingMode) {
        val uri = url.toUri()
        val provider = CustomTabsIntent.getMaxToolbarItems() >= 0  // CT 库存在即可用；真正无 CT 浏览器时下方 runCatching 兜底
        if (shouldUseCustomTabs(mode, provider)) {
            val opened = runCatching {
                CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
            }.isSuccess
            if (opened) return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
```
（注：CustomTabsIntent 在无 CT 能力浏览器时本身会退化为普通浏览器打开；`runCatching` 再兜底到 `ACTION_VIEW`。）

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*LinkOpenerDecisionTest*"`
Expected: PASS（3/3）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/util/LinkOpener.kt app/src/test/java/com/example/hackernews/ui/util/LinkOpenerDecisionTest.kt
git commit -m "feat: add LinkOpener with Custom Tabs and external fallback"
```

---

### Task 13: AppContainer（手写 DI）+ Application 接线 + ViewModel 工厂

构造并持有全部单例（DB/网络/repository），提供 Compose 取 ViewModel 的辅助。无独立测试，验证 = 构建 + 装机仍能启动占位屏。

**Files:**
- Create: `di/AppContainer.kt`
- Modify: `HackerNewsApp.kt`（持有 container）
- Create: `ui/util/AppViewModel.kt`（Compose ViewModel 工厂辅助）

**Interfaces:**
- Consumes: 全部 data 层类。
- Produces:
  - `class AppContainer(context)`：字段 `feedRepository: FeedRepository`、`preferencesStore: PreferencesStore`、`classics: List<ClassicItem>`。
  - `HackerNewsApp.container: AppContainer`。
  - `@Composable inline fun <reified VM> appViewModel(create: (AppContainer) -> VM): VM`。

- [ ] **Step 1: 实现 `AppContainer.kt`**

```kotlin
package com.example.hackernews.di

import android.content.Context
import com.example.hackernews.data.config.AssetConfigLoader
import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.local.AppDatabase
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.remote.HnApiService
import com.example.hackernews.data.remote.HnRemoteSource
import com.example.hackernews.data.remote.RssRemoteSource
import com.example.hackernews.data.repository.FeedRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.build(appContext)
    private val prefs = PreferencesStore(appContext)
    private val configLoader = AssetConfigLoader(appContext)

    private val json = Json { ignoreUnknownKeys = true }
    private val okHttp = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://hacker-news.firebaseio.com/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    private val hnApi = retrofit.create(HnApiService::class.java)

    val preferencesStore: PreferencesStore = prefs
    val feedRepository = FeedRepository(
        dao = db.articleDao(),
        prefs = prefs,
        configLoader = configLoader,
        hn = HnRemoteSource(hnApi),
        rss = RssRemoteSource(),
    )
    val classics: List<ClassicItem> by lazy { configLoader.loadClassics() }
}
```

- [ ] **Step 2: 更新 `HackerNewsApp.kt`**

```kotlin
package com.example.hackernews

import android.app.Application
import com.example.hackernews.di.AppContainer

class HackerNewsApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 3: 实现 `ui/util/AppViewModel.kt`**

```kotlin
package com.example.hackernews.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.hackernews.HackerNewsApp
import com.example.hackernews.di.AppContainer

@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as HackerNewsApp).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
```

- [ ] **Step 4: 构建 + 装机验证仍能启动**

Run: `./gradlew :app:installDebug && adb shell am start -n com.example.hackernews/.MainActivity`
Expected: 应用正常启动（仍是占位屏），无崩溃（验证 DI/网络/DB 初始化不炸）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/di app/src/main/java/com/example/hackernews/HackerNewsApp.kt app/src/main/java/com/example/hackernews/ui/util/AppViewModel.kt
git commit -m "feat: add manual DI container and ViewModel factory helper"
```

---

### Task 14: 可复用 UI 组件 + 相对时间

落地 UI 文档 §4 的复用 composable。相对时间是纯逻辑先 TDD；composable 用 `@Preview` + 后续屏幕目视验证。组件按职责聚合到 4 个文件。

**Files:**
- Create: `ui/util/TimeFormat.kt`
- Test: `app/src/test/java/com/example/hackernews/ui/util/TimeFormatTest.kt`
- Create: `ui/components/Atoms.kt`（TopicTag / BookmarkStar / BlinkingCursor / BrailleSpinner / EmptyState / StatusBanner）
- Create: `ui/components/ArticleRow.kt`
- Create: `ui/components/TerminalBars.kt`（TerminalAppBar / TerminalBottomBar / BottomNavItem）
- Create: `ui/components/Inputs.kt`（SearchField / TopicChipRow / WeightSlider）

**Interfaces:**
- Consumes: `Article`(T3)、`TerminalColors`/`AppTypography`(T2)。
- Produces:
  - `fun relativeTime(pastMillis: Long, nowMillis: Long): String`
  - `@Composable fun TopicTag(id)`, `BookmarkStar(active,onClick)`, `BrailleSpinner(label)`, `EmptyState(text)`, `StatusBanner(message,onRetry)`
  - `@Composable fun ArticleRow(article, nowMillis, onOpen, onToggleBookmark)`
  - `@Composable fun TerminalAppBar(command, onBack=null)`, `TerminalBottomBar(items)`；`data class BottomNavItem(label,selected,onClick)`
  - `@Composable fun SearchField(value,onValueChange,placeholder)`, `TopicChipRow(chips,selectedId,onSelect)`, `WeightSlider(value,enabled,onValueChange)`

- [ ] **Step 1: 写失败测试（relativeTime）**

```kotlin
package com.example.hackernews.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    private val now = 10_000_000_000L
    @Test fun justNow() = assertEquals("just now", relativeTime(now - 30_000, now))
    @Test fun minutes() = assertEquals("5m ago", relativeTime(now - 5 * 60_000, now))
    @Test fun hours() = assertEquals("3h ago", relativeTime(now - 3 * 3_600_000, now))
    @Test fun days() = assertEquals("2d ago", relativeTime(now - 2 * 86_400_000, now))
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*TimeFormatTest*"`
Expected: FAIL。

- [ ] **Step 3: 实现 `TimeFormat.kt`**

```kotlin
package com.example.hackernews.ui.util

fun relativeTime(pastMillis: Long, nowMillis: Long): String {
    val minutes = (nowMillis - pastMillis).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*TimeFormatTest*"`
Expected: PASS（4/4）。

- [ ] **Step 5: 实现 `Atoms.kt`**

```kotlin
package com.example.hackernews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors
import kotlinx.coroutines.delay

@Composable
fun TopicTag(id: String) =
    Text("[$id]", color = TerminalColors.Primary, style = MaterialTheme.typography.labelSmall)

@Composable
fun BookmarkStar(active: Boolean, onClick: () -> Unit) =
    Text(
        if (active) "★" else "☆",
        color = if (active) TerminalColors.Primary else TerminalColors.PrimaryDim,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.clickable { onClick() }.padding(8.dp),
    )

@Composable
fun BlinkingCursor() {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(600); on = !on } }
    Text(if (on) " █" else "  ", color = TerminalColors.Primary,
        style = MaterialTheme.typography.titleLarge)
}

@Composable
fun BrailleSpinner(label: String) {
    val frames = "⠋⠙⠹⠸⠼⠴⠦⠧"
    var i by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(120); i = (i + 1) % frames.length } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
        Text("${frames[i]} $label", color = TerminalColors.PrimaryDim,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(text: String) =
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TerminalColors.TextSecondary, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium)
    }

@Composable
fun StatusBanner(message: String, onRetry: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(TerminalColors.SurfaceElevated).padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("! $message", color = TerminalColors.Accent,
            style = MaterialTheme.typography.labelMedium)
        if (onRetry != null) {
            Spacer(Modifier.weight(1f))
            Text("[重试]", color = TerminalColors.Primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { onRetry() })
        }
    }
}
```

- [ ] **Step 6: 实现 `ArticleRow.kt`**

```kotlin
package com.example.hackernews.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hackernews.domain.model.Article
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.relativeTime

@Composable
fun ArticleRow(
    article: Article,
    nowMillis: Long,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable { onOpen() }.padding(16.dp, 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            article.topicIds.firstOrNull()?.let { TopicTag(it); Spacer(Modifier.width(6.dp)) }
            Text("· ${article.source}", color = TerminalColors.PrimaryDim,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(2.dp))
        Text(article.title, color = TerminalColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (article.summary.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(article.summary, color = TerminalColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(relativeTime(article.publishedAt, nowMillis), color = TerminalColors.PrimaryDim,
                style = MaterialTheme.typography.labelMedium)
            article.score?.let {
                Spacer(Modifier.width(8.dp))
                Text("▲$it", color = TerminalColors.Accent, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            BookmarkStar(article.isBookmarked) { onToggleBookmark() }
        }
    }
    HorizontalDivider(color = TerminalColors.Border)
}
```

- [ ] **Step 7: 实现 `TerminalBars.kt`**

```kotlin
package com.example.hackernews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun TerminalAppBar(command: String, onBack: (() -> Unit)? = null) {
    Column {
        Row(Modifier.fillMaxWidth().padding(16.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Text("‹", color = TerminalColors.Primary, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp))
            }
            Text(command, color = TerminalColors.Primary, style = MaterialTheme.typography.titleLarge)
            BlinkingCursor()
        }
        HorizontalDivider(color = TerminalColors.Border)
    }
}

data class BottomNavItem(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
fun TerminalBottomBar(items: List<BottomNavItem>) {
    Column {
        HorizontalDivider(color = TerminalColors.Border)
        Row(Modifier.fillMaxWidth().background(TerminalColors.Surface)) {
            items.forEach { item ->
                Box(
                    Modifier.weight(1f).clickable { item.onClick() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.label,
                        color = if (item.selected) TerminalColors.Primary else TerminalColors.PrimaryDim,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 8: 实现 `Inputs.kt`**

```kotlin
package com.example.hackernews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String = "grep> …") {
    Row(
        Modifier.fillMaxWidth().padding(16.dp, 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalColors.SurfaceElevated).padding(12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = TerminalColors.PrimaryDim,
                    style = MaterialTheme.typography.bodyMedium)
            }
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TerminalColors.TextPrimary),
                cursorBrush = SolidColor(TerminalColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun TopicChipRow(chips: List<Pair<String, String>>, selectedId: String?, onSelect: (String?) -> Unit) {
    // chips: (id, label)；selectedId=null 表示“全部”
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp, 4.dp)) {
        Chip("全部", selectedId == null) { onSelect(null) }
        chips.forEach { (id, label) ->
            Spacer(Modifier.width(8.dp))
            Chip(label, selectedId == id) { onSelect(id) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) TerminalColors.Primary else TerminalColors.Surface
    val fg = if (selected) TerminalColors.Bg else TerminalColors.PrimaryDim
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
            .clickableNoRipple(onClick).padding(10.dp, 6.dp),
    ) { Text(label, color = fg, style = MaterialTheme.typography.labelSmall) }
}

@Composable
fun WeightSlider(value: Float, enabled: Boolean, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = value, onValueChange = onValueChange, valueRange = 0f..2f, steps = 19,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = TerminalColors.Primary,
                activeTrackColor = TerminalColors.PrimaryDim,
                inactiveTrackColor = TerminalColors.Border,
            ),
        )
        Text("weight %.1f".format(value), color = TerminalColors.PrimaryDim,
            style = MaterialTheme.typography.labelMedium)
    }
}
```
并在 `Inputs.kt` 补一个无涟漪点击扩展（或直接用 `clickable`）：
```kotlin
import androidx.compose.foundation.clickable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable { onClick() }
```

- [ ] **Step 9: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL（组件编译通过；目视留待各屏幕）。

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/components app/src/main/java/com/example/hackernews/ui/util/TimeFormat.kt app/src/test/java/com/example/hackernews/ui/util/TimeFormatTest.kt
git commit -m "feat: add reusable terminal-style UI components and relative time"
```

---

### Task 15: Feed 屏 + ViewModel

主信息流：观察仓库 feed 流、下拉刷新、错误降级横幅、点击按阅读方式打开、收藏。

**Files:**
- Create: `ui/feed/FeedViewModel.kt`
- Create: `ui/feed/FeedScreen.kt`

**Interfaces:**
- Consumes: `FeedRepository`(T11)、`PreferencesStore`(T7)、`ArticleRow`/`TerminalAppBar`/`StatusBanner`/`BrailleSpinner`/`EmptyState`(T14)、`LinkOpener`(T12)、`appViewModel`(T13)。
- Produces: `@Composable fun FeedScreen()`。

- [ ] **Step 1: 实现 `FeedViewModel.kt`**

```kotlin
package com.example.hackernews.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repo: FeedRepository,
    prefs: PreferencesStore,
) : ViewModel() {
    val articles = repo.feedStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Article>())
    val readingMode = prefs.readingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.CUSTOM_TABS)

    val refreshing = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        refreshing.value = true
        val result = repo.refresh()
        error.value = if (result.failed) "刷新失败，显示缓存" else null
        refreshing.value = false
    }

    fun toggleBookmark(id: String) = viewModelScope.launch { repo.toggleBookmark(id) }
}
```

- [ ] **Step 2: 实现 `FeedScreen.kt`**

```kotlin
package com.example.hackernews.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.BrailleSpinner
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.StatusBanner
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    val vm = appViewModel { FeedViewModel(it.feedRepository, it.preferencesStore) }
    val articles by vm.articles.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val error by vm.error.collectAsState()
    val readingMode by vm.readingMode.collectAsState()
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> dev_feed --sort=hot")
        error?.let { StatusBanner(it) { vm.refresh() } }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
            when {
                articles.isEmpty() && refreshing -> BrailleSpinner("fetching feeds…")
                articles.isEmpty() -> EmptyState("~ 还没有内容，下拉刷新 ~")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(articles, key = { it.id }) { a ->
                        ArticleRow(
                            article = a, nowMillis = now,
                            onOpen = { LinkOpener.open(context, a.url, readingMode) },
                            onToggleBookmark = { vm.toggleBookmark(a.id) },
                        )
                    }
                }
            }
        }
    }
}
```
（注：`LinkOpener` 在 `ui/util` 包，import 路径为 `com.example.hackernews.ui.util.LinkOpener`。）

- [ ] **Step 3: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。（完整目视在 Task 20 接入导航后进行。）

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/feed
git commit -m "feat: add Feed screen with pull-to-refresh and bookmarking"
```

---

### Task 16: 经典屏 + ViewModel

展示 `classics.json` 精选清单，支持跳转与收藏。经典条目可能不在 Room，需要"不存在则插入再收藏"的能力，为此给仓库补两个方法。

**Files:**
- Modify: `data/repository/FeedRepository.kt`（新增 `bookmarkedIdsStream()`、`toggleBookmarkForArticle()`）
- Create: `ui/classics/ClassicsViewModel.kt`
- Create: `ui/classics/ClassicsScreen.kt`

**Interfaces:**
- Consumes: `FeedRepository`、`PreferencesStore`、`ClassicItem`(T5)、`ArticleRow`/`TerminalAppBar`/`EmptyState`、`LinkOpener`、`appViewModel`。
- Produces:
  - 仓库新增：`fun bookmarkedIdsStream(): Flow<Set<String>>`、`suspend fun toggleBookmarkForArticle(article: Article)`。
  - `fun ClassicItem.toArticle(): Article`
  - `@Composable fun ClassicsScreen()`。

- [ ] **Step 1: 给 `FeedRepository.kt` 追加方法**

在类内追加（`dao`/`toEntity` 已可用）：
```kotlin
fun bookmarkedIdsStream(): Flow<Set<String>> =
    dao.bookmarksStream().map { list -> list.map { it.id }.toSet() }

suspend fun toggleBookmarkForArticle(article: Article) {
    val existing = dao.getById(article.id)
    if (existing == null) {
        dao.upsertPreservingBookmark(listOf(article.toEntity()))
        dao.setBookmarked(article.id, true)
    } else {
        dao.setBookmarked(article.id, !existing.isBookmarked)
    }
}
```

- [ ] **Step 2: 实现 `ClassicsViewModel.kt`**

```kotlin
package com.example.hackernews.ui.classics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.config.ClassicItem
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.remote.articleIdFor
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun ClassicItem.toArticle(): Article = Article(
    id = articleIdFor(url), title = title, url = url, summary = summary,
    source = "经典必读",
    topicIds = if (topicId.isBlank()) emptyList() else listOf(topicId),
    publishedAt = 0L, isBookmarked = false, score = null,
)

class ClassicsViewModel(
    private val repo: FeedRepository,
    prefs: PreferencesStore,
    classics: List<ClassicItem>,
) : ViewModel() {
    private val base = classics.map { it.toArticle() }
    val articles = repo.bookmarkedIdsStream()
        .map { ids -> base.map { it.copy(isBookmarked = it.id in ids) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), base)
    val readingMode = prefs.readingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.CUSTOM_TABS)

    fun toggleBookmark(a: Article) = viewModelScope.launch { repo.toggleBookmarkForArticle(a) }
}
```

- [ ] **Step 3: 实现 `ClassicsScreen.kt`**

```kotlin
package com.example.hackernews.ui.classics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@Composable
fun ClassicsScreen() {
    val vm = appViewModel { ClassicsViewModel(it.feedRepository, it.preferencesStore, it.classics) }
    val articles by vm.articles.collectAsState()
    val readingMode by vm.readingMode.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> classics --must_read")
        if (articles.isEmpty()) {
            EmptyState("$ classics.json 为空")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(articles, key = { it.id }) { a ->
                    ArticleRow(
                        article = a, nowMillis = 0L,
                        onOpen = { LinkOpener.open(context, a.url, readingMode) },
                        onToggleBookmark = { vm.toggleBookmark(a) },
                    )
                }
            }
        }
    }
}
```
（注：经典无时间意义，`nowMillis=0` 时 `relativeTime` 显示为较大天数——UI 可后续隐藏经典的时间行；MVP 保留。若要隐藏，给 `ArticleRow` 加 `showTime: Boolean = true` 参数并在经典处传 false。）

- [ ] **Step 4: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/data/repository/FeedRepository.kt app/src/main/java/com/example/hackernews/ui/classics
git commit -m "feat: add Classics screen with bookmark-any-article support"
```

---

### Task 17: 我的入口屏 + 收藏屏（按主题检索）

「我的」入口列出收藏/Topic 偏好/阅读方式/关于；收藏屏支持 topic chip 筛选 + `grep>` 关键字搜索。

**Files:**
- Create: `ui/profile/ProfileScreen.kt`
- Create: `ui/profile/bookmarks/BookmarksViewModel.kt`
- Create: `ui/profile/bookmarks/BookmarksScreen.kt`

**Interfaces:**
- Consumes: `FeedRepository`(T11/16)、`PreferencesStore`、`ArticleRow`/`SearchField`/`TopicChipRow`/`TerminalAppBar`/`EmptyState`、`LinkOpener`、`appViewModel`。
- Produces:
  - `@Composable fun ProfileScreen(onOpenBookmarks, onOpenTopics, onOpenReading, onOpenAbout)`
  - `@Composable fun BookmarksScreen(onBack)`

- [ ] **Step 1: 实现 `ProfileScreen.kt`**

```kotlin
package com.example.hackernews.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun ProfileScreen(
    onOpenBookmarks: () -> Unit,
    onOpenTopics: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> whoami")
        Row("★  收藏", onOpenBookmarks)
        Row("#  Topic 偏好", onOpenTopics)
        Row("⇱  阅读方式", onOpenReading)
        Row("?  关于", onOpenAbout)
    }
}

@Composable
private fun Row(label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp, 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TerminalColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text("›", color = TerminalColors.PrimaryDim, style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider(color = TerminalColors.Border)
}
```

- [ ] **Step 2: 实现 `BookmarksViewModel.kt`**

```kotlin
package com.example.hackernews.ui.profile.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Article
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(
    private val repo: FeedRepository,
    prefs: PreferencesStore,
) : ViewModel() {
    val selectedTopic = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")

    val topicChips = repo.topicsStream()
        .map { list -> list.map { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingMode = prefs.readingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.CUSTOM_TABS)

    val articles = combine(selectedTopic, query) { t, q -> t to q }
        .flatMapLatest { (t, q) -> repo.bookmarksStream(t, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Article>())

    fun setTopic(id: String?) { selectedTopic.value = id }
    fun setQuery(q: String) { query.value = q }
    fun toggleBookmark(id: String) = viewModelScope.launch { repo.toggleBookmark(id) }
}
```

- [ ] **Step 3: 实现 `BookmarksScreen.kt`**

```kotlin
package com.example.hackernews.ui.profile.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.hackernews.ui.components.ArticleRow
import com.example.hackernews.ui.components.EmptyState
import com.example.hackernews.ui.components.SearchField
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.components.TopicChipRow
import com.example.hackernews.ui.util.LinkOpener
import com.example.hackernews.ui.util.appViewModel

@Composable
fun BookmarksScreen(onBack: () -> Unit) {
    val vm = appViewModel { BookmarksViewModel(it.feedRepository, it.preferencesStore) }
    val articles by vm.articles.collectAsState()
    val chips by vm.topicChips.collectAsState()
    val selected by vm.selectedTopic.collectAsState()
    val query by vm.query.collectAsState()
    val readingMode by vm.readingMode.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> bookmarks", onBack = onBack)
        SearchField(value = query, onValueChange = vm::setQuery)
        TopicChipRow(chips = chips, selectedId = selected, onSelect = vm::setTopic)
        if (articles.isEmpty()) {
            EmptyState(if (query.isNotBlank() || selected != null) "$ no match" else "$ no bookmarks yet")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(articles, key = { it.id }) { a ->
                    ArticleRow(
                        article = a, nowMillis = 0L,
                        onOpen = { LinkOpener.open(context, a.url, readingMode) },
                        onToggleBookmark = { vm.toggleBookmark(a.id) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/profile
git commit -m "feat: add Profile entry and searchable bookmarks by topic"
```

---

### Task 18: Topic 偏好屏 + ViewModel

列出所有 topic，开关 + 权重滑块，改动即时写 DataStore 并影响 Feed。

**Files:**
- Create: `ui/profile/topics/TopicSettingsViewModel.kt`
- Create: `ui/profile/topics/TopicSettingsScreen.kt`

**Interfaces:**
- Consumes: `FeedRepository.topicsStream()`、`PreferencesStore`、`TerminalAppBar`/`WeightSlider`、`appViewModel`。
- Produces: `@Composable fun TopicSettingsScreen(onBack)`。

- [ ] **Step 1: 实现 `TopicSettingsViewModel.kt`**

```kotlin
package com.example.hackernews.ui.profile.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.data.repository.FeedRepository
import com.example.hackernews.domain.model.Topic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicSettingsViewModel(
    repo: FeedRepository,
    private val prefs: PreferencesStore,
) : ViewModel() {
    val topics = repo.topicsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Topic>())

    fun setEnabled(id: String, enabled: Boolean) = viewModelScope.launch { prefs.setEnabled(id, enabled) }
    fun setWeight(id: String, weight: Float) = viewModelScope.launch { prefs.setWeight(id, weight) }
}
```

- [ ] **Step 2: 实现 `TopicSettingsScreen.kt`**

```kotlin
package com.example.hackernews.ui.profile.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.components.WeightSlider
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.appViewModel

@Composable
fun TopicSettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { TopicSettingsViewModel(it.feedRepository, it.preferencesStore) }
    val topics by vm.topics.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> topics --config", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            items(topics, key = { it.id }) { topic ->
                Column(Modifier.fillMaxWidth().padding(16.dp, 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(topic.name, color = TerminalColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (topic.enabled) "[ on ]" else "[off]",
                            color = if (topic.enabled) TerminalColors.Primary else TerminalColors.PrimaryDim,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { vm.setEnabled(topic.id, !topic.enabled) }
                                .padding(6.dp),
                        )
                    }
                    WeightSlider(
                        value = topic.weight, enabled = topic.enabled,
                        onValueChange = { vm.setWeight(topic.id, it) },
                    )
                }
                HorizontalDivider(color = TerminalColors.Border)
            }
        }
    }
}
```

- [ ] **Step 3: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/profile/topics
git commit -m "feat: add Topic preferences screen with toggle and weight"
```

---

### Task 19: 阅读方式屏 + ViewModel + 关于屏

阅读方式二选一（Custom Tabs / 外部浏览器）写 DataStore；关于屏为占位（版本 + 数据来源）。

**Files:**
- Create: `ui/profile/reading/ReadingModeViewModel.kt`
- Create: `ui/profile/reading/ReadingModeScreen.kt`
- Create: `ui/profile/AboutScreen.kt`

**Interfaces:**
- Consumes: `PreferencesStore`、`ReadingMode`、`TerminalAppBar`、`appViewModel`。
- Produces: `@Composable fun ReadingModeScreen(onBack)`、`@Composable fun AboutScreen(onBack)`。

- [ ] **Step 1: 实现 `ReadingModeViewModel.kt`**

```kotlin
package com.example.hackernews.ui.profile.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hackernews.data.local.PreferencesStore
import com.example.hackernews.domain.model.ReadingMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadingModeViewModel(private val prefs: PreferencesStore) : ViewModel() {
    val mode = prefs.readingMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingMode.CUSTOM_TABS)
    fun set(mode: ReadingMode) = viewModelScope.launch { prefs.setReadingMode(mode) }
}
```

- [ ] **Step 2: 实现 `ReadingModeScreen.kt`**

```kotlin
package com.example.hackernews.ui.profile.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.domain.model.ReadingMode
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors
import com.example.hackernews.ui.util.appViewModel

@Composable
fun ReadingModeScreen(onBack: () -> Unit) {
    val vm = appViewModel { ReadingModeViewModel(it.preferencesStore) }
    val mode by vm.mode.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> reading --mode", onBack = onBack)
        Option("Custom Tabs", "应用内浮层打开", mode == ReadingMode.CUSTOM_TABS) {
            vm.set(ReadingMode.CUSTOM_TABS)
        }
        Option("外部浏览器", "跳转 Chrome 等", mode == ReadingMode.EXTERNAL_BROWSER) {
            vm.set(ReadingMode.EXTERNAL_BROWSER)
        }
        Text(
            "· 设备无 Custom Tabs 时自动回退到外部浏览器",
            color = TerminalColors.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(16.dp, 12.dp),
        )
    }
}

@Composable
private fun Option(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (selected) "(•)" else "( )", color = TerminalColors.Primary,
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TerminalColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TerminalColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
    HorizontalDivider(color = TerminalColors.Border)
}
```

- [ ] **Step 3: 实现 `AboutScreen.kt`**

```kotlin
package com.example.hackernews.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hackernews.ui.components.TerminalAppBar
import com.example.hackernews.ui.theme.TerminalColors

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TerminalAppBar("> about", onBack = onBack)
        Text(
            "dev-news v1.0\n数据来源：Hacker News + 精选 RSS\n内容仅链接跳转，版权归原站所有。",
            color = TerminalColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
```

- [ ] **Step 4: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/profile/reading app/src/main/java/com/example/hackernews/ui/profile/AboutScreen.kt
git commit -m "feat: add reading mode and about screens"
```

---

### Task 20: 导航接线（NavHost + 底部导航）

3 个底部 tab（Feed/经典/我的）+ 我的下的子路由（收藏/Topic/阅读/关于），子屏隐藏底部栏并带返回。更新 MainActivity 渲染导航。

**Files:**
- Create: `ui/nav/AppNav.kt`
- Modify: `MainActivity.kt`（渲染 `AppNav()`）

**Interfaces:**
- Consumes: 全部屏幕(T15–19)、`TerminalBottomBar`/`BottomNavItem`(T14)。
- Produces: `@Composable fun AppNav()`。

- [ ] **Step 1: 实现 `ui/nav/AppNav.kt`**

```kotlin
package com.example.hackernews.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hackernews.ui.classics.ClassicsScreen
import com.example.hackernews.ui.components.BottomNavItem
import com.example.hackernews.ui.components.TerminalBottomBar
import com.example.hackernews.ui.feed.FeedScreen
import com.example.hackernews.ui.profile.AboutScreen
import com.example.hackernews.ui.profile.ProfileScreen
import com.example.hackernews.ui.profile.bookmarks.BookmarksScreen
import com.example.hackernews.ui.profile.reading.ReadingModeScreen
import com.example.hackernews.ui.profile.topics.TopicSettingsScreen

private object Routes {
    const val FEED = "feed"; const val CLASSICS = "classics"; const val PROFILE = "profile"
    const val BOOKMARKS = "bookmarks"; const val TOPICS = "topics"
    const val READING = "reading"; const val ABOUT = "about"
}

private fun NavController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val bottomRoutes = setOf(Routes.FEED, Routes.CLASSICS, Routes.PROFILE)

    Scaffold(
        bottomBar = {
            if (route in bottomRoutes) {
                TerminalBottomBar(
                    listOf(
                        BottomNavItem("FEED", route == Routes.FEED) { nav.switchTab(Routes.FEED) },
                        BottomNavItem("经典", route == Routes.CLASSICS) { nav.switchTab(Routes.CLASSICS) },
                        BottomNavItem("我的", route == Routes.PROFILE) { nav.switchTab(Routes.PROFILE) },
                    )
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.FEED, modifier = Modifier.padding(padding)) {
            composable(Routes.FEED) { FeedScreen() }
            composable(Routes.CLASSICS) { ClassicsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenBookmarks = { nav.navigate(Routes.BOOKMARKS) },
                    onOpenTopics = { nav.navigate(Routes.TOPICS) },
                    onOpenReading = { nav.navigate(Routes.READING) },
                    onOpenAbout = { nav.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.BOOKMARKS) { BookmarksScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.TOPICS) { TopicSettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.READING) { ReadingModeScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
```

- [ ] **Step 2: 更新 `MainActivity.kt`**

```kotlin
package com.example.hackernews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hackernews.ui.nav.AppNav
import com.example.hackernews.ui.theme.HackerNewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HackerNewsTheme { AppNav() } }
    }
}
```

- [ ] **Step 3: 装机目视（导航 + 主题）**

Run: `./gradlew :app:installDebug && adb shell am start -n com.example.hackernews/.MainActivity`
Expected: 黑底绿字，底部 3 tab；可在 Feed/经典/我的 间切换；「我的」进入收藏/Topic/阅读/关于并能返回。（此时 assets 未填充，Feed 可能为空——由 Task 21 填内容。）

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/hackernews/ui/nav app/src/main/java/com/example/hackernews/MainActivity.kt
git commit -m "feat: wire navigation with bottom tabs and profile subroutes"
```

---

### Task 21: 填充 assets（topics.json + classics.json）

提供开箱即用的预置 topic（含真实 RSS 源与 keyword）与经典必读清单。无效源在运行时被静默跳过（已在源码保证）。

**Files:**
- Create: `app/src/main/assets/topics.json`
- Create: `app/src/main/assets/classics.json`

- [ ] **Step 1: 写 `topics.json`**

```json
{
  "topics": [
    {
      "id": "backend", "name": "后端 & 架构", "enabled": true, "weight": 1.2,
      "feeds": [
        "https://martinfowler.com/feed.atom",
        "https://www.allthingsdistributed.com/atom.xml",
        "https://netflixtechblog.com/feed"
      ],
      "keywords": ["database", "scalability", "microservice", "architecture", "distributed", "system design", "backend", "api", "kafka", "latency"]
    },
    {
      "id": "frontend", "name": "前端 & Web", "enabled": true, "weight": 1.0,
      "feeds": [
        "https://overreacted.io/rss.xml",
        "https://css-tricks.com/feed/",
        "https://www.smashingmagazine.com/feed/"
      ],
      "keywords": ["react", "javascript", "typescript", "css", "frontend", "browser", "web", "vue", "ui"]
    },
    {
      "id": "languages", "name": "编程语言 & CS 基础", "enabled": true, "weight": 1.0,
      "feeds": [
        "https://jvns.ca/atom.xml",
        "https://danluu.com/atom.xml"
      ],
      "keywords": ["compiler", "algorithm", "memory", "kotlin", "rust", "python", "language", "data structure", "concurrency", "debugging"]
    },
    {
      "id": "ai", "name": "AI 工程", "enabled": true, "weight": 1.0,
      "feeds": [
        "https://simonwillison.net/atom/everything/",
        "https://huggingface.co/blog/feed.xml"
      ],
      "keywords": ["llm", "rag", "embedding", "machine learning", "ai", "gpt", "transformer", "prompt", "model", "inference"]
    },
    {
      "id": "devops", "name": "DevOps & 云", "enabled": true, "weight": 0.9,
      "feeds": [
        "https://kubernetes.io/feed.xml",
        "https://aws.amazon.com/blogs/devops/feed/"
      ],
      "keywords": ["kubernetes", "docker", "ci/cd", "terraform", "aws", "cloud", "devops", "observability", "deployment", "sre"]
    },
    {
      "id": "career", "name": "职业成长 & 工程实践", "enabled": true, "weight": 0.9,
      "feeds": [
        "https://newsletter.pragmaticengineer.com/feed",
        "https://blog.codinghorror.com/rss/"
      ],
      "keywords": ["career", "code review", "testing", "refactoring", "engineering", "productivity", "team", "best practice", "clean code"]
    }
  ]
}
```

- [ ] **Step 2: 写 `classics.json`**

```json
{
  "items": [
    { "title": "The Twelve-Factor App", "url": "https://12factor.net", "summary": "云原生应用的 12 条通用准则。", "topicId": "backend" },
    { "title": "Latency Numbers Every Programmer Should Know", "url": "https://gist.github.com/jboner/2841832", "summary": "常见操作的延迟量级，建立性能直觉。", "topicId": "backend" },
    { "title": "Big Ball of Mud", "url": "http://www.laputan.org/mud/", "summary": "关于失控架构的经典论文。", "topicId": "backend" },
    { "title": "The Law of Leaky Abstractions", "url": "https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/", "summary": "所有抽象都会在某处泄漏。", "topicId": "languages" },
    { "title": "Parse, Don't Validate", "url": "https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/", "summary": "用类型把非法状态挡在门外。", "topicId": "languages" },
    { "title": "Falsehoods Programmers Believe About Names", "url": "https://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-names/", "summary": "关于姓名的错误假设合集。", "topicId": "career" },
    { "title": "What Every Programmer Should Know About Memory", "url": "https://people.freebsd.org/~lstewart/articles/cpumemory.pdf", "summary": "内存层级与性能的深度长文。", "topicId": "languages" },
    { "title": "On the Criteria To Be Used in Decomposing Systems into Modules", "url": "https://www.win.tue.nl/~wstomv/edu/2ip30/references/criteria_for_modularization.pdf", "summary": "Parnas 关于模块化的奠基论文。", "topicId": "backend" }
  ]
}
```

- [ ] **Step 3: 装机验证内容加载**

Run: `./gradlew :app:installDebug && adb shell am start -n com.example.hackernews/.MainActivity`
Expected: Feed 下拉刷新后出现文章；「我的 → Topic 偏好」列出 6 个 topic；「经典」列出 8 条。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/topics.json app/src/main/assets/classics.json
git commit -m "feat: add starter topics and classics config"
```

---

### Task 22: 真机端到端验证（vivo X100S PRO）

在目标设备上跑通全部关键路径，确认真实网络下 HN + RSS 能拉到内容、交互闭环。

**Files:** 无（验证任务）。

- [ ] **Step 1: 跑全部单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全绿（ArticleMapper / Config / KeywordMatcher / RssFormat / FeedRanker / ArticleMerge / TimeFormat / LinkOpenerDecision）。

- [ ] **Step 2: 跑 instrumented 测试（设备已连）**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: 全绿（ArticleDaoTest / PreferencesStoreTest）。

- [ ] **Step 3: 安装并逐项目视验证**

Run: `./gradlew :app:installDebug && adb shell am start -n com.example.hackernews/.MainActivity`

逐项确认：
- [ ] Feed 下拉刷新出现聚合文章（HN 有 `▲` 分数、RSS 有简介），终端绿黑观感、等宽字体。
- [ ] 点击文章：默认以 Custom Tabs 打开；到「我的 → 阅读方式」切「外部浏览器」后再点，跳系统浏览器。
- [ ] 收藏某文章 → 「我的 → 收藏」出现；选 topic chip 只显示该 topic；`grep>` 输入关键字过滤生效。
- [ ] 「我的 → Topic 偏好」：关掉某 topic 后回 Feed，其内容消失；调高某 topic 权重后其内容更靠前。
- [ ] 「经典」列出 8 条并可跳转/收藏。
- [ ] 关闭网络后下拉刷新：显示缓存 + 顶部 `! 刷新失败，显示缓存` 横幅。

- [ ] **Step 4: 若某 RSS 源始终无内容**

用 `adb logcat` 观察是否该源解析失败被跳过（预期行为）。如需替换，编辑 `assets/topics.json` 的 `feeds` 后重装。此为内容维护，不阻塞交付。

- [ ] **Step 5: 最终提交**

```bash
git add -A
git commit -m "chore: verify end-to-end on device"
```

---

## 计划自检（Self-Review）

**Spec 覆盖：**
- 内容源 HN + RSS → T8/T9；配置驱动 topic/classics → T5/T21；keyword 打标签 → T8。
- feed-only（无通知/后端）→ 全程无通知代码；抓取层可插拔（`FeedRepository.refresh` 可被未来 Worker 调用）。
- 极客终端绿黑 + JetBrains Mono → T2/T14（配色 token、字体、组件）。
- 3 tab 导航 Feed/经典/我的 → T20；我的含收藏/Topic/阅读/关于 → T17/T18/T19。
- 收藏 → T6/T11；按主题检索 → T17（TopicChipRow + query）。
- Custom Tabs 默认 + 可切外部浏览器 + 自动回退 → T12（LinkOpener）/T19。
- 排序按 topic 权重 × 时效 × HN 分数 → T10；URL 去重 → T4/T11。
- 局部失败降级 → T8/T9（per-source runCatching）/T11（refresh failed 标志）。
- 可扩展性 → T5/T21（改 JSON 即扩展）。

**占位符扫描：** 无 TBD/TODO；每个 code step 均含完整代码。

**类型一致性：** `FeedRepository` 的 `feedStream/topicsStream/refresh/bookmarksStream/toggleBookmark/bookmarkedIdsStream/toggleBookmarkForArticle`、`ArticleDao` 方法名、`PreferencesStore` 方法名、组件签名在各 task 间一致。`LinkOpener` 位于 `ui/util` 包（T12 起统一）。

**已知取舍（有意为之）：**
- usecase 层精简为直接用 repository（YAGNI，见 T12 前说明）。
- 经典条目 `nowMillis=0`，相对时间显示为大天数；如介意，按 T16 注记给 `ArticleRow` 加 `showTime` 参数。
- RSS 字段名依 RssParser 6.x；若依赖版本不同按其 API 调整（T9 注记）。
- 依赖版本为 AGP 9.1.1 环境的合理取值；若同步报兼容错误，按 T1 Step 6 提示微调。

