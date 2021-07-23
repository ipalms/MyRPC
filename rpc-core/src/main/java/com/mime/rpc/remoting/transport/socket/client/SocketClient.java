package com.mime.rpc.remoting.transport.socket.client;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.ResponseCode;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.factory.SingletonFactory;
import com.mime.rpc.loadbalancer.LoadBalancer;
import com.mime.rpc.loadbalancer.RandomLoadBalancer;
import com.mime.rpc.registry.ServiceDiscovery;
import com.mime.rpc.registry.nacos.NacosServiceDiscovery;
import com.mime.rpc.remoting.transport.RpcClient;
import com.mime.rpc.remoting.transport.socket.util.ObjectReader;
import com.mime.rpc.remoting.transport.socket.util.ObjectWriter;
import com.mime.rpc.serializer.CommonSerializer;
import com.mime.rpc.util.RpcMessageChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Socket方式远程方法调用的消费者（BIO 客户端）
 */
public class SocketClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);

    private final ServiceDiscovery serviceDiscovery;

    //在客户端设置序列化方式
    private final CommonSerializer serializer;

    public SocketClient() {
        this(DEFAULT_SERIALIZER);
    }
    public SocketClient(Integer serializer) {
        this.serviceDiscovery = SingletonFactory.getInstance(NacosServiceDiscovery.class);
        this.serializer = CommonSerializer.getByCode(serializer);
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        if(serializer == null) {
            logger.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            //向socket写出自定义协议的二进制数据
            ObjectWriter.writeObject(outputStream, rpcRequest, serializer);
            //读取响应
            Object obj = ObjectReader.readObject(inputStream);
            RpcResponse rpcResponse = (RpcResponse) obj;
            if (rpcResponse == null) {
                logger.error("服务调用失败，service：{}", rpcRequest.getInterfaceName());
                throw new RpcException(RpcError.SERVICE_INVOCATION_FAILURE, " service:" + rpcRequest.getInterfaceName());
            }
            if (rpcResponse.getStatusCode() == null || rpcResponse.getStatusCode() != ResponseCode.SUCCESS.getCode()) {
                logger.error("调用服务失败, service: {}, response:{}", rpcRequest.getInterfaceName(), rpcResponse);
                throw new RpcException(RpcError.SERVICE_INVOCATION_FAILURE, " service:" + rpcRequest.getInterfaceName());
            }
            RpcMessageChecker.check(rpcRequest, rpcResponse);
            return rpcResponse;
        } catch (IOException e) {
            logger.error("调用时有错误发生：", e);
            throw new RpcException("服务调用失败: ", e);
        }
    }
}
