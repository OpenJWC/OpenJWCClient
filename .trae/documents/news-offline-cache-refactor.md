# 资讯离线缓存重构方案（v2）

## Context（背景）

当前资讯数据流完全在线：`NewsViewModel` 的分页状态只存在内存（App 重启即失），`NewsRepository.getNews/getLabels` 直连网络，无任何本地缓存；通知 Worker 的"已通知水位"存 DataStore 字符串（容量 300）。结果是：**断网时资讯页空白，通知判断依赖脆弱的 id 字符串水位**；同时收藏表以裸 id 为主键，**跨数据源同 id 会撞主键**。

本次重构目标（用户已确认）：
1. 新建统一**资讯缓存表**（Room），以**数据源（host+port）+ 标签**为维度，换服务器互不污染；**切换数据源后列表只显示新源内容**（分区隔离），旧源数据留存、切回可离线看
2. App 内刷新和后台通知 Worker 都写入缓存；**离线可看资讯**（列表 + 详情）；表现：**先缓存后刷新**
3. DataStore 水位废弃，改用缓存行 `notified` 字段（数据库即水位）
4. 设置 → 通用 新增**"存储与缓存"**页："清除资讯缓存"（仅缓存表，不动收藏；清除后 Worker 静默重建基线）
5. **收藏表（favorite_notices）保持自包含（存全字段）**，但增加 host/port 维度（复合主键），修复跨源同 id 冲突；**收藏列表按当前数据源隔离显示**

## 数据模型

### 新增实体（`data/models/NewsCacheEntity.kt`）

```kotlin
@Entity(tableName = "news_cache",
    primaryKeys = ["host", "port", "noticeId"],
    indices = [Index("host", "port", "label")])
data class NewsCacheEntity(
    val host: String, val port: Int, val noticeId: String,
    val label: String, val title: String, val date: String,
    val sortTime: Long,          // date 解析出的排序键（多格式 runCatching，失败=0），不信任 date 字符串排序
    val detailUrl: String, val isPage: Boolean, val contentText: String?,
    val attachmentUrls: List<String>?,   // 复用 Converters → TEXT（已核实兼容）
    val cachedAt: Long,
    val notified: Boolean = false
)

@Entity(tableName = "news_cache_labels", primaryKeys = ["host", "port"])
data class NewsLabelCacheEntity(
    val host: String, val port: Int,
    val labels: List<String>,    // Converters → TEXT
    val cachedAt: Long
)
```
附 `NewsCacheEntity.toFetchedNotice()` / `FetchedNotice.toNewsCacheEntity(host, port, now)` 映射器（仿 [NoticeEntity.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/data/models/NoticeEntity.kt) 现有 `toFetchedNotice()`）。

### 收藏实体调整（[NoticeEntity.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/data/models/NoticeEntity.kt)）

- `NoticeEntity` 增加 `host: String = ""`、`port: Int = 0` 字段
- `FetchedNotice.toNoticeEntity()` 映射器（[NewsModels.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/net/models/NewsModels.kt)）增加 host/port 参数

### DAO 扩展（追加到 [NewsDao.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/data/dao/NewsDao.kt)）

**缓存部分：**
- `@Upsert upsertNewsCache(items)` / `getNewsCache(host, port, label, limit)`（`ORDER BY sortTime DESC`）
- `countNewsCache(host, port, label)` / `getNotifiedIds(host, port)`（`notified=1`）/ `markNotified(host, port, ids)`
- `pruneNewsCache(host, port, label, keep)`：子查询保留 sortTime 最新 keep 行
- `observeNewsCacheCount(): Flow<Int>`（全表计数，设置页用）
- `upsertLabelCache` / `getLabelCache` / `clearNewsCache()` / `clearNewsLabelCache()`

**收藏部分（签名加源维度）：**
- `getAllFavorites(host, port): Flow<List<NoticeEntity>>`
- `isFavorited(host, port, noticeId)` / `deleteFavoriteById(host, port, noticeId)`
- `updateLegacyFavoritesOwner(host: String, port: Int)`：`UPDATE favorite_notices SET host=:host, port=:port WHERE host=''`

### 迁移（[AppDatabase.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/data/db/AppDatabase.kt)）

