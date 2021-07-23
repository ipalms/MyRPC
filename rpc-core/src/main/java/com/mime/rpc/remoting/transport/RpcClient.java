package com.mime.rpc.remoting.transport;


import com.mime.rpc.entity.RpcRequest;
import com.mime.rpc.serializer.CommonSerializer;

/**
 * 客户端类通用接口
 */
public interface RpcClient {

    int DEFAULT_SERIALIZER = CommonSerializer.KRYO_SERIALIZER;

    Object sendRequest(RpcRequest rpcRequest);

}
