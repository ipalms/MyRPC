package com.mime.rpc.api;

/**
 * 客户端和服务端都可以访问到通用的接口，但是只有服务端有这个接口的实现类
 * 测试用api的接口
 */
public interface HelloService {

    String hello(HelloObject object);

}
