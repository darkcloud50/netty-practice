package com.cloud.netty.heartbeat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * HeartBeatServerHandler
 *
 * @author: cloud
 * @date: 2021/3/4 16:49
 * @version: 1.0.0
 */
public class HeartBeatServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * @param ctx 上下文
     * @param evt 事件
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {

            // 将evt向下转型 IdleStateEvent
            IdleStateEvent event = (IdleStateEvent) evt;

            String eventType;

            switch (event.state()) {
                case READER_IDLE:
                    eventType = "读空闲";
                    break;
                case WRITER_IDLE:
                    eventType = "写空闲";
                    break;
                case ALL_IDLE:
                    eventType = "读写空闲";
                    break;
                default:
                    eventType = "";
            }

            System.out.println(ctx.channel().remoteAddress() + "--超时时间--" + eventType);
            System.out.println("服务器做相应处理..");

            // 如果发生空闲，我们关闭通道
            // ctx.channel().close();

        }

    }
}
