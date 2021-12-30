# lottie_blend 动画混合

## 前景、背景单独动画json

   前景、背景作为单独的动画素材分开给到，变老结果图和原图替换掉动画的背景素材；分别解帧背景、前景的序列帧图，然后背景帧做全局滤镜处理，最后融合前景背景序列帧，通过ffmpeg将序列帧转成视频并添加背景音乐

1. 背景为待替换的素材图，背景素材需要包括
   
    * bg/data.json（背景动画json）
    * bg/images（替换的素材图，图片命名为**img_0，img_1，img_2**以此类推）
   
2. 前景为动画的遮罩图

    * fg/data.json（前景动画json）
    * fg/images（前景序列帧图，用于动画遮罩）


### blend.json 用于描述前景遮罩、背景素材、滤镜帧，背景音频文件

* bg：背景，必选
    * data：背景动画json，必选
    * images：背景动画占位素材，必选
    * filters：滤镜帧，可选；滤镜帧同一帧片段可叠加多层滤镜
        * id：滤镜自定义id，详细参看[预设滤镜](#jump1)
        * start：起始帧
        * end：结束帧，-1为全局结束
        * intensit：float，滤镜默认强度，范围0.0-1.0
* fg：前景遮罩，可选
* audio：融合背景音频，可选
    * data：音频名

```json
{
  "bg": {
    "data": "bg/data.json",
    "images": "bg/images",
    "filters": [
      {
        "id": 2,
        "start": 0,
        "end": 30,
        "intensity": 0.1
      },
      {
        "start": 31,
        "end": 110,
        "id": 6,
        "intensity": 0.1
      },
      {
        "start": 64,
        "end": 110,
        "id": 5,
        "intensity": 0.1
      }
    ]
  },
  "fg": {
    "data": "fg/data.json",
    "images": "fg/images"
  },
  "audio": {
    "data": "bg.m4a"
  }
}
```

### <span id="jump1">预设滤镜，参考自趣玩相机的故障风滤镜组</span>

|  滤镜 可从https://photomosh.com/抓包       |    id| 默认强度（0.0-1.0）|sample|
|                ---                        | --- | --- | --- |
| Glitch（JitterFilter）| 1 | 0.18| ![](./screenshots/glitch.gif)|
| RGB Shift（RGBShiftFilter） | 2 | 0.28  | ![](./screenshots/rgbshift.gif)|
| Noise（ScanlinesFilter） | 3 | 1.00  |![](./screenshots/noise.gif)|
| BadTV（BadTVFilter） | 4 | 0.40  |![](./screenshots/badtv.gif)|
| Shake（ShakeFilter） | 5 | 0.44  |![](./screenshots/shake.gif)|
| Rainbow（RainbowFilter） | 6 | 0.50  |![](./screenshots/rainbow.gif)|
| Dot Matrix（DotMatrixFilter） | 7 | 0.55  |![](./screenshots/dot_matrix.gif)|
| Blur（BarrelBlurFilter） | 8 | 0.44  |![](./screenshots/blur.gif)|
| Monochrome（HalftoneJitterFilter） | 9 | 0.28  |![](./screenshots/monochrome.gif)|
| Vertigo（RGBShiftShakeFilter） | 10 | 0.11  |![](./screenshots/vertigo.gif)|
