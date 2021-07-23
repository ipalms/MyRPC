package com.mime.rpc.serializer.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.enumeration.SerializerCode;
import com.mime.rpc.exception.SerializeException;
import com.mime.rpc.serializer.CommonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 使用JSON格式的序列化器--jackson库
 */
public class JsonSerializer implements CommonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            logger.error("序列化时有错误发生:", e);
            throw new SerializeException("序列化时有错误发生");
        }
    }


    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try {
            Object obj = objectMapper.readValue(bytes, clazz);
            System.out.println("obj:"+obj);
            if (obj instanceof RpcRequest) {
                obj = handleRequest(obj);
            }
            System.out.println("obj1:"+obj);
            return obj;
        } catch (IOException e) {
            logger.error("序列化时有错误发生:", e);
            throw new SerializeException("序列化时有错误发生");
        }
    }

    /**
     * 这里由于使用JSON序列化和反序列化RpcRequest中的Object数组（参数数组）
     * 无法保证反序列化后仍然为原实例类型，需要重新判断处理
     * 本质原因时JSON格式中不保存类型信息
     *
     * 在RpcRequest反序列化时，由于其中有一个字段是Object数组，
     * 在反序列化时序列化器会根据字段类型进行反序列化，而Object就是一个十分模糊的类型，
     * 会出现反序列化失败的现象，这时就需要RpcRequest中的另一个字段ParamTypes
     * 来获取到Object数组中的每个实例的实际类，辅助反序列化，这就是handleRequest()方法的作用。
     */
    private Object handleRequest(Object obj) throws IOException {
        RpcRequest rpcRequest = (RpcRequest) obj;
        for (int i = 0; i < rpcRequest.getParamTypes().length; i++) {
            Class<?> clazz = rpcRequest.getParamTypes()[i];
            if (!clazz.isAssignableFrom(rpcRequest.getParameters()[i].getClass())) {
                byte[] bytes = objectMapper.writeValueAsBytes(rpcRequest.getParameters()[i]);
                rpcRequest.getParameters()[i] = objectMapper.readValue(bytes, clazz);
            }
        }
        return rpcRequest;
    }

    @Override
    public int getCode() {
        return SerializerCode.valueOf("JSON").getCode();
    }

}
