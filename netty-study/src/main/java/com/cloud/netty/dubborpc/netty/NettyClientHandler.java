package com.cloud.netty.dubborpc.netty;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

/**
 * NettyClientHandler
 *
 * @author: cloud
 * @date: 2021/3/10 21:10
 * @version: 1.0.0
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    /**
     * 上下文
     */
    private ChannelHandlerContext context;

    /**
     * 结果
     */
    private String result;

    /**
     * 参数
     */
    private String param;

    /**
     * 被代理对象调用，发送数据给服务器，等待被唤醒
     *
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {

        System.out.println("线程池线程名：" + Thread.currentThread().getName());
        System.out.println(" call1 被调用  ");
        context.writeAndFlush(param);
        wait();
        System.out.println(" call2 被调用  ");
        return result;
    }


    /**
     * 与服务器连接后就会建立连接
     *
     * @param ctx 上下文
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(" channelActive 被调用  ");
        context = ctx;
    }

    /**
     * 收到服务器的数据后
     *
     * @param ctx 上下文
     * @param msg 数据
     * @throws Exception
     */
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(" channelRead 被调用  ");
        System.out.println("客户端线程名：" + Thread.currentThread().getName());
        result = msg.toString();

        // 唤醒等待的线程
        notify();


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    //(2)
    void setParam(String param) {
        System.out.println(" setPara  ");
        this.param = param;
    }

}
