package com.mime.rpc.remoting.transport.netty.client;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.factory.SingletonFactory;
import com.mime.rpc.loadbalancer.LoadBalancer;
import com.mime.rpc.loadbalancer.RandomLoadBalancer;
import com.mime.rpc.registry.ServiceDiscovery;
import com.mime.rpc.registry.nacos.NacosServiceDiscovery;
import com.mime.rpc.remoting.codec.CommonDecoder;
import com.mime.rpc.remoting.codec.CommonEncoder;
import com.mime.rpc.remoting.codec.MessageDecoder;
import com.mime.rpc.remoting.handler.RequestHandler;
import com.mime.rpc.remoting.transport.RpcClient;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * NIO方式消费侧客户端类
 * NIO的客户端应该要接收服务器的调用的返回值(这个返回值无论如何都是要阻塞式的获取)
 * 即应该找一个能够存储结果的类
 */
@Slf4j
public class NettyClient implements RpcClient {

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final ServiceDiscovery serviceDiscovery;
    private final ChannelProvider channelProvider;
    private final UnprocessedRequests unprocessedRequests;


    //数个客户端的构造器
    public NettyClient() {
        this(DEFAULT_SERIALIZER);
    }

    public NettyClient(Integer serializer) {
        //初始化EventLoopGroup、Bootstrap等资源
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //连接超时的时间
                //如果超过此时间或无法建立连接，则连接失败。
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20000)
                //channel连接建立后会初始化添加这些channel
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        //如果在15秒内没有数据发送到服务器，则发送心跳请求
                        p.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        //编解码器
                        p.addLast(new CommonEncoder(CommonSerializer.getByCode(serializer)));
                        p.addLast(new MessageDecoder());
                        //处理响应数据包的handler
                        p.addLast(new NettyClientHandler());
                    }
                });
        this.serviceDiscovery = SingletonFactory.getInstance(NacosServiceDiscovery.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        //构建返回值--也可以用netty提供的Promise
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        //获取服务地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        log.info("负载均衡选择结果：{}",inetSocketAddress.toString());
        //获取服务器地址相关通道
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            //放置未处理的请求
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) future -> {
                //由其他线程调用回调方法
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcRequest.toString());
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        //返回的相当于句柄
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        //如果之前没有连接过或则channel通道失效了
        if (channel == null) {
            //重连连接
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    /**
     * 连接服务器并获取通道，以便可以向服务器发送rpc消息
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        //连接的过程是异步的
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                //放入结果（channel通道）
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        //阻塞式获得结果
        return completableFuture.get();
        //return bootstrap.connect(inetSocketAddress).sync().channel();
    }

    public void close() {
        group.shutdownGracefully();
    }
}
