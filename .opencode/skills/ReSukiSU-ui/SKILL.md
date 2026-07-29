---
name: ReSukiSU-ui
description: Use when building Android apps with Material 3 Expressive (M3E) design style. Covers ReSukiSU's component library, theme system, predictive back animations, settings widgets, navigation architecture, blur/glass morphism, and SegmentedColumn patterns. Reference the actual source files at app/src/main/java/org/openjwc/client/ui/ for working examples.
---

# ReSukiSU M3E UI Architecture

## Core Design Philosophy

Every screen follows the same layered structure:
1. **Theme layer** — `OpenJWCClientTheme` renders `BackgroundLayer` (custom wallpaper + dim overlay) at the root
2. **NavContainer layer** — `NavEntryDecorator` wraps each page in a `Box` with `.background(surfaceContainer)` for solid fallback
3. **Screen layer** — `Scaffold(containerColor = Color.Transparent)` with `LargeFlexibleTopAppBar`
4. **Content layer** — `Column(Modifier.fillMaxSize().nestedScroll(...).verticalScroll(...).padding(innerPadding))` with `SegmentedColumn` blocks

See `app/src/main/java/org/openjwc/client/ui/` for live implementations.

---

## Theme System (`ui/theme/`)

### `Theme.kt` (1430 lines)
- `OpenJWCClientTheme(dpi, darkTheme, dynamicColor, content)` — root composable
- `ThemeConfig` — `@Stable object` with `mutableStateOf` properties for all reactive theme state
  - `forceDarkMode: Boolean?` — null=system, true=dark, false=light
  - `seedColor: Int`, `useDynamicColor: Boolean`
  - `customBackgroundUri: Uri?`, `backgroundDim: Float`
  - `isEnableBlur`, `isEnableBlurExp`, `isHighContrastMode`
  - Add new reactive state with `var mySetting by mutableStateOf(default)`
- `ThemeManager` — save/load helpers using `context.appPreferences` (SharedPreferences wrapper)
- `BackgroundManager` — wallpaper persistence, blur cache management
- `SystemBarController` — `enableEdgeToEdge()` integration
- `MonetCompatInitializer` — for pre-Android 12 dynamic color fallback
- `BackgroundLayer` — renders wallpaper + dim overlay at theme root
- `generateTypography()` — applies high-contrast text shadows to all typography styles
- `createColorScheme()` — seed color logic: dynamic > monetCompat > manual seed

### `CardConfig.kt`
- `@Stable object CardConfig` — global card transparency manager
- `cardAlpha: Float` — applied as `.copy(alpha = CardConfig.cardAlpha)` to surface colors
- `setThemeDefaults(isDark)` — auto-sets 0.88f alpha in dark mode

### `Color.kt`
- `ThemeSeedColors` — preset seed colors (Default, Green, Purple, etc.)
- `DarkThemeStyle` enum — `Auto, Light, Dark`
- `ColorItem` composable — circular color swatch with check mark

### `AppPreferences.kt` (`data/`)
- `Context.appPreferences` extension returning synchronous shared-pref wrapper
- Theme settings use this for persistence; other settings use `SettingsDataSource` (DataStore)

---

## Navigation System (`navigation3/`)

### `Navigator.kt`
```kotlin
class Navigator(initialKey: NavKey) {
    val backStack: SnapshotStateList<NavKey>
    fun push(key: NavKey)     // ignore duplicate top
    fun pop()                  // debounced (100ms)
    fun replace(key)           // replace top
    fun replaceAll(keys: List)
    fun popUntil(predicate)
    fun navigateForResult(route, requestKey)
    fun <T> setResult(requestKey, value: T)
    fun <T> observeResult(requestKey): SharedFlow<T>
}
```
- Uses `rememberSaveable(saver = Navigator.Saver)` for process death survival
- Exposed via `LocalNavigator` CompositionLocal

### `Screen.kt`
```kotlin
sealed interface Screen : NavKey, Parcelable {
    @Parcelize @Serializable data object Main : Screen
    @Parcelize @Serializable data object Settings : Screen
    // ...
}
enum class MainTab(val titleRes: Int, val iconSelected: ImageVector, val iconNotSelected: ImageVector) {
    Chat, News, Timetable, Me
}
```
- All routes are `@Parcelize @Serializable` for Navigation3 state saving

