package com.mime.rpc.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mime.rpc.enumeration.SerializerCode;
import com.mime.rpc.exception.SerializeException;
import com.mime.rpc.serializer.CommonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo序列化效率很高,但是只兼容JAVA语言
 * 因为项目服务端、客户端都是使用JAVA语言所以采用Kryo就行
 * Kryo不支持增加或删除Class中的字段，可扩展性不好
 * （如果将序列化结果--字节码传输或者存储到redis中class中的属性变化了，那么反序列化就会失败）
 * Kryo支持把对象的类型信息放进序列化的结果里，反序列化可以不提供类型信息
 */
public class KryoSerializer implements CommonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(KryoSerializer.class);


    /**
     * Kryo对象不是线程安全的，所以需要借用ThreadLocal来保证线程安全性--保证每个线程使用的Kryo对象是唯一的
     */
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        /*
        //Kryo支持对类进行注册注册行为会给每一个Class编一个号码，编号从0开始（这样使得序列化大小更小，比起默认的全限定类名）
        //但是，Kryo并不保证同一个Class每一次的注册的号码都相同（比如重启JVM后，用户访问资源的顺序不同，就会导致类注册的先后顺序不同）
        //也就是说，同样的代码、同一个Class ，在两台机器上的注册编号可能不一致。
        //那么一台机器序列化之后的结果可能就无法在另一台机器上反序列化（即多个JVM、分布式下不要开启注册功能）。
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);
        //该值默认为false，如果要使用类注册需要置为true
        kryo.setRegistrationRequired(true);
        */
        //对循环引用的检查支持（A依赖B B又依赖A），kryo默认会打开这个属性帮你检验循环引用情况，可以有效防止栈内存溢出
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            //获得当前线程的 Kryo 实例
            Kryo kryo = kryoThreadLocal.get();
            // Object->byte:将对象序列化为byte数组
            kryo.writeObject(output, obj);
/*            //这个方法还可以写入序列化对象的类型信息，这样反序列化时候就可以不提供类型信息
            kryo.writeClassAndObject(output, obj);*/
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e) {
            logger.error("序列化时有错误发生:", e);
            throw new SerializeException("序列化时有错误发生");
        }
    }

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            // byte->Object:从byte数组中反序列化出对对象
            Object o = kryo.readObject(input, clazz);
/*            //相应的，Kryo也有可以直接读取带有对象信息字节流的方法
            kryo.readClassAndObject(input);*/
            kryoThreadLocal.remove();
            return o;
        } catch (Exception e) {
            logger.error("反序列化时有错误发生:", e);
            throw new SerializeException("反序列化时有错误发生");
        }
    }

    @Override
    public int getCode() {
        return SerializerCode.valueOf("KRYO").getCode();
    }
}
