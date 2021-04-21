十三  
1. Netty服务端创建的步骤  
1.1 创建ServerBootstrap实例；  
1.2 设置并绑定Reactor线程池；  
1.3 设置并绑定服务端Channel；(Netty通过工厂类，利用反射创建NioServerSocketChannel对象)  
1.4 TCP链路建立的时候创建并初始化ChannelPipeline；  
1.5 添加并设置ChannelHandler；  
1.6 绑定并启动监听端口；  
1.7 Selector轮询；  
1.8 当轮询到准备就绪的Channel之后，就由Reactor线程的；  
1.9 执行Netty系统ChannelHandler和用户添加定制的ChannelHandler。

2. NioEventLoopGroup实际就是Reactor线程池，负责调度和执行客户端的接入、网络读写事件的处理、用户自定义任务和定时任务的执行。

3. backlog指定了内核为此套接字接口排队的最大连接个数，对于给定的监听套接口，内核要维护两个队列：未连接队列和已连接队列。backlog被规定为两个队列总和的最大值。

4. NioServerSocketChannel的注册：  
 首先判断是否是NioEventLoop自身发起的操作，如果是，则不存在并发操作，直接执行Channel注册，如果由其它线程发起，则封装成一个Task放入消息队列中异步执行。此处，由于是ServerBootstrap所在的线程执行的注册操作，所以会将其封装成Task投递到NioEventLoop中执行。  

5. ChannelRegisted事件传递完成后，判断ServerSocketChannel监听是否成功，如果成功，需要触发NioServerSocketChannel的ChannelActive事件。

6. isActive()也是个多态方法。如果是服务端，判断监听是否启动；如果是客户端，判断TCP连接是否完成。由于不同类型的Channel对读操作的准备工作不同，因此unsafe.beginRead也是个多态方法，对于NIO通信，无论是客户端还是服务端，都是要修改网络监听操作位为为自身感兴趣的，对于NioServerSocketChannel感兴趣的操作是OP_ACCEPT(16)，于是重新修改注册的操作位为OP_ACCEPT。

7. 客户端接入步骤：处理网络读写、连接和客户端请求接入的Reactor线程就是NioEventLoop。当多路复用器检测到新的准备就绪的Channel时，默认执行processSelectedKeysOptimizes方法。由于Channel的Attachment是NioServerSocketChannel，所以执行processSelectedKey，根据就绪的操作位，执行不同的操作。由于监听的是连接操作，所以执行unsafe.read()方法。对于NioServerSocketChannel，它使用的是NioMessageUnsafe。对于doReadMessage实际就是接收新的客户端连接并创建NioSocketChannel。接收到新的客户端连接后，触发ChannelPipeline的ChannelRead方法。执行headChannelHandlerContext的fireChannelRead方法，事件在ChannelPipeline中传递，执行ServerBootstrapAcceptor的channeRead方法，该方法主要分位三个步骤：1.将启动时传入的childHandler加入客户端SocketCHannel的ChannelPipeline中；2.设置客户端的SocketChannel的TCP参数；3.注册SocketChannel到多路复用器。后续多路复用器检测到写操作调用unsafe.read()调用用户自定义handler。


十四
1. Netty客户端创建流程  
1.1. 用户线程创建Bootstrap实例，通过API设置创建客户端相关的参数，异步发起客户端连接；  
1.2. 创建处理客户端连接、I/O读写的Reactor线程组NioEventLoopGroup；
1.3. 通过Bootstrap的ChannelFactory和用户指定的Channel类型创建用于客户端连接的NioSocketChannel；
1.4. 创建默认的ChannelHanderPipeline，用户调度和执行网络事件；
1.5. 异步发起TCP连接，判断连接是否成功。如果成功，则直接将NioSocketChannel注册到多路复用器上监听读操作位，用于数据报读取和发送；如果没有立即连接成功，则注册连接监听位到多路复用器上，等待连接结果；  
1.6. 注册对应的网络监听状态到多路复用器；  
1.7. 由多路复用器在I/O线程中轮询各Channel，处理连接结果；  
1.8 如果连接成功，设置Future结果，发送连接成功事件，触发ChannelPipeline执行；  
1.9 由ChannelPipeline调度执行系统和用户的ChannelHandler，执行业务逻辑。

