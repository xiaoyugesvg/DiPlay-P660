# GitHub Actions 编译 DiPlay（日产 P660 / P760）

1. 把整个源码目录上传到 GitHub 仓库。
2. 确认 `.github/workflows/android.yml` 存在。
3. 打开 GitHub 仓库 → Actions → `Build DiPlay P660 APK`。
4. 点击 `Run workflow`。
5. 构建成功后打开该次运行页面，在 `Artifacts` 下载 `DiPlay-P660-standalone-debug`。

本工作流编译的是 `:mobile:assembleStandaloneDebug`，不需要 Android 签名密钥，也不需要你提供任何认证文件。

## MFi 身份从哪来

CarPlay 认证需要的 `assets/offline-mfi/identity.pk8` 和 `certificate.p7b` 刻意不入库（`.gitignore` 忽略、`scripts/check_public_tree.py` 会拦、`rejectBundledCredentials` 会拒绝）。工作流在构建前会自动：

1. 下载上游 release 的 `DiPlay-0.2.21.apk`；
2. 用同目录的 `SHA256SUMS.txt` 校验；
3. 只解出 `assets/offline-mfi/` 这两个文件到 runner 的临时目录；
4. 通过 `DIPLAY_AUTH_ASSETS_DIR` 注入构建。

文件只存在于 runner 临时目录和最终 APK 里，不会提交进仓库，也不会作为独立产物上传。升级上游版本时改工作流顶部的 `DIPLAY_RELEASE_VERSION` 即可。

项目当前配置：AGP 9.3.0 / Gradle 9.5.0 / JDK 17 / compileSdk 37。
