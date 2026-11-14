# 验证与交付记录

## 已完成（本仓库可复核）

| 项 | 证据 | 状态 |
| --- | --- | --- |
| F01–F12 核心规则 | `bash core/run-check.sh` → `CoreCheck: all checks passed` | 已验证 |
| 题目配置接入 | `android-app/app/src/main/assets/question_config.json` | 已编码 |
| 本地真实适配器 | `LocalSherpaAdapter.kt` + sherpa Kotlin JNI 包装 | 已编码；真机识别待验证 |
| 远程协议适配 | `RemoteAdapter.java` + `samples/remote-adapter/`；`check_protocol.py` 回环测试通过 | 控制帧/音频/Done 样例已验证；手机对 B 待验证 |
| 身份边界 | `IdentityGateway` / `account_id` 过滤 | 本机测试身份已编码；正式认证待 C |
| B 电脑端映射 | `B_电脑识别服务` unittest 3 项通过；Q01 `answer_complete` 见 evidence | 独立于 D 手机联调 |
| A/B/C 交接表 | `docs/ABC_HANDOFF.md` | 已整理待填 |
| 真机验收表 | `docs/DEVICE_ACCEPTANCE.md` | 模板已就绪；勾选待手机 |
| CI | `.github/workflows/android-ci.yml` | **当前源码** `d2d658e` 已成功 |
| 第 8 节 / 图 | `第8节_安卓客户端组件设计.md`、`图8-1_组件依赖.puml` | 已与代码对齐 |

## 上游固定

见 `SHERPA_ONNX_COMMIT.txt`。预编译库版本：`v2.0.1`。模型：`sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23`。

## CI 成功记录（当前）

详见 `docs/CI_BUILD_RECORD.md`。

- CI：https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43348788191
- APK Artifact：https://github.com/YYT-GUIYANG/interview-client-d/actions/runs/43348788191/artifacts/14002991886
- APK SHA-256：`4c64c717167d7c5244de2bdf44d823caa7250581c452e07683087552e835ae25`（109M）
- 提交：`d2d658e271c8377e8d81ece6d113ce4641f67a82`

## 待真机（OPPO · 实测由现场完成）

- [ ] OPPO 安装、麦克风授权、录屏（填 `DEVICE_ACCEPTANCE.md`）
- [ ] 本地路径真实语音识别
- [ ] 远程路径对接 B 的 `wss://` 与 `answer_complete`
- [ ] D-T1～D-T7 全表手机勾选
- [ ] 实测 APK SHA-256 与安装退出码
- [ ] 官方 `android/SherpaOnnx` 独立工程编译与安装（A）
- [ ] C 正式账号认证、记录所有权及删除契约冻结后接入

无证据时保持「待验证」，不以模拟路径或 B 电脑端回放冒充手机识别通过。

## 真机本地路径（2026-11-14 截图）

设备：OPPO Find X7 Ultra（PHY110）。证据：`docs/evidence/`。

| 项 | 状态 |
| --- | --- |
| 本地路径 D-T1 / T3 / T5（换账号）/ T6（删除入口）/ T7 | 已实现并验证（截图） |
| D-T2 修订过程、D-T4 断网、远程 WSS、杀进程重开 | 未测 |
| ABI | 未截取 |

详见 `docs/DEVICE_ACCEPTANCE.md`。
