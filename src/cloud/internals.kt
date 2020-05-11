package cloud

import javafx.application.Platform
import javafx.collections.FXCollections
import java.io.*
import java.lang.Exception
import java.net.*
import java.util.concurrent.*
import java.util.logging.Logger


internal class CloudMulticast(val cloud: Cloud, val host: String, val port: Int, val tcpPort: Int, val tcp: CloudTCP, val logger: Logger?, val autoConnect: Boolean, val broadcastInterval: Long) {
    val socket = MulticastSocket(port)
    val address = InetSocketAddress(InetAddress.getByName(host), port)
    val peers = ArrayList<Peer>(listOf(cloud.localPeer))
    private val maxPacketLength = 1024
    private val receivedPacket = DatagramPacket(ByteArray(maxPacketLength), maxPacketLength)
    private val connectionTime = System.nanoTime()

    var pingService: ScheduledFuture<*>? = null
    var receiveService: Future<*>? = null


    init {
        logger?.info("Joining multicast group $address on port $port (local port $port)")
        socket.joinGroup(address, null)
        logger?.info("Multicast is now connected")
    }


    private fun receiveSinglePacket() {
        while (true) {
            socket.receive(receivedPacket)  // also receives own messages
            val content = String(receivedPacket.data!!, 0, receivedPacket.length)
            if (content.startsWith("Cyclone;")) {
                val parts = content.split(";")
                val id = parts[2]
                val name = parts[3]
                if (peers.none { p -> p.id == id }) {
                    val tcpPort = parts[1].toInt()
                    val peer = Peer(false, name, receivedPacket.address.hostAddress, id)
                    val otherTime = parts[4].toLong()
                    val isOlder = otherTime < connectionTime
                    peers.add(peer)
                    Platform.runLater { cloud.peers.add(peer) }
                    logger?.info("<- Received ping from $name ($id). ${if (isOlder) "It is older." else "I am older"}")
                    if (isOlder && autoConnect) {
                        tcp.connect(peer, InetSocketAddress(receivedPacket.address, tcpPort))
                    }
                } else {
                    logger?.finest("<- Ignoring ping from $name ($id) which is already registered.")
                }
            } else {  // Unknown message
                logger?.finest("<- Received unknown message from ${receivedPacket.address}:${receivedPacket.port} with length ${receivedPacket.length}. Content:")
                logger?.finest(content)
            }
        }
    }

    fun startReceiving() {
        receiveService = Executors.newFixedThreadPool(1).submit(Runnable {
            while (true) {
                receiveSinglePacket()
            }
        })
    }

    private fun send(string: String) {
        logger?.finest("-> Sending ping")
        val buffer = string.toByteArray()
        val packet = DatagramPacket(buffer, buffer.size, address)
        socket.send(packet)
    }

    fun startPinging() {
        val helloMessage = "Cyclone;$tcpPort;${cloud.localPeer.id};${cloud.localPeer.name};$connectionTime"
        pingService = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(Runnable { send(helloMessage) }, 0, broadcastInterval, TimeUnit.MILLISECONDS)
    }


    fun disconnect() {
        pingService?.cancel(false)
        receiveService?.cancel(true)
        socket.leaveGroup(address, null)
        socket.close()
        cloud.peers.setAll(cloud.localPeer)
    }
}


internal class CloudTCP(val cloud: Cloud, val logger: Logger?) {
    val serverSocket = ServerSocket(0)
    val connections = FXCollections.observableArrayList<CloudTCPConnection>()

    var acceptService: Future<*>? = null

    private val connectionTime = System.nanoTime()

    fun synchronizedUpdated(data: SynchronizedData) {
        for (connection in connections) {
            connection.sendSyncUpdate(data)
        }
    }

    fun dataUpdated(localData: ArrayList<Data>, affectedClasses: Collection<Class<out Data>>) {
        for (connection in connections) {
            connection.sendUpdate(localData, affectedClasses)
        }
    }

