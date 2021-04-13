package com.cloud.netty.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.time.LocalDateTime;

/**
 * @author: cloud
 * @date: 2021/3/11 14:21
 * @version: 1.0.0
 */
public class MyTextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    /*
    @Override
    protected void initChannel(final Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpRequestDecoder());
        p.addLast(new HttpResponseEncoder());
        p.addLast(new InHandler1());
        p.addLast(new InHandler2());
        p.addLast(new OutHandler1());
        p.addLast(new OutHandler2());
    }

    如果在 InHandler2 中, 调用了 write(...) 则不会触发调用 OutHandler2 和 OutHandler1 的, 因为 ctx.write(...) 只会触发离它最近的 out handler, 但是, InHandler2 前面没有 out handler了~
    但, 如果通过 channel.write(...)的话, 则它会从 OutHandler2 -> OutHandler1 这样子流穿
    */

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        System.out.println("服务器收到消息: " + msg.text());

        // ctx.writeAndFlush()

        ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器时间" + LocalDateTime.now() + " " + msg.text()));

    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // id 表示唯一的值，LongText 是唯一的 ShortText 不是唯一
        System.out.println("handlerAdded 被调用" + ctx.channel().id().asLongText());
        System.out.println("handlerAdded 被调用" + ctx.channel().id().asShortText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved 被调用");
        System.out.println("handlerRemoved 被调用" + ctx.channel().id().asLongText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
