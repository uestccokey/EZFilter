# ![Logo](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/logo.png)
# EZFilter

A lightweight(<180KB) and extensible Android filter framework that supports Camera, Camera2, Video, Bitmap and View.

[中文](README-CN.md)

[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZFilter/images/download.svg) ](https://bintray.com/uestccokey/maven/EZFilter/_latestVersion)

### Demo

[Download](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/demo.apk)

### Screenshot

![View加滤镜](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/view-filter.gif)

### Features

1.Support Camera, Camera2, Video, Bitmap, View

2.Support recording video

3.Support screenshot

4.Support offscreen rendering

### Usage

#### Gradle
``` gradle
dependencies {
    compile 'cn.ezandroid:EZFilter:1.5.5' // Gradle version < 3.0
    // or
    implementation 'cn.ezandroid:EZFilter:1.5.5' // Gradle version >= 3.0
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

in addition, if `enableRecord` is called, you can record a video by the `RenderPipeline` object.

``` java
mPipeline = EZFilter.input(camera)
                .addFilter(filter)
                .enableRecord(path, true, true)
                .into(view);

// start recording
mPipeline.startRecording()

// stop recording
mPipeline.stopRecording()
```

