package com.cloud.netty.codec2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * client
 *
 * @author cloud
 * @date: 2021/3/2 10:47
 * @version: 1.0.0
 */
public class NettyClient {

    public static void main(String[] args) throws InterruptedException {

        // 客户端需要一个事件循环组
        NioEventLoopGroup group = new NioEventLoopGroup();


        try {
            // 创建客户端启动对象
            // 注意客户端使用的不是 ServerBootstrap 而是 Bootstrap
            Bootstrap bootstrap = new Bootstrap();

            // 设置相关参数
            // 设置线程组
            bootstrap.group(group)
                    // 设置客户端通道的实现类(反射)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 加入自己的处理器
                            pipeline.addLast("encoder", new ProtobufEncoder());
                            pipeline.addLast(new NettyClientHandler());

                        }
                    });


            System.out.println("客户端 ok..");

            // 启动客户端去连接服务器端
            // 关于 ChannelFuture 要分析，涉及到netty的异步模型
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 6668).sync();
            // 给关闭通道进行监听
            channelFuture.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }

    }

}
