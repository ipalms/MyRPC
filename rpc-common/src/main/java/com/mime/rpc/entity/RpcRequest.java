package com.mime.rpc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消费者向提供者发送的请求对象
 */
@Data
/*//lombok对构造方法的链式调用的支持注解
@Builder*/
@AllArgsConstructor
//@NoArgsConstructor 无参
//先使用jdk默认自带的序列化方式
public class RpcRequest implements Serializable {

    /**
     * 请求号
     */
    private String requestId;

    /**
     * 待调用接口名称
     */
    private String interfaceName;

    /**
     * 待调用方法名称
     */
    private String methodName;

    /**
     * 调用方法的参数
     */
    private Object[] parameters;

    /**
     * 调用方法的参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 是否是心跳包
     */
    private Boolean heartBeat;


    /**
     * 无参构造器，序列化时例如Kryo需要
     */
    public RpcRequest() {
    }
}
