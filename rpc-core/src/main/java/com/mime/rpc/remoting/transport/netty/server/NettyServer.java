package com.mime.rpc.remoting.transport.netty.server;

import com.mime.rpc.hook.ShutdownHook;
import com.mime.rpc.provider.ServiceProviderImpl;
import com.mime.rpc.registry.nacos.NacosServiceRegistry;
import com.mime.rpc.remoting.codec.CommonDecoder;
import com.mime.rpc.remoting.codec.CommonEncoder;
import com.mime.rpc.remoting.codec.MessageDecoder;
import com.mime.rpc.remoting.transport.AbstractRpcServer;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;


import java.util.concurrent.TimeUnit;

/**
 * NIO方式服务提供侧
 */
public class NettyServer extends AbstractRpcServer {

    private final CommonSerializer serializer;

    public NettyServer() {
        this(DEFAULT_SERIALIZER);
    }

    public NettyServer(Integer serializer) {
        serviceRegistry = new NacosServiceRegistry();
        serviceProvider = new ServiceProviderImpl();
        this.serializer = CommonSerializer.getByCode(serializer);
        scanServices();
    }

    @Override
    public void start() {
        //注册（调用）钩子函数
        ShutdownHook.getShutdownHook().addClearAllHook();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 1024)
                 /*   //是否开启 TCP 底层心跳机制,当设置该选项以后，如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文。
                    .childOption(ChannelOption.SO_KEEPALIVE, true)*/
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //30s内没有读取到channel的消息会产生读事件
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new CommonEncoder(serializer))
                                    .addLast(new MessageDecoder())
                                    .addLast(new NettyServerHandler());
                        }
                    });
            //服务端绑定端口（不指定ip的话默认ip就是0.0.0.0任意网卡地址）
            ChannelFuture future = serverBootstrap.bind(port).sync();
            //这里sync会阻塞主线程继续执行，直到其他地方将channel通道关闭了--即其他地方调用了channel.close（）方法
            //目的是优雅的释放nio资源
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("启动服务器时有错误发生: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
