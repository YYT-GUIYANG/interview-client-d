# 组员 D · 安卓面试转写客户端

可编译安卓工程：题目配置、AnswerSession/关键词规则、真实麦克风、本地 sherpa-onnx 适配器、远程 WSS 适配器、加密测试档案与本机测试身份边界。

## 上游与工具记录

| 项 | 值 |
| --- | --- |
| sherpa-onnx 源码提交 | 见 `SHERPA_ONNX_COMMIT.txt` / `docs/sherpa-onnx-upstream.txt` |
| 官方示例路径 | `android/SherpaOnnx` |
| 预编译 JNI | `sherpa-onnx-v2.0.1-android.tar.bz2`（CI/脚本拉取） |
| 中文模型 | `sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23`（type 9 int8 encoder/joiner） |
| AGP / Gradle / JDK | 8.7.3 / 8.9 / 17 |
| compileSdk | 35 |

## 本地构建

```bash
bash core/run-check.sh
bash scripts/prepare-native-assets.sh
cd android-app
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## CI

`.github/workflows/android-ci.yml`：核心规则检查 + Debug APK，上传 APK 与 SHA-256/日志。

## 路径说明

- **模拟**：仅界面/事件规则，不算识别证据。
- **远程**：PCM16→float32 LE，结束发 `Done`；需 `wss://`。样例见 `samples/remote-adapter/`。
- **本地**：`LocalSherpaAdapter` 调 sherpa-onnx JNI，需模型 assets 与 jniLibs。真机联调仍为待验证。

## 身份与 C 待确认

`IdentityGateway` / `LocalTestIdentityGateway` 仅为本机测试身份；`account_id` 过滤已编码。正式多用户认证待 C 冻结契约后接入。
