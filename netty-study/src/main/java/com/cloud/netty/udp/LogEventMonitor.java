package com.cloud.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;

/**
 * LogEventMonitor
 *
 * @author: cloud
 * @date: 2021/3/5 14:43
 * @version: 1.0.0
 */
public class LogEventMonitor {

    private final EventLoopGroup group;

    private final Bootstrap bootstrap;


    public LogEventMonitor(InetSocketAddress address) {

        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();

        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                // 设置套接字选项 SO_BROADCAST
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LogEventDecoder());
                        pipeline.addLast(new LogEventHandler());
                    }
                })
                .localAddress(address);

    }

    public Channel bind() {
        // 绑定Channel。注意，DatagramChannel是无连接的
        return this.bootstrap.bind().syncUninterruptibly().channel();
    }


    public void stop() {
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        LogEventMonitor monitor = new LogEventMonitor(new InetSocketAddress(9090));
        try {
            Channel channel = monitor.bind();
            System.out.println("Monitor is running");
            channel.closeFuture().sync();
        } finally {
            monitor.stop();
        }
    }

}
