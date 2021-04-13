package com.cloud.netty.dubborpc.customer;


import com.cloud.netty.dubborpc.netty.NettyClient;
import com.cloud.netty.dubborpc.provider.ServerBootstrap;
import com.cloud.netty.dubborpc.publicinterface.HelloService;

/**
 * ClientBootstrap
 *
 * @author: cloud
 * @date: 2021/3/10 21:10
 * @version: 1.0.0
 */
public class ClientBootstrap {

    public static void main(String[] args) throws InterruptedException {

        // 创建一个消费者
        NettyClient customer = new NettyClient();

        HelloService service = (HelloService) customer.getBean(HelloService.class, ServerBootstrap.PROVIDER_NAME);

        System.out.println("代理对象：" + service.getClass().getName());

        for (; ; ) {
            Thread.sleep(2 * 1000);
            // 通过代理对象调用服务提供者的方法(服务)
            String res = service.hello("你好 dubbo~");
            System.out.println("调用的结果 res= " + res);
        }

    }


}
