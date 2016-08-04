## 概述
* 本库依赖于另一个Java库[CommonLib](https://github.com/gdyanry/CommonLib)。
* 本库编写的目的在于降低Android应用的开发难度，提高开发效率。
* 本库中的内容大部分已在一些上线项目中应用，使用文档会陆续补充。

## API数据处理

### 请求&响应

对业务层来说，发送请求应该是可以一次请求多个API，并且API的请求顺序是可以保证的。RequestPack正是对此模型作的封装，开发者首先要根据具体项目的请求方式来实现RequestPack。

在业务层上创建RequestPack实例后的可添加多个API请求：

```java
requestPack.append(apiTag, param, new RequestDataHook() {

  @Override
  public void onResponse(Object responseData) {
    // 处理响应数据
  }
});
```

对于响应数据，一般都是在UI进行操作，且一般都带有错误码，UI上只处理正常错误码所对应的数据，所以上面第三个参数一般不直接实例化RequestDataHook接口，而是写一个实现ResponseParser接口的抽象类，对具体项目的响应数据格式进行解析，在此基础上再实现RequestDataHook接口，并根据业务层需要选择性地处理线程问题。此时业务层只需要处理成功响应的数据便可。

发送请求：

```java
requestPack.send(visualizer);
```

> visualizer为RequestLiveListener对象，是请求过程中各种状态的回调，一般会在这些回调中操作UI，所以需要处理线程问题，因此一般只要实现AndroidRequestLiveListener即可，该类已经处理好了线程问题。

### 缓存

API的数据缓存是使用sqlite和内存实现的二级缓存。缓存使用一个字符串类型的key进行读写。对缓存的操作是在SqliteCacheDataManager中实现，该对象在项目中应该做成单例。

读取缓存：

```java
manager.getData(key, new CacheDataHook() {

  @Override
  public void onNoData() {
    // 无缓存数据回调
  }

  @Override
  public void onDataLoaded(boolean inMemory, Object data) {
    // 处理缓存数据
  }

  @Override
  public Object deserializeData(String localCachedData) {
    // 根据缓存的字符串返回序列化之后的对象
  }
});
```

从服务端获得响应数据后更新缓存：

```java
manager.saveCache(key, data, removeOnLogout);
```

> 若数据是用户相关的则第三个参数为true。

退出登录时清空用户相关的缓存：

```java
manager.cleanUserCache();
```

### 综合运用

很多情况下缓存和请求都是结合使用的。发送请求前先查看缓存中是否有数据，如果有就先显示在界面上；再根据是否有缓存决定是否发请求，请求时使用的RequestLiveListener也会有所不同；请求得到正常响应数据后再更新界面及缓存。

MainCacheRequest和SubCacheRequest是对以上处理逻辑的实现。MainCacheRequest即“主请求”，一般在一个页面中有且最多只有一个主请求，该请求的缓存是否存在决定了请求时所使用的RequestLiveListener。SubCacheRequest即“附加请求”，如果页面加载时有多个API请求，除了主请求之外的都是附加请求。附加请求不能单独发送，只能通过MainCacheRequest的preposeSubCacheRequest或者appendSubCacheRequest方法和主请求绑定起来，这两个方法的区别在于附加请求和主请求之间的执行顺序不同而已，可根据需要选用。

```java
MainCacheRequest main = new MainCacheRequest() {

  @Override
  public Object deserializeData(String localCachedData) {
    // 根据缓存的字符串返回序列化之后的对象
  }

  @Override
  public Object getBusinessSuccessData(Object responseData) {
    // 返回对响应数据进行错误码解析之后的业务数据，若错误码为非正常错误码则返回null
  }

  @Override
  protected boolean onData(Object data, DataSource from) {
    // 处理数据。若数据来自缓存，返回值表示是否发送请求；若数据来自服务端响应，返回值表示是否更新缓存
    return true;
  }

  @Override
  protected boolean ifRemoveCacheOnLogout() {
    // 若数据是用户相关的则返回true
    return false;
  }

  @Override
  protected Object getRequestParam() {
    // 返回请求参数
  }

  @Override
  protected CacheDataManager getCacheDataManager() {
    // 返回CacheDataManager的单例对象
  }

  @Override
  protected String getApiTag() {
    // 返回该请求的API标识
  }

  @Override
  protected RequestLiveListener getSmallLoading() {
    // 返回有缓存时发送请求的RequestLiveListener
  }

  @Override
  protected RequestLiveListener getPageLoading() {
    // 返回无缓存时发送请求的RequestLiveListener
  }

  @Override
  protected RequestPack createRequest() {
    // 返回发送请求的RequestPack对象
  }
};
```

SubCacheRequest的构建与MainCacheRequest类似，就不再详述了。

### 列表

针对列表数据可以在MainCacheRequest的基础上进行一系列扩展。

#### PagingDataHandler

继承MainCacheRequest，主要添加了对分页数据的处理。注意加载页面时调用loadPage而不是send发送请求。

#### PullPagingDataHandler

继承PagingDataHandler，添加了本库自有的下拉刷新及上拉加载更多功能。

#### PullListPageHandler

继承PullPagingDataHandler，只用于AbsListView。

## 图片处理

### 缩略图

定义Decoder：

```java
Decoder decoder = new Decoder() {

  @Override
  public Bitmap decode(Options opts) throws Exception {
    // 此处根据不同的图片来源分别调用BitmapFactory中对应的方法解码图片
    return BitmapFactory.decodeFile(pathName, opts);
  }
};
```

实例化BitmapThumb对象：

```java
BitmapThumb thumb = new BitmapThumb(decoder)；
```

自定义缩略图参数：

```java
thumb.width(width);
thumb.height(height);
// 自动调整图片旋转角度，需提供原图路径
thumb.autoRotate(srcPath);
// 打印调试信息
thumb.debug();
// 当宽高同时设置时，一般会计算出两个不同的缩放比例，默认采用缩放程度较大的比例，调用此方法则会采用缩放程度较小（更接近1）的比例。
thumb.useNearScale();
```

> 缩略图的宽高为`ThumbDimemsion`对象， 默认采用`FitType.RoundNear`类型，即通过`Options.inSampleSize`  来设定缩放比例，而且会先把运算得到的比例值向下转换为2的指数次幂，比如3变成2，7变成4，20变成16，再进行缩放。 这样导致的结果是得到的缩略图很可能比你给定的宽高要大一些，一般对于显示图片的场景而言这是没有问题的。如果你希望得到的缩略图不大于给定的宽高， 则可以把类型指定为`FitType.RoundFar`；如果你希望得到的缩略图切确等于给定宽高，则使用类型  `FitType.Exact`，但是此类型会先使用`FitType.RoundNear`  生成一张较大缩略图，再进行矩阵变换得到切确宽高的`Bitmap`，性能较差，如非必要都不应该使用此类型。

生成缩略图：

```java
Bitmap bmp = thumb.createThumb();
```

> 若此时可用内存不足则会打印错误信息并返回null，避免OOM。

压缩图片用于上传等：

```java
byte[] bytes = thumb.compress(sizeLimitKb, format);
```

### 图片加载

#### 初始化

程序启动时实例化BitmapLoader：

```java
BitmapLoader loader = new BitmapLoader(maxMemorySize, threadNumber, maxConcurrency);
```

> 第三个参数为最大并发数，可以理解为界面上最多能同时加载的图片数量，如果给的太小会导致一些图片加载没有被执行。

按实际需要进行初始化配置：

* 支持由资源id加载图片

```java
loader.supportRes(resources);
```

* 支持由Uri加载图片

```java
loader.supportUri(contentResolver);
```

* 支持由Url加载图片

```java
// 下载的图片保存到数据库中
loader.supportUrlInBlob(taskCacheLimit, conn, dao, table, keyField, blobField);
// 下载的图片保存到缓存文件夹中
loader.supportUrlInFile(fileMapper, taskCacheLimit, conn, supportResume);
```

* 支持加载HTML标签的图片

```java
loader.supportHtmlImage(mapper);
```

* 打印调试信息

```java
loader.debug();
```

#### 加载图片

```java
BitmapRequest request = loader.getRequest(src);
```

配置加载参数：

```java
request.width(width).height(height).into(imageView).load(new LoadHook() {

  @Override
  public boolean onStartLoading() {
    // 当内存缓存没有匹配的图片时会触发此方法，若此时不需要执行加载可返回false
    return true;
  }

  @Override
  public void onShow(Bitmap bmp) {   
  }

  @Override
  public void onError() {
  }

  @Override
  public boolean isAbort() {
    // 在执行加载的过程中会多次调用此方法，可在关闭activity等情况下返回true以终止加载
    return false;
  }
})；
```

显示下载进度：

```java
request.download(new DownloadHook() {

  @Override
  protected void onUpdate(long currentPos) {
    // 更新下载进度条
  }

  @Override
  protected void onStart(long startPos, long totalLen) {
    // 开始下载
  }

  @Override
  protected void onFinish() {
  }

  @Override
  protected void onError() {
  }

  @Override
  protected int getUpdateInterval() {
    // 返回更新下载进度的最小时间间隔
    return 300;
  }
});
```

执行加载：

```java
request.commit();
```

#### 其它功能

加载HTML标签图片：

```java
loader.loadHtmlImage(source, textView, defaultDrawable);
```

获取本地缓存大小：

```java
Set<File> exclusive = new HashSet<File>();
loader.getLocalCacheSize(ifClear, exclusive);
```

> 若第一个参数为true，则在计算缓存大小的同时执行清理本地缓存。

使用图片url访问图片文件：

```java
new UrlFileTask() {

  @Override
  public void onGenerateException(Exception e) {
    // 处理下载文件过程中抛出的异常
  }

  @Override
  public int getUpdateInterval() {
    // 进度更新间隔
    return Integer.MAX_VALUE;
  }

  @Override
  protected void onUpdate(long currentPos, long totalLen) {
    // 更新进度
  }

  @Override
  protected void onFileAvailable(File file) {
    // 处理文件
  }
}.start(loader.getLevel2FileAccess(), url);
```

## 下拉刷新

下拉刷新采用非侵入性的设计，没有自定义的控件，只是添加了下拉对象、刷新视图等概念，通过灵活组合可以实现高度自定义的下拉刷新效果。

### 模型

首先定义一个“下拉源”：

```java
// 构造函数的参数为下拉的触摸对象
PullDownSource pull = new PullDownSource(findViewById(R.id.iv_pull)) {

  @Override
  protected boolean isReadyToPull() {
    // 对于可滚动的ViewGroup，只有第一个可见子view完全可见时才返回true
    return true;
  }
};
```

再为PullDownSource添加PullDownHook：

```java
pull.addHook(new PullDownHook() {

  @Override
  public void onRelease(int distance) {
    // 松手的回调
  }

  @Override
  public void onPullingDown(int distance) {
    // 下拉的回调
  }

  @Override
  public boolean onPreparedToPull() {
    // 如果需要禁止下拉则返回false
    return true;
  }
});
```

### 扩展

#### PullDownAdapterView

针对AbsListView的PullDownSource的实现。

#### DragAndShrink

PullDownHook的抽象实现类，增加下拉松手后的复原处理。

#### ZoomHandler

DragAndShrink的子类，提供下拉放大效果。

#### RefreshHandler

DragAndShrink的子类，增加了下拉刷新过程的四种状态：初始、下拉刷新、松手刷新、正在刷新。

#### RefreshViewHandler

RefreshHandler的子类，增加了刷新视图的位置处理。

#### RefreshHeaderListView

RefreshViewHandler的子类，只用于ListView，把刷新视图添加到ListView的headerView中，避免下拉过程中ListView的高度发生变化，进而引发其itemView的重绘。

## 加载更多

LoadMore定义了在处理分页加载数据时的行为接口，你可以实现此接口来创建你想要的加载更多的交互样式。

AutoLoadMore是该接口在AbsListView上的一种实现，当AbsListView滑到底部时会自动触发加载更多。它的构造函数需要一个LoadMoreView对象，LoadMoreView是加载更多的视图的行为接口，开始加载更多和结束加载更多时的视图交互通过实现这个接口来提供。

## 图片选择

实例化ImageSelectHandler：

```java
ImageSelectHandler handler = new ImageSelectHandler() {

  @Override
  protected void onAlbumDataLoaded() {
    // 相册数据加载完成的回调，比如可以使用EventBus发布该事件，如果界面上需要处理该事件的话
  }

  @Override
  protected void execute(Runnable r) {
    // 加载相册数据属于耗时任务，应该放到线程池中执行
    Singletons.get(DefaultExecutor.class).execute(r);
  }
};
```

建议把ImageSelectHandler对象做成单例的，在程序启动的时候初始化相册数据，而不是每次进入相册时才加载，可以提高相册的响应速度。

```java
handler.initAlbum(context, filter);
```

> 第二个参数用于对相册的数据作筛选。

在相册选择结果页面中定义选择选项及处理选择结果：

```java
handler.setImageSelectHook(new ImageSelectHook() {

  @Override
  public void onSelectImages(List<File> images) {
    // 多选结果回调
  }

  @Override
  public void onSelectImage(File image) {
    // 单选结果回调
  }

  @Override
  public boolean isMultiSelect() {
    // 若是多选则返回true
    return false;
  }

  @Override
  public ImageCrop customizeImageCrop(ImageCrop rawCrop) {
    // 只有单选才会进入此方法，若不需要裁剪图片则返回null
    return rawCrop.keepScale().setOutput(300, 400).setAspect(3, 4);
  }
});
```

在相册选择界面上实例化ImageSelectViewHelper：

```java
ImageSelectViewHelper helper = new ImageSelectViewHelper(handler, gridView, desiredImageSizeDp, imageItem, leadItems) {

  @Override
  protected void onPermissionDenied() {
    // 没有查询相册数据权限的回调
  }
};
```

> ImageSelectViewHelper构造函数最后两个参数的类型分别为ImageItem和LeadItem。ImageItem就是GridView图片项，它有两个抽象实现类，SingleSelectImageItem和MultiSelectImageItem，下面会分别说明。LeadItem是GridView中图片项前面的可选项，常见的如拍照项，后面也会有具体说明。

重写相册选择界面activity的onActivityResult方法：

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (!helper.onActivityResult(requestCode, resultCode, data)) {
	// 其它activity的返回结果处理
  }
}
```

### 单选

```java
SingleSelectImageItem sigle = new SingleSelectImageItem() {

  @Override
  public int getItemViewId() {
    // 返回图片项视图资源id
  }

  @Override
  public void display(ViewHolder holder, File imageFile, int viewWidth, boolean isClick) {
    // 图片项的显示处理
  }

  @Override
  protected File getTempFolder() {
    // 返回裁剪图片的临时文件夹目录
  }

  @Override
  protected int getCropImageRequestCode() {
    // 因为裁剪图片是调用系统界面，需要提供request code
    return 10086;
  }

  @Override
  protected void onResult(File file) {
    // 默认点击选择完成后关闭当前activity，如有需要可重写此方法
	super.onResult(file);
  }

  @Override
  protected ImageSelectHandler getImageSelectHandler() {
    return handler;
  }
};
```

### 多选

```java
MultiSelectImageItem multi = new MultiSelectImageItem(9) {

  @Override
  public int getItemViewId() {
    // 返回图片项视图资源id
  }

  @Override
  public void display(ViewHolder holder, File imageFile, int viewWidth, boolean isClick) {
    // 图片项的显示处理
  }

  @Override
  protected void onSelectedNumberChange() {
    // 选中图片改变时的回调
  }

  @Override
  protected ImageSelectHandler getImageSelectHandler() {
    return handler;
  }
};
```

多选的界面一般会有完成按钮，需要在其点击事件处理代码中添加：

```java
handler.getHook().onSelectImages(handler.getSelectedImages());
```

### 拍照

当拍照作为相册选择界面的一个图标时，可通过扩展ImageCaptureItem来实现：

```java
ImageCaptureItem capture = new ImageCaptureItem() {

  @Override
  public boolean isClickable() {
    // 当选择模式为多选且已选择图片数量达到上限时应返回false
    return true;
  }

  @Override
  public int getViewId() {
    // 返回拍照图标视图资源id
  }

  @Override
  public void display(ViewHolder holder, boolean isClick) {
    // 显示
  }

  @Override
  protected File getTempFolder() {
    // 返回拍照及裁剪产生的临时文件存放目录
  }

  @Override
  protected int getCropImageRequestCode() {
    // 返回裁剪图片的request code
  }

  @Override
  protected int getRequestCode() {
    // 返回拍照的request code
  }

  @Override
  protected ImageSelectHandler getImageSelectHandler() {
    return handler;
  }
  
  @Override
  protected ImageSelectViewHelper getImageSelectViewHelper() {
    return helper;
  }
};
```

拍照也可以独立于相册单独使用：

```java
ImageCaptureHandler captureHandler = new ImageCaptureHandler() {

  @Override
  protected File getTempFolder() {
    // 返回拍照及裁剪产生的临时文件存放目录
  }

  @Override
  protected int getCropImageRequestCode() {
    // 返回裁剪图片的request code
  }

  @Override
  protected ImageCrop customizeImageCrop(ImageCrop rawCrop) {
    // 配置裁剪参数，若不需要裁剪图片则返回null
    return rawCrop.keepScale().setOutput(300, 400).setAspect(3, 4);
  }

  @Override
  protected void onResult(File file) {
    // 结果回调
  }

  @Override
  protected int getRequestCode() {
    // 返回拍照的request code
  }
};
captureHandler.start(activity);
```

单独使用时需要重写当前界面activity的onActivityResult方法：

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (!captureHandler.onActivityResult(requestCode, resultCode, data)) {
	// 其它activity的返回结果处理
  }
}
```