2. 客户端连接操作
2.1 首要要创建和初始化NioSocketChannel  
2.2 从NioEventLoopGroup中获取NioEventLoop，然后使用其作为参数创建NioSocketChannel  
2.3 初始化Channel之后，将其注册到Selector上  
2.4 链路创建成功后，发起异步的TCP连接  
需要注意的是，SocketChannel执行connect操作后有以下三种结果：  
(1) 连接成功，返回True；  
(2)暂时没有连接上，服务端没有返回ACK应答，连接不确定，返回False；  
(3)连接失败，直接抛出I/O异常。  
如果是第二种情况，需要将NioSocketChannel中的selectionKey设置为OP_CONNECT，监听连接结果。  
异步连接返回之后，需要判断连接结果，如果连接成功，则触发ChannelActive事件，最终会将NioSocketChannel中selectionKey设置为OP_READ，用于监听网络读操作。  
如果没有立即连接上服务器，则注册OP_CONNETCT到多路复用器；如果连接过程发生异常，则关闭链路，进入连接失败处理流程。


十五  
1. ByteBuffer的局限性：  
1.1 ByteBuffer长度固定，一旦分配完成，它的容量不能动态扩展和收缩，当它需要编码的POJO对象大于ByteBuffer的容量时，会发生索引越界异常；  
1.2 ByteBuffer只有一个标识位置的指针position，读写的时候需要手工调用flip()和rewind()等，使用者必须小心谨慎地处理这些API，否则容易导程序处理失败；
1.3 ByteBuffer的API功能有限，一些高级和实用的特性它不支持，需要使用者自己编程实现。

2. ByteBuf通过两个位置指针来协助缓冲区的读写操作，读写使用readerIndex，写操作使用writeIndex。

3. readerIndex和writerIndex的取值一开始都是0，随着数据的写入writerIndex会增加，读数据会使readerIndex增加，但它不会超过writerIndex。在读取之后，0~readerIndex就被视为discard的，调用discardReadBytes方法，可以释放这部分空间，它的作用类似于ByteBuffer的compact方法。readerIndex和writerIndex之间的数据是可读取的，等价于ByteBuffer position 和 limit 之间的数据。writeInedex和capacity之间的空间是可写的，等价于ByteBuffer limit 和 capacity之间的可用空间。

4. ByteBuf是如何实现动态扩展的：  
4.1 通常情况下，当我们对ByteBuffer进行put操作的时候，如果缓冲区剩余可写空间不够，就会发生BufferOverflowException异常。为了避免这个问题，通常在进行put操作的时候会对剩余可用空间进行校验。如果剩余空间不足，需要重新创建一个新的ByteBuffer，并将之前的ByteBuffer复制到新创建的ByteBuffer中，最后释放老的ByteBuffer。
4.2 ByteBuf对write操作进行了封装，由ByteBuf的write操作负责进行剩余可用空间的校验。如果可用缓冲区不足，ByteBuf会自动进行动态扩容。

5. 由于NIO的Channel读写的参数都是ByteBuffer，因此，Netty的ByteBuf接口必须提供API，以方便地将ByteBuf转换成ByteBuffer，或者将ByteBuffer包装成ByteBuf。考虑到性能，应该尽量避免缓冲区的复制，内部实现的时候可以考虑聚合一个ByteBuffer的私有指针来代表ByteBuffer。

6. 调用ByteBuf的read操作时，从readerIndex处开始读取。readerIndex到writerIndex之间的空间为可读的字节缓冲区；从writerIndex到capacity之间为可写的字节缓冲区；0到readerIndex之间是已经读取的缓冲区，可以调用discardReadBytes操作来重用这部分空间，以节约内存，防止ByteBuf的动态扩张。这在私有协议消息解码的时候非常有用，因为TCP底层可能粘包，几百个整包消息被TCP粘包后作为一个整包发送。这样，通过discardReadBytes操作可以重用之前已经解码过的缓冲区，从而防止接收缓冲区因为容量不足导致扩张。但是，discardReadBytes操作是把双刃剑，不能滥用。

7. 需要指出的是，调用discardReadBytes会发生字节数组的内存复制，所以，频繁调用将会导致性能下降。因此在调用它之前要确认你确实需要这样做，例如牺牲性能来换取更多的可用内存。

8. Clear操作：正如JDK ByteBuffer的clear操作，它不会清空缓冲区内容本身，例如填充为NUL(0x00)。它主要操作位置指针，例如position、limit和mark。对于ByteBuf，它也是用来操作readerIndex和wirterIndex，将他们还原为初始分配值。

