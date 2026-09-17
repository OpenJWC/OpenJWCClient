# 数据源脚本格式

OpenJWC 客户端的资讯抓取完全由**本地脚本**驱动：脚本在 App 内置的 QuickJS 引擎里执行，
配合宿主注入的 HTTP / HTML 解析桥完成抓取与解析。本文件是脚本作者的完整参考。

- 运行环境：QuickJS（**同步**执行，无 DOM、无 `fetch`、无定时器、无 `async/await`）
- 内置参考实现：`app/src/main/assets/sources/`（教务处 `seu-jwc.js` 默认订阅，其余为各院系官网）
- 相关源码：`app/src/main/java/org/openjwc/client/script/`、`data/source/SourceRunner.kt`

---

## 1. 脚本清单（头部注释）

脚本开头用注释声明元信息，格式为 `@key value`，注释符可用 `//` 或 `#`：

```js
// @id seu-jwc
// @name 东南大学教务处
// @version 1.4.0
// @schedule 360
// @domains jwc.seu.edu.cn
// @labels 最新动态,教务信息,学籍管理
```

| 键 | 必填 | 说明 |
| --- | --- | --- |
| `@id` | ✅ | 数据源唯一标识；也是覆盖脚本的落盘文件名 `filesDir/sources/<id>.js` |
| `@name` | | 显示名，缺省用 `@id` |
| `@version` | | 版本号，缺省 `1.0.0` |
| `@schedule` | | 抓取周期（分钟），最小 15，缺省 360 |
| `@domains` | | 域名白名单，可用 `,` `，` 空格 `\|` 分隔；为空表示不限制（不建议） |
| `@labels` | | 栏目列表，分隔符同上；决定资讯流里该源的栏目顺序 |

`@id` 缺失时注册/导入会失败。

---

## 2. 入口函数

```js
function fetchNotices() {
  return [ /* 条目对象 */ ];
}
```

- 宿主把脚本与 `;JSON.stringify(fetchNotices());` 拼成**同一次 evaluate**，
  所以 `fetchNotices` 必须定义在全局作用域（不要用 IIFE 把整个脚本包起来）。
- 返回值必须是**数组**；宿主会 `JSON.stringify` 后按 `ScriptNotice` 反序列化（未知字段忽略）。
- **顶层不要做实际抓取**：导入/保存时会用惰性 stub 求值一次做静态校验
  （只检查语法与 `typeof fetchNotices === "function"`，5 秒超时）。

---

## 3. 条目字段

