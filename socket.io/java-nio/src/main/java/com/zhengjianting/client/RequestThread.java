package com.zhengjianting.client;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RequestThread implements Runnable {
    int index;
    CountDownLatch countDownLatch;

    public RequestThread(int index, CountDownLatch countDownLatch) {
        this.index = index;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        InputStream in = null;
        OutputStream out = null;

        try {
            Socket socket = new Socket("127.0.0.1", 8888);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            this.countDownLatch.await();

            // send request to server
            out.write(("request " + this.index + " sent message to server.").getBytes(StandardCharsets.UTF_8));
            out.flush();
            // 告知数据传输完毕, 否则在服务端 socket.getInputStream().read() 无法等于 -1.
            socket.shutdownOutput();
            log.info("request " + this.index + " is sent, waiting for server response.");

            // receive response from server
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            // InputStream.read() method blocks until input data is available.
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                byteOut.write(buffer, 0, len);
            }
            log.info("request " + this.index + " receive response from server: " + byteOut.toString("utf-8"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}