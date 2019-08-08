package com.xinchen.netty.common;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 *
 * 公共{@link SslContext}生成器
 *
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 12:42
 */
public final class SslContextMaker {

    public static SslContext server() throws CertificateException, SSLException {
        // 生成临时自签名证书以进行测试
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(),ssc.privateKey()).build();
    }

    public static SslContext client() throws SSLException {
        return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    }
}
