# 瓷白 MPV（Android）

基于 [mpvKt](https://github.com/abdallahmehiz/mpvKt) 定制的本地番剧播放器，保留 `libmpv` 播放内核与 `gpu-next` 渲染，默认采用柔雾瓷白主题，并支持深色模式与 Android 12+ Material You 动态取色。

## 核心功能

- **本地番剧库**：按文件夹自动整理本地视频，自然剧集排序，支持同目录连播、断点续播与同目录外挂字幕加载。
- **播放与手势**：打开视频自动播放；单指右滑快进/左滑后退、左右上下滑调亮度与音量、双击快进/后退、长按倍速、双指捏合切换铺满与自适应；独立对数倍速滑块（`0.25x–4.0x`）。
- **移动端画质增强**：内置原画、轻锐化（QCOM SGEDS）、Anime4K 轻量 / 清晰 / 降噪 5 档着色器预设，可叠加 GPU 去色带。
- **章节跳过 OP / ED**：识别片头、片尾与预告章节，支持询问跳过、自动跳过与关闭，并提供撤销提示。
- **120Hz 原生弹幕引擎**：支持自动匹配弹幕（同目录 XML、文件前 16MB 哈希、番名检索、Bangumi 中文名回补）、手动搜索与本地 XML 导入、时间偏移微调，以及 7 组磁性贴合滑块实时调节样式、5 档预设、过滤屏蔽与简繁转换。
- **Bangumi 进度同步**：支持粘贴 Access Token 登录、自定义 API/镜像地址与连通性测试、文件夹番剧绑定、片尾 90 秒询问/自动标记已看，并将识别到的番名与剧集副标题同步显示到顶栏与视频列表。
- **检查更新**：在「设置 → 关于」中点击「应用版本」，可直接检查 GitHub Releases 是否有新版本并下载 APK。

## 构建说明

- 环境要求：JDK 21、Android SDK（API 36/37）
- 构建 `arm64-v8a` 安装包：
  ```bash
  ./gradlew :app:assemblePreview
  ```

## 致谢

- 上游项目：[abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt)
- 桌面端配套配置：[CWINDOWX/mpv-soft-porcelain](https://github.com/CWINDOWX/mpv-soft-porcelain)
- 内置中文字体：LXGW WenKai Mono Lite（霞鹜文楷等宽）
