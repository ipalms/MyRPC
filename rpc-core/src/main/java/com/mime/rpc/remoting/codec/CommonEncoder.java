package com.mime.rpc.remoting.codec;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.enumeration.PackageType;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * 通用的编码拦截器
 */
public class CommonEncoder extends MessageToByteEncoder<Object> {

    private static final int MAGIC_NUMBER = 0x77466258;

    private final CommonSerializer serializer;

    public CommonEncoder(CommonSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        out.writeInt(MAGIC_NUMBER);
        if (msg instanceof RpcRequest) {
            out.writeInt(PackageType.REQUEST_PACK.getCode());
        } else {
            out.writeInt(PackageType.RESPONSE_PACK.getCode());
        }
        out.writeInt(serializer.getCode());
        //依据客户端/服务端传入的序列化方式进行序列化
        byte[] bytes = serializer.serialize(msg);
        //写入的长度是实体数据的长度而非总长度
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
