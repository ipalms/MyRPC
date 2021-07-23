package com.mime.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.config.RpcConfig;
import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.loadbalancer.LoadBalancer;
import com.mime.rpc.registry.ServiceDiscovery;
import com.mime.rpc.registry.nacos.util.NacosUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;


public class NacosServiceDiscovery implements ServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(NacosServiceDiscovery.class);

    //服务发现需要一种负载均衡策略
    private static LoadBalancer loadBalancer;

    static {
        loadBalancer= RpcConfig.getLoadBalancer();
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        try {
            List<Instance> instances = NacosUtil.getAllInstance(rpcRequest.getInterfaceName());
            if(instances.size() == 0) {
                logger.error("找不到对应的服务: " + rpcRequest.getInterfaceName());
                throw new RpcException(RpcError.SERVICE_NOT_FOUND);
            }
            //负载均衡选择
            Instance instance = loadBalancer.select(instances,rpcRequest);
            return new InetSocketAddress(instance.getIp(), instance.getPort());
        } catch (NacosException e) {
            logger.error("获取服务时有错误发生:", e);
        }
        return null;
    }
}
