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
异步连接返回之后，需要判断连接结果，如果连接成功，则触发ChannelActive事件，最终会将NioSocketChannel中selectionKey设置为OP_READ(unsafe.beginRead)，用于监听网络读操作。  
如果没有立即连接上服务器，则注册OP_CONNETCT到多路复用器；如果连接过程发生异常，则关闭链路，进入连接失败处理流程。

3. NioEvertLoop的Selector轮询客户端连接Channel，当服务端返回握手应答之后，对连接结果进行判断；doConnect用于判断JDK的SocketChannel的连接结果，如股票返回true表示连接成功。其它值或者发生异常表示连接失败；连接成功之后，调用fullfillConnectPromise方法，触发链路激活事件，该事件由ChannelPipeline进行传播(pipeline.fireChannelActive -> 用于修改监听网络监听位为读操作)

4. 客户端连接超时机制：用户在创建Netty客户端的时候，可以通过ChannelOption.CONNECT_TIMEOUT_MILLIS配置项设置超时时间，发起连接的同时，启动连接超时检测定时器，一旦超时定时器执行，说明客户端连接超时，构造连接超时异常，将异常结果设置到connectPromise中，同时关闭客户端连接。释放句柄；如果在连接超时之前获取到连接结果，则删除连接超时定时器，防止其被触发。无论连接是否成功，只要获取到连接结果，之后就删除连接超时定时器。


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

4. NioServerSocketChannel：  
首先通过NioServerSocketChannel的accept接收新的客户端连接，如果SocketChannel不为空，则利用当前的NioServerSocketChannel、EveentLoop和SocketChannel创建新的NioSocketChannel，并将其加入到List\<Object\>中，最后返回1，表示服务端消息读取成功。对于NioServerSocketChannel，它的读取操作就是接收客户端的连接，创建NioServerSocketChannel对象。

5. 就像循环读一样，我们需要对一次Selector轮询的写操作次数进行上限控制，因为如果TCP的发送缓冲区满，TCP处于KEEP-ALIVE状态，消息会无法发送出去，如果不对上限进行控制，就会长时间处于发送状态，Reactor线程无法及时读取其它消息和执行排队的Task。所以，我们必须对循环次数上限做控制。对写入的字节进行判断，如果为0，说明TCP发送缓冲区已满，很有可能无法再写进去，因此从循环中跳出，同时将写半包标识设置为true，用于向多路复用器注册写操作位，告诉多路复用器有没有发送完的半包消息，需要轮询出就绪的SocketChannel继续发送。

6. AbstractUnsafe：  
6.1 register：首先判断当前所在的线程是否是Channel对应的NioEventLoop线程，如果是同一个线程，则不存在多线程并发操作问题，直接调用register0进行注册；如果是由用户线程或者其它线程发起的注册操作，则将注册操作封装成Runnable，放到NioEventLoop任务队列中执行。  
6.2 bind：bind方法主要用于绑定指定的端口，对于服务端，用于绑定监听端口，可以设置backlog参数，对于客户端，主要用于指定客户端Channel的本地Socket地址。  
6.3 close：在链路关闭之前首先需要判定是否处于刷新状态，如果处于刷新状态说明还有消息尚未发送出去，需要等到所有消息发送完成再关闭链路，因此，将关闭操作封装成Runnable稍后再执行。如果链路没有处于刷新状态，需要从closeFuture中判断关闭操作是否完成，如果已经完成，不需要重复关闭链路，设置ChannelPromise的操作结果为成功并返回。执行关闭操作，将消息发送缓冲数组设置为空，通知JVM进行内存回收。调用抽象的doClose关闭链路。如果关闭操作成功，设置ChannelPromise结果为成功。如果操作失败，则设置异常对象到ChannelPromise中。调用ChannelOutboundBuffer的close方法释放缓冲区的消息，随后构造链路关闭通知Runnable放到NioEventLoop中执行。最后，调用deregister方法，将Channel从多路复用器上取消注册。  
6.4 write：wirte方法实际上将消息添加到环形发送数组中，并不是真正的写Channel。  
6.5 flush：负责发送缓冲区中待发送的消息全部写入到Channel中，并发送给通信对方。（拿，写，清除半包）

十七  
1. ChannelPipeline是ChannelHandler的容器，它负责ChannelHandler的管理和事件拦截与调度。

2. 事实上，用户不需要自己创建pipeline，因为使用ServerBootstrap或者bootstrap启动服务器或者客户端时，Netty会为每个Channel连接创建一个独立的pipeline。

3. ChannelPipeline支持动态运行动态添加或者动态添加ChannelHandler，在某些场景下这个特性非常实用。例如业务高峰期需要对系统做拥塞保护时，就可以根据当前的系统时间进行判断，如果处于业务高峰期，则动态地将系统拥塞保护Channelhandler添加到当前的ChannelPipeline中，当高峰期过去之后，就可以动态删除拥塞保护的ChannelHandler了。

4. ChannelPipeline是线程安全的，这意味着N个业务线程可以并发地操作ChannelPipeline而不存在多线程并发问题。但是，ChannelHandler而不是线程安全的，这意味着尽管CHannelPipeline是线程安全的，但是用户仍然需要自己保证ChannelHandler的线程安全。

