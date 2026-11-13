# A / B / C 交接清单（D 侧已整理）

本文件把「待 A/B/C 提供」拆成可勾选字段。D 侧已填入工作区现有证据与客户端**临时约定**；未冻结项保持空白，真机验收时由对应同学补齐。

手机实测由 D 执行；本表只收齐输入，不把电脑端证据写成手机已通过。

---

## A · 设备与官方示例

| 字段 | 需要内容 | 当前状态 |
| --- | --- | --- |
| OPPO 型号 | 如 Reno / Find 等完整型号 | ⬜ 待 A 填：________ |
| Android 版本 | 设置 → 关于手机 | ⬜ 待 A 填：________ |
| ABI | `adb shell getprop ro.product.cpu.abi`（常见 arm64-v8a） | ⬜ 待 A 填：________ |
| 官方 `android/SherpaOnnx` 安装证据 | 编译成功截图 / 录屏 / APK 哈希 | ⬜ 待 A 提供；D 已固定上游 `52b705f6…` |
| 是否同意装 D 的 Debug APK | 是/否 | ⬜ |

D 已准备：CI Debug APK（见 `docs/CI_BUILD_RECORD.md`）、安装与核对步骤（见 `docs/DEVICE_ACCEPTANCE.md`）。

---

## B · 远程 WSS（手机可访问）

| 字段 | 需要内容 | 当前状态 |
| --- | --- | --- |
| 手机可访问的 `wss://` 地址 | 非仅 `127.0.0.1`；手机同网或公网 | ⬜ 待 B 填：________ |
| TLS 证书 | 系统可信或安装指导；自签需说明如何导入 | ⬜ 待 B |
| Bearer 令牌 | 与 `adapter_server.py --token` 一致 | ⬜ 待 B（勿提交到公开仓库正文） |
| 上游官方流式服务 | 本机 `ws://127.0.0.1:6006` 是否已起 | 电脑端已有独立证据 |

### B 已有独立证据（不能替代手机联调）

| 证据 | 路径 / 结论 |
| --- | --- |
| Q01 回放摘要 | `B_电脑识别服务/evidence/adapter_Q01_summary.json`：`complete=true`，约 57s WAV，123 事件 |
| Q01 事件流 | `…/adapter_Q01.jsonl`：含 `segment_id`/`revision`/`is_final`，末行 `answer_complete: true` |
| 官方服务日志 | `…/official_Q01.log` |
| 映射单测 | `PYTHONPATH=B_电脑识别服务 … unittest` → 3 passed |
| 适配器默认 | `--host 127.0.0.1 --port 6007`；LAN 监听强制 `--cert --key --token` |

D 远程协议（与 B 对齐的项目契约）：

```json
{"answer_id":"..."}
```

随后 float32 LE 音频帧；结束文本 `Done`。服务事件示例：

```json
{"answer_id":"...","segment_id":"2","revision":3,"text":"…","is_final":true,"sample_start":-1,"sample_end":-1}
{"answer_complete":true}
```

---

## C · 账号 / 记录 / 排序 / 删除

| 字段 | 需要 C 冻结 | D 当前临时实现（待确认） |
| --- | --- | --- |
| 账号主键名 | `account_id` 还是 `candidate_id`？ | 使用 `account_id` |
| 身份令牌 / 会话校验 | 正式鉴权入口 | 仅 `LocalTestIdentityGateway` 本机测试身份 |
| 记录字段 | 正式 schema | 见下方「D 已编码记录字段」 |
| segment 排序键 | 正式键名 | `segment_id`（字符串）；同段用 `revision` 递增替换 |
| 修订键 | | `revision`（long） |
| 错误状态 | 枚举 / 文案 | 界面：权限、录音、WSS、队列、坏事件、超时；记录 `status`=`COMPLETE`/`PARTIAL` |
| 删除规则 | 谁可删、是否软删 | `deleteForAccount`：仅本人且 `canAccess`；硬删出 `records.enc` |
| 保留规则 | 保留多久、密钥轮换 | Keystore AES-GCM；**未**实现服务端保留/轮换 |

### D 已编码记录字段（prototype）

`answer_id`, `account_id`, `question_id`, `config_version`, `path`, `status`, `final_text`, `started_elapsed_nanos`, `saved_at_epoch_ms`, `analysis_extension`, `trace`

无主账号的旧行在列表中被隐藏。正式多用户认证**未**接入。

---

## 收齐后 D 的动作

1. 按 `构建与联调说明.md` + `docs/DEVICE_ACCEPTANCE.md` 装 APK、跑 D-T1～D-T7。
2. 补截图、录屏、安装退出码、实测勾选到 `DEVICE_ACCEPTANCE.md`。
3. 更新 `docs/VERIFICATION.md` / 第 8 节状态（有证据才改「已验证」）。