```js
{
  id: util.sha256(detailUrl),   // 建议：detail_url 的 sha256（与后端一致，保证去重稳定）
  label: "教务信息",             // 栏目，会进入资讯流的标签页
  title: "关于……的通知",         // 标题
  date: "2026-09-04",           // 建议 yyyy-MM-dd；用于排序、日报归日、日期目录
  detail_url: "https://…",      // 必填
  is_page: true,                // true=网页正文；false=附件直链（如 .pdf）
  content_text: "正文纯文本",     // 可为 null（例如仅校内可访问）
  attachments: ["https://…pdf"] // 可为 null
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 唯一标识；留空时宿主退化为使用 `detail_url` |
| `label` | string | 留空时宿主记为「资讯」 |
| `title` | string | 标题 |
| `date` | string | 用于解析排序键（支持 `yyyy-MM-dd`、`yyyy-MM-dd HH:mm[:ss]`、ISO）；无法解析时排序键为 0 |
| `detail_url` | string | 官方链接，详情页会打开它 |
| `is_page` | bool | 决定详情页是否展示正文/是否算「需要正文」 |
| `content_text` | string? | 正文纯文本；为 null 时该条目**每次抓取都会重试补正文** |
| `attachments` | string[]? | 附件直链（App 不下载、不解析附件内容） |

---

## 4. 注入的全局对象

> ⚠️ 不要用同名局部变量遮蔽这些全局名（例如 `var dom = ...`、`var http = ...`）。
> 遮蔽后调用桥方法会报 `xxx is not a function`。

### `http` —— 同步 HTTP（阻塞）

| 方法 | 说明 |
| --- | --- |
| `http.get(url)` | GET，返回响应体字符串 |
| `http.post(url, body, contentType)` | POST，返回响应体字符串 |

- 自动跟随重定向；**非 2xx 抛异常**（`HTTP <code> <url>`），建议用 `try/catch` 包住并按需计入失败。
- 每次调用都会计入沙箱的 HTTP 次数与下载字节数（按字符数计）。
- 域名必须命中 `@domains` 白名单，否则抛「域名不在白名单」。

### `dom` —— HTML 解析（基于 Jsoup）

| 方法 | 说明 |
| --- | --- |
| `dom.query(html, selector)` | 返回 **JSON 字符串**，需 `JSON.parse`；元素形如 `{ tag, text, html, attrs: {…} }` |
| `dom.text(html, selector)` | 第一个匹配元素的文本（`""` 表示没匹配到） |
| `dom.attr(html, selector, name)` | 第一个匹配元素的属性值 |
| `dom.markdown(html, selector, baseUrl)` | 第一个匹配元素的 **Markdown**（保留标题/段落/列表/表格/加粗/链接，相对链接按 `baseUrl` 转绝对；没匹配到返回 `""`） |

- 选择器是 **CSS / Jsoup 选择器**（支持 `tr:has(td.main)`、`a[title]`、`li.news` 等）。
- 对行片段（`<tr>` / `<td>` / `<th>` 开头）会自动包一层 `<table>` 再解析，可直接对单行查 `td.main`。

### `util` —— 工具

| 方法 | 说明 |
| --- | --- |
| `util.sha256(text)` | 十六进制 SHA-256，用于生成稳定的 `id` |
| `util.resolveUrl(base, relative)` | 按 base 解析相对地址为绝对地址 |
| `util.now()` | 当前时间戳（**Double** 毫秒；桥不支持 Java `long`） |

### `params` —— 运行参数

| 方法 | 说明 |
| --- | --- |
| `params.crawlDaysGap()` | 抓取回溯天数（设置 → 资讯数据源 → 抓取回溯天数） |
| `params.crawlCutoffDate()` | 回溯截止日期 `yyyy-MM-dd`；早于它的条目可以跳过 |
| `params.knownIdsJson()` | 该数据源**已有正文**（或 `is_page=false`）的条目 id，JSON 数组字符串 |

`knownIdsJson()` 的语义很关键：**正文为空的条目不在其中**，因此会被重新尝试，
便于在能访问正文的网络环境（如校内）下自动补全。建议：

```js
var known = JSON.parse(params.knownIdsJson());
var knownSet = {};
for (var k = 0; k < known.length; k++) knownSet[known[k]] = true;
// …
if (knownSet[id]) continue;   // 已有正文，跳过详情抓取
```

### `console`

`console.log(msg)` —— 输出到 App 日志（logcat 标签 `ScriptHost`）。

### `report` —— 运行结果上报

| 方法 | 说明 |
| --- | --- |
| `report.stats(scanned, skipped, failed, noContent, restricted, skippedOld, skippedDuplicate, skippedKnown)` | 本次扫描统计，会显示在「最近运行」里 |
| `report.warn(message)` | 一条不影响整体完成的警告（最多保留 20 条，单条截断到 200 字符） |
| `report.progress(scanned, skipped, failed, fraction, detail)` | 增量进度（建议每翻一页调一次）；`fraction` 是本次运行的**整体完成比例 0..1**（脚本自己估算），App 据此画更细粒度的进度条 |

`stats` 各字段：

| 字段 | 含义 |
| --- | --- |
| `scanned` | 扫描到的行数 |
| `skipped` | 跳过总数（= 超出回溯窗口 + 重复 + 已入库） |
| `failed` | **真失败**（列表页不可访问、行缺日期、栏目解析不出条目等） |
| `noContent` | 标题与链接已入库、但正文没抓到（校内限制、正文为空等） |
| `restricted` | 其中因「仅校内 IP 可访问」而暂缺的条数（**仍会入库**，不计为失败） |
| `skippedOld` / `skippedDuplicate` / `skippedKnown` | 跳过明细：超出回溯窗口 / 重复 / 已入库 |

- 运行结果会以「首行摘要 + 逐条 `· 警告`」的多行文本保存（上限 2000 字符），
  列表页只显示首行，数据源属性页的「最近运行」可点开查看完整内容。

---

## 5. 沙箱与限制

单次运行（`SourceRunner`）：

| 项 | 值 |
| --- | --- |
| 执行超时 | 240 秒 |
| HTTP 调用次数 | 600 |
| 下载字节数 | 32 MB（按字符数累计） |
| 域名 | `@domains` 白名单 |
| 并发 | 单线程串行；同一进程内多个数据源依次执行 |
| 静态校验超时 | 5 秒 |

超出限制会抛异常 → 本次抓取记为**致命失败**，结果不入库，运行结果里会写明原因。

脚本自身建议设置的上限（内置脚本的做法）：

```js
var MAX_DETAILS_PER_RUN = 120;   // 单次最多抓多少条正文，剩下的下次继续
var MAX_PAGES = 30;              // 每个栏目最多翻多少页
```

---

## 6. 校验规则（导入 / 保存时）

- **导入**（设置 → 资讯数据源 → 从文件导入）：读取文件 → 静态校验 → 注册；
  读取失败、语法错误、未定义 `fetchNotices`、缺少 `@id` 都会以 Toast 提示原因。
- **保存脚本**（数据源属性页 →「编辑脚本」全屏编辑器）：保存前同样先静态校验。
- **内置脚本**的修改会写成**覆盖版本**（`filesDir/sources/<id>.js`），此后不再随内置更新；
  属性页提供「恢复内置脚本」丢弃覆盖版本。

---

## 7. 完整示例

```js
// @id demo
// @name 示例站点
// @version 1.0.0
// @schedule 360
// @domains example.com
// @labels 通知

