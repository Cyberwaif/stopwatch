# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

一个极简 Android 秒表应用（包名 `com.hellozh.stopwatch`），单一 Activity、无第三方依赖、无测试。特殊之处在于：它同时被设计为**普通 Gradle 工程**和 **AOSP 源码树内的模块**，并且额外编译一个原生 C 可执行文件 `memstress`（内存压测工具，与秒表 UI 无功能关联）。

## 常用命令

```bash
./gradlew assembleDebug              # 调试包 → app/build/outputs/apk/debug/
./gradlew assembleRelease            # 发布包 → app/build/outputs/apk/release/
./gradlew clean                      # 清理
./gradlew externalNativeBuildDebug   # 只编译 memstress，输出到 app/src/main/jniLibs/arm64-v8a/
```

`local.properties`（SDK 路径）和代理配置在 `gradle.properties` 中，均为本地环境相关项。

AOSP 内编译（需先 `source build/envsetup.sh && lunch`）：

```bash
make Stopwatch     # 或 mma -j8（在项目目录内）
```

## 构建系统（两套并存，改动时需同步）

1. **Gradle**：AGP 8.5.1，compileSdk/targetSdk 33，minSdk 26，只打包 `arm64-v8a`。`app/build.gradle` 通过 `externalNativeBuild.cmake` 引入 `app/src/main/cpp/CMakeLists.txt`。
2. **AOSP `Android.mk`**（仓库根目录）：`LOCAL_PACKAGE_NAME := Stopwatch` 构建应用，同时用 `BUILD_EXECUTABLE` 编译 `native/memstress.c` 为可执行文件。**新增源文件/res/清单改动时，需确认两套构建都仍能覆盖**——`Android.mk` 用 `all-java-files-under` 自动收集 Java，但资源、清单和原生源文件是显式列出的。

CMake 的 `memstress` 目标把输出目录直接设为 `app/src/main/jniLibs/${ANDROID_ABI}`，因此编译产物会覆盖仓库中已提交的预编译 ELF（`app/src/main/jniLibs/arm64-v8a/memstress`，arm64 PIE 可执行文件）。改完 `native/memstress.c` 后需要重新生成并一并提交该二进制。

`memstress` 参数：`-m <目标MB>`、`-b <块KB>`、`-i <间隔ms>`、`--mmap`（用 mmap 而非 malloc）、`--infinite`（持续分配）。收到 SIGINT/SIGTERM 后释放内存退出。

## 代码结构

- `app/src/main/java/com/hellozh/stopwatch/StopwatchActivity.java` — 全部业务逻辑。用 `SystemClock.uptimeMillis()` + `Handler.postDelayed(mRunnable, 10)` 每 10ms 刷新，`mTimeSwapBuff` 累积已计时的毫秒数以支撑 Start/Stop/Resume。时间格式 `%02d:%02d.%03d`（分:秒.毫秒），分钟不截断，可超过 100 分钟。`onPause` 停掉刷新循环与背景动画，`onResume` 重建（计时基数不变，回到前台不丢时间）。
- `app/src/main/java/com/hellozh/stopwatch/RunnersView.java` — 全屏"跑动小人"层（用于在另一台相机前测试人脸检测）：4 个火柴人身体 + Lenna 头像（`res/drawable-nodpi/lenna_head.png`，256x256，正面、不旋转、不形变），撞墙反弹，`postOnAnimation` 驱动。头部大小取宽 1/4 与高 1/5 的较小值以利于检测。`start()/stop()` 由生命周期驱动，`setRunning()` 由 Runners 开关驱动，两者都满足才播放；关闭时隐藏，背景为纯黑/纯白。原色块跳变背景 `ToneBackgroundDrawable` 已下线（git 历史可查）。
- `app/src/main/res/layout/activity_stop_watch.xml` — 相对布局，底部有 "powered by Camera HAL" 标语；计时器 TextView 使用 autoSizeText。
- 主题切换：`applyTheme()` 以硬编码 ARGB 颜色设置文字/按钮颜色，并同步 `RunnersView.setDark()` 并按主题设置纯黑/纯白背景；计时数字带同背景色的 `setShadowLayer` 光晕，避免被小人干扰识别。字段 `mIsDarkTheme` 初始为 `false`（浅色）。无 `styles.xml`、无夜间模式资源；布局里控件默认色不参与主题，改 UI 时颜色统一写在 `applyTheme()` 里。新增按钮记得在这里补颜色，否则深浅切换时会漏掉。
- 开关：`Runners` 是原生 `android.widget.Switch`（与 Invert Colors 并排），控制 RunnersView 是否显示/运行。
- 全屏：`enterFullscreen()` 在 API 30+ 用 `WindowInsetsController`，旧版本用 `SYSTEM_UI_FLAG_IMMERSIVE_STICKY`；`onWindowFocusChanged` 里重新应用，以在用户划出系统栏后自动恢复。

## 注意

- 使用原生 `android.app.Activity` 而非 AppCompat/Fragment，不要引入 AndroidX 组件（`gradle.properties` 中的 `android.enableJetifier`/`useAndroidX` 仅为兼容预留，当前无依赖）。
- 布局文件中的 `tools:context=".StopWatchActivity"` 大小写与真实类名 `StopwatchActivity` 不一致（仅影响 IDE 预览）。
