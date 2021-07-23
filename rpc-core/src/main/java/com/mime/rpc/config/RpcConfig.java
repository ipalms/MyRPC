package com.mime.rpc.config;

import com.mime.rpc.factory.SingletonFactory;
import com.mime.rpc.loadbalancer.ConsistentHashLoadBalance;
import com.mime.rpc.loadbalancer.LoadBalancer;
import com.mime.rpc.loadbalancer.RandomLoadBalancer;
import com.mime.rpc.loadbalancer.RoundRobinLoadBalancer;
import com.mime.rpc.registry.nacos.NacosServiceDiscovery;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

//创建配置文件
public class RpcConfig {
    static Properties properties;
    public final static String DEFAULT_RPC_ADDRESS="127.0.0.1:8848";
    public final static String DEFAULT_SERVER_ADDRESS="127.0.0.1:9999";

    static {
        try (InputStream in = RpcConfig.class.getResourceAsStream("/RpcConfig.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    //获取服务端的地址
    public static String getServerAddress() {
        String value = properties.getProperty("server.address");
       /* if(value!=null){
            return value;
        }else {
            return DEFAULT_SERVER_ADDRESS;
        }*/
        return Objects.requireNonNullElse(value, DEFAULT_SERVER_ADDRESS);
    }

    //获取注册中心地址
    public static String getRpcAddress() {
        String value = properties.getProperty("nacos.address");
        /*if(value!=null){
            return value;
        }else {
            return DEFAULT_RPC_ADDRESS;
        }*/
        return Objects.requireNonNullElse(value, DEFAULT_RPC_ADDRESS);
    }

    //获取客户端选择的负载均衡算法
    public static LoadBalancer getLoadBalancer(){
        String value = properties.getProperty("client.loadbalancer");
        switch (value) {
            case "random":
                return SingletonFactory.getInstance(RandomLoadBalancer.class);
            case "hash":
                return SingletonFactory.getInstance(ConsistentHashLoadBalance.class);
            default:
                return SingletonFactory.getInstance(RoundRobinLoadBalancer.class);
        }
    }
}