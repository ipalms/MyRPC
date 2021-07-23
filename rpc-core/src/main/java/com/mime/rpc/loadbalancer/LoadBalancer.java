package com.mime.rpc.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.entity.RpcRequest;

import java.util.List;


/**
 * RPC内部的负载均衡相当于本地级别（客户端）负载均衡
 * Nginx是服务器负载均衡（也可称之为集中式LB，将请求集中起来做负载均衡再转发给对应提供服务主机），
 * 客户端所有请求都会交给nginx,然后由nginx实现转发请求。即负载均衡是由服务端实现的。
 *
 * Ribbon本地负载均衡（也可称之为进程式LB，将LB逻辑集成到消费方，消费方将从服务注册中心获得那些发侮可用，然后按照算法选出目的主机）
 * 在调用微服务接口时候，会在注册中心上获取注册信息服务列表之后缓存到JVM本地，根据负载均衡算法选择出
 * 从而在本地实现RPC远程服务调用技术。
 */
public interface LoadBalancer {

    Instance select(List<Instance> instances, RpcRequest rpcRequest);

}
