package com.mime.rpc.registry;

import com.mime.rpc.entity.RpcRequest;

import java.net.InetSocketAddress;

/**
 * 服务发现接口
 */
public interface ServiceDiscovery {

    /**
     * 根据服务名称查找服务实体
     *
     * @param rpcRequest 服务请求体
     * @return 服务实体
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);

}
