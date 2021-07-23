package com.mime.rpc.remoting.transport.socket.util;



import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.enumeration.PackageType;
import com.mime.rpc.serializer.CommonSerializer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 向socket写入封装协议的数据包
 */
public class ObjectWriter {

    private static final int MAGIC_NUMBER = 0x77466258;

    public static void writeObject(OutputStream outputStream, Object object, CommonSerializer serializer) throws IOException {
        //写入魔数
        outputStream.write(intToBytes(MAGIC_NUMBER));
        if (object instanceof RpcRequest) {
            outputStream.write(intToBytes(PackageType.REQUEST_PACK.getCode()));
        } else {
            outputStream.write(intToBytes(PackageType.RESPONSE_PACK.getCode()));
        }
        outputStream.write(intToBytes(serializer.getCode()));
        byte[] bytes = serializer.serialize(object);
        outputStream.write(intToBytes(bytes.length));
        outputStream.write(bytes);
        outputStream.flush();
    }

    //int 转 byte 数组
    private static byte[] intToBytes(int value) {
        byte[] des = new byte[4];
        des[3] =  (byte) ((value>>24) & 0xFF);
        des[2] =  (byte) ((value>>16) & 0xFF);
        des[1] =  (byte) ((value>>8) & 0xFF);
        des[0] =  (byte) (value & 0xFF);
        return des;
    }
}