### 裁剪

图片裁剪功能也可以单独使用：

```java
ImageCrop crop = new ImageCrop(data, outUri) {

  @Override
  protected int getRequestCode() {
    // 返回裁剪图片的request code
  }
};
crop.execute(activity);
```

此时也是需要重写当前界面activity的onActivityResult方法：

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
  if (!crop.onActivityResult(requestCode, resultCode, data, new OnCropResultListener() {

    @Override
    public void onCropResult(Bitmap bmp) {
      // 裁剪结果回调。如果构造函数的outUri为null，则此处bmp不为null。
    }
  })) {
	// 其它activity的返回结果处理
  }
}
```

## 会话管理

客户端登录后把会话凭证（session id）保存在本地，每次发请求需带上该凭证。在Android客户端，由AndroidSuite负责会话管理，它的对象应该是单例的。其用法如下：

* 每次启动应用时调用startUp。
* 需要保存用户登录记录（比如单击登录按钮）时调用saveAccount。保存的账号信息可以在需要的时候调用getAccount获得。
* 登录（或注册）成功（取得服务端返回的session id及uid）后调用updateSession。
* 主动或被动退出登录时调用logout。
* 任何时候都可以通过调用isLogin获取当前的登录状态。
* 退出进程时调用release。

需要在AndroidManifest.xml中声明如下权限：

- <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
- <uses-permission android:name="android.permission.INTERNET" />

AndroidSuite除了管理会话还提供了一些额外的功能：

* getSharedPreferences得到用户无关的SharedPreferences对象。
* getUserPreferences得到当前用户相关的SharedPreferences对象。
* getNetworkConnectionManager得到NetworkConnMngr对象，用于访问网络状况。
* getCacheRoot得到当前应用的缓存根目录。

## 网络连接

NetworkConnMngr是对ConnectivityManager的封装，用于查询和监听当前网络连接状况。用法如下：

```java
// 此对象应做成单例
NetworkConnMngr connection = new NetworkConnMngr(context);
// 初始化
connection.init();
// 注册监听
connection.register(new ConnectivityListener() {

  @Override
  public void onDisconnect() {
    // 断开网络连接
  }

  @Override
  public void onConnected(String typeName) {
    // 成功连接网络
  }
});
// 取消注册
connection.unregister(listener);
// 判断当前是否连接网络
connection.isConnected();
// 获取当前网络连接信息对象
NetworkInfo info = connection.getCurrentConnectedNetworkInfo();
if (info != null && info.isAvailable() && info.getType() == ConnectivityManager.TYPE_WIFI) {
	// 连接wifi
}
if (info != null && info.isAvailable() && info.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE) {
	// 连接4g
}
// 退出进程时释放资源
connection.release();
```

## 信息管理

这里的信息指的是程序运行过程的调试和异常信息，以及界面上的提示信息。AndroidInfoHandler可以指定调试信息的打印级别，对于界面提示信息，可以指定相同提示信息在指定时间间隔内不会重复出现，并且处理好了线程问题。用法可以查看InfoHandler接口文档。

## 输入校验

提交数据前，一般需要对一个或多个输入信息作校验，只有通过验证后才发送提交请求。EditTextChecker专门用于EditText的输入校验，该对象可使用单例模式。

* 首先对每个输入框指定一个StringMatcher对象，即定义校验规则。
* 依次对各个EditText调用EditTextChecker的check方法。
* 最后调用isPass，只有返回true才进行提交。

下面是登录校验的示例代码：

```java
if (Singletons.get(EditTextChecker.class).check(etUser, new StringMatcher().mobile(new OnViolationListener() {

  @Override
  public void onViolate() {
    Toast.makeText(context, "用户名必须为手机号", Toast.LENGTH_SHORT).show();
  }
})).check(etPassword, new StringMatcher().length(8, 12, new OnViolationListener() {

  @Override
  public void onViolate() {
    Toast.makeText(context, "密码长度必须为8到12位", Toast.LENGTH_SHORT).show();
  }
}).noWhitespace(false, new OnViolationListener() {

  @Override
  public void onViolate() {
    Toast.makeText(context, "密码不能含有空格", Toast.LENGTH_SHORT).show();
  }
}).chineseChars(false, true, new OnViolationListener() {

  @Override
  public void onViolate() {
    Toast.makeText(context, "密码不能含有中文", Toast.LENGTH_SHORT).show();
  }
})).isPass()) {
	// 提交登录请求
}
```

## 线程切换

主线程跳转到工作线程：

```java
Singletons.get(DefaultExecutor.class).execute(new Runnable() {
  public void run() {
	// 耗时操作代码
  }
});
```

工作线程跳转到主线程：

```java
Singletons.get(MainHandler.class).post(new Runnable() {
  public void run() {
	// 操作UI
  }
});
```

如果需要到主线程执行一些代码，但又不确定当前处于什么线程：

```java
CommonUtils.runOnUiThread(new Runnable() {
  public void run() {
	// 操作UI
  }
});
```

## 倒计时

比较典型的应用场景是获取验证码，每隔一段固定周期对计数器进行自减，直到0为止。使用SimpleCountDown可以很方便地实现此功能：

```java
// 60秒倒计时，每秒更新计数
SimpleCountDown count = new SimpleCountDown(60, 1000) {

  @Override
  protected void onFinish() {
    textView.setText("点击获取验证码");
  }

  @Override
  protected void onCounterChange(int count) {
    textView.setText(count + "秒");
  }
};
// 记得在退出页面时调用
count.stop();
```

## 密码显示和隐藏

密码输入框旁边一般会有个按钮，点击切换显示或隐藏密码。使用PasswordVisibilitySwitcher可以很方便地实现此功能：

```java
final PasswordVisibilitySwitcher switcher = new PasswordVisibilitySwitcher(etPwd);
btnSwitch.setOnClickListener(new OnClickListener() {

  @Override
  public void onClick(View v) {
    if (switcher.switchVisibility()) {
      btnSwitch.setText("隐藏");
    } else {
      btnSwitch.setText("显示");
    }
  }
});
```

## 提交按钮禁用状态切换

如果提交信息界面有多个输入框，只有当所有必填项都不为空时，提交按钮才显示为可点击状态。使用SubmitButtonStateAgent可以很方便地实现此功能，下面以注册为例：

```java
// 用户名、密码、验证码为必填项
final SubmitButtonStateAgent agent = new SubmitButtonStateAgent(etUser, etPassword, etCaptcha) {

  @Override
  protected void changeButtonState(boolean enable) {
    // 切换按钮状态
  }
};
btnRegister.setOnClickListener(new OnClickListener() {

  @Override
  public void onClick(View v) {
    if (agent.isEnable()) {
		// 发送注册请求
    }
  }
});
```

## 简单易用的PopupWindow

使用PopupWindow的时候，经常对于它的位置的计算是比较麻烦的。使用ConvenientPopupWindow可以让开发者免于进行这些计算，只需要指定popup位于anchorView的内部还是外部、popup相对于anchorView的方位（上|下|左|右）、popup与anchorView的对齐方式（起始对齐|中对齐|末尾对齐）、popup与anchorView的间距（padding）即可。
