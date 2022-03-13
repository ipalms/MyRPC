package com.mime.rpc.remoting.codec;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.PackageType;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.serializer.CommonSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
/**
 * custom protocol decoder
 *   0     1     2     3     4     5     6     7     8      9     10   11    12     13    14    15    16
 *   +-----+-----+-----+-----+-----+----+----+----+------+-----+-----+-----+-----+-----+-----+-----+-----+
 *   |   magic   code        |          messageType       | serializerCode        |       full length     |
 *   +-----------------------+--------+-------------------+-----------+--------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔数）  4B messageType（数据包类型） 4B  serializerCode（序列化算法）
 * 4B full length（消息长度）  body（object类型数据）
 * <p>
 */
/**
 * 通用的解码拦截器  可以实现LengthFieldBasedFrameDecoder类一样的依照自定义协议格式解码数据包
 * ReplayingDecoder、LengthFieldBasedFrameDecoder同继承自ByteToMessageDecoder，不可加Sharable注解
 * ReplayingDecoder在处理数据时可以认为所有的数据（ByteBuf）已经接收完毕，而不用判断接收数据的长度。
 * 即可以理解相较LengthFieldBasedFrameDecoder，ReplayingDecoder不用指定最大帧大小，理论上即可接收很大的数据帧
 */
/**
 * ReplayingDecoder传递一个专门的ByteBuf实现，当缓冲区中没有足够的数据时，会抛出某种类型的一个Error。
 * 例如当调用buf.readInt()时，只能假定缓冲区有4个或更多的字节。
 * 如果，缓冲区中确实有4个字节，会返回你所期望的这个值。否则，会抛出这个Error。
 * 如果ReplayingDecoder捕获了这个Error,会重置缓冲区的readerindex到缓冲区的开始位置
 * 并且当更多的数据到达缓冲区时，会再次调用decode()方法
 * （所以读取出的内容如果要加入到外部容器时应该要考虑到数据包不完整要反复加入的情况，这时应该先清空容器的内容再加）。
 *  ReplayingDecoder可以配合枚举类使得重新读时读指针指向特定的位置（多次decode时可以不用重复读一些内容，提高效率）
 */
public class CommonDecoder extends ReplayingDecoder<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CommonDecoder.class);
    //换成10进制 2001101400
    private static final int MAGIC_NUMBER = 0x77466258;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magic = in.readInt();
        //校验魔数
        if (magic != MAGIC_NUMBER) {
            logger.error("不识别的协议包: {}", magic);
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
            logger.error("不识别的数据包: {}", packageCode);
            throw new RpcException(RpcError.UNKNOWN_PACKAGE_TYPE);
        }
        int serializerCode = in.readInt();
        //找到该数据包序列化的形式
        CommonSerializer serializer = CommonSerializer.getByCode(serializerCode);
        if (serializer == null) {
            logger.error("不识别的反序列化器: {}", serializerCode);
            throw new RpcException(RpcError.UNKNOWN_SERIALIZER);
        }
        //根据自定义协议内容读入数据
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        //反序列化
        Object obj = serializer.deserialize(bytes, packageClass);
        out.add(obj);
    }
}
