package com.cloud.netty.protocoltcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author: cloud
 * @date: 2021/3/10 15:59
 * @version: 1.0.0
 */
public class MyMessageEncoder extends MessageToByteEncoder<MessageProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageProtocol msg, ByteBuf out) throws Exception {
        System.out.println("MyMessageEncoder encode 方法被调用");
        System.out.println("encoder length : " + msg.getLen());
        out.writeInt(msg.getLen());
        out.writeBytes(msg.getContent());
    }

}
