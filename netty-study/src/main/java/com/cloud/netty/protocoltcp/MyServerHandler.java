package com.cloud.netty.protocoltcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author: cloud
 * @date: 2021/3/10 15:19
 * @version: 1.0.0
 */
public class MyServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    private int count = 0;

    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {


        //接收到数据，并处理
        int len = msg.getLen();
        byte[] content = msg.getContent();

        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("服务器接收到信息如下");
        System.out.println("长度=" + len);
        System.out.println("内容=" + new String(content, StandardCharsets.UTF_8));

        System.out.println("服务器接收到消息包数量=" + (++this.count));

        // 回复消息
        String responseContent = UUID.randomUUID().toString();
        System.out.println("回复消息：" + responseContent);
        byte[] contentBytes = responseContent.getBytes(StandardCharsets.UTF_8);
        int responseLen = contentBytes.length;
        // 构建一个协议包
        MessageProtocol messageProtocol = new MessageProtocol();
        messageProtocol.setLen(responseLen);
        messageProtocol.setContent(contentBytes);

        ctx.writeAndFlush(messageProtocol);

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
