package com.cloud.netty.udp;

import com.cloud.netty.udp.pojo.LogEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * LogEventHandler
 *
 * @author: cloud
 * @date: 2021/3/5 15:20
 * @version: 1.0.0
 */
public class LogEventHandler extends SimpleChannelInboundHandler<LogEvent> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogEvent event) throws Exception {

        // 构建字符串
        StringBuilder builder = new StringBuilder();
        builder.append(event.getReceivedTimestamp())
                .append(" [")
                .append(event.getSource().toString())
                .append("] [")
                .append(event.getLogfile())
                .append("] : ")
                .append(event.getMsg());

        // 打印LogEvent的数据
        System.out.println(builder.toString());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常发生时，打印栈跟踪信息，并关闭对应的Channel
        cause.printStackTrace();
        ctx.close();
    }
}
