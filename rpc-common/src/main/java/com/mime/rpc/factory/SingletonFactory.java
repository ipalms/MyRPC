package com.mime.rpc.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * 单例工厂--首先会减少频繁创建对象的成本
 * 所以需要存储在本地内容的多个映射关系的集合的类都应该是单例的，这样存储的非static修饰的映射（map）的内容才能共享
 */
public class SingletonFactory {

    //hashmap存储class文件对应对象
    private static Map<Class, Object> objectMap = new HashMap<>();

    private SingletonFactory() {}

    //单例模式
    public static <T> T getInstance(Class<T> clazz) {
        Object instance = objectMap.get(clazz);
        if(instance==null){
            synchronized (clazz) {
                if(instance == null) {
                    try {
                        //未缓存过就进行创建并缓存
                        instance = clazz.newInstance();
                        objectMap.put(clazz, instance);
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            }
        }
        //cast方法将对象强制转型为范型T类型
        return clazz.cast(instance);
    }
}
