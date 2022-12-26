package com.netty.code;

/**
 *  自定义协议格式
 * @author yehuisheng
 */
public class MsgProtocol {

    private int length;
    private byte[] msg;

    public int getLength() {
        return length;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }

    public MsgProtocol() {
    }

    public MsgProtocol(byte[] msg) {
        this.msg = msg;
        this.length = msg.length;
    }


}
