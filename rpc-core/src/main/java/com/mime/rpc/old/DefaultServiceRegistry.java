package com.mime.rpc.old;

import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的服务注册表 （基于内存old）
 */
public class DefaultServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServiceRegistry.class);

    //内存版保存注册了的服务清单，即服务注册表
    //map --键为服务实现的接口的全限定类名，值为实现类的对象
    private final static Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    //set 存储接口实现类的全限定类名用来校验该服务是否已经注册了
    private final static Set<String> registeredService = ConcurrentHashMap.newKeySet();

    /**
     * 将一个服务注册进注册表
     * @param service 待注册的服务实体
     */
    @Override
    public synchronized <T> void register(T service) {
        //接口实现类的全限定类名
        String serviceName = service.getClass().getCanonicalName();
        if(registeredService.contains(serviceName)) return;
        registeredService.add(serviceName);
        Class<?>[] interfaces = service.getClass().getInterfaces();
        if(interfaces.length == 0) {
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for(Class<?> i : interfaces) {
            serviceMap.put(i.getCanonicalName(), service);
        }
        logger.info("接口: {}  实现服务注册: {}", interfaces, serviceName);
    }

    /**
     * 根据服务名称获取服务实体
     * @param serviceName 服务实现接口的名称
     */
    @Override
    public synchronized Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
