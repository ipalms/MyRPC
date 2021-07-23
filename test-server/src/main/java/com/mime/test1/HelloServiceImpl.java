package com.mime.test1;

import com.mime.rpc.annotation.Service;
import com.mime.rpc.api.HelloObject;
import com.mime.rpc.api.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(HelloObject object) {
        logger.info("接收到消息：{}", object.getMessage());
        return "8686 hello "+object.getMessage();
    }

}
