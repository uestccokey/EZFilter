# ![Logo](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/logo.png)
# EZFilter

A lightweight (<180KB), easy-to-extend Android filter and dynamic sticker framework for adding filters and stickers for camera, video, bitmap and view.

[中文](README-CN.md)

[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZFilter/images/download.svg) ](https://bintray.com/uestccokey/maven/EZFilter/_latestVersion)

### Demo

[Download](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/demo.apk)

### Screenshot

![View加滤镜](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/view-filter.gif)

### Features

1.Support Camera, Camera2, Video, Bitmap and View add filters

2.Support Camera, Camera2, Video, Bitmap and View add dynamic stickers

3.Support recording video

4.Support screenshot

5.Support offscreen rendering

### Usage

#### Gradle
``` gradle
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    compile 'cn.ezandroid:EZFilter:x.x.x' // Gradle version < 3.0
    // or
    implementation 'cn.ezandroid:EZFilter:x.x.x' // Gradle version >= 3.0
}
```

#### Sample

you can use ` EZFilter.input(xxx).addFilter(filter).into(view)` to add filter and display.

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

After calling the `into` method, you will get a `RenderPipeline` object, then you can use it for screenshots.

``` java
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    }, true);
view.requestRender();
    // or
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    }, width, height, true);
view.requestRender();
```
Other functions, such as recording videos, adding dynamic stickers, etc., please refer to demo.

