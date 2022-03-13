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
 * 根据一致性哈希算法，每一个请求就就被绑定到了按照顺时针找最近服务节点
 * 具体的：
 * 一致性哈希将整个哈希值空间组织成一个虚拟的圆环，如假设某空间哈希函数H的值空间是0-2^32-1（即哈希值是一个32位无符号整形）
 * 下一步将各个服务器使用哈希算法计算出每台机器的位置，具体可以使用服务器的IP地址或者主机名作为关键字，并且是按照顺时针排列
 * 每一次请求时，利用调用方法的全限定类型加参数使用相同Hash算法计算出数据的哈希值,并由此确定数据在此哈希环上的位置
 *
 * 这样得到的哈希调度方法，有很高的容错性和可扩展性（服务节点变化的话）只需要改变部分数据定位
 * 常用于缓存中间件的负载均衡选择（redis使用的并非一致性Hash算法，而是使用的16834个hash槽，映射槽是CRC(key)%16384实现）
 * 【集群模式下的负载均衡算法，有利于节点的变化是更少的数据收到牵连（仅仅部分数据需要转移）】
 *
 * 参照dubbo实现的
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

        /**
         * 各个服务器使用哈希算法计算出每台机器的位置
         * 具体可以使用服务器的IP地址+编号作为关键字（为了请求更加平均分配到已有节点），并且是按照顺时针排列
         * 以这个规则而言，同样的主机hash后的结果依旧是一样的，即一台主机任然对应那一片数据
         *
         * 当节点很少的时候可能会出现这样的分布情况，某个节点可能会承担大部分请求（具体联想环状的空间）。
         * 这种情况就叫做数据倾斜，本类的构造方法解决了数据倾斜问题
         * 加入虚拟节点。首先一个服务器根据需要可以有多个虚拟节点。
         * 假设一台服务器有n个虚拟节点。那么哈希计算时，可以使用IP+端口+编号的形式进行哈希值计算。
         * 其中的编号就是0到n的数字。由于IP+端口是一样的，所以这n个节点都是指向的同一台机器。
         */
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

        //根据一致性哈希算法，每一个请求就就被绑定到了按照顺时针（hash空间是圆环）找最近服务节点
        public Instance selectForKey(long hashCode) {
            //tailMap（）方法用于返回此映射，其键大于或等于fromKey的部分视图
            //firstEntry（）用于返回获取第一个（排在最低的）对象的值
            Map.Entry<Long, Instance> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            //如果这个hash地址在最后面，顺时针的话就将请求映射到第一个机器
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }
}
