# domino：local/remote两级缓存工具

## 想法和设计思路
1. [缓存进化史](http://www.mobabel.net/%E8%BD%AC%E7%BC%93%E5%AD%98%E8%BF%9B%E5%8C%96%E5%8F%B2/)
2. [聊聊如何把缓存玩出一种境界](http://www.mobabel.net/%E6%80%BB%E7%BB%93%E8%81%8A%E8%81%8A%E5%A6%82%E4%BD%95%E6%8A%8A%E7%BC%93%E5%AD%98%E7%8E%A9%E5%87%BA%E4%B8%80%E7%A7%8D%E5%A2%83%E7%95%8C/)

## 特性
1. 请求合并（可选）：当热点数据失效时，会有大量请求同请求同一个key，合并请求之后只会有一个请求真正得到处理，其他相同的请求只需等待结果即可---注意选择必须复写方法参数或key的hashcode！
2. 多级缓存：目前只有本地（可选）和redis（可选）两级
3. loadOnStartUp：支持缓存在应用启动时加载（只支持无参方法）
4. metrics：hit/miss;hit.local/miss.local;hit.remote/miss.remote;load/error;load.time/error.time
5. 多环境配置：配合qconfig实现多环境配置，因为FAT的数据量太小而且qps也不够，配置的参数值与线上通常需要有所区别
6. 自动打clog日志：配合ContextAwareClogger & ThreadLocalVariableManager可以将自己想要打的tag打在缓存日志中
7. 自动刷新（可选）：当数据的超时时间小于数据的刷新间隔时，数据永不过时（如果设置较大的redis失效时间的话，即时服务方挂掉了也能“苟活”一段时间），同时能保证一定的时效性，对于一些数据量比较小的热点数据建议使用这种配置
   1. retries：数据刷新防抖动，会在抛出异常时进行重试
   2. 随机interval：为了防止数据同时失效，刷新的初始延迟为interval之内的随机数
   3. 缓存同步：一台机器更新某个key的缓存后会通过redis pub/sub 刷新其他机器
   4. refreshTimeout：对于超过限定时间没有再次访问的key会被移除刷新队列


## 实现
1. 依托于Spring @Cacheable实现自动存取
2. 本地缓存使用caffeine，分布式缓存使用credis
3. 使用覆盖取代过期，尽量保证数据的可用性
4. Redis sub/pub同步数据
5. ScheduledThreadPoolExecutor调度刷新任务
6. 在key相同的请求间使用锁和future合并请求
7. Jackson序列化redis key/value,借助于公司的SSJsonSerializer支持契约对象

## 组件
![组件关系图](https://github.com/IQException/domino/blob/master/domino_%E6%88%AA%E5%9B%BE20190707210853.png?raw=true "主要组件" )
## 流程
![数据流程图](https://github.com/IQException/domino/blob/master/domino_%E6%95%B0%E6%8D%AE%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg?raw=true "数据流")
## 使用
### 配置
#### pom
```
    <groupId>com.ctrip.train.tieyouflight</groupId>
    <artifactId>domino</artifactId>
    <version>1.0.20</version>
```
使用springboot autoconfigure，只需引入jar包即可。


#### annotation
依托于spring @Cacheable， 使用meta-annotation二次开发，支持@Cacheable的大多数配置参数：@Domino(cond)=@Cacheable(condition);@Domino(except)=@Cacheable(unless)

```java
    @Domino(refreshable = true, name = "CommonPassengerStub.getCommonPassengers", except = "#result==null||#result.size()<=20")
    public List<CommonPassengerBO> getCommonPassengers(String ctripUId) throws Exception {}
```

#### qconfig
配置中心找到模板domino.t,根据它设置自己的缓存参数
![domino.t](https://github.com/IQException/domino/blob/master/domino_%E6%88%AA%E5%9B%BE20190707224113.png?raw=true "domino.t")

### 监控
tag见DashBoardStatsCounter
![metrics](https://github.com/IQException/domino/blob/master/domino_metrics.png?raw=true "metrics")

### 日志
待补充

### TIPS
1. springcache基于代理实现，所以类内调用(同一类的A方法调B方法)不会走缓存；如果需要走缓存，请在bean内配置当前bean，调用缓存方法使用bean.B. 示例如下
```java
@Component
public class CommonPassengerStub {
    private static final String LTP = "CommonPassengerStub.";
    private static BbzmbrcommonpassengerClient client = BbzmbrcommonpassengerClient.getInstance();

    @Resource
    private CommonPassengerStub self;

    public static BbzmbrcommonpassengerClient getClient() {
        return client;
    }

    static {
        client.setConnectTimeout(1000);
        client.setRequestTimeout(1000);
        client.setSocketTimeout(1000);
        client.setFormat("json");
    }

    public List<CommonPassengerBO> getCommonPassengers(String ctripUId, int limit) throws Exception {

        List<CommonPassengerBO> commonPassengers = self.getCommonPassengers(ctripUId);

        if (limit > 0 && commonPassengers != null) {
            return commonPassengers.stream().limit(limit).collect(Collectors.toList());
        }
        return commonPassengers;
    }
    
    @Domino(refreshable = true, name = "CommonPassengerStub.getCommonPassengers", except = "#result==null||#result.size()<=20")
    public List<CommonPassengerBO> getCommonPassengers(String ctripUId) throws Exception {

        String logTitle = LTP + "getCommonPassengers";

        GetCommonPassengerRequestType request = new GetCommonPassengerRequestType();
        request.setUID(ctripUId);
        ParameterItem parameterItem = new ParameterItem("BizType", "TRA");
        request.setParameterList(Arrays.asList(parameterItem));
        QueryCondition queryCondition1 = new QueryCondition("PageIndex", "1");
        QueryCondition queryCondition2 = new QueryCondition("PageSize", "50");
        request.setQueryConditionList(Arrays.asList(queryCondition1, queryCondition2));

        GetCommonPassengerResponseType response = client.getCommonPassenger(request);

        List<CommonPassenger> commonPassengers = MoreObjects.firstNonNull(response.getCommonPassengers(), Lists.newArrayList());

        List<CommonPassengerBO> commonPassengerBOS = Lists.newArrayList();
        for (CommonPassenger commonPassenger : commonPassengers) {
            if (CollectionUtils.isEmpty(commonPassenger.getCommonPassengerCardList())) {
                continue;
            }
            CommonPassengerCard commonPassengerCard = commonPassenger.getCommonPassengerCardList().stream().filter(passengerCard ->
                    "1".equals(passengerCard.getCardType()) && StringUtils.isNotBlank(passengerCard.getCardNo())).findFirst().orElse(null);

            if (commonPassengerCard != null) {
                commonPassengerBOS.add(new CommonPassengerBO(commonPassenger.getCNName(), PassportType.Identity_Card.getName(), commonPassengerCard.getCardNo()));
            }
        }

        CLogger.logRequestResponse(logTitle, request, StringUtils.EMPTY);

        return commonPassengerBOS;
    }




}
```
2. 缓存的数据模型发生变化需要修改缓存名称，防止反序列化错误。例如方法返回的响应由class A换到了class B，则需要修改注解中的缓存名称
   
### TODO
0. **优化metric和日志**
1. 分布式锁实现的请求合并
2. 缓存支持手动刷新
3. support separate or manual configured cache
5. 支持@CachePut
6. 支持@CacheEvict
7. redisPubSub改造(事件驱动，线程池)
8. 增加扩展点：metric/cache provider/cache manager