    fun acceptSingleSocket() {
        val socket = serverSocket.accept()
        val connection = CloudTCPConnection(socket, cloud, logger, connectionTime)
        if (connections.any { c -> c.peer == connection.peer }) {
            // already connected via TCP
            logger?.info("Refusing TCP connection from ${socket.inetAddress.hostAddress} because peer is already connected.")
            socket.close()
        } else {
            logger?.info("Accepting TCP connection from ${socket.inetAddress.hostAddress}")
            initConnection(connection)
        }
    }

    fun startAccepting() {
        acceptService = Executors.newFixedThreadPool(1).submit(Runnable {
            while (true) {
                acceptSingleSocket()
            }
        })
    }

    fun connect(peer: Peer, socketAddress: InetSocketAddress) {
        if (connections.any { c -> c.peer == peer }) return
        logger?.info("Opening TCP connection to ${peer.name} / ${socketAddress.address.hostAddress}")
        val socket = Socket(socketAddress.address, socketAddress.port)
        val connection = CloudTCPConnection(socket, cloud, logger, connectionTime)
        initConnection(connection)
    }

    private fun initConnection(connection: CloudTCPConnection) {
        connections.add(connection)
        connection.sendEverything()
        connection.startHandlingInput()
        cloud.fireUpdate()
    }

    fun disconnect() {
        serverSocket.close()
        for (connection in connections) {
            connection.close()
        }
        connections.clear()
    }
}


internal class CloudTCPConnection(val socket: Socket, val cloud: Cloud, val logger: Logger?, localConnectionTime: Long) {
    val outputStream = ObjectOutputStream(socket.getOutputStream())
    val inputStream = ObjectInputStream(socket.getInputStream())
    val peer: Peer
    val peerConnectionTime: Long
    val isPeerOlder: Boolean

    var inputService: Future<*>? = null
    val senderThread = Executors.newFixedThreadPool(1)

    var sharedSData: List<SynchronizedData>? = null

    init {
        outputStream.writeUTF(cloud.localPeer.id)
        outputStream.writeUTF(cloud.localPeer.name)
        outputStream.writeLong(localConnectionTime)
        outputStream.flush()

        val id = inputStream.readUTF()
        val name = inputStream.readUTF()
        peerConnectionTime = inputStream.readLong()
        peer = Peer(false, name, socket.inetAddress.hostAddress, id)
        peer.socketAddress = InetSocketAddress(socket.inetAddress, socket.port)
        isPeerOlder = peerConnectionTime < localConnectionTime

        logger?.info("Pleasantries exchanged with $peer")
    }

    fun sendEverything() {
        val sData = cloud.getSynchronizedData()
        val data = cloud.getLocalData()
        if(senderThread.isShutdown) logger?.warning("Cannot send all data to $peer because thread is shut down")
        senderThread.submit {
            logger?.fine("Sending all data to $peer: ${sData.size} synchronized, ${data.size} owned.")
            outputStream.writeUTF("all")
            outputStream.writeObject(sData)
            outputStream.writeObject(data)
            outputStream.flush()
        }
    }

    fun sendSyncUpdate(data: SynchronizedData) {
        senderThread.submit(Runnable {
            logger?.fine("Sending synchronized to $peer: $data")
            outputStream.writeUTF("s")
            outputStream.writeObject(data)
            outputStream.flush()
        })
    }

    fun sendUpdate(localData: List<Data>, affectedClasses: Collection<Class<out Data>>) {
        val copiedData = ArrayList(localData)
        val classNames = ArrayList(affectedClasses)
        senderThread.submit {
            logger?.fine("Sending data of class $affectedClasses to $peer: $copiedData")
            outputStream.writeUTF("d")
            outputStream.writeObject(classNames)
            outputStream.writeObject(copiedData)
            outputStream.flush()
        }
    }

    fun openFileStream(path: String, fileSize: Long): InputStream {
        val receiver = ServerSocket(0)
        senderThread.submit(Runnable {
            logger?.info("Sending file request to $peer: $path")
            outputStream.writeUTF("f")
            outputStream.writeUTF(path)
            outputStream.writeInt(receiver.localPort)
            outputStream.flush()
        })
        val remote = receiver.accept()
        val stream = remote.getInputStream()
        val buffer = ByteArrayOutputStream(fileSize.toInt())
        stream.transferTo(buffer)
        return ByteArrayInputStream(buffer.toByteArray())
    }

