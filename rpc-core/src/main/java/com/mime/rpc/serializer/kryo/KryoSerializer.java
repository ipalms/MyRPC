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
 *
 * 关于threadLocal的一些知识：
 * 对threadLocal的理解、底层原理：
 * ThreadLocal是 JDK java.lang 包下的一个类，ThreadLocal为变量在每个线程中都创建了一个副本
 * 那么每个线程可以访问自己内部的副本变量，并且不会和其他线程的局部变量冲突，实现了线程间的数据隔离。
 * ThreadLocal的应用场景主要有：
 * （1）保存线程上下文信息，在需要的地方可以获取   ***主要的***
 * （2）线程间数据隔离
 * （3）数据库连接
 *
 * 底层原理：
 * 每个线程的内部都维护了一个 ThreadLocalMap，它是一个键值对数据格式
 * key是一个弱引用，也就是ThreadLocal本身，而value是强引用，存的是线程变量的值。
 * 也就是说ThreadLocal本身并不存储线程的变量值，它只是一个工具，用来维护线程内部的Map，帮助存和取变量。
 * 
 * 使用threadLocal会出现什么问题？
 * ThreadLocal在 ThreadLocalMap（注意是ThreadLocalMap弱引用了ThreadLocal）中是以一个弱引用身份被 Entry 中的 Key 引用的，
 * 因此如果ThreadLocal没有外部引用来引用它，那么ThreadLocal会在下次 JVM 垃圾收集时被回收。
 * 这个时候 Entry 中的 key 已经被回收，但是 value 又是一强引用不会被垃圾收集器回收，
 * 这样ThreadLocal的线程如果一直持续运行，value 就一直得不到回收，这样就会发生内存泄露。
 *
 * ThreadLocal的key是哪种引用类型？为啥这么设计？
 * ThreadLocalMap中的 key 是弱引用，而 value 是强引用才会导致内存泄露的问题
 * i:若key 使用强引用：这样会导致一个问题，引用的ThreadLocal的对象被回收了，但是 ThreadLocalMap还持有ThreadLocal的强引用毫无意义，如果没有手动删除，ThreadLocal不会被回收，则会导致内存泄漏。
 * ii:若key 使用弱引用：这样的话，引用的ThreadLocal的对象被回收了，由于 ThreadLocalMap持有ThreadLocal的弱引用，即使没有手动删除，ThreadLocal也会被回收。value 在下一次 ThreadLocalMap调用 set、get、remove 的时候会被清除（清理key为null的记录），使用完了ThreadLocal最好在手动的remove一下。
 * iii:比较以上两种情况：由于 ThreadLocalMap的生命周期跟 Thread 一样长，如果都没有手动删除对应 key，都会导致内存泄漏，但是使用弱引用可以多一层保障，弱引用ThreadLocal不会内存泄漏，对应的 value 在下一次 ThreadLocalMap调用 set、get、remove 的时候被清除，算是最优的解决方案。
 *
 * 什么是内存泄漏
 * 内存泄漏是指用户向系统申请分配内存进行使用，可是使用完了以后却没有释放，结果那块内存用户不能访问（也许你把它的地址给弄丢了），而系统也不能再把它分配给需要的程序。
 */
public class KryoSerializer implements CommonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(KryoSerializer.class);


    /**
     * Kryo对象不是线程安全的，所以需要借用ThreadLocal来保证线程安全性--保证每个线程使用的Kryo对象是唯一的
     */
    private static final ThreadLocal<Kryo> kryoThreadLocal= ThreadLocal.withInitial(() -> {
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
            //为了放置ThreadLocal的value属性内存泄露的举措，但是这样Kryo对象似乎只使用了一次就不再用了
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
