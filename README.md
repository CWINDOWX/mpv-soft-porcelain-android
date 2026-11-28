# 瓷白 MPV（Android）

一款面向本地追番场景定制的开源 Android 播放器，基于 [mpvKt](https://github.com/abdallahmehiz/mpvKt) 开发。主打浅色清透的「柔雾瓷白」界面，集成本地番剧整理、自动匹配弹幕、画质增强、跳过 OP/ED 与 Bangumi 观影进度同步。

> 桌面端配套配置：[Soft Porcelain MPV（Windows）](https://github.com/CWINDOWX/mpv-soft-porcelain)

---

## 主要功能

- **柔和清透与多主题外观**：默认柔雾瓷白浅色主题，内置 6 套与桌面端统一的柔和配色（柔雾瓷白 / 樱雾浅粉 / 青瓷抹茶 / 暖纸琥珀 / 暮夜深蓝 / 极夜墨黑），支持深浅模式、跟随系统、Material You 动态取色，以及独立导入自定义界面字体。
- **本地番剧库**：按文件夹自动整理本地视频并按剧集顺序排列，支持断点续播、同目录连播和自动加载外挂字幕。
- **顺手的播放控制**：打开视频自动播放；支持滑动调进度 / 亮度 / 音量、双击快进退、长按临时倍速、双指捏合切换画面比例，以及精细的倍速滑块。
- **画质增强**：内置原画、轻锐化与多档动漫超分 / 降噪预设，可叠加去色带，老番与低码率片源更清晰。
- **自动跳过 OP / ED**：识别视频章节中的片头、片尾与预告，可设为弹窗询问或自动跳过，误跳支持一键退回。
- **自动弹幕与样式面板**：播放本地番剧自动匹配并加载弹幕，也支持手动搜索、导入本地 XML 与时间偏移微调；提供实时样式面板，可调整字号、透明度、速度、区域、屏蔽规则、简繁转换，并支持独立选择或导入自定义弹幕字体。
- **Bangumi 进度同步**：登录后自动识别番名与集数，片尾支持提醒或自动标记已看，并将番剧名与单集副标题显示到顶栏与列表中（不修改原文件名）。
- **检查更新**：在「设置 → 关于」中点击「应用版本」，可直接检查并下载新版本。

---

## 安装与使用

1. 前往 [Releases](https://github.com/CWINDOWX/mpv-soft-porcelain-android/releases) 下载 `Porcelain-MPV-arm64.apk` 并安装（适用于 64 位 Android 设备，可与原版 mpvKt 共存）。
2. 授予本地存储读取权限后即可浏览并播放手机中的视频。

**使用提示：**

- 本应用仅用于播放本地视频，不提供任何影视资源或在线片源。
- 弹幕默认使用社区公共中继接口，如遇访问波动或失效，可在设置中替换为自己的中继地址。
- Bangumi 同步默认连接官方接口，如有需要可在设置中填写自定义镜像地址并测试连通性。
- 如喜欢特定字体（如霞鹜文楷等），可在「设置 → 外观」或「弹幕样式面板」中独立导入本地 `.ttf` / `.otf` 字体文件。

---

## 致谢与协议

本项目基于 Apache-2.0 协议开源（见 [LICENSE](LICENSE)），感谢以下上游项目与开源组件：

| 组件 | 来源 | 协议 |
| :--- | :--- | :--- |
| mpvKt | [abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt) | Apache-2.0 |
| Anime4K 着色器 | [bloc97/Anime4K](https://github.com/bloc97/Anime4K) | MIT |
| QCOM SGEDS 着色器 | [SnapdragonStudios/snapdragon-gsr](https://github.com/SnapdragonStudios/snapdragon-gsr) | Snapdragon GSR License |
| hdeband 去色带 | an3223 / [mpv_PlayKit](https://github.com/hooke007/mpv_PlayKit) | LGPL-2.1 |
| 弹幕生态参考 | 弹弹play 开放接口 / [uosc_danmaku](https://github.com/Tony15246/uosc_danmaku) | MIT |

<details>
<summary>开发者自行编译</summary>

需要 JDK 21 与 Android SDK，在仓库根目录执行：

```bash
./gradlew :app:assemblePreview
```

若需打正式签名包，请在根目录创建 `keystore.properties`（已被 `.gitignore` 忽略），填入 `storeFile`、`storePassword`、`keyAlias`、`keyPassword` 后执行 `./gradlew :app:assembleRelease`。

</details>
