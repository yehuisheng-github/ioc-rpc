package com.netty.code;

import com.netty.model.Void;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;

/**
 * @author yehuisheng
 */
public class ProDeCoder extends ReplayingDecoder<Void> {

    /**
     *  解码器 - 从字节流的缓冲区中解析通信节点发送过来的数据
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        // 获取数据长度
        int length = byteBuf.readInt();
        // 建立缓存区
        byte[] bytes = new byte[length];
        // 正确读取数据到缓冲区中
        byteBuf.readBytes(bytes);
        /*
         *  将解码的数据通过 pipeline 调用链给下一个处理器
         */
        MsgProtocol protocol = new MsgProtocol();
        protocol.setLength(length);
        protocol.setMsg(bytes);
        list.add(protocol);
    }

}
