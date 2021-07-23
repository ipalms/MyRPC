package com.mime.rpc.old;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.ResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 进行过程调用的处理器
 */
public class RequestHandler1 {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler1.class);

    //可以在调用的时候就封装调用调用成功和失败的RPCResponse包
    public Object handle(RpcRequest rpcRequest, Object service) {
        Object result = null;
        try {
            System.out.println("rpcRequest"+rpcRequest);
            result = invokeTargetMethod(rpcRequest, service);
            logger.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            //调用异常返回异常信息
            logger.error("调用或发送时有错误发生：", e);
            result=e.getCause();
            return result;
        }
        return result;
    }

    //反射调用具体实现的方法
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) throws IllegalAccessException, InvocationTargetException {
        Method method;
        try {
            //根据方法名和参数类型找到要调用的方法
            method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        } catch (NoSuchMethodException e) {
            return RpcResponse.fail(ResponseCode.METHOD_NOT_FOUND,rpcRequest.getRequestId());
        }
        //反射调用方法
        return method.invoke(service, rpcRequest.getParameters());
    }
}
