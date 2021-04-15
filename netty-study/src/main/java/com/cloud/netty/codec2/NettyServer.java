package com.cloud.netty.codec2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Server
 *
 * @author: cloud
 * @date: 2021/3/2 9:37
 * @version: 1.0.0
 */
public class NettyServer {

    public static void main(String[] args) throws InterruptedException {

        // 创建BossGroup 和 WorkerGroup
        // 说明
        // 1. 创建两个线程组 bossGroup 和 workerGroup
        // 2. bossGroup 只是处理连接请求 , 真正的和客户端业务处理，会交给 workerGroup完成
        // 3. 两个都是无限循环
        // 4. bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数
        //   默认实际 cpu核数 * 2
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 创建服务器端的启动对象，配置参数
            ServerBootstrap bootstrap = new ServerBootstrap();

            // 设置两个线程组
            bootstrap.group(bossGroup, workerGroup)
                    // 使用NioSocketChannel 作为服务器的通道实现
                    .channel(NioServerSocketChannel.class)
                    // 设置线程队列得到连接个数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 设置保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 该 handler对应 bossGroup , childHandler 对应 workerGroup
                    /// .handler(null)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // 给pipeline 设置处理器
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 指定对哪种对象进行解码
                            pipeline.addLast("decoder", new ProtobufDecoder(MyDataInfo.MyMessage.getDefaultInstance()));
                            // 给我们的workerGroup 的 EventLoop 对应的管道设置处理器
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });

            System.out.println(".....服务器 is ready...");

            // 绑定一个端口并且同步, 生成了一个 ChannelFuture 对象(调用sync()方法阻塞等待直到绑定完成)
            // 启动服务器(并绑定端口)
            final ChannelFuture cf = bootstrap.bind(6668).sync();

            // 给cf 注册监听器，监控我们关心的事件
            cf.addListener(new GenericFutureListener() {

                @Override
                public void operationComplete(Future future) {
                    if (cf.isSuccess()) {
                        System.out.println("监听端口 6668 成功");
                    } else {
                        System.out.println("监听端口 6668 失败");
                    }
                }

            });

            // 对关闭通道进行监听（获取Channel的closeFuture，并且阻塞当前线程直到它完成）
            cf.channel().closeFuture().sync();

        } finally {
            // 关闭 EventLoopGroup，释放所有的资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


    }

}