    fun handleSingleInput() {
        val objType = inputStream.readUTF()
        if (objType == "all") {
            try {
                val sData = inputStream.readObject() as List<*>
                val data = inputStream.readObject() as List<*>
                logger?.fine("Received data from $peer: ${sData.size} synchronized, ${data.size} owned.")
                // Update data
                val classes = HashSet<Any>()
                for (obj in data) {
                    classes.add(obj!!.javaClass)
                }
                cloud.remoteUpdate(peer, classes, data)
                // Update synchronized
                for (sObj in sData) {
                    cloud.remoteUpdateSynchronized(sObj as SynchronizedData, false, isPeerOlder, logger)
                }
            } catch (exc: ClassNotFoundException) {
                logger?.warning("Failed to receive synchronized data from $peer: $exc")
            }
        } else if (objType == "s") {  // synchronized data update
            try {
                val data = inputStream.readObject() as SynchronizedData
                logger?.fine("Received synchronized data from $peer: $data")
                cloud.remoteUpdateSynchronized(data, true, false, logger)
            } catch (exc: ClassNotFoundException) {
                logger?.warning("Failed to receive synchronized data from $peer: $exc")
            }
        } else if (objType == "d") {  // owned data update
            try {
                val affectedClasses = inputStream.readObject() as List<*>
                val data = inputStream.readObject() as List<*>
                logger?.fine("Received $affectedClasses update from $peer: $data")
                cloud.remoteUpdate(peer, affectedClasses, data)
            } catch (exc: ClassNotFoundException) {
                logger?.warning("Failed to receive synchronized data from $peer: $exc")
            }
        } else if (objType == "f") {  // file streaming request
            val path = inputStream.readUTF()
            val remotePort = inputStream.readInt()
            logger?.info("Received streaming request by $peer for file $path")
            // TODO check access rights
            Thread(Runnable {
                val fileStream = FileInputStream(path)
                val fileSocket = Socket(socket.inetAddress, remotePort)
                fileStream.use {
                    fileStream.transferTo(fileSocket.getOutputStream())
                }
                fileSocket.getOutputStream().flush()
                fileSocket.getOutputStream().close()
            }).start()
        } else {
            logger?.warning("Received unknown input from $peer: $objType")
        }
    }

    fun startHandlingInput() {
        inputService = Executors.newFixedThreadPool(1, ThreadFactory { r -> DeserializerThread(cloud, peer, r) }).submit(Runnable {
            while (!socket.isClosed){
                try{
                    handleSingleInput()
                } catch(exc: IOException) {
                    if (socket.isClosed) {
                        logger?.info("Connection to $peer terminated.")
                    } else if (exc is EOFException){
                        logger?.warning("Connection to $peer was closed remotely.")
                        socket.close()
                    } else {
                        logger?.warning("I/O error on connection with $peer: $exc")
                        exc.printStackTrace()
                    }
                } catch (exc: Exception) {
                    exc.printStackTrace()
                    logger?.warning("Error during input analysis from $peer: $exc. Connection may be corrupted.")
                }
            }
            cloud.peerDisconnected(peer)
            cloud.tcp?.connections?.remove(this)
        })
    }

    fun close() {
        socket.close()
        senderThread.shutdown()
        inputService?.cancel(true)
    }
}



//fun main(args: Array<String>) {
//    val c = CloudMulticast()
//    c.startPinging()

//    DeserializerThread(cloud, Peer.getLocal(), Runnable {
//        val original = CloudFile(File("C:\\song.mp3"))
//        val buffer = ByteArrayOutputStream(1024)
//        ObjectOutputStream(buffer).writeObject(original)
//        println(buffer.size())  // actual size of object (119 for peer)
//        val recovered = ObjectInputStream(ByteArrayInputStream(buffer.toByteArray())).readObject() as CloudFile
//        println(recovered)
//        println(recovered.originatesHere())
//    }).start()
//}


class DeserializerThread(val cloud: Cloud, val fromPeer: Peer, target: Runnable) : Thread(target)