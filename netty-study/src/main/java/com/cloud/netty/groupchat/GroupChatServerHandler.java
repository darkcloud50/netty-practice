package com.cloud.netty.groupchat;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;

/**
 * GroupChatServerHandler
 *
 * @author: cloud
 * @date: 2021/3/4 14:05
 * @version: 1.0.0
 */
public class GroupChatServerHandler extends SimpleChannelInboundHandler<String> {

    private static DefaultChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // handlerAdded表示连接建立，一旦连接，第一个被执行
    // 将当前channel加入到channelGroup
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();

        // 将该客户加入聊天的信息推送给其它在线的客户端
        /*
        该方法会将 channelGroup 中所有的channel 遍历，并发送 消息，
        我们不需要自己遍历
         */
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 加入聊天" + sdf.format(new java.util.Date()) + " \n");

        channelGroup.add(channel);
        System.out.println("[handlerAdded] channelGroup size :" + channelGroup.size());
    }

    // 断开连接, 将xx客户离开信息推送给当前在线的客户
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        Channel channel = ctx.channel();
        channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 离开了\n");

        System.out.println("[handlerRemoved] channelGroup size :" + channelGroup.size());
    }

    // 表示channel 处于活动状态, 提示 xx上线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        System.out.println(ctx.channel().remoteAddress() + " 上线了~");

    }

    // 表示channel 处于不活动状态, 提示 xx离线了
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        System.out.println(ctx.channel().remoteAddress() + " 离线了~");
    }

    // 读取数据, 并将数据推送给其它客户端
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        System.out.println(msg);

        Channel channel = ctx.channel();

        channelGroup.forEach(ch -> {
            if (ch != channel) {
                ch.writeAndFlush("[客户]" + channel.remoteAddress() + " 发送了消息: " + msg + "\n");
            } else {
                // 回显自己发送的消息给自己
                ch.writeAndFlush("[自己]发送了消息: " + msg + "\n");
            }
        });


    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 关闭通道
        ctx.close();
    }
}
