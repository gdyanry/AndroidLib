## 接入

####  获取interestId

   * 所有推送的数据都是以“兴趣点”（即服务端的“能力”）的形式存在，通过唯一id标识，比如“天气预警”推送的id为“10025”。

   * 一个应用可以订阅多个兴趣点。

   * 兴趣点id从后台申请得到。

####  添加gradle依赖

   * 在以项目名为命名的顶层build.gradle文件中，添加推送maven库地址：
   ```groovy
   maven {
       url "http://bigdata.tclking.com/nexus/repository/maven-public"
   }
   ```
   * 在app/build.gradle文件中引用推送SDK依赖库：
   ```groovy
   dependencies {
       ...
       implementation 'com.tcl.common:pushlib:+'
   }
   ```
---

## API使用

#### 接口类说明

|接口|说明|
|-----------|-----------|
| PushClient | SDK 功能接口，用于调用推送相关功能接口 |
| PushCommandCallback | 指令回调接口 |
| PushMessageListener | 推送消息监听接口 |

#### 获取PushClient对象
   ```java
   public static PushClient getInstance()
   ```
   *返回*
   * 返回PushManager单例对象

   *说明*

   * 所有推送的接口均是通过该实例调用。

#### 初始化

   ```java
   public void init(Context context, Executor executor)
   ```
   *参数*
   * context: 可以为各种类型的Context
   * executor: 用于执行跨进程调用，避免阻塞主线程

   *说明*

   * 调用其他API之前必须先进行初始化。

#### 添加标签
   ```java
public void addTag(String tag, PushCommandCallback callback)
   ```
   *参数*

   * tag: 要添加的标签名称
   * callback: 执行结果的回调

   *说明*
   * 设置标签的作用是定向推送，比如按区域推送，只有设置了特定区域标签的设备才会收到推送。
   * 注意回调是运行在初始化时提供的Executor中，而非主线程，如有需要请自行切换回主线程。

#### 订阅推送
   ```java
   public void addMessageListener(String interestId, PushMessageListener listener)
   ```
   *参数*
   * interestId: 兴趣点id，从后台申请得到
   * listener: 推送监听接口

   *说明*

   * 如果是在特定页面订阅推送消息，在页面销毁时记得取消订阅，避免内存泄漏。

#### 取消订阅
   ```java
   public void removeMessageListener(String interestId, PushMessageListener listener)
   ```
   *参数*

   * interestId: 兴趣点id
   * listener: 需要取消订阅的监听接口

#### 释放
   ```java
   public void release()
   ```
   *说明*
   * 当不需要接收任何推送消息时调用此方法。
   * 调用此方法后若要重新接收推送，必须重新初始化。

---

## 测试

* 测试前确保设备上安装了包名为"com.tcl.common.pushservice"的apk。
* logcat使用"yanry.push"关键字过滤，正常情况下执行了初始化方法后会出现类似如下日志：

   > 12-07 16:16:55.515 1768-7025/com.tcl.common.pushservice D/yanry.push.service: push client id: fe79a2eb34b5976d45301cd250c79cb9

   > 12-07 16:16:55.690 6936-6936/com.tcl.common.clientdemo D/yanry.push.lib: service connected.

* 排除其它因素后若没有看到如上所示日志，说明该设备尚未部署推送服务。