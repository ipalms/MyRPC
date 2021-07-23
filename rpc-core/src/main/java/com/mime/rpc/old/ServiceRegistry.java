package com.mime.rpc.old;

/**
 * 服务注册接口
 */
public interface ServiceRegistry {

    /**
     * 将一个服务注册进注册表
     * @param service 待注册的服务实体
     * @param <T> 服务实体类
     */
    <T> void register(T service);

    /**
     * 根据服务名称获取服务实体
     * @param serviceName 服务实现接口的名称
     * @return 服务实体
     */
    Object getService(String serviceName);

}