9. Mark和Rest   
当对缓冲区进行读操作时，由于某种原因，可能需要对之前的操作进行回滚。读操作并不会改变缓冲区的内容，回滚操作主要就是重新设置索引信息。
对于JDK的ByteBuffer，调用mark操作会将当前的位置指针备份到mark变量中，当调用reset操作之后，重新将指针的当前位置恢复为备份在mark中的值、
Netty的ByteBuf也有类似的rest和mark接口，因为ByteBuf有读索引和写索引，因此，它总共有4个相关的方法，分别如下。  
markReaderIndex：将当前的readerIndex备份到markedReaderIndex中；  
resetReaderIndex：将当前的readerIndex设置为markedReaderIndex；  
markWriterIndex：将当前的WriterIndex备份到markedWriterIndex中；  
resetWriterIndex：将当前的writerIndex设置为markedWriterIndex 。 

10. 无论是get还是set操作，ByteBuf都会对其索引和长度等进行合法性校验，与顺序读写一致。但是，set操作与write操作不同的是它不支持动态扩展缓冲区，所以使用者必须保证当前的缓冲区可写的字节数大于需要写入的字节长度，否则会抛出数组或者缓冲区越界异常。

11. ByteBuf的分类  
从内存分配的角度：  
(1)堆内存(HeapByteBuf)字节缓冲区：特点是内存的分配和回收速度快，可以被JVM自动回收；缺点就是如果进行Socket的I/O读写，需要额外做一次内存复制，将堆内存对应的缓冲区复制到内核Channel中，性能会有一定程度的下降。  
(2)直接内存(DirectByteBuf)字节缓冲区：非堆内存，它在堆外进行内存分配，相比于堆内存，它的分配和回收速度会慢一点，但是将它写入或者从Socket Channel中读取时，由于少了一次内存复制，速度比堆快。  
经验表明，ByteBuf的最佳实践时在I/O通信线程的读写缓冲区使用DirectByteBuf，后端业务消息的编解码模块使用HeapByteBuf，这样组合可以达到最佳性能。
从内存回收角度看，ByteBuf也可分为两类：基于对象池的ByteBuf和普通的ByteBuf。两者的主要区别就是基于对象池的ByteBuf可以重用ByteBuf对象，它自己维护了一个内存池，可以循环利用创建的ByteBuf，提升内存的使用效率，降低由于高负载导致的频繁GC。测试表明使用内存池后的Netty在高负载、大并发的冲击下内存和GC更加平稳。

12. UnpooledHeapByteBuf使用byte数组表示字节缓冲区，UnpooledDirectByteBuf直接使用ByteBuffer，它们的功能都是相同的，操作的结果是等价的。

13. Netty的ByteBuf可以动态扩展，为了保证安全，允许使用者指定最大的容量，在容量范围内，可以先分配较小的初始容量，后面不够用再动态扩展，这样可以达到功能和性能的最优组合。  
首先设置门限阈值为4MB，当需要的新容量正好等于门限阈值时，使用阈值作为新的缓冲区容量。如果新申请的内存空间大于阈值，不能采用倍增的方式(防止内存膨胀和浪费)扩张内存，而采用每次步进4MB的方式进行内存扩张。扩张的时候需要对扩大后的内存和最大内存(MaxCapacity)进行比较，如果大于缓冲区的最大长度，则使用maxCapacity作为扩容后的缓冲区容量。  
如果扩容后的新容量小于阈值，则以64为计数进行倍增，直到倍增后的结果大于或等于需要的容量值。  
采用倍增或者步进算法的原因如下：如果以minNewCapacity作为目标容量，则本次扩容后的可写字节数刚好够本次写入使用。写入完成后，它的可写字节数会变为0，下次做写入操作的时候，需要再次动态扩容。这样就会形成第一次动态扩张后，每次写入操作都会进行动态扩张，由于动态扩张需要进行内存复制，频繁的内存复制会导致性能下降。  
采用先倍增后步进的原因如下：当内存比较小的情况下，倍增操作并不会带来太多的内存浪费。但是，当内存增长到一定阈值后，再进行倍增就可能会带来额外的内存浪费。由于每个客户端连接都可能维护自己独立的接收和发送缓冲区，这样伴随客户读的线性增长，内存浪费也会成比例增加，因此，达到某个阈值后就需要以步进的方式对内存进行平滑的扩张。


