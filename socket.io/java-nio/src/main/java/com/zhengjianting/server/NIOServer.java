package com.zhengjianting.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class NIOServer {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        ssChannel.register(selector, SelectionKey.OP_ACCEPT);

        ServerSocket serverSocket = ssChannel.socket();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8888);
        serverSocket.bind(address);

        while (true) {
            /**
             * This method performs a blocking selection operation.
             * It returns only after at least one channel is selected.
             */
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isValid() && key.isAcceptable()) {
                    /**
                     * 获取与事件 SelectionKey 相关联的通道 Channel
                     * 返回值类型为 SelectableChannel, 它是所有可以在选择器上注册的 Channel 的父类
                     */
                    ServerSocketChannel ssChannel1 = (ServerSocketChannel) key.channel();

                    // 服务器会为每个新连接创建一个 SocketChannel
                    SocketChannel sChannel = ssChannel1.accept();
                    sChannel.configureBlocking(false);

                    /**
                     * 这个 Socket Channel 主要用于从客户端读取数据
                     * 需要将其注册到选择器 Selector 上, 否则无法监听到达 sChannel 的数据
                     */
                    sChannel.register(selector, SelectionKey.OP_READ);
                }
                else if (key.isValid() && key.isReadable()) {
                    SocketChannel sChannel = (SocketChannel) key.channel();
                    readDataFromSocketChannel(sChannel);
                }

                // 处理完毕后就移除, 避免事件被重复处理
                keyIterator.remove();
            }
        }
    }

    private static void readDataFromSocketChannel(SocketChannel socketChannel) throws IOException {
        // 获取客户端 InetSocketAddress
        InetSocketAddress address = (InetSocketAddress) socketChannel.getRemoteAddress();
        int port = address.getPort();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuffer data = new StringBuffer();

        while (true) {
            buffer.clear();
            int len =  socketChannel.read(buffer);
            if (len == -1)
                break;
            buffer.flip(); // 由写切换为读, 即 limit = position; position = 0;
            int limit = buffer.limit();
            char[] dst = new char[limit];
            for (int i = 0; i < limit; i++) {
                dst[i] = (char) buffer.get(i);
            }
            data.append(dst);
            buffer.clear();
        }

        log.info("receive request from port " + port + ": " + data.toString());

        // send response to client
        ByteBuffer response = ByteBuffer.wrap(("response information").getBytes(StandardCharsets.UTF_8));
        socketChannel.write(response);

        // close SocketChannel
        socketChannel.close();
    }
}