### `NavContainer.kt`
- Central navigation hub (~300 lines)
- Creates ALL shared ViewModels at Activity scope: `MainViewModel, ChatViewModel, NewsViewModel, TimetableViewModel, SettingsViewModel, AuthViewModel`
- Passes VMs to entries via `entryProvider { entry<Screen.Xxx> { XxxScreen(navigator, vm1, vm2) } }`
- Predictive back animation: reads from `ThemeConfig.predictiveBackAnimation`
- Blur backdrop: `rememberMaterial3BlurBackdrop(ThemeConfig.isEnableBlur)`
- Page decorator pattern:
```kotlin
NavEntryDecorator { content ->
    Box(
        modifier = Modifier.fillMaxSize().imePadding()
            .predictiveBackAnimationDecorator(gestureState?.transitionState, content.contentKey, navigator.current())
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) // solid bg child = scales with animation
        CompositionLocalProvider(LocalBlurState provides blurState, LocalSnackbarHost provides snackBarHostState) {
            backgroundImagePainter?.let { /* painter Box */ }
            content.Content()
        }
    }
}
```

---

## Predictive Back Animations (`ui/animation/predictiveback/`)

### Interface
```kotlin
interface PredictiveBackAnimationHandler {
    suspend fun onBackPressed(transitionState, currentPageKey)
    fun onPagePop(contentPageKey, animationScope)
    fun Modifier.predictiveBackAnimationDecorator(transitionState, contentPageKey, currentPageKey): Modifier
    fun onPredictivePopTransitionSpec(swipeEdge): ContentTransform
    fun onPopTransitionSpec(): ContentTransform
    fun onTransitionSpec(): ContentTransform
}
```

### Five variants:
| Class | Behavior |
|---|---|
| `AOSPCrossActivityAnimation` | Scale + translate exit, `CubicBezierEasing`, device corner clipping |
| `ScalePredictiveBackAnimation` | Scale to 0.85x, touch-pivot-based, dim overlay on bottom page |
| `KernelSUClassicPredictiveBackAnimation` | Simple slide + scale transitions |
| `MiuixPredictiveBackAnimation` | Delegates to Navigation3 default transitions |
| `NoPredictiveBackAnimation` | `BackHandler` intercepts, no gesture |

### PredictiveBackExitDirection enum
`FOLLOW_GESTURE`, `ALWAYS_RIGHT`, `ALWAYS_LEFT`

### Bundled NavigationEvent classes
ReSukiSU bundles patched `androidx.navigationevent.compose.*` classes directly in source tree (`androidx/navigationevent/compose/`). The published library is excluded via:
```kotlin
configurations.all { exclude(group = "androidx.navigationevent", module = "navigationevent-compose") }
```
This provides `NavigationBackHandler(state, isBackEnabled, onBackCompleted: (() -> Unit) -> Unit, onBackCancelled: (() -> Unit) -> Unit)` where `onBackCompleted` receives a callback that must be called AFTER animations complete.

---

## Settings Widget System (`ui/component/settings/`)

### `SettingsBaseWidget` (546 lines)
- Foundation composable wrapping Material3 `ListItem`
- Parameters: `icon`, `iconColor`, `title`, `description`, `descriptionColor`, `descriptionStyle`, `enabled`, `isError`, `selected`, `renderBackgroundBlur`, `onClick((Offset)->Unit)?`, `onLongClick`, `clickHaptic`, `leadingContent`, `foreContent`, `descriptionColumnContent`, `containerColor`, `trailingContent`
- Reads `LocalSegmentedItemShape` for corner radius from parent `SegmentedColumn`
- Applies `CardConfig.cardAlpha` to background colors
- Animated shape transitions via `rememberAnimatedShape` from `material3internal/AnimatedShape.kt`

### `SegmentedColumn`
- DSL-style vertical layout: `SegmentedColumn(title) { item { ... } }`
- Handles first/last item rounded corners (16dp outer, 5dp inner)
- Uses `AnimatedVisibility(enter = expandVertically + fadeIn, exit = shrinkVertically + fadeOut)` with spring animation (damping 0.5f, stiffness 800f)
- `LocalSegmentedItemShape` provides `RoundedCornerShape` to children

### `LazySegmentedColumn`
- LazyListScope extension: `lazySegmentColumn(items, title, noHorizontalPadding, key, contentType) { index, item -> ... }`
- Provides `LocalSegmentedItemShape` to each item with proper corner shapes

### Specialized widgets (all built on `SettingsBaseWidget`):
| Widget | Purpose |
|---|---|
| `SettingsSwitchWidget` | Switch with haptic toggle feedback, Check/Close icons |
| `SettingsChooseWidget` | Single-select (RadioButton dialog) or Multi-select (Checkbox dialog) |
| `SettingsDropdownWidget` | Dropdown menu positioned at touch point |
| `SettingsJumpPageWidget` | Navigation item with trailing ChevronRight icon |
| `SettingsTextFieldWidget` | `BasicTextField` with floating label animation, error state, focus handling, `readOnly` and `onClick` modes |
| `AppBackButton` | Standardized back button: `AppBackButton(onClick = { navigator.pop() })` |

