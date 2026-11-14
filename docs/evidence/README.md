# D 真机证据索引（2026-11-14）

设备：OPPO Find X7 Ultra（型号 PHY110）。路径均为本目录相对文件名。

| 文件名 | 内容 | 支撑用例 |
| --- | --- | --- |
| `app-debug.apk` | CI 安装包（已核 SHA `4c64c717…`） | D2 |
| `apk.sha256.txt` | 本地核验记录 | D2 |
| `02_关于手机_OPPO_Find_X7_Ultra_PHY110.jpg` | 关于本机 | A1 |
| `03_本地路径完成_Q01_关键词人工智能.jpg` | 本地 sherpa、COMPLETE、黄高亮「人工智能」 | D-T1,T3,T7 |
| `05_档案列表_local-test-user_三条COMPLETE.jpg` | 账号 local-test-user 列表 3 条 COMPLETE | D-T1 保存查看 |
| `05b_档案列表_local-test-user_含最终文本背景.jpg` | 同上列表（带最终文本背景） | D-T1 |
| `06_账号隔离_yyt看不到他人档案.jpg` | 换账号 yyt 后列表空；背景高亮「老年人」命中1 | D-T5,T3 |
| `07_档案详情_路径local_账号yyt_可删除.jpg` | 详情 path=local、账号 yyt、删除按钮 | D-T1,T6,T7 |

## 未测（无证据，不得勾通过）

- D-T2 同段修订过程（无录屏）
- D-T4 断网/超时
- 远程 WSS 全路径
- 杀进程后重开列表（无单独截图）
- ABI（关于本机页未显示；待 `adb shell getprop ro.product.cpu.abi`）
- 官方 Demo 离线录屏（A3）
