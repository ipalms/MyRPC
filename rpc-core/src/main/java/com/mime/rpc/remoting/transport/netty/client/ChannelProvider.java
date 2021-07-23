package com.mime.rpc.remoting.transport.netty.client;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 用于获取 Channel 对象
 */
@Slf4j
public class ChannelProvider {

    //维护一个地址到channel的映射关系
    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        //判断对应地址是否有连接
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            //如果是，确定连接是否可用，如果是，则直接获取
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    //保存channel
    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