十六  
1. io.netty.channel.Channel是Netty网络操作抽象类，它聚合了一组功能，包括但不限于网络的读、写，客户端发起连接，主动关闭连接，链路关闭，获取通讯双方的网络地址等。它也包含了Netty框架相关的一些功能，包括获取该Channel的EventLoop，获取缓冲分配器ByteBufAllocator和pipeline等。

2. AbstractNioChannel
public static final int OP_READ = 1 << 0; // 读操作位
public static final int OP_WRITE = 1 << 2; // 写操作位  
public static final int OP_CONNECT = 1 << 3; // 客户端连接服务端操作位  
public static final int OP_ACCEPT = 1 << 4; // 服务端接收客户端连接操作位  
AbstractNioChannel注册的是0，说明对任何事件都不感兴趣，仅仅完成注册操作。注册的时候可以指定附件，后续Channel接收到网络事件通知时可以从SelectionKey中重新获取之前的附件进行处理，此处将AbstractNioChannel的实现子类当作附件注册。如果注册Channel成功，则返回selectionKey，通过selectionKey可以从多路复用器中获取Channel对象。  
doBeginRead():将Selectionkey当前的操作位与读操作位进行按位与操作，如果等于0，说明目前没有设置读操作位，通过interestOps | readInterestOps设置读操作位，最后调用selectionKey的interestOps方法重新设置网络通道的网络操作位，这样就可以监听网络的读事件了。

3. AbstractNioByteChannel
5. NioServerSocketChannel：  
首先通过NioServerSocketChannel的accept接收新的客户端连接，如果SocketChannel不为空，则利用当前的NioServerSocketChannel、EveentLoop和SocketChannel创建新的NioSocketChannel，并将其加入到List<Object>中，最后返回1，表示服务端消息读取成功。  
对于NioServerSocketChannel，它的读取操作就是接收客户端的连接，创建NioServerSocketChannel对象。

十九
1. 当I/O操作完成之后，I/O线程会回调ChannelFuture中的GenericFutureListener的operationComplete的方法，并把ChannelFuture对象当作方法的入参。如果用户需要做上下文相关的操作，需要将上下文信息保存到对应的ChannelFuture中。

2. 推荐通过GenericFutureListener代替ChannelFuture的get等方法的原因是：当我们进行异步I/O操作时，完成的时间是无法预测的，如果不设置超时时间，它会导致调用线程长时间被阻塞，甚至挂死。而设置超时时间，时间又无法准确预测。利用异步通知机制回调GenericFutureListener是最佳的解决方案，它的性能最优。

3. 需要注意的是：不要在ChannelHandler中调用ChannelFutere的await()方法，它会导致死锁。原因是发起I/O操作之后，由I/O线程负责异步通知发起I/O操作的用户线程，如果用户线程和I/O线程是同一个线程，就会导致I/O线程等待自己通知操作完成，这就导致了死锁，这跟经典的两个线程互等待死锁不同，属于自己把自己挂死。

4. 异步I/O操作有两类超时：一个是TCP层面的I/O超时，另一个是业务逻辑层面的操作超时。两者没有必然的联系，但是通常情况下业务逻辑超时应该大于I/O超时时间。它们两者是包含关系。

5. Netty发起I/O操作的时候，会创建一个新的Promise对象，例如调用ChannelHandlerContext的write(Object object)方法时，会创建一个新的ChannelPromise。

6. setSuccess0()  
首先判断当前Promise的操作是否已经被设置，如果已经被设置，则不允许重复设置，返回设置失败。  
由于可能存在I/O线程和用户线程同时操作Promise，所以设置操作结果的时候需要加锁保护，防止并发操作。  
对操作结果是否被设置进行二次判断(为了提升并发性能的二次判断)，如果已经被设置，则返回操作失败。  
对操作结果result进行判断，如果为空，说明仅仅需要notify在等待的业务线程，不包含具体的业务逻辑对象。因此，将result设置为系统默认的SUCCESS。如果操作结果为空，将结果设置为result。  
如果有正在等待异步I/O操作完成的用户线程或者其它系统线程，则调用notifyAll方法唤醒所有正在等待的线程。注意notifyAll和wait方法都必须在同步代码块中使用。

7. 通过同步关键字锁定当前Promise对象，使用循环判断对isDone结果进行判断，进行循环判断的原因是防止线程被意外唤醒导致的功能异常。