package com.mime.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.registry.ServiceRegistry;
import com.mime.rpc.registry.nacos.util.NacosUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Nacos服务注册中心
 */
public class NacosServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NacosServiceRegistry.class);

    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            //服务注册
            NacosUtil.registerService(serviceName, inetSocketAddress);
        } catch (NacosException e) {
            logger.error("注册服务时有错误发生:", e);
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
    }
}
