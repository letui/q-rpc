package io.letuismart.rpc.registry;

import io.letuismart.rpc.spec.RpcMethod;
import io.letuismart.rpc.spec.RpcScan;
import io.letuismart.rpc.spec.RpcService;
import io.letuismart.rpc.transform.ParamsConverter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.letuismart.rpc.spec.SpecEnum.Q_BEAN;
import static io.letuismart.rpc.spec.SpecEnum.Q_METHOD;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class QRpcBoot extends ChannelInitializer<SocketChannel> {

    private QRpcBoot() {
    }

    private final static QRpcBoot qRpcBoot=new QRpcBoot();
    private final static HashMap<String, QRpcDescribe> qRpcMap = new HashMap<String, QRpcDescribe>();

    private static SslContext sslCtx;
    private static Class entryClass;
    private static boolean ssl=false;
    private static int port=9079;

    public static void run(Class entry, String[] args) throws Exception {
        try {
            entryClass=entry;
            qRpcBoot.scanRpcService();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        // Configure SSL.
        if (ssl) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.ERROR))
                    .childHandler(qRpcBoot);
            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        p.addLast(new HttpRequestDecoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        //p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpResponseEncoder());
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast(new SimpleChannelInboundHandler<Object>() {
            String qBean;
            String qMethod;

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                ctx.flush();
            }
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                if (msg instanceof HttpRequest) {
                    HttpRequest request = (HttpRequest) msg;
                    qBean = request.headers().get(Q_BEAN.val());
                    qMethod = request.headers().get(Q_METHOD.val());
                }
                if (msg instanceof HttpContent) {
                    HttpContent httpContent = (HttpContent) msg;
                    ByteBuf content = httpContent.content();
                    if (msg instanceof LastHttpContent) {
                        String reqBody = content.toString(Charset.forName("utf-8"));
                        System.out.println(reqBody+" -----");
                        FullHttpResponse fullHttpResponse = callRpcService(qBean, qMethod, reqBody);
                        channelHandlerContext.writeAndFlush(fullHttpResponse);
                        channelHandlerContext.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            }
        });
    }


    public void scanRpcService() throws IllegalAccessException, InstantiationException {
        RpcScan rpcScan = (RpcScan) QRpcBoot.entryClass.getAnnotation(RpcScan.class);
        for (String package_ : rpcScan.packages()) {
            Set<Class<?>> classes = getClassesInPackage(package_);
            for (Class target : classes) {
                if (target.getAnnotation(RpcService.class) != null) {
                    RpcService rpcService = (RpcService) target.getAnnotation(RpcService.class);
                    String qBean = rpcService.value();
                    Map<String, Map<String, Parameter[]>> rpcMethods = new HashMap<>();

                    QRpcDescribe rpcDescribe = new QRpcDescribe();
                    rpcDescribe.setRpcClassName(target.getName());
                    rpcDescribe.setRpcMethods(rpcMethods);
                    rpcDescribe.setRpcBeanInstance(target.newInstance());

                    Method[] methods = target.getMethods();
                    int i = 0;
                    while (i < methods.length) {
                        boolean isRpcMethod = methods[i].getAnnotation(RpcMethod.class) != null;
                        if (isRpcMethod) {
                            Map<String, Parameter[]> defDetail = new HashMap<>();
                            defDetail.put(methods[i].getReturnType().getName(), methods[i].getParameters());
                            rpcMethods.put(methods[i].getName(), defDetail);
                        }
                        i++;
                    }
                    qRpcMap.put(qBean, rpcDescribe);
                }
            }
        }
    }


    public FullHttpResponse callRpcService(String qBean, String qMethod, String reqBody) {


        QRpcDescribe rpcDescribe = qRpcMap.get(qBean);
        Object rpcBeanInstance = rpcDescribe.getRpcBeanInstance();
        Map<String, Parameter[]> methodDefDetail = rpcDescribe.getRpcMethods().get(qMethod);
        Parameter[] rpcMethodParamsType = methodDefDetail.values().iterator().next();
        Class[] methodParamsClass=new Class[rpcMethodParamsType.length];
        for(int i=0;i<rpcMethodParamsType.length;i++){
            methodParamsClass[i]=rpcMethodParamsType[i].getType();
        }

        try {
            Method rpcMethod = rpcBeanInstance.getClass().getMethod(qMethod,methodParamsClass);
            Object[] convertParams = new ParamsConverter().convert(reqBody, rpcMethodParamsType);
            Object invokedValue = rpcMethod.invoke(rpcBeanInstance, convertParams);
            String repsonseBody=String.valueOf(invokedValue);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                    Unpooled.copiedBuffer(repsonseBody, CharsetUtil.UTF_8));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, repsonseBody.getBytes().length);
            return response;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<Class<?>> getClassesInPackage(String pack) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        boolean recursive = true;
        String packageName = pack;
        String packageDirName = pack.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(
                    packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClasses(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection())
                                .getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx)
                                            .replace('/', '.');
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(".class")
                                            && !entry.isDirectory()) {
                                        String className = name.substring(
                                                packageName.length() + 1, name
                                                        .length() - 6);
                                        classes.add(Class
                                                .forName(packageName + '.'
                                                        + className));
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        // log.error("在扫描用户定义视图时从jar包获取文件出错");
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    private void findAndAddClasses(String packageName,
                                         String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findAndAddClasses(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                String className = file.getName().substring(0,
                        file.getName().length() - 6);
                try {
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
