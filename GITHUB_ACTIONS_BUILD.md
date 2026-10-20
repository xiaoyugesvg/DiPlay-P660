# GitHub Actions 编译 DiPlay（日产 P660 / P760）

1. 把整个源码目录上传到 GitHub 仓库。
2. 确认 `.github/workflows/android.yml` 存在。
3. 打开 GitHub 仓库 → Actions → `Build DiPlay P660 APK`。
4. 点击 `Run workflow`。
5. 构建成功后打开该次运行页面，在 `Artifacts` 下载 `DiPlay-P660-debug`。

本工作流编译的是 `:mobile:assembleDebug`，不需要 Android 签名密钥，也不需要提供任何认证文件。

项目当前配置：AGP 9.3.0 / Gradle 9.5.0 / JDK 17 / compileSdk 37。
