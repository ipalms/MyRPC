package com.mime.rpc.old;

import com.mime.rpc.remoting.transport.RpcClient;
import com.mime.rpc.remoting.codec.CommonDecoder;
import com.mime.rpc.remoting.codec.CommonEncoder;
import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.serializer.CommonSerializer;
import com.mime.rpc.util.RpcMessageChecker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NIO方式消费侧客户端类
 * NIO的客户端应该要接收服务器的调用的返回值(这个返回值无论如何都是要阻塞式的获取)
 * 即应该找一个能够存储结果的类
 */
public class NettyClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
    private static final Bootstrap bootstrap;

    private CommonSerializer serializer;

    static {
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    private String host;
    private int port;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        if(serializer == null) {
            logger.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                logger.info("ch:{}",ch);
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new CommonDecoder())
                        .addLast(new CommonEncoder(serializer))
                        .addLast(new NettyClientHandler());
            }
        });
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            logger.info("客户端连接到服务器 {}:{}", host, port);
            Channel channel = future.channel();
            if (channel != null) {
                channel.writeAndFlush(rpcRequest).addListener(future1 -> {
                    if (future1.isSuccess()) {
                        logger.info(String.format("客户端发送消息: %s", rpcRequest.toString()));
                    } else {
                        logger.error("发送消息时有错误发生: ", future1.cause());
                    }
                });
                //这里sync会阻塞主线程继续执行，直到其他地方将channel通道关闭了--即其他地方调用了channel.close（）方法
                channel.closeFuture().sync();
                AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcRequest.getRequestId());
                //获得调用的结果--通过AttributeKey
                RpcResponse rpcResponse = channel.attr(key).get();
                //检验调用结果，是否为正常调用
                RpcMessageChecker.check(rpcRequest, rpcResponse);
                return rpcResponse.getData();
            }
        } catch (InterruptedException e) {
            logger.error("发送消息时有错误发生: ", e);
        }
        return null;
    }
}
