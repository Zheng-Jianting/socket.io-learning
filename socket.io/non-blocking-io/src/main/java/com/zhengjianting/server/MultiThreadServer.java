package com.zhengjianting.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MultiThreadServer {
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 1L;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        serverSocket.setSoTimeout(100);

        // thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            while (true) {
                Socket socket = null;

                /**
                 * 应用进程通过设置超时时间
                 * 通过系统调用不断轮询 (polling) 操作系统是否有 TCP Segment 到达 8888 端口
                 * 若没有 Segment 到达, 则服务端进程先干一会其它工作, 过一段时间再轮询
                 * 可以防止在第一阶段 (等待数据到达内核缓冲区) 服务端进程进入阻塞状态
                 * 但是在第二阶段 (将数据从内核缓冲区复制到进程缓冲区) 服务端进程仍然是阻塞的
                 * 即 非阻塞 同步 I/O
                 */
                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    log.info("执行系统调用, 操作系统响应为: 还没有 TCP Segment 到达 8888 端口");
                    // ...
                    continue;
                }

                executor.execute(new ResponseThread(socket));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
            executor.shutdown();
        }
    }
}

@Slf4j
class ResponseThread implements Runnable {
    private Socket socket;

    public ResponseThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            /**
             * 应用进程设置超时时间, 不断轮询操作系统内核: 是否有数据到达内核缓冲区
             * 若没有数据到达内核缓冲区, 则服务端进程先干一会其它工作, 过一段时间再轮询
             * 若有数据到达, 则应用进程阻塞直至将数据从内核缓冲区复制到应用进程缓冲区
             *
             * 即非阻塞 同步 I/O
             * 在第一阶段 (等待数据到达内核缓冲区时) 通过轮询避免应用进程进入阻塞状态, 体现了非阻塞
             * 但在第二阶段 (将数据从内核缓冲区复制到应用进程缓冲区时) 应用进程仍然是阻塞的, 体现了同步
             */
            socket.setSoTimeout(10);

            // receive request
            StringBuffer request = new StringBuffer();
            byte[] buffer = new byte[1024];
            int len;
            Polling:
            while (true) {
                try {
                    while ((len = in.read(buffer, 0, 1024)) != -1) {
                        request.append(new String(buffer, 0, len));
                    }
                    break Polling;
                } catch (SocketTimeoutException e) {
                    /**
                     * 说明这次轮询操作系统内核的答复是: 没有数据到达内核缓冲区
                     * 因此先干一会其它工作再次轮询
                     */
                    log.info("没有数据到达内核缓冲区");
                    continue;
                }
            }
            log.info("receive request from port " + socket.getPort() + ": " + request.toString());

            // send response to client
            out.write(("response information").getBytes(StandardCharsets.UTF_8));

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}