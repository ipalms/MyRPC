package com.mime.rpc.registry.nacos.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.config.RpcConfig;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理Nacos连接等工具类
 * 实例发送心跳包到服务器心跳机制：
 *   --nacos在服务注册方（服务实例）会发送心跳给nacos服务器，默认为每5s由实例向nacos服务中心发送心跳包
 *   --nacos的定时任务粒度是service，也就是每个service就开启一个定时任务，这种控制带来的就是更灵敏。但是占用内存更高。
 *   --心跳是每5秒执行一次
 *   --如果超过15秒没有心跳那么nacos注册中心会将该实例设置为不健康状态
 *   --如果超过30秒没有心跳那么nacos注册中心就会删除该节点
 * nacos默认情况下是将节点注册为临时节点类型
 *      --即AP节点类型，通过以上的心跳措施维系节点--由实例向naocs注册中心发心跳包
 *      --临时节点 节点数据不会持久化保存，服务器在30s内没收到节点会剔除该节点
 *      --nacos默认注册的是临时节点，即处于AP模式，在该模式下即使发生网络分区还是可以进行节点的注册
 *      --nacos集群模式下实例发生变化是通过广播通知其他节点更新实例状态
 * nacos可以通过配置切换到CP模式，即注册的是永久节点
 *      --在这种模式下，心跳包将由nacos注册中心向实例每20s发送一次心跳包，若没有反馈就会将该节点标记为不健康
 *      --持久节点在没有响应注册中心发来的心跳包并不会被注册中心剔除，这也是两种节点类型不同之一
 *      --CP模式下nacos注册中心以Raft协议为集群运行模式，因此网络分区下不能够注册实例
 *      --CP模式下对数据的修改，包括服务实例的变化、业务相关数据的变化
 *      都应由leader节点负责并将结果同步给其他非leader节点（通过将日志复制给其他节点实现数据同步）
 *      也就是说只有leader节点宕机的情况下的时刻，整个集群是不可用的。在此期间需要根据选举算法选出一个新的leader节点
 *
 * 直白解释CAP理论:
 * 一致性（C）：在分布式系统中的所有数据备份，在同一时刻是否同样的值。（等同于所有节点访问同一份最新的数据副本）
 * 可用性（A）：在集群中一部分节点故障后，集群整体是否还能响应客户端的读写请求。（对数据更新具备高可用性）
 * 分区容错性（P）：以实际效果而言，分区相当于对通信的时限要求。系统如果不能在时限内达成数据一致性，就意味着发生了分区的情况，必须就当前操作在C和A之间做出选择。
 * 即网络因为当前通信不好发生分区后系统还要继续履行职责。在分布式的环境下，网络无法做到100%可靠，有可能出现故障，因此分区是一个必须的选项
 * 如果选择了CA而放弃了P，若发生分区现象，为了保证C，系统需要禁止写入，此时就与A发生冲突，如果是为了保证A，则会出现正常的分区可以写入数据，有故障的分区不能写入数据，则与C就冲突了。
 * 因此分布式系统理论上不可能选择CA架构，而必须选择CP或AP架构。
 */
public class NacosUtil {

    private static final Logger logger = LoggerFactory.getLogger(NacosUtil.class);

    //NamingService是nacos提供的管理服务注册、注销、发现的接口，唯一实现是NacosNamingService
    private static final NamingService namingService;
    //记录注册过的接口名称
    private static final Set<String> serviceNames = new HashSet<>();
    //使用缓存记录服务实例，不用每次请求的时候去拉取服务实例。服务实例注册回调函数来维系服务实例的变化
    private static final Map<String, List<Instance>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static InetSocketAddress address;

    private static String serverAddress;


    static {
        serverAddress = RpcConfig.getRpcAddress();
        namingService = getNacosNamingService();
    }

