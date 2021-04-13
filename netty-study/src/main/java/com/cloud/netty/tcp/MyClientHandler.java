package com.cloud.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * @author: cloud
 * @date: 2021/3/10 15:15
 * @version: 1.0.0
 */
public class MyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private int count = 0;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 使用客户端发送10条数据 hello,server 编号
        for (int i = 0; i < 10; i++) {
            ByteBuf byteBuf = Unpooled.copiedBuffer("hello, server " + i, CharsetUtil.UTF_8);
            ctx.writeAndFlush(byteBuf);
        }

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] buffer = new byte[msg.readableBytes()];
        msg.readBytes(buffer);

        String s = new String(buffer, CharsetUtil.UTF_8);

        System.out.println("客户端接收的数据：" + s);
        System.out.println("客户端接收的次数：" + (++count));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
