package com.mime.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标识是请求还是响应数据包
 */
@AllArgsConstructor
@Getter
public enum PackageType {

    REQUEST_PACK(0),
    RESPONSE_PACK(1);

    private final int code;

}
