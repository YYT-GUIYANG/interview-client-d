# 验证与交付记录

## 已完成（本仓库可复核）

| 项 | 证据 | 状态 |
| --- | --- | --- |
| F01–F12 核心规则 | `bash core/run-check.sh` → `CoreCheck: all checks passed` | 已验证 |
| 题目配置接入 | `android-app/app/src/main/assets/question_config.json` | 已编码 |
| 本地真实适配器 | `LocalSherpaAdapter.kt` + sherpa Kotlin JNI 包装 | 已编码；真机识别待验证 |
| 远程协议适配 | `RemoteAdapter.java` + `samples/remote-adapter/`；`check_protocol.py` 回环测试通过 | 控制帧/音频/Done 样例已验证；手机对 B 待验证 |
| 身份边界 | `IdentityGateway` / `account_id` 过滤 | 本机测试身份已编码；正式认证待 C |
| B 电脑端映射 | `PYTHONPATH=B_电脑识别服务 B_电脑识别服务/.venv/bin/python -m unittest discover -s B_电脑识别服务/tests -v` | 3 项通过；独立于 D 手机联调 |
| CI | `.github/workflows/android-ci.yml` | `f65b4d1` 已成功；本轮改动待新运行 |
| 第 8 节 / 图 | `第8节_安卓客户端组件设计.md`、`图8-1_组件依赖.puml` | 已与代码对齐更新 |

## 上游固定

见 `SHERPA_ONNX_COMMIT.txt`。预编译库版本：`v2.0.1`。模型：`sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23`。

## 待真机（OPPO / A·B）

- OPPO 安装、麦克风授权、录屏
- 本地路径真实语音识别
- 远程路径对接 B 的 `wss://` 与 `answer_complete`
- D-T1～D-T7 全表手机勾选
- APK SHA-256 安装退出码
- 官方 `android/SherpaOnnx` 独立工程编译与安装
- C 的正式账号认证、记录所有权及删除契约

无证据时保持「待验证」，不以模拟路径冒充识别通过。


## CI 成功记录（已取得）

详见 `docs/CI_BUILD_RECORD.md`。

- CI：https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43191822623 （仅对应 `f65b4d1`）
- APK Artifact：https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43191822623/artifacts/13931286950
- APK SHA-256：`6e879a86e02d8058684bab42714225127e38421729d650afaffdda14f669674d`（109M）
- CoreCheck 与 assembleDebug 均已通过

本机尝试下载旧 Artifact 时，签名下载链接过期；GitHub 公共 API 随后触发限流，因此本地没有可核验的 APK 文件。上表哈希来自成功 CI 日志，不应描述为当前源码产物。
