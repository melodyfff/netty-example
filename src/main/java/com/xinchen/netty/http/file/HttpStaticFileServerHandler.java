package com.xinchen.netty.http.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 *
 *
 * 一个简单的处理程序： 向传入的http请求返回http响应
 *
 * 本示例还实现了{@code 'if-Modified-Since'} header来利用浏览器缓存 ,参考： <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>浏览器缓存是如何工作</h3>
 *
 * web浏览器缓存生效headers示例：
 *
 * <ol>
 * <li>请求＃1返回{@code /file1.txt}。</ li>的内容
 * <li>浏览器缓存{@code /file1.txt}的内容。</ li>
 * <li> {@code /file1.txt}的请求＃2不返回该内容再次归档。而是返回304 Not Modified。这说明了浏览器使用存储在其缓存中的内容。</ li>
 * <li>服务器知道文件未被修改，因为{@code If-Modified-Since}日期与文件的最后日期相同修改日期。</ li>
 * </ ol>
 *
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 *
 * @author xinchen
 * @version 1.0
 * @date 12/08/2019 10:45
 */
public class HttpStaticFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /** 格林威治标准时间+8 */
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT+8";

    /** 浏览器缓存多长时间 */
    public static final int HTTP_CACHE_SECONDS = 60;

    private FullHttpRequest request;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        this.request = request;

        // 如果request请求没有被解码 400, "Bad Request"
        if (!request.decoderResult().isSuccess()){
            sendError(ctx,HttpResponseStatus.BAD_REQUEST);
            return;
        }

        // 如果请求类型不是GET 405, "Method Not Allowed"
        if (!HttpMethod.GET.equals(request.method())){
            this.sendError(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        final String uri = request.uri();
        final String path = sanitizeUri(uri);

        // 如果路径为空返回  403, "Forbidden"
        if(null == path){
            this.sendError(ctx,HttpResponseStatus.FORBIDDEN);
            return;
        }

        File file = new File(path);

        // 如果文件找不到 404, "Not Found"
        if(file.isHidden()||!file.exists()){
            this.sendError(ctx,HttpResponseStatus.NOT_FOUND);
            return;
        }

        // 如果是文件夹
        if(file.isDirectory()){
            if (uri.endsWith("/")){
                // 返回文件列表
                this.sendListing(ctx,file,uri);
            } else {
                // 重新发送请求,并在结尾添加上'/'
                this.setdRedirect(ctx,uri+'/');
            }
            return;
        }

        // 如果不是文件 403, "Forbidden"
        if(!file.isFile()){
            this.sendError(ctx,HttpResponseStatus.FORBIDDEN);
            return;
        }


        // 缓存校验
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (null!=ifModifiedSince && !ifModifiedSince.isEmpty()){
            SimpleDateFormat dateFormat = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);

            // 缓存的文件上次修改时间
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000L;
            // 文件上次修改时间
            long fileLastModifiedSeconds = file.lastModified() / 1000L;


            // 如果文件没有被修改，返回 304 Not Modified
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds){
                this.sendNotModified(ctx);
                return;
            }

        }

        RandomAccessFile raf;
        try {
            // 读取文件
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e){
            sendError(ctx,HttpResponseStatus.NOT_FOUND);
            return;
        }


        // 构造返回
        long fileLength = raf.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        // 响应头设置文件长度
        HttpUtil.setContentLength(response, fileLength);
        // 响应头设置文件类型
        setContentTypeHeader(response,file);
        // 响应头设置日期和缓存头
        setDateAndCacheHeaders(response,file);


        checkKeepAlive(request, keepAlive, response);

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;

        // 判断是否是SSL连接
        if (ctx.pipeline().get(SslHandler.class) == null){
            // newProgressivePromise 特殊{@link ChannelPromise}，一旦相关字节传输，将通知
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
            // 输入结束标识
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());

            // HttpChunkedInput will write the end marker (LastHttpContent) for us
            lastContentFuture = sendFileFuture;
        }


        // 添加执行监听器
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                // 当文件传输的时候触发此监听方法
                // 本方法要文件有一定大小才会有详细输出
                if(total < 0){
                    //total unknown
                    System.err.println(future.channel()+" Transfer progress: "+ progress);
                } else {
                    System.err.println(future.channel()+" Transfer progress: "+ progress +" / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });


        // Decide whether to close the connection or not.
        if (!keepAlive){
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


    /** uri安全检查 */
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
    private static String sanitizeUri(String uri){
        // Decode the path
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty()||uri.charAt(0) != '/'){
            return null;
        }


        // Convert file separators
        uri = uri.replace('/', File.separatorChar);

        // 假装安全检查，在实践使用中需要做一些严肃的事情
        if (uri.contains(File.separator+'.')||
                uri.contains('.'+File.separator)||
                uri.charAt(0) == '.' ||
                uri.charAt(uri.length()-1) == '.'||
                INSECURE_URI.matcher(uri).matches()
        ){
            return null;
        }

        // 转换为绝对路径
        return SystemPropertyUtil.get("user.dir") + File.separator + uri;
    }

    /** 文件名安全检查 */
    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");
    /** 发送返回(文件列表) */
    private void sendListing(ChannelHandlerContext ctx,File dir,String dirPath){

        StringBuilder buf = new StringBuilder();
        buf.append("<!DOCTYPE html>\r\n")
                .append("<html><head><meta charset='utf-8' /><title>")
                .append("Listing of: ")
                .append(dirPath)
                .append("</title></head><body>\r\n")

                .append("<h3>Listing of: ")
                .append(dirPath)
                .append("</h3>\r\n")

                .append("<ul>")
                .append("<li><a href=\"../\">..</a></li>\r\n");

        for (File f: Objects.requireNonNull(dir.listFiles())){
            // 如果是隐藏文件或者不能读取则中断
            if (f.isHidden()||!f.canRead()){
                continue;
            }

            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()){
                // 如果不是被允许的文件名则中断
                continue;
            }
            buf.append("<li><a href=\"")
                    .append(name)
                    .append("\">")
                    .append(name)
                    .append("</a></li>\r\n");

        }
        buf.append("</ul></body></html>\r\n");


        // 分配空间,并写入数据
        ByteBuf buffer = ctx.alloc().buffer(buf.length());
        buffer.writeCharSequence(buf.toString(), CharsetUtil.UTF_8);

        // HTTP1.1默认开启keep-alive
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        // 返回并清除连接
        this.sendAndCleanupConnection(ctx, response);
    }


    private void setdRedirect(ChannelHandlerContext ctx,String newUri){
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER);
        // 设置跳转
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        this.sendAndCleanupConnection(ctx,response);
    }

    private void sendError(ChannelHandlerContext ctx,HttpResponseStatus status){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8");

        this.sendAndCleanupConnection(ctx,response);
    }

    private void sendNotModified(ChannelHandlerContext ctx){
        // 文件没有被更改
        FullHttpResponse response= new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
        setDateHeader(response);

        this.sendAndCleanupConnection(ctx,response);
    }

    /**
     * 如果keep-alive被禁止，将"Connection: close" 添加到返回头里面，并在发送响应后关闭连接
     * @param ctx ChannelHandlerContext
     * @param response FullHttpResponse
     */
    private void sendAndCleanupConnection(ChannelHandlerContext ctx,FullHttpResponse response){
        final FullHttpRequest request = this.request;
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        // 设置返回content-length
        HttpUtil.setContentLength(response,response.content().readableBytes());


        checkKeepAlive(request, keepAlive, response);

        // 写入数据
        ChannelFuture flushPromise = ctx.writeAndFlush(response);


        // 如果keep-alive没开启，则添加关闭连接监听器
        if (!keepAlive){
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 为HTTP response 设置时间响应头
     * @param response Http response
     */
    private static void setDateHeader(FullHttpResponse response){
        response.headers().set(HttpHeaderNames.DATE, getTimeString());
    }


    /**
     * 为HTTP response 设置时间响应头和缓存响应头
     * @param response Http response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response,File fileToCache){
        SimpleDateFormat dateFormat = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormat.format(time.getTime()));

        // ADD cache header
        time.add(Calendar.SECOND,HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormat.format(time.getTime()))
                .set(HttpHeaderNames.CACHE_CONTROL, "private,max-age=" + HTTP_CACHE_SECONDS)
                .set(HttpHeaderNames.LAST_MODIFIED, dateFormat.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     * @param response HTTP Response
     * @param file file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response,File file){
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
    }

    private static String getTimeString(){
        SimpleDateFormat dateFormat = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        return dateFormat.format(time.getTime());
    }


    private void checkKeepAlive(FullHttpRequest request, boolean keepAlive, HttpResponse response) {
        if(!keepAlive){
            // 如果没有keep-alive则设置响应头 connection: close
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)){
            // 如果是HTTP1.0开启keep-alive
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
    }

}
