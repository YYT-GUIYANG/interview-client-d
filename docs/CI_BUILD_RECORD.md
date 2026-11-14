# CI 构建记录

## 当前源码对应成功构建（请用这一条装真机）

| 项 | 链接 / 值 |
| --- | --- |
| 仓库 | https://github.com/YYT-GUIYANG/interview-client-d |
| 成功 CI | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43348788191 |
| APK Artifact | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43348788191/artifacts/14002991886 |
| 源码提交 | `d2d658e271c8377e8d81ece6d113ce4641f67a82` |
| Artifact 名 | `interview-client-d-artifacts` |
| Artifact ID | `14002991886`（约 113471026 bytes） |
| Artifact zip SHA-256 | `5f2787546a7f69e7caeff0ec0fc08a69a7ef51feb98fe7892e7f51a881ed6a49` |

### 构建结果

- CoreCheck: `all checks passed`
- `:app:assembleDebug`: BUILD SUCCESSFUL
- APK 路径（CI）：`android-app/app/build/outputs/apk/debug/app-debug.apk`
- APK 大小：109M
- **APK SHA-256：`4c64c717167d7c5244de2bdf44d823caa7250581c452e07683087552e835ae25`**

## 上游与模型（与本次 CI 一致）

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

## 历史成功构建（旧哈希，勿用于当前源码安装）

| 提交 | CI | APK SHA-256 |
| --- | --- | --- |
| `f65b4d1…` | [43191822623](https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43191822623) | `6e879a86e02d8058684bab42714225127e38421729d650afaffdda14f669674d` |

## 真机待办

见 `docs/DEVICE_ACCEPTANCE.md`、`docs/ABC_HANDOFF.md`。下载须登录 GitHub；`git clone` 不含 APK。

## 远程 WSS 联调构建（2026-11-14）

| 项 | 值 |
| --- | --- |
| 提交 | `90699381049039dc832ae0e639f453b0edece745` |
| CI | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43552908036 |
| Artifact | https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43552908036/artifacts/14096559884 |
| APK SHA-256 | `d87e1598345cf42458fb3bf71cc5829d0add5b0058b2b8313a6c86061cca16bc` |
| 变更 | RemoteAdapter Bearer + 联调自签信任；UI 令牌框 |

安装包本地副本：`D_安卓客户端/docs/evidence/app-debug-wss-lab.apk`（勿与令牌一并公开传播）。

