package com.mime.rpc.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {

    //rpc message type
    private byte messageType;
    //serialization type
    private byte codec;
 /*   //compress type
    private byte compress;*/
    //request id
    private int requestId;
    //request data
    private Object data;

}
