package com.mime.rpc.serializer;

import com.mime.rpc.serializer.hessian.HessianSerializer;
import com.mime.rpc.serializer.json.GsonSerializer;
import com.mime.rpc.serializer.json.JsonSerializer;
import com.mime.rpc.serializer.kryo.KryoSerializer;
import com.mime.rpc.serializer.protobuf.ProtobufSerializer;

/**
 * 序列化就是把对象转换为二进制数据，反序列化就把二进制数据转换为对象
 * 通用的序列化反序列化接口
 */
public interface CommonSerializer {

    Integer KRYO_SERIALIZER = 0;
    Integer JSON_SERIALIZER = 1;
    Integer HESSIAN_SERIALIZER = 2;
    Integer PROTOBUF_SERIALIZER = 3;
    Integer GSON_SERIALIZER = 4;

    Integer DEFAULT_SERIALIZER = KRYO_SERIALIZER;

    /**
     * 应当单例设计---这里先不管了
     */
    static CommonSerializer getByCode(int code) {
        switch (code) {
            case 0:
                return new KryoSerializer();
            case 1:
                return new JsonSerializer();
            case 2:
                return new HessianSerializer();
            case 3:
                return new ProtobufSerializer();
            case 4:
                return new GsonSerializer();
            default:
                return null;
        }
    }

    byte[] serialize(Object obj);

    Object deserialize(byte[] bytes, Class<?> clazz);

    int getCode();

}
