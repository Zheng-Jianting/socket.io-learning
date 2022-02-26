package com.zhengjianting.server;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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

        // thread pool
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        while (true) {
            Socket socket = serverSocket.accept();
            executor.execute(new ResponseThread(socket));
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

            // receive request
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                byteOut.write(buffer, 0, len);
            }
            log.info("receive request from port " + socket.getPort() + ": " + byteOut.toString("utf-8"));

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