var HOST = "https://example.com";
var BODY_SELECTOR = ".article-content";
var MAX_DETAILS_PER_RUN = 120;

function fetchNotices() {
  var cutoff = params.crawlCutoffDate();
  var known = JSON.parse(params.knownIdsJson());
  var knownSet = {};
  for (var k = 0; k < known.length; k++) knownSet[known[k]] = true;

  var listHtml = http.get(HOST + "/list.htm");
  var rows = JSON.parse(dom.query(listHtml, "li.item"));
  var out = [];
  var details = 0;
  var scanned = 0;
  var skipped = 0;
  var failed = 0;
  var skippedOld = 0;
  var skippedKnown = 0;

  for (var i = 0; i < rows.length; i++) {
    var links = JSON.parse(dom.query(rows[i].html, "a[title]"));
    if (!links.length) continue;
    scanned++;

    var dateMatch = rows[i].text.match(/\d{4}-\d{2}-\d{2}/);
    if (!dateMatch) { failed++; continue; }
    if (dateMatch[0] < cutoff) { skipped++; skippedOld++; continue; }

    var url = util.resolveUrl(HOST, links[0].attrs.href);
    var id = util.sha256(url);
    if (knownSet[id]) { skipped++; skippedKnown++; continue; }
    if (details >= MAX_DETAILS_PER_RUN) break;

    var content = null;
    try {
      var page = http.get(url);
      content = dom.text(page, BODY_SELECTOR) || null;
      details++;
    } catch (e) {
      failed++;
      report.warn("详情失败：" + url + "（" + e + "）");
      continue;              // 不入库，下次重试
    }

    out.push({
      id: id,
      label: "通知",
      title: links[0].attrs.title,
      date: dateMatch[0],
      detail_url: url,
      is_page: true,
      content_text: content,
      attachments: null
    });
  }

  report.stats(scanned, skipped, failed, 0, 0, skippedOld, 0, skippedKnown);
  return out;
}
```

---

## 8. 常见坑

| 现象 | 原因 / 处理 |
| --- | --- |
| `xxx is not a function` | 用局部变量遮蔽了桥名（如 `var dom`、`var http`），换个变量名 |
| 抓不到任何条目 | 选择器与站点结构不匹配；导入/保存时只能查语法，抓取结果要靠 `report.warn` 定位 |
| 条目重复出现 | `id` 不稳定（例如用了带时间戳/参数的 URL）；统一用 `util.sha256(detail_url)` |
| 正文一直是空 | 站点限制（如仅校内可访问）或正文选择器失效；看「最近运行」里的警告 |
| 抓取很慢/超时 | 单次正文条数太多；用 `MAX_DETAILS_PER_RUN` 分批，靠 `knownIdsJson` 跳过已抓条目 |
| 日期排序不对 | `date` 不是 `yyyy-MM-dd` 之类可解析格式，排序键会退化为 0 |

---

## 9. 与后端爬虫的关系

后端（`openjwc_webapi_golang`）的内置适配器（`internal/service/crawler/site.go`、`ingest.go`、`run.go`）
与本格式语义一致：行匹配规则、`id = sha256(detail_url)`、附件扩展名、正文选择器、
`list.htm → list2.htm` 翻页与 `.all_pages` 末页判定、`crawler_days_gap` 回溯窗口等。
内置脚本即按该行为复刻，并额外做了两件事：

1. 用 `knownIdsJson()` 跳过已抓正文的条目（后端每次都重抓全部正文）；
2. 把「仅校内 IP 可访问」单独识别为 `restricted`，保留标题与链接、正文待补。

后端另外支持一套**外部爬虫 NDJSON 协议**（管理员安装本机可执行文件，stdin/stdout 事件流），
客户端未复刻该协议，侧载脚本请使用本文档的 `fetchNotices()` 契约。
