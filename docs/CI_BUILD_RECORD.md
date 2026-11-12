# CI 构建记录（2026-11-12）

## 链接

| 项 | 链接 |
| --- | --- |
| 仓库 | https://github.com/YYT-GUIYANG/interview-client-d |
| 成功 CI | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43191822623 |
| APK Artifact | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43191822623/artifacts/13931286950 |
| 源码提交 | `f65b4d13ebdebf3dd65f3c3749defe65b5a352de` |

## 构建结果

- CoreCheck: `all checks passed`
- `:app:assembleDebug`: BUILD SUCCESSFUL
- APK 路径（CI）：`android-app/app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：109M
- APK SHA-256：`6e879a86e02d8058684bab42714225127e38421729d650afaffdda14f669674d`
- Artifact zip SHA-256：`7588aaabe80edf03b2a04312a6e2480333e1eaa042bc344f50f51cf5a7738bfe`
- Artifact ID：`13931286950`（约 113469753 bytes）

## 上游与模型

- sherpa-onnx 提交：`52b705f6db8db2f2c2e1af024d492c01528280b1`
- 预编译 JNI：`sherpa-onnx-v2.0.1-android.tar.bz2`
  - SHA-256：`39003753ec293425ed56723ea9b972b98841a490af3311670c3060548d654330`
- 模型包：`sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2`
  - SHA-256：`2cbd71b640d9c37d3784f29367333a4577b0398b62e9deeed418170b081cba8b`
- 所需文件：
  - encoder int8：`1c556ea57cec304e55ec4b72e52c1cc098bb01476ed7d90f3de939fe126487b1`
  - decoder：`5ee0f03a2768ff1d5c83ef3a493243c7935d316cd41280037b14783a3467cc78`
  - joiner int8：`a7cf9d82757bdcf786059454495a9ca95e4bd7347f72473fc08d794475c36169`
  - tokens：`8b294db9045d6e5f94647f4c1eec1af4da143a75053c399611444b378ff966ac`

## 工具版本（CI）

- JDK 17（Temurin）
- Gradle 8.9 / AGP 8.7.3
- Android SDK platforms;android-35、build-tools;35.0.0

## 真机待办（无证据保持待验证）

1. 从 Artifact 下载 APK，核对 SHA-256 后 `adb install -r`
2. OPPO 授权麦克风；本地路径真实语音
3. 远程 `wss://` 对接 B；确认 `answer_complete`
4. 勾选 D-T1～D-T7；录屏交给组长
