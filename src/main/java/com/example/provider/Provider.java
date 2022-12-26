package com.example.provider;

import com.netty.annotation.EnableRpcServer;
import com.netty.reflection.Applications;

/**
 * @author yehuisheng
 */
@EnableRpcServer
public class Provider {

    public static void main(String[] args) {
        Applications.run();
    }

}
