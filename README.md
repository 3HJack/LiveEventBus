# LiveEventBus

基于LiveData设计思想而设计的一款Android消息总线


## LiveEventBus的特点

- [x] 具有生命周期感知能力，消息随时订阅，会自动取消订阅
- [x] 无反射，高性能
- [x] 支持Sticky粘性消息
- [x] 支持AndroidX
- [x] 非Forever模式只在激活态可以收到消息，避免用户反复操作引起的无用消息导致界面卡顿

## 在工程中引用

Via Gradle: 在项目根目录的build.gradle的如下位置添加 <br><br>
maven { url "https://dl.bintray.com/onepiece/maven" }

```
allprojects {
    repositories {
        google()
        jcenter()
        maven { url "https://dl.bintray.com/onepiece/maven" }
    }
}
```

For Pre-AndroidX
```
implementation 'com.hhh.onepiece:live-event-bus:0.0.5'
```
For AndroidX:
```
implementation 'com.hhh.onepiece:live-event-bus-x:0.0.5'
```

## 实现原理
- 受LiveData设计思想启发而设计

- 参考了[eremyLiao/LiveEventBus](https://github.com/JeremyLiao/LiveEventBus)同学的
实现，在他的基础上去掉了多进程支持，去掉了反射，简化了用法，重写了实现

## 其他
- 欢迎提Issue与作者交流
- 欢迎提Pull request，帮助 fix bug，增加新的feature，让LiveEventBus变得更强大、更好用