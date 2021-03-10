package com.cloud.netty.inboundhandlerandoutboundhandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author: cloud
 * @date: 2021/3/10 10:46
 * @version: 1.0.0
 */
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 对入站进行解码
        //pipeline.addLast(new MyByteToLongDecoder());
        pipeline.addLast(new MyByteToLongDecoder2());

        // 对出站进行编码
        pipeline.addLast(new MyLongToByteEncoder());
        // 自定义handler 处理业务逻辑
        pipeline.addLast(new MyServerHandler());
    }

}
