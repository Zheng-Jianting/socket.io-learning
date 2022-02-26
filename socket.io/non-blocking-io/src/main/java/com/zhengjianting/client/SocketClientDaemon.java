package com.zhengjianting.client;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class SocketClientDaemon {
    public static void main(String[] args) {
        final int clientCount = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(clientCount);

        for (int i = 0; i < clientCount; i++) {
            new Thread(new RequestThread(i + 1, countDownLatch)).start();
            countDownLatch.countDown();
        }

        synchronized (SocketClientDaemon.class) {
            try {
                SocketClientDaemon.class.wait();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}