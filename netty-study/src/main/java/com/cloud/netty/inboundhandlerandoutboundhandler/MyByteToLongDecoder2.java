package com.cloud.netty.inboundhandlerandoutboundhandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @author: cloud
 * @date: 2021/3/10 14:25
 * @version: 1.0.0
 */
public class MyByteToLongDecoder2 extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("MyByteToLongDecoder2 被调用");

        // 在ReplayingDecoder 不需要判断数据是否足够读取，内部会进行处理判断
        /*if (in.readableBytes() >= 8) {
            out.add(in.readLong());
        }*/

        out.add(in.readLong());
    }

}