`version = 6`，entities 注册两个新实体，`addMigrations` 追加 `MIGRATION_5_6`（沿用原生 SQL 风格）：

1. CREATE `news_cache`（含索引）+ `news_cache_labels`，列名与 Kotlin 属性逐字一致
2. **重建 favorite_notices**（SQLite 不能改主键）：建新表 `favorite_notices_new`（PK = `(host, port, id)`，含新列 host TEXT NOT NULL、port INTEGER NOT NULL）→ `INSERT INTO favorite_notices_new SELECT ..., '', 0 FROM favorite_notices`（旧行归入 `host=''` 遗留分区）→ DROP 旧表 → RENAME

## 核心逻辑

### Repository（[NewsRepository.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/data/repository/NewsRepository.kt)）

host/port 一律在调用时从 `settingsDataSource.userSettings.first()` 解析（换源自动生效）：

```kotlin
// 缓存
suspend fun getCachedNews(label: String, limit: Int = CACHE_LIMIT): List<FetchedNotice>
suspend fun countCachedNews(label: String): Int
suspend fun getCachedLabels(): List<String>?   // 无缓存返回 null
fun observeNewsCacheCount(): Flow<Int>

// 三步法保 notified（@Upsert 整行覆盖会重置 notified，必须先读后写再回写）：
// 1) notifiedIds = newsDao.getNotifiedIds(host, port)
// 2) upsert（新行 notified=false）
// 3) markNotified(notifiedIds) 回写原状态；新插入行 = fetched - 分区已有 id
suspend fun upsertNewsCachePreservingNotified(items: List<FetchedNotice>): List<FetchedNotice>
suspend fun refreshNewsCacheFromUi(label: String, items: List<FetchedNotice>)  // 上者 + 新行立即 markNotified + prune
suspend fun saveLabelsCache(labels: List<String>)
suspend fun clearNewsCache()   // 两张缓存表全清，不动 favorite_notices
companion object { const val CACHE_LIMIT = 200 }

// 收藏（对外签名不变，内部解析当前源；旧接口逐个加源维度）
suspend fun insertFavoriteNews(notice: NoticeEntity)      // notice 已带 host/port（调用方经 mapper 注入）
suspend fun deleteFavoriteNews(noticeId: String)          // → deleteFavoriteById(host, port, id)
suspend fun isFavoriteNews(noticeId: String): Boolean     // → isFavorited(host, port, id)
fun allFavorites(): Flow<List<NoticeEntity>>              // → getAllFavorites(host, port)
suspend fun adoptLegacyFavorites()                        // host='' 的遗留行一次性归属当前源（幂等）
```

Worker 组装（沿用手动 DI 风格，同 NavContainer L127）：
`NewsRepository(AppDatabase.getDatabase(ctx).newsDao(), SettingsDataSource(ctx), AuthDataSource(ctx))`

### ViewModel（[NewsViewModel.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/viewmodels/NewsViewModel.kt)）

- `loadCategory`：首次加载前先 `hydrateFromCache(label)`——DAO 读缓存填入 `_pagingStates`（仅当前状态为空时，不覆盖网络数据）；网络流程不变
- `executeLoadNews` Success 分支追加 `refreshNewsCacheFromUi(label, newData)`
- `loadLabels`：Failure/Error 时先回退标签缓存（静默，不设 labelError）；Success 时写标签缓存
- `favoriteNews` 改为 `settingsRepository.userSettings.flatMapLatest { newsRepository.allFavorites() }`（按源隔离的关键）+ init 里调一次 `adoptLegacyFavorites()`
- `loadNextPage` 不动（分页仍纯网络，缓存只服务页 1 —— 已知限制）
- 详情页/收藏按钮：`isFavorited` 走 Repository（内部带源），UI 无感
- 新增 `newsCacheCount: StateFlow<Int>` + `clearNewsCache()`（toast 反馈）

### Worker（[NewsCheckWorker.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/work/NewsCheckWorker.kt)）

- 删除 DataStore 水位依赖（`SettingsDataSource` 的 `getNotifiedNewsIds/saveNotifiedNewsIds/NOTIFIED_NEWS_IDS/WATERMARK_CAPACITY` 移除；NetClient/fetchNews 直连 import 清理）
- 每个 label：`oldCount = countCachedNews(label)` → upsert（保 notified）→ prune
  - `oldCount == 0` → **首跑静默基线**：`markNotified(全部)` 不发通知
  - `newlyInserted` 非空 → `NewsNotifier.postNewNews(newlyInserted)` → `markNotified(new ids)`
