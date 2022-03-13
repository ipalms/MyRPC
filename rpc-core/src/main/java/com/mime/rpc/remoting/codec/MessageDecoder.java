package com.mime.rpc.remoting.codec;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.PackageType;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    //允许接收最大帧长 8MB
    private final static int MAX_FRAME_LENGTH = 8 * 1024 * 1024;

    //换成10进制 2001101400
    private final static int MAGIC_NUMBER = 0x77466258;

    public MessageDecoder() {
        //lengthFieldOffset:魔数为4B，数据包类型4B，序列化选择算法4B，然后是全长。所以长度起始值为12
        //lengthFieldLength:表示长度的位长度是4B。所以值为4(注意这个长度是整个数据包的长度，而非除去了魔数、请求类型等等的长度)，而这样会影响第三个字段
        //lengthAdjustment:如果这个全长是整个数据包长度，那么这里就要调整位-12。如果仅仅是剩下数据实体的长度就不需要调整添0即刻
        //initialBytesToStrip:我们将手动检查魔数，因此不要删除任何字节。所以值为0
        this(MAX_FRAME_LENGTH, 12, 4, 0, 0);
    }

    /**
     * @param maxFrameLength      是指最大包长度，如果Netty最终生成的数据包超过这个长度，Netty就会报错（该数据包也会被丢弃）
     * @param lengthFieldOffset   长度字段偏移量。长度字段是跳过指定长度字节的字段。
     * @param lengthFieldLength   长度字段中的字节数。
     * @param lengthAdjustment    要添加到长度字段值的补偿值（读取长度代表的是总长情况要补偿减去前面的字节数，如果仅代表读入实体数据的长度就添0就行）
     * @param initialBytesToStrip 跳过的字节数。如果需要接收所有的header+body数据，该值为0如果只想接收body数据，则需要跳过header消耗的字节数。
     */
    public MessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                          int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        int magic = in.readInt();
        //校验魔数
        if (magic != MAGIC_NUMBER) {
            log.error("不识别的协议包: {}", magic);
            throw new RpcException(RpcError.UNKNOWN_PROTOCOL);
        }
        //packageCode 该数据包类型（请求/响应）
        int packageCode = in.readInt();
        Class<?> packageClass;
        if (packageCode == PackageType.REQUEST_PACK.getCode()) {
            packageClass = RpcRequest.class;
        } else if (packageCode == PackageType.RESPONSE_PACK.getCode()) {
            packageClass = RpcResponse.class;
        } else {
            log.error("不识别的数据包: {}", packageCode);
            throw new RpcException(RpcError.UNKNOWN_PACKAGE_TYPE);
        }
        int serializerCode = in.readInt();
        //找到该数据包序列化的形式
        CommonSerializer serializer = CommonSerializer.getByCode(serializerCode);
        if (serializer == null) {
            log.error("不识别的反序列化器: {}", serializerCode);
            throw new RpcException(RpcError.UNKNOWN_SERIALIZER);
        }
        //根据自定义协议内容读入数据
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        //反序列化
        return serializer.deserialize(bytes, packageClass);
    }
}
