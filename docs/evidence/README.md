# D 真机证据索引（2026-11-14）

设备：OPPO Find X7 Ultra（型号 PHY110）。

| 文件名 | 内容 | 支撑用例 |
| --- | --- | --- |
| `app-debug.apk` | CI 安装包（SHA `4c64c717…`） | D2 |
| `apk.sha256.txt` | 本地核验 | D2 |
| `02_关于手机_OPPO_Find_X7_Ultra_PHY110.jpg` | 关于本机 | A1 |
| `03_本地路径完成_Q01_关键词人工智能.jpg` | 本地 COMPLETE、关键词 | D-T1,T2佐证,T3,T7 |
| `05_档案列表_local-test-user_三条COMPLETE.jpg` | 本人列表 | D-T1；杀进程重开后应仍为此类列表 |
| `05b_档案列表_local-test-user_含最终文本背景.jpg` | 列表另一角度 | D-T1 |
| `06_账号隔离_yyt看不到他人档案.jpg` | 换账号隔离；「老年人」命中 | D-T5,T3 |
| `07_档案详情_路径local_账号yyt_可删除.jpg` | 详情 / 删除 | D-T1,T6,T7 |
| `08_飞行模式_本地路径_Q03_COMPLETE.jpg` | 飞行模式+本地 COMPLETE、关键词老年人 | D-T4,T3 |
| `09_远程WSS_PARTIAL_无识别文本.jpg` | 远程路径两档 PARTIAL、命中0 | 远程已配置；未达 COMPLETE |

## 现场确认（截图无法区分处）

- **D-T2**：现场确认说话时同段文本刷新、最终不重复累加。
- **D-T5 杀进程重开**：现场确认杀进程后再开，同一身份下列表仍在；与杀前截图外观相同属预期。

## 待现场自测补证

- 远程 WSS（D 自行联调 B）
- ABI（`adb shell getprop ro.product.cpu.abi`）
