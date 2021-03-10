package com.cloud.netty.protocoltcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author: cloud
 * @date: 2021/3/10 15:15
 * @version: 1.0.0
 */
public class MyClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    private int count = 0;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 使用客户端发送10条数据 "今天天气冷，吃火锅" 编号
        for (int i = 0; i < 5; i++) {
            String mes = "今天天气冷，吃火锅";
            byte[] msgBytes = mes.getBytes(StandardCharsets.UTF_8);
            int length = msgBytes.length;

            // 创建协议包对象
            MessageProtocol messageProtocol = new MessageProtocol();
            messageProtocol.setLen(length);
            messageProtocol.setContent(msgBytes);
            ctx.writeAndFlush(messageProtocol);
        }

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {

        int len = msg.getLen();
        byte[] content = msg.getContent();

        System.out.println("客户端接收到消息如下");
        System.out.println("长度=" + len);
        System.out.println("内容=" + new String(content, StandardCharsets.UTF_8));

        System.out.println("客户端接收消息数量=" + (++this.count));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
