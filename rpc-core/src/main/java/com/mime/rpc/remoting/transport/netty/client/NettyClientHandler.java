package com.mime.rpc.remoting.transport.netty.client;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.factory.SingletonFactory;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;

/**
 * Netty客户端侧处理器
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse<Object>> {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private final NettyClient nettyClient;
    private final UnprocessedRequests unprocessedRequests;

    public NettyClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyClient=SingletonFactory.getInstance(NettyClient.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse<Object> msg) throws Exception {
        logger.info(String.format("客户端接收到消息: %s", msg));
        //放入请求结果
        unprocessedRequests.complete(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("过程调用时有错误发生:");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            //触发读事件
            if (state == IdleState.WRITER_IDLE) {
                logger.info("发送心跳包 [{}]", ctx.channel().remoteAddress());
                //使用的是默认的序列化方式
                Channel channel = nettyClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcRequest rpcRequest = new RpcRequest();
                rpcRequest.setHeartBeat(true);
                channel.writeAndFlush(rpcRequest).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
