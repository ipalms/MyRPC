package com.mime.rpc.remoting.handler;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.enumeration.ResponseCode;
import com.mime.rpc.provider.ServiceProvider;
import com.mime.rpc.provider.ServiceProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 进行过程调用的处理器--实际处理方法的调用
 */
public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private static final ServiceProvider serviceProvider;

    static {
        serviceProvider = new ServiceProviderImpl();
    }

    public RpcResponse<Object> handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getServiceProvider(rpcRequest.getInterfaceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    //直接在调用处构造成功失败结果
    private RpcResponse<Object> invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        RpcResponse<Object> response;
        try {
            //根据方法名及方法参数类型获得要调用的方法
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            //反射调用该方法
            result = method.invoke(service, rpcRequest.getParameters());
            //封装结果返回给客户端
            response = RpcResponse.success(result, rpcRequest.getRequestId());
            logger.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            //异常抛出调用失败的结果
            return RpcResponse.fail(ResponseCode.METHOD_NOT_FOUND, rpcRequest.getRequestId());
        }
        return response;
    }
}
