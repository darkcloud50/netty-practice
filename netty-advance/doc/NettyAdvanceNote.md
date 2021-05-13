一  
1. NioEventLoop是非守护进程。  
   NioEventLoop运行之后，不会主动退出。  
   只有调用shutdown系列方法，NioEventLoop才会退出。  
  
2. 通过注册监听器GenericFutureListener，可以异步等待I/O执行结果。  
   通过syc或者await，主动阻塞当前调用方的线程，等待操作结果，也就是通常说的异步转同步。
   
3. Netty优雅退出总结起来有如下三大操作类。  
  3.1 把NIO线程的状态位设置成ST_SHUTTING_DOWN，不再处理新的消息(不允许再对外发送消息)。  
  3.2 推出前的预处理操作：把发送队列中尚未发送或者正在发送的消息发送完（备注：不保证能够发送完）、把已经到期或在退出超时之前到期的
  定时任务执行完成、把用户注册到NIO线程的退出HOOK任务执行完成。  
  3.3 资源的释放操作：所有Channel的释放、多路复用器的去注册和关闭、所有队列和定时任务的清空取消，最后是NioEventLoop线程的退出。  
  
4. 调用NioEventLoop的shutdownGracefully方法，首先要修改线程状态为正在关闭状态，它的实现在父类SingleThreadEventExecutor中，需要
注意的是，修改线程状态时要对并发调用做保护，因为shutdownGracefully方法可由NioEventLoop线程发起，也可能多个应用线程并发执行。对于
线程状态的修改需要做并发保护，最简单的策略就是加锁，或者采用原子累加自旋锁的方式，Netty5采用的是加锁策略，Netty4则采用后者。  

5. Netty优雅退出的一些误区  
  5.1 待发送的消息：调用优雅退出方法之后不会立即关闭链路。ChannelOutboundBuffer中的消息可以继续发送，本轮发送操作执行完成之后，
  无论是否还有消息尚未发送出去，在下一轮的Selector轮询中，链路都将被关闭，没有发送完成的消息将被释放和丢弃。  
  5.2 需要发送的新消息：由于应用线程可以随时通过调用Channel的write系列接口发送消息，即便ShutDownHook触发了Netty的优雅退出方法，
  在Netty优雅退出方法执行期间，应用线程仍然有可能继续调用Channel发送消息，这些消息将发送失败。  
  因此，应用程序的正确性不能完全依赖Netty的优雅退出机制，需要在应用层面做容错设计和处理。例如，服务端在返回响应前关闭了，导致响应
  没有发送给客户端，这可能会触发客户端的I/O异常，或者恰好发生了超时异常，客户端需要对I/O或超时异常做容错处理，采用Failover重试其它
  可用的服务端，而不能寄希望于服务端永远正确。Netty优雅退出更重要的是保证资源、句柄、线程的快速释放、以及相关对象的清理。  
  5.3 Netty优雅退出通常用于应用进程退出时，在应用ShutdownHook中调用EventLoopGroup的shutdownGracefully(long quietPeriod, long
   timeout, TimeUnit unit)接口，指定退出的超时时间，以防止因为一些任务执行被阻塞而无法正常退出。  
   
二  
1. 尽管Bootstrap自身不是线程安全的，但是执行Bootstrap的连接操作是串行执行的，而且connect(String inetHost, int port)方法本身是
线程安全的，它会创建一个新的NioSocketChannel，并从初始构造的EventLoopGroop中选择一个NioEventLoop线程执行真正的Channel连接操作，
与执行Bootstrap的线程无关，所以通过一个Bootstrap连续发起多个连接操作是安全的。  
 
2. 在同一个Bootstrap中连续创建多个客户端连接，需要注意的是，EventLoopGroup是共享的，也就是说这些连接共用同一个NIO线程组EventLoopGroup，
当某个链路发生异常或者关闭时，只需要关闭并释放Channel本身即可，不能同时销毁Channel所使用的NioEventLoop和所在的线程组EventLoopGroup。  
 
