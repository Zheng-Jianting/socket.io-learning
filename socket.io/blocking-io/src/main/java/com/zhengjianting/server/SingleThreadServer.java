package com.zhengjianting.server;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SingleThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);

        try {
            while (true) {
                // ServerSocket.accept() method blocks until a connection is made.
                Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                // receive request from client
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while((len = in.read(buffer, 0, 1024)) != -1) {
                    byteOut.write(buffer, 0, len);
                }
                log.info("receive request from port " + socket.getPort() + ": " + byteOut.toString("utf-8"));

                // send response to client
                out.write(("response information").getBytes(StandardCharsets.UTF_8));

                in.close();
                out.close();
                socket.close();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}