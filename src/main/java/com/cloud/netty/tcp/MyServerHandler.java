package com.cloud.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.util.UUID;

/**
 * @author: cloud
 * @date: 2021/3/10 15:19
 * @version: 1.0.0
 */
public class MyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private int count = 0;

    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        byte[] buffer = new byte[msg.readableBytes()];
        msg.readBytes(buffer);

        String s = new String(buffer, CharsetUtil.UTF_8);

        System.out.println("服务器接收的数据：" + s);
        System.out.println("服务器接收的次数：" + (++count));

        // 服务器回送数据给客户端, 回送一个随机id ,
        ByteBuf responseByteBuf = Unpooled.copiedBuffer(UUID.randomUUID().toString() + " ", CharsetUtil.UTF_8);
        ctx.writeAndFlush(responseByteBuf);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