---

## Screen Patterns

### Full screen (phone / push navigation):
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XxxScreen(navigator: Navigator, sharedViewModel: XxxViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Title") },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            SegmentedColumn(title = "Section") {
                item { SettingsBaseWidget(...) {} }
                item { SettingsSwitchWidget(...) }
                item { SettingsChooseWidget(...) }
            }
        }
    }
}
```

### Inline content (tablet right pane):
```kotlin
@Composable
fun XxxContent(navigator: Navigator, sharedViewModel: XxxViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SegmentedColumn(title = "Section") {
            item { /* same settings widgets */ }
        }
    }
}
```

### Dual-pane Settings pattern:
```
Column(Modifier.fillMaxSize()) {
    LargeFlexibleTopAppBar(title = "Settings", navigationIcon = back)
    Row(Modifier.fillMaxWidth().weight(1f)) {
        Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll()) {
            SegmentedColumn { item { SettingsBaseWidget(selected = selectedPage == page, onClick = { selectedPage = page }) {} } }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (selectedPage) {
                Screen.Xxx -> XxxContent(navigator, vm)
                null -> { /* placeholder */ }
            }
        }
    }
}
```
- `isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact`
- Pass `settingsViewModel, authViewModel, newsViewModel, timetableViewModel` as nullable from NavContainer

---

## NavigationBar & MainScreen

### `NavigationBar.kt` (in `ui/main/`)
```kotlin
@Composable
fun MainNavigationBar(isBottomBar: Boolean) {
    val tabs = MainTab.entries
    val page = LocalSelectedPage.current
    val handlePageChange = LocalHandlePageChange.current
    if (isBottomBar) {
        FlexibleBottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(CardConfig.cardAlpha)
        ) { tabs.forEachIndexed { ... } }
    } else {
        WideNavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(CardConfig.cardAlpha)
        ) { tabs.forEachIndexed { ... } }
    }
}
```

### `MainScreen.kt`
- `HorizontalPager` replaced by `Box` + conditional rendering (avoid intrinsic measurement bugs)
- Tab state via `mainViewModel.currentTab` (StateFlow), `selectedPage = tabs.indexOf(currentTab).coerceAtLeast(0)`
- `ModalNavigationDrawer` for Chat tab (chat history sessions)
- `TopAppBar` with tab-specific title + hamburger menu (Chat) or action buttons (News)

---

## Common Components (`ui/component/`)

### `Dialog.kt`
- `rememberLoadingDialog()` — returns `LoadingDialogHandle`
- `rememberConfirmDialog(onConfirm, onDismiss)` — returns `ConfirmDialogHandle` with `awaitConfirm()` coroutine API

### `SwipeableSnackbarHost.kt`
- Wraps `SnackbarHost` in `SwipeToDismissBox`

### `WarningCard.kt`
- Uses `SettingsBaseWidget` with `errorContainer` color + close button

---

## Chat System

### `ChatInputBar`
- Surface with `RoundedCornerShape(20.dp)` (all corners rounded)
- Attach button → `ModalBottomSheet` (news attachment picker)
- Send button: `Surface(shape = CircleShape, color = primary)`
- Attachment chips above text field: FlowRow with close buttons

### `MessageBubble`
- User messages: plain text + attachment titles (`AttachFile` icon + labelSmall)
- AI messages: `MarkdownText` rendering
- Loading spinner when generating
- Dropdown menu: Copy, Share, Delete

---

## IME Handling

- Manifest: `android:windowSoftInputMode="adjustResize"`
- NavContainer decorator Box: `.imePadding()` at the outermost level (all sub-pages auto-avoid keyboard)
- ChatMainContent: additional `.consumeWindowInsets(contentPadding)` for nested scaffold handling
- **Never** put `.imePadding()` on individual input components (causes double-padding)

---

## ViewModel Architecture

- All shared ViewModels created ONCE in `NavContainer` at Activity scope
- Passed to entries via `entryProvider` lambdas: `entry<Screen.Xxx> { XxxScreen(navigator, sharedViewModel) }`
- Each sub-screen receives ViewModels as parameters — NEVER creates them internally
- Repositories created via `remember { AppDatabase.getDatabase(context) }` etc.

---

## Key Imports Pattern
```kotlin
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import org.openjwc.client.ui.component.settings.*
import org.openjwc.client.ui.theme.*
```

---

## build.gradle.kts Essentials
```kotlin
// Exclude published navigationevent-compose (use bundled version)
configurations.all { exclude(group = "androidx.navigationevent", module = "navigationevent-compose") }

// Required opt-ins
freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")

// Manifest override for miuix-blur minSdk
// <uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur" />
```
