## I/O 模型

一个输入操作通常包括两个阶段：

- 等待数据准备好
- 从内核向进程复制数据

对于一个套接字上的输入操作，第一步通常涉及等待数据从网络中到达。当所等待数据到达时，它被复制到内核中的某个缓冲区。第二步就是把数据从内核缓冲区复制到应用进程缓冲区。

Unix 有五种 I/O 模型：

- 同步 I/O
  - 阻塞式 I/O
  - 非阻塞式 I/O
  - I/O 复用（select 和 poll）
  - 信号驱动式 I/O（SIGIO）
- 异步 I/O（AIO）

同步 I/O 和异步 I/O 的区别：在第二阶段进行 I/O 时应用进程是否会阻塞。当然，异步 I/O 通知应用进程时已经完成 I/O 了。

### Blocking I/O

在 `blocking-io` 包中使用 `java.util.concurrent.CountDownLatch` 模拟了 10 个客户端同时向服务端发送请求，当使用单线程实现服务端时：

- 在第一阶段，当 ServerSocket 监听的端口没有 TCP Segment 到达时，服务端进程由于执行了以下语句，会一直阻塞直至数据通过网络传输到达操作系统内核缓冲区。

  ```java
  Socket socket = serverSocket.accept();
  ```

- 在第二阶段，服务端进程也会一直阻塞直至将数据从内核缓冲区复制到服务端进程缓冲区。

  ```java
  InputStream in = socket.getInputStream();
  while ((len = in.read(buffer, 0, 1024)) != -1) {
  	// ...
  }
  ```

由于服务端一次只能处理一个请求，并且处理完成后才能接收下一个请求，这在高并发时显然是不可取的。

可以使用多线程优化，服务端的主线程用于接收请求，每接收到一个请求，就创建一个子线程进行处理，主线程则继续接收请求，但这样做也有局限性：

- 请求报文仍然是一个个接收的。
- 线程数量是有限的，即使用 `ThreadPoolExecutor`线程池缓解，也会导致 `BlockingQueue`积压任务。
- 处理线程在第二阶段仍然是阻塞的。

### Non Blocking I/O

应用进程执行系统调用之后，内核返回一个错误码。应用进程可以继续执行，但是需要不断的执行系统调用来获知 I/O 是否完成，这种方式称为轮询（polling）。

由于 CPU 要处理更多的系统调用，因此这种模型的 CPU 利用率比较低。

在 `non-blocking-io` 包中通过设置 socket 的超时时间实现：

```java
ServerSocket.setSoTimeout()
Socket.setSoTimeout()
```

例如 `socket = serverSocket.accept()` 超时，则表示还没有 TCP Segment 到达内核缓冲区，那么就先做一会其它工作再轮询操作系统。