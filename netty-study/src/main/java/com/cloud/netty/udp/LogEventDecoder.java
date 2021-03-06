package com.cloud.netty.udp;

import com.cloud.netty.udp.pojo.LogEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * LogEventDecoderHandler
 *
 * @author: cloud
 * @date: 2021/3/5 14:44
 * @version: 1.0.0
 */
public class LogEventDecoder extends MessageToMessageDecoder<DatagramPacket> {

    /**
     * @param ctx
     * @param datagramPacket
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket datagramPacket, List<Object> out) throws Exception {

        // 获取对DatagramPacket中的数据（ByteBuf）的引用
        ByteBuf data = datagramPacket.content();

        // 获取 SEPARATOR 的索引
        int idx = data.indexOf(0, data.readableBytes(), LogEvent.SEPARATOR);

        // 获取文件名
        String filename = data.slice(0, idx).toString(CharsetUtil.UTF_8);

        // 获取日志信息
        String logMsg = data.slice(idx + 1, data.readableBytes()).toString(CharsetUtil.UTF_8);

        // 构建一个新的LogEvent对象，并将它添加到（已经解码的信息的）列表中
        LogEvent logEvent = new LogEvent(datagramPacket.sender(), System.currentTimeMillis(), filename, logMsg);
        out.add(logEvent);

    }

}