十八  
1. Reactor多线程模型的特点如下：  
1.1 有专门的一个NIO线程——Acceptor线程用于监听服务端，接收客户端TCP连接请求；  
1.2 网络I/O操作——读、写由一个NIO线程池负责，线程池可以采用标准的JDK线程池实现，它包含一个任务队列和N个可用的线程，由这些NIO线程负责消息的读取、解码、编码和发送；
1.3 一个NIO线程可以同时处理N条链路，但是一个链路只能对应NIO线程，防止发生并发操作问题；

2. 主从Reactor线程模型的特点是：服务端用于接收客户端连接的不再是一个单独的NIO线程，而是一个独立的NIO线程池。Acceptor接收到客户端TCP连接请求并处理完成后(可能包含接入认证等)，将新创建的SocketChannel注册到I/O线程池（sub reactor线程池）的某个I/O线程上，由它负责SocketChannel的读写和编解码工作。Acceptor线程池仅仅用于客户端的登录、握手、和安全认证，一旦链路建立成功，就将链路注册到后端subReactor线程池的I/O线程上，由I/O线程负责后续的I/O操作。利用主从NIO线程模型，可以解决一个服务端监听线程无法有效处理所有客户端连接的性能不足问题。

3. Netty用于接收客户端请求的线程池职责如下：  
3.1 接收客户端TCP连接，初始化Channel参数；  
3.2 将链路状态变更事件通知给ChannelPipeline。

4. Netty处理I/O操作的Reactor线程池职责如下：  
4.1 异步读取通信对端的数据报，发送读事件到ChannelPipeline；  
4.2 异步发送消息到通信对端，调用ChannelPipeline的消息发送接口；  
4.3 执行系统调用Task；  
4.4 执行定时任务Task，例如链路空闲状态检测定时任务。

5. 所有的逻辑都是在for循环体内进行（io.netty.channel.nio.NioEventLoop.run），只有当NioEventLoop接收到退出指令的时候才会退出循环，否则一直执行下去。  

6. 通过hasTasks()方法判断当前的消息队列中是否有消息尚未处理，如果有则调用selectNow()方法立即进行一次select操作，看看是否有准备就绪的Channel需要处理。Selector的selectNow()方法会立即触发Selector的选择操作，如果有准备就绪的Channel，则返回就绪Channel的集合，否则返回0。如果消息队列中没有消息需要处理，则执行select()方法，由Selector多路复用器轮询，看是否有准备就绪的Channel；获取系统当前的纳秒时间，调用delayNanos方法计算NioEventLoop中定时任务的触发时间；计算下一个将要触发的定时任务的剩余时间，将它转换成毫秒，将超时时间增加0.5毫秒的调整值。对剩余的超时时间进行判断，如果需要立即执行或者已经超时，则调用selector.selectNow()进行轮询操作，将selectCnt设置为1，并退出当前循环。将定时任务剩余的超时时间作为参数进行select操作，每完成一次select操作，对select计数器selectCnt加1。

7. select操作完成后，需要对结果进行判断，如果存在下列任意一种情况，则退出当前循环：  
7.1 有Channel处于就绪状态，selectedkeys不为0，说明有读写事件需要处理；  
7.2 oldWakeUp为true；  
7.3 系统或用户调用了wakeup操作；  
7.4 消息队列中有新的任务需要处理。  

8. JDK空轮询的Bug修复策略：  
8.1 对Selector的select操作洲际进行统计；  
8.2 没完成一次空的select操作进行一次计数；  
8.3 在某个周期（例如100ms）内如果连续发生N次空轮询，说明触发了JDK NIO的epoll死循环bug。

9. 检测到Selector处于死循环后，需要通过重建Selector的方式让系统恢复正常。调用OpenSelector方法创建并发开新的Selector，通过循环，将原Selector上注册的SocketChannel从旧的Selector上去注册，重新注册到新的Selector上，并将老的Selector关闭。通过销毁旧的、有问题的多路复用器，使用新建的Selector，就可以解决空轮询Selector导致的I/O线程CPU占用100%的问题。

10. 由于NioEventLoop需要同时处理I/O事件和非I/O事件，为了保证两者都能够得到足够的CPU事件被执行，Netty提供了I/O比例供用户定制。如果I/O操作多于定时任务和Task，则可以将I/O比例调大，反之则调小，默认为50%。

11. 从定时任务消息队列中弹出消息进行处理，，如果消息队列为空，则退出循环。根据当前的事件戳进行判断，如果该定时任务已经或处于超时状态，则将其加入到执行Task Queue中，同时从延时队列中删除。定时任务如果没有超时，说明本轮循环不需要处理，直接退出即可。执行Task Queu中原有的任务和从延时队列中复制的已经超时或者正在处于超时状态的定时任务。最后判断系统是否进行优雅停机状态，如果处于关闭状态，则需要调用closeAll方法，释放资源，并让NioEventLoop线程退出循环，结束运行。遍历获取所有的Channel，调用它的Unsage.close()方法关闭所有链路，释放线程池、ChannelPipeline和ChannelHandler等资源。

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