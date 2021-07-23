package com.mime.test1;


import com.mime.rpc.annotation.ServiceScan;
import com.mime.rpc.remoting.transport.RpcServer;
import com.mime.rpc.remoting.transport.socket.server.SocketServer;
import com.mime.rpc.serializer.CommonSerializer;

/**
 * 测试用服务提供方（服务端）
 */
@ServiceScan
public class SocketTestServer {

    public static void main(String[] args) {
        RpcServer server = new SocketServer(CommonSerializer.HESSIAN_SERIALIZER);
        server.start();
    }
}
