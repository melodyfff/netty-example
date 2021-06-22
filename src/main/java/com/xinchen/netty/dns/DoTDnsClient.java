package com.xinchen.netty.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.dns.DefaultDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DefaultDnsResponse;
import io.netty.handler.codec.dns.DnsOpCode;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.dns.TcpDnsQueryEncoder;
import io.netty.handler.codec.dns.TcpDnsResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.NetUtil;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.X509TrustManager;

/**
 *
 */
final class DoTDnsClient {
  private static final String QUERY_DOMAIN = "www.bing.com";
  private static final int DNS_SERVER_PORT = 853;
  private static final String DNS_SERVER_HOST = "8.8.8.8";

  private DoTDnsClient() {
  }

  private static void handleQueryResp(DefaultDnsResponse msg) {
    if (msg.count(DnsSection.QUESTION) > 0) {
      DnsQuestion question = msg.recordAt(DnsSection.QUESTION, 0);
      System.out.printf("name: %s%n", question.name());
    }
    for (int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++) {
      DnsRecord record = msg.recordAt(DnsSection.ANSWER, i);
      if (record.type() == DnsRecordType.A) {
        //just print the IP after query
        DnsRawRecord raw = (DnsRawRecord) record;
        System.out.println(NetUtil.bytesToIpAddress(ByteBufUtil.getBytes(raw.content())));
      }
    }
  }

  public static void main(String[] args) throws Exception {
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      final SslContext sslContext = SslContextBuilder
          .forClient()
          .protocols("TLSv1.3", "TLSv1.2")
          // 忽略证书认证
          .trustManager(new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          })
          .build();

      Bootstrap b = new Bootstrap();
      b.group(group)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
              ChannelPipeline p = ch.pipeline();
              p.addLast(sslContext.newHandler(ch.alloc(), DNS_SERVER_HOST, DNS_SERVER_PORT))
                  .addLast(new TcpDnsQueryEncoder())
                  .addLast(new TcpDnsResponseDecoder())
                  .addLast(new SimpleChannelInboundHandler<DefaultDnsResponse>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DefaultDnsResponse msg) {
                      try {
                        handleQueryResp(msg);
                      } finally {
                        ctx.close();
                      }
                    }
                  });
            }
          });
      final Channel ch = b.connect(DNS_SERVER_HOST, DNS_SERVER_PORT).sync().channel();

      int randomID = new Random().nextInt(60000 - 1000) + 1000;
      DnsQuery query = new DefaultDnsQuery(randomID, DnsOpCode.QUERY)
          .setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(QUERY_DOMAIN, DnsRecordType.A));
      ch.writeAndFlush(query).sync();
      boolean success = ch.closeFuture().await(10, TimeUnit.SECONDS);
      if (!success) {
        System.err.println("dns query timeout!");
        ch.close().sync();
      }
    } finally {
      group.shutdownGracefully();
    }
  }

}
