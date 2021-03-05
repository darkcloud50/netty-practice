package com.cloud.netty.udp;

import com.cloud.netty.udp.pojo.LogEvent;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * LogEventBroadcaster
 * <p>
 * ncat下载地址: http://nmap.org/dist/ncat-portable-5.59BETA1.zip
 * 使用命令: ncat -l -u -p 9090
 *
 * @author: cloud
 * @date: 2021/3/5 10:54
 * @version: 1.0.0
 */
public class LogEventBroadcaster {

    private final EventLoopGroup group;

    private final Bootstrap bootstrap;

    private final File file;

    public LogEventBroadcaster(InetSocketAddress address, File file) {
        group = new NioEventLoopGroup();
        this.file = file;
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LogEventEncoder(address));
    }

    public void run() throws Exception {
        // 绑定channel
        Channel channel = bootstrap.bind(0).sync().channel();
        long pointer = 0;
        for (; ; ) {
            long len = file.length();

            if (len < pointer) {
                pointer = len;
            } else {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                // 设置当前的文件指针，以确保没有任何的旧日志被发送
                raf.seek(pointer);

                String line;
                while ((line = raf.readLine()) != null) {
                    String newLine = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

                    System.out.println("send：" + newLine);
                    // 对于每个日志条目，写入一个LogEvent到channel中

                    channel.writeAndFlush(new LogEvent(file.getAbsolutePath(), newLine));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        break;
                    }

                }

                // 存储其在文件中的当前位置
                pointer = raf.getFilePointer();

                raf.close();

            }

            // 休眠1秒，如果被中断，则推出循环；否则重新处理它
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }

        }

    }

    public void stop() {
        this.group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\DELL\\Desktop\\2020-12-07.log";
        // 受限广播地址或者零地址网络地址255.255.255.255，发送到这个地址的消息将会被定向给本地网络(0,0,0,0)上的所有主机，而不会被路由器转发给其它网络
        LogEventBroadcaster broadcaster = new LogEventBroadcaster(new InetSocketAddress("255.255.255.255", 9090), new File(path));
        try {
            broadcaster.run();
        } finally {
            broadcaster.stop();
        }
    }
}