3. Netty客户端创建流程说明如下。  
 3.1 用户线程创建Bootstrap实例，通过API设置创建客户端相关的参数，异步发起客户端连接。  
 3.2 创建处理客户端连接、I/O读写的Reactor线程组NioEventLoopGroup，可以通过构造函数指定I/O线程的个数，默认为CPU内核数的2倍。  
 3.3 通过Bootstrap的ChannelFactory和用户指定的Channel类型创建用于客户端连接的NioSocketChannel，它的功能类似于JDK NIO类库提供
     的SocketChannel。  
 3.4 创建默认的ChannelPipeline，用于调度和执行网络事件。  
 3.5 异步发起TCP客户端连接，判断连接是否成功，如果成功，则直接将NioSocketChannel注册到多路复用器上，监听读操作位，用于数据报
     读取和消息发送；如果没有立即连接成功，则注册连接监听位到多路复用器，等待连接结果。  
 3.6 注册对应的网络监听状态位到多路复用器。
 3.7 由多路复用器在I/O现场轮询各Channel，处理连接结果。  
 3.8 如果接连成功，设置Future结果，发送连接成功事件，触发ChannelPipeline执行。  
 3.9 由ChannelPileline调度执行系统和用户的ChannelHandler执行业务逻辑。
    
三
1. 为了提升消息接收和发送性能，Netty针对ByteBuf的申请和释放采用池化技术，通过PooledByteAllocator可以创建基于内存池分配的ByteBuf对象，这样就可以避免了每次消息读写都申请和释放ByteBuf。由于ByteBuf涉及byte[]数组的创建和销毁，对于性能要求苛刻的系统而言，重用ByteBuf带来的性能收益是非常可观的。

2. 业务ChannelInboundHandler继承自SimpleChannelInboundHandler，实现它的抽象方法channelRead0(ChannelHandlerContext ctx, I msg)， ByteBuf的释放业务不用关心，由SimpleChannelInboundHandler负责释放。  

3. 在业务ChannelInboundHandler中调用ctx.fireChannelRead(msg)方法，让请求消息继续向后执行，直到调用DefaultChannelPipeline的内部类TailContext，由它来负责释放请求消息。

4. 无论是基于内存池还是非内存池分配的ByteBuf，如果是堆内存，则将堆内存转换成堆外内存，然后释放HeapByteBuf，待消息发送完成，再释放转换后的DirectByteBuf，如果是DirectByteBuffer，则不需要转换，待消息发送完成之后释放。因此对于需要发送响应的ByteBuf，由业务创建，但是不需要由业务来释放。

四
1. 为了提升性能，Netty默认的I/O Buffer使用直接内存DirectByteBuf，可以减少JVM用户态到内核态Socket读写的内存拷贝，即“零拷贝”。由于是直接内存，无法直接转换成堆内存，因此它不支持array()方法，用户需要自己做内存拷贝操作。

五
1. 为了防止在高并发场景下，由于服务端处理慢导致客户端消息积压，除了服务端要做流控，客户端也需要做并发保护，防止自身发生消息积压。当发送队列待发送的字节数组达到高水位时，对应的Channel就变为不可写状态。由于高水位并不影响业务线程调用write方法把消息加入到待发送队列，因此，必须在消息发送时对Channel的状态进行判断：当达到高水位时，Channel的状态被设置为不可写，通过对Channel的可写状态进行判断来决定是否发送消息。  
ctx.channel().config().setWriteBufferHighWaterMark(10 * 1024 * 1024);  
if (ctx.channel().isWritable()) { }

十  
1. 尽管LinkedBlockingQueue通过读写锁来提升性能，但是当业务线程数和写操作比较多时，锁竞争对性能的影响还是蛮大的。  

2. 当客户端并发接入数比较多时，可以利用Netty提供的网络I/O线程和ChannelHandler执行线程绑定机制来降低锁竞争，提升系统性能。当然，如果业务自定义线程池自己实现并做了锁竞争优化，可以达到同样的优化效果。如果业务采用自定义线程池，优化方向时尽量消除锁竞争。

3. 关键技术点如下：  
3.1 利用Netty的ChannelId绑定业务线程池的某个业务线程，后续该Channel的所有消息读取和发送都由绑定的Netty NioEventLoop和业务线程来执行，把锁竞争降到最低。  
3.2 业务线程池采用一个线程对应一个消息队列的方式，降低队列的锁竞争。可以继承JDK的ExecutorService自己实现，或者利用Executors的newSingleThreadExecutor()方法创建多个SingleThreadExecutor，这样就实现了工作线程和消息队列的一对一关系。

    
   




  
