package com.mime.rpc.remoting.transport;

import com.mime.rpc.annotation.Service;
import com.mime.rpc.annotation.ServiceScan;
import com.mime.rpc.config.RpcConfig;
import com.mime.rpc.enumeration.RpcError;
import com.mime.rpc.exception.RpcException;
import com.mime.rpc.provider.ServiceProvider;
import com.mime.rpc.registry.ServiceRegistry;
import com.mime.rpc.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Set;

public abstract class AbstractRpcServer implements RpcServer {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static String host;
    protected static int port;

    static {
        //从配置文件中读取出地址、端口等信息
        String serverAddress = RpcConfig.getServerAddress();
        host=serverAddress.substring(0,serverAddress.lastIndexOf(":"));
        port=Integer.parseInt(serverAddress.substring(serverAddress.lastIndexOf(":")+1));
    }

    protected ServiceRegistry serviceRegistry;
    protected ServiceProvider serviceProvider;

    /**
     * 扫描注解注册服务
     */
    public void scanServices() {
        //获得服务端启动类的全限定类名--该启动类应位于当前方法调用栈的栈底
        String mainClassName = ReflectUtil.getStackTrace();
        Class<?> startClass;
        try {
            //加载该启动类
            startClass = Class.forName(mainClassName);
            if(!startClass.isAnnotationPresent(ServiceScan.class)) {
                logger.error("启动类缺少 @ServiceScan 注解");
                throw new RpcException(RpcError.SERVICE_SCAN_PACKAGE_NOT_FOUND);
            }
        } catch (ClassNotFoundException e) {
            logger.error("出现未知错误");
            throw new RpcException(RpcError.UNKNOWN_ERROR);
        }
        //获得@ServiceScan注解值
        String basePackage = startClass.getAnnotation(ServiceScan.class).value();
        //默认值
        if("".equals(basePackage)) {
            //默认情况下将获取启动类所在的包---后续将对此包的class文件逐个扫描获取带有@Service注解标注的类
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));
        }
        //获取该包下的所有class对象集合
        Set<Class<?>> classSet = ReflectUtil.getClasses(basePackage);
        for(Class<?> clazz : classSet) {
            if(clazz.isAnnotationPresent(Service.class)) {
                String serviceName = clazz.getAnnotation(Service.class).name();
                Object obj;
                try {
                    //创建空参实例 clazz.getConstructor().newInstance()
                    obj = clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("创建 " + clazz + " 时有错误发生");
                    continue;
                }
                //默认情况下注册的服务名称是该服务所实现的接口名称
                if("".equals(serviceName)) {
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> oneInterface: interfaces){
                        publishService(obj, oneInterface.getCanonicalName());
                    }
                } else {
                    publishService(obj, serviceName);
                }
            }
        }
    }

    @Override
    public <T> void publishService(T service, String serviceName) {
        serviceProvider.addServiceProvider(service, serviceName);
        serviceRegistry.register(serviceName, new InetSocketAddress(host, port));
    }
}
