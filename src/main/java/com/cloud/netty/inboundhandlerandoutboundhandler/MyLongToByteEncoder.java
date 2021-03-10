package com.cloud.netty.inboundhandlerandoutboundhandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author: cloud
 * @date: 2021/3/10 11:07
 * @version: 1.0.0
 */
public class MyLongToByteEncoder extends MessageToByteEncoder<Long> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Long msg, ByteBuf out) throws Exception {
        System.out.println("MyLongToByteEncoder 被调用");
        System.out.println("msg = " + msg);
        out.writeLong(msg);
    }

}
