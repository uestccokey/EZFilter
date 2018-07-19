# ![Logo](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/logo.png)
# EZFilter

一个轻量级（<180KB）、易扩展的Android滤镜和动态贴纸框架，支持摄像头、视频、图片和视图添加滤镜和贴纸。

[English](README.md)

[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZFilter/images/download.svg) ](https://bintray.com/uestccokey/maven/EZFilter/_latestVersion)

### Demo

[下载地址](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/demo.apk)

### 截图

![View加滤镜](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/view-filter.gif)

### 功能

1.支持Camera、Camera2、Video、Bitmap和View添加滤镜

2.支持Camera、Camera2、Video、Bitmap和View添加动态贴纸

3.支持录制视频

4.支持截图

5.支持离屏渲染

### 使用

#### Gradle

``` gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    compile 'cn.ezandroid:EZFilter:x.x.x' // Gradle 3.0以下
    // 或者
    implementation 'cn.ezandroid:EZFilter:x.x.x' // Gradle3.0及以上
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
其他功能，如录制视频，添加动态贴纸等，请参考Demo工程。


