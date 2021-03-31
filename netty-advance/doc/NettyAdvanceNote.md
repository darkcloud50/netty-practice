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

5.Netty优雅退出的一些误区  
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
    3.3通过Bootstrap的ChannelFactory和用户指定的Channel类型创建用于客户端连接的NioSocketChannel，它的功能类似于JDK NIO类库提供
    的SocketChannel。  
    3.4 创建默认的ChannelPipeline，用于调度和执行网络事件。  
    3.5 异步发起TCP客户端连接，判断连接是否成功，如果成功，则直接将NioSocketChannel注册到多路复用器上，监听读操作位，用于数据报
    读取和消息发送；如果没有立即连接成功，则注册连接监听位到多路复用器，等待连接结果。  
    3.6 注册对应的网络监听状态位到多路复用器。
    3.7 由多路复用器在I/O现场轮询各Channel，处理连接结果。  
    3.8 如果接连成功，设置Future结果，发送连接成功事件，触发ChannelPipeline执行。  
    3.9 由ChannelPileline调度执行系统和用户的ChannelHandler执行业务逻辑。  
    
   




  
