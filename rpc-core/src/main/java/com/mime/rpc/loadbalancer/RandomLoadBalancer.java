package com.mime.rpc.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.entity.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public Instance select(List<Instance> instances, RpcRequest rpcRequest) {
        return instances.get(new Random().nextInt(instances.size()));
    }
}
