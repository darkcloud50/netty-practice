/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package demo2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by 李林峰 on 2018/8/5.
 */
public final class ClientPool {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "18081"));

    public static void main(String[] args) throws Exception {
//        TimeUnit.SECONDS.sleep(30);
        initClientPool(100);
    }

    static void initClientPool(int poolSize) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
        /**
         * TCP链路注册成功后，调用initChannel {@link ChannelInitializer#initChannel(io.netty.channel.ChannelHandlerContext)}
         * 它的主要功能有两个。
         * （1）执行客户端初始化应用实现的{@link ChannelInitializer#initChannel(io.netty.channel.Channel)}，将应用自定义的ChannelHandler添加到ChannelPipeline中
         * （2）将Bootstrap注册到ChannelPipeline用于初始化应用ChannelHandler的ChannelInitializer删除掉，完成应用ChannelPipeline的初始化工作。 {@link ChannelInitializer#initChannel(io.netty.channel.ChannelHandlerContext)} line 137
         */
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new LoggingHandler());
                    }
                });
        for (int i = 0; i < poolSize; i++) {
            b.connect(HOST, PORT).sync();
        }
    }
}
