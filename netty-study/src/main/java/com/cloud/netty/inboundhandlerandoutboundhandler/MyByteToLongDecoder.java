package com.cloud.netty.inboundhandlerandoutboundhandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author: cloud
 * @date: 2021/3/10 10:50
 * @version: 1.0.0
 */
public class MyByteToLongDecoder extends ByteToMessageDecoder {

    /**
     * 解码
     *
     * @param ctx 上下文对象
     * @param in  入站的ByteBuf
     * @param out List集合，将解码后的数据传给下一个handler处理
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        System.out.println("MyByteToLongDecoder 被调用");

        if (in.readableBytes() >= 8) {
            out.add(in.readLong());
        }
    }

}
