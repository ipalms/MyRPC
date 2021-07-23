package com.mime.test1;


import com.mime.rpc.annotation.ServiceScan;
import com.mime.rpc.remoting.transport.RpcServer;
import com.mime.rpc.remoting.transport.netty.server.NettyServer;
import com.mime.rpc.serializer.CommonSerializer;

/**
 * 测试用Netty服务提供者（服务端）
 */
@ServiceScan
public class NettyTestServer {

    public static void main(String[] args) {
        RpcServer server = new NettyServer(CommonSerializer.KRYO_SERIALIZER);
        server.start();
    }
}
