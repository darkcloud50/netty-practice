package com.cloud.netty.dubborpc.publicinterface;

/**
 * 这个是接口，是服务提供方和 服务消费方都需要
 *
 * @author: cloud
 * @date: 2021/3/10 21:07
 * @version: 1.0.0
 */
public interface HelloService {

    String hello(String msg);

}
