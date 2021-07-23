package com.mime.test1;


import com.mime.rpc.api.ByeService;
import com.mime.rpc.api.HelloObject;
import com.mime.rpc.api.HelloService;
import com.mime.rpc.remoting.transport.RpcClientProxy;
import com.mime.rpc.remoting.transport.socket.client.SocketClient;
import com.mime.rpc.serializer.CommonSerializer;

/**
 * 测试用消费者（客户端）
 */
public class SocketTestClient {

    public static void main(String[] args) {
        SocketClient client = new SocketClient(CommonSerializer.KRYO_SERIALIZER);
        RpcClientProxy proxy = new RpcClientProxy(client);
        HelloService helloService = proxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);
        ByeService byeService = proxy.getProxy(ByeService.class);
        System.out.println(byeService.bye("Netty"));
    }
}
