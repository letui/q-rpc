/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.letuismart.rpc.registry;

import com.google.gson.Gson;
import io.letuismart.rpc.exception.BuildClientException;
import io.letuismart.rpc.exception.NotFoundRpcParamException;
import io.letuismart.rpc.exception.UnsupportException;
import io.letuismart.rpc.spec.RpcClient;
import io.letuismart.rpc.spec.RpcParam;
import io.letuismart.rpc.transform.ReturnConverter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.letuismart.rpc.spec.SpecEnum.Q_BEAN;
import static io.letuismart.rpc.spec.SpecEnum.Q_METHOD;

public final class QRpcClient {

    static final Gson gson = new Gson();

    public static <T> T build(Class<T> clazz) {
        if (clazz.isInterface() && clazz.getAnnotation(RpcClient.class) != null) {
            Proxy proxy = (Proxy) Proxy.newProxyInstance(QRpcClient.class.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    RpcClient annoRpcClient = clazz.getAnnotation(RpcClient.class);
                    String qBean = annoRpcClient.service();
                    String qMethod = method.getName();
                    Parameter[] parameters = method.getParameters();
                    Map<String, String> qParam = new HashMap<>();
                    int i = 0;
                    for (Parameter param : parameters) {
                        RpcParam annoRpcParam = param.getAnnotation(RpcParam.class);
                        if (annoRpcParam == null) {
                            throw new NotFoundRpcParamException("" + method);
                        }
                        qParam.put(annoRpcParam.value(), String.valueOf(args[i]));
                        i++;
                    }
                    String response = call(qBean, qMethod, qParam);
                    return new ReturnConverter().convert(response, method.getReturnType());
                }
            });
            return (T) proxy;
        }
        throw new BuildClientException("[RpcClient] is not interface or not found @RpcClient annotation");
    }

    private static String call(String qBean, String qMethod, Map<String, String> params) throws Exception {
        final StringBuffer responseBody = new StringBuffer();
        Properties properties=new Properties();
        properties.load(System.class.getResourceAsStream("/q-rpc.properties"));
        String servers=properties.getProperty("q-rpc.servers");
        String server=servers.split("[;]]")[0];
        URI uri = new URI(server);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new UnsupportException("Only HTTP(S) is supported.");
        }
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer() {
                                 @Override
                                 protected void initChannel(Channel channel) throws Exception {
                                     ChannelPipeline p = channel.pipeline();
                                     if (sslCtx != null) {
                                         p.addLast(sslCtx.newHandler(channel.alloc()));
                                     }
                                     p.addLast(new HttpClientCodec());
                                     p.addLast(new HttpContentDecompressor());
                                     p.addLast(new SimpleChannelInboundHandler<HttpObject>() {
                                         @Override
                                         public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
                                             if (msg instanceof HttpResponse) {
                                                 HttpResponse response = (HttpResponse) msg;
                                                 System.err.println("STATUS: " + response.status());
                                                 System.err.println("VERSION: " + response.protocolVersion());
                                             }
                                             if (msg instanceof HttpContent) {
                                                 HttpContent content = (HttpContent) msg;
                                                 System.out.println(content.content().toString(CharsetUtil.UTF_8));
                                                 responseBody.append(content.content().toString(CharsetUtil.UTF_8));
                                                 ctx.close();
                                             }
                                         }
                                         @Override
                                         public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                             cause.printStackTrace();
                                             ctx.close();
                                         }
                                     });
                                 }
                             }
                    );
            Channel ch = b.connect(host, port).sync().channel();
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().set(Q_BEAN.val(), qBean);
            request.headers().set(Q_METHOD.val(), qMethod);
            String reqBody = gson.toJson(params);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH, reqBody.getBytes().length);
            request.content().writeBytes(reqBody.getBytes());
            ch.writeAndFlush(request);
            while (responseBody.length()==0) {

            }
            ch.closeFuture().sync();
            return responseBody.toString();
        } finally {
            group.shutdownGracefully();
        }
    }
}
