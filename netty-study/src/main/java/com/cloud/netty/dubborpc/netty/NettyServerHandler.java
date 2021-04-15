package com.cloud.netty.dubborpc.netty;


import com.cloud.netty.dubborpc.provider.HelloServiceImpl;
import com.cloud.netty.dubborpc.provider.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * NettyServerHandler
 *
 * @author: cloud
 * @date: 2021/3/10 21:10
 * @version: 1.0.0
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        System.out.println("msg = " + msg);

        // 客户端在调用服务器的api 时，我们需要定义一个协议
        // 比如我们要求 每次发消息是都必须以某个字符串开头 "HelloService#hello#你好"

        String message = (String) msg;

        if (message.startsWith(ServerBootstrap.PROVIDER_NAME)) {
            String result = new HelloServiceImpl().hello(message.substring(message.lastIndexOf("#") + 1));
            ctx.writeAndFlush(result);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
