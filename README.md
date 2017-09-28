![Logo](https://raw.githubusercontent.com/uestccokey/EZFilter/master/logo.png)
# EZFilter
一个轻量级，易扩展的Android滤镜框架，支持拍照，视频，图片添加滤镜，支持离屏渲染
[ ![Download](https://api.bintray.com/packages/uestccokey/maven/EZFilter/images/download.svg) ](https://bintray.com/uestccokey/maven/EZFilter/_latestVersion)
### Demo

[下载地址](https://raw.githubusercontent.com/uestccokey/EZFilter/master/demo.apk)

### 截图

![View加滤镜](https://raw.githubusercontent.com/uestccokey/EZFilter/develop/view-filter.gif)

### 功能介绍

1.Camera添加滤镜

2.Camera2添加滤镜（Api21以上）

3.Video添加滤镜（支持自定义播放器，支持离屏渲染）

4.Bitmap添加滤镜（支持离屏渲染）

5.View添加滤镜

### 依赖配置

``` gradle
dependencies {
    compile 'cn.ezandroid:EZFilter:1.4.6' // Gradle 3.0以下
    // 或者
    implementation 'cn.ezandroid:EZFilter:1.4.6' // Gradle3.0及以上
}
```

### 使用方式

图片加滤镜显示

``` java
EZFilter.input(bitmap)
        .addFilter(filter)
        .into(view);
```

图片加滤镜离屏渲染

``` java
Bitmap bitmap = EZFilter.input(bitmap)
                    .addFilter(filter)
                    .output();

```

视频加滤镜显示

``` java
mPipeline = EZFilter.input(video)
                .setLoop(true)
                .setVolume(0.5f)
                .addFilter(filter)
                .into(view);
```

视频加滤镜保存

``` java
EZFilter.input(video)
    .addFilter(filter)
    .output(path)
```

拍照加滤镜（Camera）

``` java
mPipeline = EZFilter.input(camera)
                .addFilter(filter)
                .into(view);
```

拍照加滤镜（Camera2）

``` java
mPipeline = EZFilter.input(camera2)
                .addFilter(filter)
                .into(view);
```

视图加滤镜（View）

``` java
mPipeline = EZFilter.input(glview)
                .addFilter(filter)
                .into(view);
```

视频、拍照和视图加滤镜后截图

``` java
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    },true); // 第二个boolean参数表示是否截原图还是截添加了滤镜之后的图
view.requestRender();
    // 或者
mPipeline.output(new BitmapOutput.BitmapOutputCallback() {
        @Override
        public void bitmapOutput(Bitmap bitmap){
        }
    }, width, height, true); // 中间的两个参数表示截图的宽高
view.requestRender();
```

