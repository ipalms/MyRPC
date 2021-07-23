package com.mime.rpc.hook;

import com.mime.rpc.factory.ThreadPoolFactory;
import com.mime.rpc.registry.nacos.util.NacosUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ShutdownHook {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    //该类对象为单例
    private static final ShutdownHook shutdownHook = new ShutdownHook();

    public static ShutdownHook getShutdownHook() {
        return shutdownHook;
    }

    //钩子函数--执行JVM关闭后的资源清理过程
    public void addClearAllHook() {
        logger.info("关闭后将自动注销所有服务");
        //Runtime对象是JVM虚拟机的运行时环境，调用其addShutdownHook方法增加一个钩子函数，创建一个新线程调用clearRegistry方法完成注销工作。
        //这个钩子函数会在JVM关闭之前被调用。
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NacosUtil.clearRegistry();
            ThreadPoolFactory.shutDownAll();
        }));
    }
}
