package com.cloud.netty.groupchat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * GroupChatClientHandler
 *
 * @author: cloud
 * @date: 2021/3/4 15:32
 * @version: 1.0.0
 */
public class GroupChatClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 输出读到的消息
        System.out.println(msg.trim());
    }

}
