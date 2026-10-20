# DiPlay for HSAE 车机（日产 P660 / P760）构建说明

本分支把 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay) 的 **v0.2.21（2026-10-17）** 与本仓库原有的日产 HSAE 蓝牙适配重新合并。

## 与上游的差异

只保留日产 HSAE 平台相关的改动，其余全部跟随上游最新代码：

- `shared/src/main/aidl/com/hsae/bluetoothsdk/`：HSAE `IBluetoothManager` AIDL 声明，以及它使用的 `BluetoothDevice` Parcelable。
- `shared/src/main/java/com/shilapi/xcertplay/NissanP170Bluetooth.kt`：通过 `com.hsae.bluetoothservice` 读取已配对手机，并在打开 iAP2 RFCOMM 之前调用车厂自己的 `connect` / `connectA2dp` / `connectHfp`。
- `shared/.../orchestration/CarPlayController.kt`：`selectWirelessBluetoothDevice()` 在 Android `bondedDevices` 为空时改用 HSAE 的配对列表，并在每个返回点调用 `prepareForCarPlay()`。
- `common/.../DiPlayActivity.kt`：手动选择 iPhone 的对话框同样读取 HSAE 配对列表。
- `shared/src/main/AndroidManifest.xml`：`<queries>` 增加 `com.hsae.bluetoothservice`（Android 11+ 绑定所需，Android 9 无影响）。
- `shared/build.gradle`：上游已经打开 `aidl true`，无需额外配置。

非 HSAE 车机仍然走原来的 Android 蓝牙路径，因此这份代码可以直接替换上游使用。

## 中文界面

DiPlay 自带简体中文（`values-zh-rCN`），0.2.21 的 1115 条字符串里除 4 条 `translatable="false"` 的 FPS 占位符外全部已有中文，不需要额外汉化补丁。

车机系统语言是中文时默认显示中文；也可以在 DiPlay 的 **设置 → 语言** 里手动选“简体中文”。

## 构建

本机没有 Android SDK / Gradle 环境时，用仓库自带的 GitHub Actions：

1. 把源码推到 GitHub 仓库。
2. Actions → `Build DiPlay P660 APK` → `Run workflow`。
3. 构建完成后在该次运行页面下载 `DiPlay-P660-standalone-debug` 产物。

本工作流跑的是 `:mobile:assembleStandaloneDebug`，会先下载上游 release APK、用 `SHA256SUMS.txt` 校验，再从中取出 `assets/offline-mfi/identity.pk8` 与 `certificate.p7b`，经 `DIPLAY_AUTH_ASSETS_DIR` 注入构建。仓库本身不保存这两份凭据。

本地构建：JDK 17 + Android SDK（platform 37、build-tools 36.0.0、NDK 28.2.13676358），准备好存放 `offline-mfi/` 的目录后

```
DIPLAY_AUTH_ASSETS_DIR=/absolute/path/to/runtime-assets ./gradlew :mobile:assembleStandaloneDebug
```

## 安装到车机

先把 iPhone 和车机原厂蓝牙（HSAE）配对，再打开 DiPlay → 无线 CarPlay → 选择 iPhone。

注意 P760 的 vendor 分区里有 Tbh 专用脚本：`tbh-bt-arbiter.sh` 只在 `com.tbh.carplay` 运行时把蓝牙控制器让给 Android 蓝牙栈，`tbh-carplay-hotspot.sh` 也会关掉车机 Wi-Fi station 来做 5GHz 热点。这些脚本不认 DiPlay，因此在这台车上 DiPlay 的无线 CarPlay 可能需要先停用 TbhCarPlay，或修改 vendor 脚本里的包名判断。
