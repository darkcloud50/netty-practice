package com.cloud.netty.udp.pojo;

import java.net.InetSocketAddress;

/**
 * LogEvent
 *
 * @author: cloud
 * @date: 2021/3/5 10:21
 * @version: 1.0.0
 */
public final class LogEvent {

    public static final byte SEPARATOR = (byte) ':';

    private final InetSocketAddress source;

    private final String logfile;

    private final String msg;

    private final long receivedTimestamp;

    /**
     * 用于传出消息的构造函数
     *
     * @param logfile
     * @param msg
     */
    public LogEvent(String logfile, String msg) {
        this(null, -1, logfile, msg);
    }

    /**
     * 用于传入消息的构造函数
     *
     * @param source
     * @param receivedTimestamp
     * @param logfile
     * @param msg
     */
    public LogEvent(InetSocketAddress source, long receivedTimestamp, String logfile, String msg) {
        this.source = source;
        this.logfile = logfile;
        this.msg = msg;
        this.receivedTimestamp = receivedTimestamp;
    }

    public static byte getSEPARATOR() {
        return SEPARATOR;
    }

    public InetSocketAddress getSource() {
        return source;
    }

    public String getLogfile() {
        return logfile;
    }

    public String getMsg() {
        return msg;
    }

    public long getReceivedTimestamp() {
        return receivedTimestamp;
    }
}
