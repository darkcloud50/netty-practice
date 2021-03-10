package com.cloud.netty.inboundhandlerandoutboundhandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author: cloud
 * @date: 2021/3/10 11:14
 * @version: 1.0.0
 */
public class MyclientHandler extends SimpleChannelInboundHandler<Long> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Long msg) throws Exception {
        System.out.println("服务器的id=" + ctx.channel().remoteAddress());
        System.out.println("收到服务器数据: " + msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("MyclientHandler 发送数据");
        // ctx.writeAndFlush("123132");

        // 发送16字节的数据
        //ctx.writeAndFlush(Unpooled.copiedBuffer("afasfaasdfsfaaaa", CharsetUtil.UTF_8));
        ctx.writeAndFlush(123456L);
    }
}
