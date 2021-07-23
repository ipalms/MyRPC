package com.mime.rpc.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.entity.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡算法思路：先获取提供服务的所有实例的原始哈希值
 * 利用这个哈希值判断是否存在过选择器（用于选择出具体负载均衡下的服务器实例），或者提供服务的所有实例状态有变化
 * 如果存在这个选择器且获取到的提供服务的所有实例没有变化就使用该已经创建的选择器进行选择
 * refer to dubbo consistent hash load balance:
 * https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 */
@Slf4j
public class ConsistentHashLoadBalance implements LoadBalancer{
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();


    @Override
    public Instance select(List<Instance> instances, RpcRequest rpcRequest) {
        //hashCode方法可以被重写并返回重写后的值，identityHashCode会返回对象的hash值而不管对象是否重写了hashCode方法
        int identityHashCode = System.identityHashCode(instances);
        String rpcServiceName = rpcRequest.getInterfaceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        //检查提供服务的所有实例有无变化
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(instances, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        //根据调用方法的全限定类型加参数 整体做hash
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        private final TreeMap<Long, Instance> virtualInvokers;

        private final int identityHashCode;

        ConsistentHashSelector(List<Instance> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (Instance invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker.toInetAddr() + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        public Instance select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        public Instance selectForKey(long hashCode) {
            //tailMap（）方法用于返回此映射，其键大于或等于fromKey的部分视图
            //firstEntry（）用于返回获取第一个（排在最低的）对象的值
            Map.Entry<Long, Instance> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }
}
