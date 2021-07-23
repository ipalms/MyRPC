package com.mime.test1;


import com.mime.rpc.api.ByeService;
import com.mime.rpc.api.HelloObject;
import com.mime.rpc.api.HelloService;
import com.mime.rpc.remoting.transport.RpcClientProxy;
import com.mime.rpc.remoting.transport.netty.client.NettyClient;
import com.mime.rpc.serializer.CommonSerializer;

import java.io.IOException;

/**
 * 测试用Netty消费者

 */
public class NettyTestClient {

    public static void main(String[] args) throws IOException {
        NettyClient client = new NettyClient(CommonSerializer.KRYO_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "000111This is a message555");
        String res = helloService.hello(object);
        System.out.println("res "+res);
        String res1 = helloService.hello(object);
        System.out.println("res1 "+res1);
        String res3 = helloService.hello(object);
        System.out.println("res3 "+res3);
        String res4 = helloService.hello(object);
        System.out.println("res4 "+res4);
        String res5 = helloService.hello(object);
        System.out.println("res5 "+res5);
        ByeService byeService = rpcClientProxy.getProxy(ByeService.class);
        System.out.println(byeService.bye("000111Netty555"));
    }
}
