package com.netty.code;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author yehuisheng
 */
public class ProEnCoder extends MessageToByteEncoder<MsgProtocol> {

    /**
     *  编码器 - 将数据的长度放入缓冲区中，发送给通信的节点
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MsgProtocol msgProtocol, ByteBuf byteBuf) {
        byteBuf.writeInt(msgProtocol.getLength());
        byteBuf.writeBytes(msgProtocol.getMsg());
    }

}
