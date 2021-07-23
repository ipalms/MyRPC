package com.mime.rpc.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mime.rpc.entity.RpcRequest;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

//参考com.netflix.loadbalancer 的 RoundRobinRule策略
public class RoundRobinLoadBalancer implements LoadBalancer {


    private static final ConcurrentHashMap<String, RoundRobinSelector> selectors = new ConcurrentHashMap<>();

    @Override
    public Instance select(List<Instance> instances, RpcRequest rpcRequest) {
        int identityHashCode = System.identityHashCode(instances);
        String rpcServiceName = rpcRequest.getInterfaceName();
        RoundRobinSelector selector = selectors.get(rpcServiceName);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new RoundRobinSelector(identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        return instances.get(selector.getAndIncrement(instances.size()));
/*        if (index >= instances.size()) {
            index %= instances.size();
        }
        return instances.get(index++);*/
    }

    static class RoundRobinSelector {
        private final int identityHashCode;
        private final AtomicInteger atomicInteger= new AtomicInteger(0);
        RoundRobinSelector(int identityHashCode) {
            this.identityHashCode = identityHashCode;
        }

        //死循环+CAS增加请求次数值，并返回此次要请求的主机编号
        public int getAndIncrement(int modulo) {
            for (;;) {
                int current = atomicInteger.get();
                int next = (current + 1) % modulo;
                if (atomicInteger.compareAndSet(current, next))
                    return next;
            }
        }

        public final int getAndIncrement1() {
            int current;
            int next;
            do {
                current = this.atomicInteger.get();
                // 超过最大值重置为0,重新计数 2147483647 Integer.MAX_VALUE
                next = current >= 2147483647 ? 0 : current + 1;
                // 自旋锁
            } while (!atomicInteger.compareAndSet(current, next));
            return next;
        }
    }
}
