package com.mime.rpc.remoting.transport.socket.server;



import com.mime.rpc.factory.ThreadPoolFactory;
import com.mime.rpc.hook.ShutdownHook;
import com.mime.rpc.provider.ServiceProviderImpl;
import com.mime.rpc.registry.nacos.NacosServiceRegistry;
import com.mime.rpc.remoting.handler.RequestHandler;
import com.mime.rpc.remoting.transport.AbstractRpcServer;
import com.mime.rpc.serializer.CommonSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * Socket方式远程方法调用的提供者（服务端）
 */
public class SocketServer extends AbstractRpcServer {

    private final ExecutorService threadPool;
    private final CommonSerializer serializer;
    private final RequestHandler requestHandler = new RequestHandler();

    public SocketServer() {
        this(DEFAULT_SERIALIZER);
    }

    public SocketServer(Integer serializer) {
        threadPool = ThreadPoolFactory.createDefaultThreadPool("socket-rpc-server");
        this.serviceRegistry = new NacosServiceRegistry();
        this.serviceProvider = new ServiceProviderImpl();
        this.serializer = CommonSerializer.getByCode(serializer);
        scanServices();
    }

    @Override
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(port));
            logger.info("服务器启动……");
            ShutdownHook.getShutdownHook().addClearAllHook();
            Socket socket;
            while ((socket = serverSocket.accept()) != null) {
                logger.info("消费者连接: {}:{}", socket.getInetAddress(), socket.getPort());
                threadPool.execute(new SocketRequestHandlerThread(socket, requestHandler, serializer));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            logger.error("服务器启动时有错误发生:", e);
        }
    }
}
