# 验证与交付记录

## 已完成（本仓库可复核）

| 项 | 证据 | 状态 |
| --- | --- | --- |
| F01–F12 核心规则 | `bash core/run-check.sh` → `CoreCheck: all checks passed` | 已验证 |
| 题目配置接入 | `android-app/app/src/main/assets/question_config.json` | 已编码 |
| 本地真实适配器 | `LocalSherpaAdapter.kt` + sherpa Kotlin JNI 包装 | 已编码；真机识别待验证 |
| 远程协议适配 | `RemoteAdapter.java` + `samples/remote-adapter/` | 已编码；B 服务联调待验证 |
| 身份边界 | `IdentityGateway` / `account_id` 过滤 | 本机测试身份已编码；正式认证待 C |
| CI | `.github/workflows/android-ci.yml` | 推送后以 Actions 运行链接为准 |
| 第 8 节 / 图 | `第8节_安卓客户端组件设计.md`、`图8-1_组件依赖.puml` | 已与代码对齐更新 |

## 上游固定

见 `SHERPA_ONNX_COMMIT.txt`。预编译库版本：`v2.0.1`。模型：`sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23`。

## 待真机（OPPO / A·B）

- OPPO 安装、麦克风授权、录屏
- 本地路径真实语音识别
- 远程路径对接 B 的 `wss://` 与 `answer_complete`
- D-T1～D-T7 全表手机勾选
- APK SHA-256 安装退出码

无证据时保持「待验证」，不以模拟路径冒充识别通过。
