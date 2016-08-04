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
