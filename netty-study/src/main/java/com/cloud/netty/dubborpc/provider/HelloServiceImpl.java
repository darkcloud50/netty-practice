package com.cloud.netty.dubborpc.provider;


import com.cloud.netty.dubborpc.publicinterface.HelloService;

/**
 * @author: cloud
 * @date: 2021/3/10 21:10
 * @version: 1.0.0
 */
public class HelloServiceImpl implements HelloService {

    private static int count = 0;

    @Override
    public String hello(String msg) {
        System.out.println("收到客户端消息=" + msg);

        //根据mes 返回不同的结果
        if (msg != null) {
            return "你好客户端, 我已经收到你的消息 [" + msg + "] 第" + (++count) + " 次";
        } else {
            return "你好客户端, 我已经收到你的消息 ";
        }
    }

}