    public static NamingService getNacosNamingService() {
        try {
            //NamingFactory 根据ip地址创建NamingService实例的工厂
            //NacosFactory 也封装了获得NamingService实例的方法（实际任然是封装的NamingFactory方法）
            //NacosNamingService（NamingService唯一实现类）会在构造方法中调用init()方法
            //这个方法会调用一系列方法产生一系列线程来进行后台工作
            //（如处理事件响应的线程--EventDispatcher、处理与nacos服务器心跳联系的线程--BeatReactor、处理服务实例的变化的线程--HostReactor）
            return NamingFactory.createNamingService(serverAddress);
        } catch (NacosException e) {
            logger.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException(RpcError.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }

    public static void registerService(String serviceName, InetSocketAddress address) throws NacosException {
        //注册服务
        namingService.registerInstance(serviceName, address.getHostName(), address.getPort());
        NacosUtil.address = address;
        serviceNames.add(serviceName);
    }

    public static List<Instance> getAllInstance(String serviceName) throws NacosException {
        //查看内存中是否有此服务实例信息，有就直接返回
        if(SERVICE_ADDRESS_MAP.containsKey(serviceName)){
            return SERVICE_ADDRESS_MAP.get(serviceName);
        }
        List<Instance> instances = null;
        try {
            instances = namingService.getAllInstances(serviceName);
            SERVICE_ADDRESS_MAP.put(serviceName,instances);
            //注册回调函数监听变化
            registerWatcher(serviceName);
        } catch (Exception e) {
            logger.info("---获取服务实例失败---:"+serviceName);
        }
        return instances;
    }

    /**
     * 注册回调函数监听实例的变化并修改内存中的服务实例状态
     * 通过源码可知，在第一次执行subscribe方法时就会触发一次onEvent()方法（之后只有提供服务的所有实例发生变化【服务实例增加、减少、信息被修改】以后才会触发onEvent()方法）
     * 触发事件NamingEvent（Event唯一实现类），但是服务器所有实例并没有变化
     * 所以需要判断这个时候实例的hashcode值是否一样，一样则说明没被修改
     *
     * 源码（类EventDispatcher）是将所有改变了的服务实例放入BlockingQueue<ServiceInfo> changedServices阻塞队列中，特殊的是第一次调用subscribe（）方法的时候也会把对应的ServiceInfo加入阻塞队列
     * 然后由这个类开辟的一个线程就会一直处理阻塞队列中的任务（加入的ServiceInfo），并且调用已存储的该服务实例的回调函数onEvent（）
     * 这个存放回调函数的集合申明ConcurrentMap<String, List<EventListener>> observerMap，当调用subscribe（）方法订阅时最终就会将此回调函数加入实例对应的List集合中
     */
    private static void registerWatcher(String serviceName) throws NacosException {
        namingService.subscribe(serviceName, new EventListener() {
            @Override
            public void onEvent(Event event) {
                List<Instance> instances = ((NamingEvent)event).getInstances();
                if(instances.size() == 0){
                    logger.info("------服务下线------:"+serviceName);
                    SERVICE_ADDRESS_MAP.remove(serviceName);
                }else if(instances.size()==SERVICE_ADDRESS_MAP.get(serviceName).size()){
                    if(instances.hashCode()==SERVICE_ADDRESS_MAP.get(serviceName).hashCode()){
                        logger.info("实例状态并没有改变");
                    }else {
                        logger.info("------服务实例有变化-------"+serviceName);
                        SERVICE_ADDRESS_MAP.put(serviceName,instances);
                    }
                }else {
                    logger.info("------服务实例有变化-------"+serviceName);
                    SERVICE_ADDRESS_MAP.put(serviceName,instances);
                }
            }
        });
    }


    //当服务器关闭的同时注销nacos中已注册存在的数据，实际上实例在不发送心跳包后会被服务器标记为不健康继而被移除
    public static void clearRegistry() {
        if(!serviceNames.isEmpty() && address != null) {
            String host = address.getHostName();
            int port = address.getPort();
            for (String serviceName : serviceNames) {
                try {
                    //遍历注销
                    namingService.deregisterInstance(serviceName, host, port);
                } catch (NacosException e) {
                    logger.error("注销服务 {} 失败", serviceName, e);
                }
            }
        }
    }
}
