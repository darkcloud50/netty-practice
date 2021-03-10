package com.cloud.netty.protocoltcp;

/**
 * @author: cloud
 * @date: 2021/3/10 15:53
 * @version: 1.0.0
 */
public class MessageProtocol {

    private int len; //关键

    private byte[] content;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
