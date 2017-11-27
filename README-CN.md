# ![Logo](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/logo.png)
# EZFilter

一个轻量级（<180KB）、易扩展的Android滤镜框架，支持拍照、视频、图片和视图添加滤镜。

[English](README.md)

[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZFilter/images/download.svg) ](https://bintray.com/uestccokey/maven/EZFilter/_latestVersion)

### Demo

[下载地址](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/demo.apk)

### 截图

![View加滤镜](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/view-filter.gif)

### 功能

1.支持Camera、Camera2、Video、Bitmap和View加滤镜

2.支持录制视频

3.支持截图

4.支持离屏渲染

### 使用

#### Gradle

``` gradle
dependencies {
    compile 'cn.ezandroid:EZFilter:1.5.7' // Gradle 3.0以下
    // 或者
    implementation 'cn.ezandroid:EZFilter:1.5.7' // Gradle3.0及以上
}
```

#### 示例

使用 ` EZFilter.input(xxx).addFilter(filter).into(view)` 添加滤镜并显示

``` java
EZFilter.input(bitmap)
        .addFilter(filter)
        .into(view);
```

``` java
EZFilter.input(video)
        .setLoop(true)
        .setVolume(0.5f)
        .addFilter(filter)
        .into(view);
```

``` java
EZFilter.input(camera)
        .addFilter(filter)
        .into(view);
```

``` java
EZFilter.input(camera2)
        .addFilter(filter)
        .into(view);
```

``` java
EZFilter.input(glview)
        .addFilter(filter)
        .into(view);
```

在调用`into`方法后，你会得到一个`RenderPipeline`对象，可以使用它来进行截图

``` java
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    },true);
view.requestRender();
    // 或者
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    }, width, height, true);
view.requestRender();
```

另外，假如`enableRecord`被调用过，那么你也可以使用该对象可以进行视频录制

``` java
mPipeline = EZFilter.input(camera)
                .addFilter(filter)
                .enableRecord(path, true, true)
                .into(view);

// 开始录制
mPipeline.startRecording()

// 结束录制
mPipeline.stopRecording()

// 修改影像录制开关
mPipeline.enableRecordVideo(enable);

// 修改音频录制开关
mPipeline.enableRecordAudio(enable);

// 修改录制输出路径
mPipeline.setRecordOutputPath(path);
```

