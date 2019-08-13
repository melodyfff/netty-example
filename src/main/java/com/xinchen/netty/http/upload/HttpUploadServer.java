package com.xinchen.netty.http.upload;

import com.xinchen.netty.common.SslContextMaker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * A HTTP server showing how to use the HTTP multipart package for file uploads and decoding post data.
 *
 * @author xinchen
 * @version 1.0
 * @date 13/08/2019 13:37
 */
public class HttpUploadServer {
    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));


    public static void main(String[] args) throws Exception {
        // 启动服务
        Server.start();
    }


    static class Server {
        public static void start() throws CertificateException, SSLException, InterruptedException {
            // configure SSL
            final SslContext sslCtx = SSL ? SslContextMaker.server() : null;

            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workGroup = new NioEventLoopGroup();


            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new HttpUploadServerInitializer(sslCtx));

                final Channel channel = bootstrap.bind(PORT).sync().channel();


                System.err.println("Open your web browser and navigate to " + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + "/");

                channel.closeFuture().sync();

            } finally {
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
            }

        }
    }
}
