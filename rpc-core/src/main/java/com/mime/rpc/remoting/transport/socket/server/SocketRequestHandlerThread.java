package com.mime.rpc.remoting.transport.socket.server;

import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.entity.RpcResponse;
import com.mime.rpc.remoting.handler.RequestHandler;
import com.mime.rpc.remoting.transport.socket.util.ObjectReader;
import com.mime.rpc.remoting.transport.socket.util.ObjectWriter;
import com.mime.rpc.serializer.CommonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 处理RpcRequest的工作线程  继承了Runnable接口，会被当作一个任务被线程池执行
 */
public class SocketRequestHandlerThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SocketRequestHandlerThread.class);

    private Socket socket;
    private RequestHandler requestHandler;
    private CommonSerializer serializer;

    public SocketRequestHandlerThread(Socket socket, RequestHandler requestHandler, CommonSerializer serializer) {
        this.socket = socket;
        this.requestHandler = requestHandler;
        this.serializer = serializer;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {
            RpcRequest rpcRequest = (RpcRequest) ObjectReader.readObject(inputStream);
            //调用具体方法
            RpcResponse<Object> result = requestHandler.handle(rpcRequest);
            ObjectWriter.writeObject(outputStream, result, serializer);
        } catch (IOException e) {
            logger.error("调用或发送时有错误发生：", e);
        }
    }
}