- 单 label 网络失败 `?: continue` 不炸整轮；顶层异常仍 `Result.retry()`
- "清除缓存 → 分区清空 → 下轮静默重建基线" 自然闭环

### 设置页 + 路由

- [Screen.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/navigation/Screen.kt) 新增 `object StorageCache : Screen`；[NavContainer.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/navigation3/NavContainer.kt) 注册 entry
- 新增 `ui/me/settings/storage/StorageCacheScreen.kt`（Screen + Content 双组件，仿 [NotificationSettingsScreen.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/ui/me/settings/notification/NotificationSettingsScreen.kt)）：
  - "资讯缓存" 条目显示条数（`newsCacheCount`，全源合计）
  - "清除资讯缓存" 条目 → AlertDialog 确认（照抄 [AccountScreen.kt#L113-L132](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/ui/me/settings/auth/AccountScreen.kt#L113-L132) 解绑模式，confirmButton 用 error 色）
- [SettingsScreen.kt](file:///home/sakimidare/AndroidStudioProjects/OpenJWCClient/app/src/main/java/org/openjwc/client/ui/me/settings/SettingsScreen.kt) "通用" 分组宽窄两处各加一条入口（宽屏 `selectedPage` 分发需把 `newsViewModel` 传入）

## 字符串（5 个 locale，注意 **values-zh-rTW 之前被遗漏**，本次连同通知相关 key 一并补齐）

新增：`storage_and_cache` / `news_cache_count`（"%1$d 条"）/ `clear_news_cache` / `clear_news_cache_confirm` / `cache_cleared`；confirm/cancel 复用现有 key。

## 边界情况

1. **换源**：列表/标签/收藏都只命中当前分区；旧源缓存与收藏留存，切回可看
2. **遗留收藏归属**：迁移后旧收藏在 `host=''` 分区，`adoptLegacyFavorites()` 在 VM init 一次性归属当前源（幂等）
3. **排序**：`sortTime` 解析列（"yyyy-MM-dd HH:mm:ss"/"yyyy-MM-dd" 等，失败=0）
4. **缓存上界**：每 (host,port,label) 200 行，upsert 后 prune；每源标签 1 行
5. **VM/Worker 并发写**：Room 串行化写连接，三步法最坏情形极端窗口重复通知一次，可接受
6. **鉴权过期**：Worker 入口校验，单 label 失败跳过
7. **收藏与缓存解耦**：清除缓存不删收藏（自包含设计）；收藏的资讯仍会因 prune 从缓存消失，但收藏列表本身完整

## 涉及文件汇总

新增：`NewsCacheEntity.kt`、`StorageCacheScreen.kt`
修改：`AppDatabase.kt`、`NewsDao.kt`、`NoticeEntity.kt`（+host/port）、`NewsModels.kt`（mapper）、`NewsRepository.kt`、`NewsViewModel.kt`、`NewsCheckWorker.kt`、`SettingsDataSource.kt`（删水位）、`Screen.kt`、`NavContainer.kt`、`SettingsScreen.kt`、5×`strings.xml`

## 验证

1. `assembleDebug` 编译通过（Room 编译期校验复合 PK/@Upsert/Converters）
2. **迁移**：旧 v5 包覆盖安装新包 → 启动无 crash；旧收藏自动归属当前源且列表可见（App Inspection 查三表结构）
3. **离线读**：联网刷新 → 飞行模式 → 重进：标签正常（缓存回退）、列表秒出缓存、下拉失败但列表仍在、详情可看
4. **Worker 闭环**：清除缓存 → 下轮 Worker 无通知（静默基线）→ 服务器造新资讯 → 收到通知且仅该条 notified 翻转
5. **换源**：切换 host/port → 资讯列表/收藏列表都只显示新源；切回旧源，离线仍可读旧源缓存与收藏
6. **清除**：确认弹窗 → toast → 计数归零，**收藏不受影响**，DataStore 无 `notified_news_ids`
7. 收藏/取消收藏 → 切源再切回 → 状态正确
8. 真机重装测试后验证 MIUI 下行为
