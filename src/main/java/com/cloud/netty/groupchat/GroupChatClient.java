package com.cloud.netty.groupchat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Scanner;

/**
 * GroupChatClient
 *
 * @author: cloud
 * @date: 2021/3/4 15:27
 * @version: 1.0.0
 */
public class GroupChatClient {

    private int port;

    private String host;

    public GroupChatClient(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void run() throws InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup();


        try {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {

                            // 得到pipeline
                            ChannelPipeline pipeline = ch.pipeline();
                            //加入相关handler
                            pipeline.addLast("decoder", new StringDecoder());
                            pipeline.addLast("encoder", new StringEncoder());
                            //加入自定义的handler
                            pipeline.addLast(new GroupChatClientHandler());
                        }

                    });

            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();

            // 得到channel
            Channel channel = channelFuture.channel();
            System.out.println("-------" + channel.localAddress() + "--------");

            // 客户端需要输入信息，创建一个扫描器
            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNextLine()) {
                String msg = scanner.nextLine();
                System.out.println("即将发送消息.....");
                // 通过channel 发送到服务器端
                channel.writeAndFlush(msg + "\r\n");
            }


        } finally {
            System.out.println("group shut down......");
            group.shutdownGracefully();
        }


    }


    public static void main(String[] args) throws InterruptedException {
        new GroupChatClient("127.0.0.1", 7000).run();
    }

}
