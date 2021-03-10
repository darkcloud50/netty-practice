package com.cloud.netty.dubborpc.provider;


import com.cloud.netty.dubborpc.netty.NettyServer;

/**
 * ServerBootstrap
 * @author: cloud
 * @date: 2021/3/10 21:10
 * @version: 1.0.0
 */
public class ServerBootstrap {

    //这里定义协议头
    public static final String PROVIDER_NAME = "HelloService#hello#";

    public static void main(String[] args) throws InterruptedException {
        NettyServer.startServer("127.0.0.1",7000);
    }

}
