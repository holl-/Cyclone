package cloud

import javafx.application.Platform
import javafx.collections.FXCollections
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


internal class CloudMulticast(val cloud: Cloud, val host: String, val port: Int, val tcpPort: Int, val tcp: CloudTCP, val logger: Logger?) {
    val socket = MulticastSocket(port)
    val address = InetSocketAddress(InetAddress.getByName(host), port)
    val peers = ArrayList<Peer>(listOf(cloud.peer))
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
                    println(otherTime)
                    println(connectionTime)
                    peers.add(peer)
                    Platform.runLater { cloud.peers.add(peer) }
                    logger?.info("<- Received ping from $name ($id). ${if (isOlder) "It is older." else "I am older"}")
                    if (isOlder) {
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

    fun startPinging(delayMillis: Long = 1000) {
        val helloMessage = "Cyclone;$tcpPort;${cloud.peer.id};${cloud.peer.name};$connectionTime"
        pingService = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(Runnable { send(helloMessage) }, 0, delayMillis, TimeUnit.MILLISECONDS)
    }


    fun disconnect() {
        pingService?.cancel(false)
        receiveService?.cancel(true)
        socket.leaveGroup(address, null)
        socket.close()
    }
}


internal class CloudTCP(val cloud: Cloud, localPort: Int, val logger: Logger?) {
    val serverSocket = ServerSocket(localPort)
    val connections = FXCollections.observableArrayList<CloudTCPConnection>()

    var acceptService: Future<*>? = null

    fun synchronizedUpdated(data: SynchronizedData) {
        for (connection in connections) {
            connection.sendSyncUpdate(data)
        }
    }

    fun acceptSingleSocket() {
        val socket = serverSocket.accept()
        val connection = CloudTCPConnection(socket, cloud, logger)
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
        val connection = CloudTCPConnection(socket, cloud, logger)
        initConnection(connection)
    }

    private fun initConnection(connection: CloudTCPConnection) {
        connections.add(connection)
        connection.sendEverything()
        connection.startHandlingInput()
    }
}


internal class CloudTCPConnection(val socket: Socket, val cloud: Cloud, val logger: Logger?) {
    val outputStream = ObjectOutputStream(socket.getOutputStream())
    val inputStream = ObjectInputStream(socket.getInputStream())
    val peer: Peer
    val senderThread = Executors.newFixedThreadPool(1)

    var sharedSData: List<SynchronizedData>? = null

    init {
        outputStream.writeUTF(cloud.peer.id)
        outputStream.writeUTF(cloud.peer.name)
        outputStream.flush()

        val id = inputStream.readUTF()
        val name = inputStream.readUTF()
        peer = Peer(false, name, socket.inetAddress.hostAddress, id)
        peer.socketAddress = InetSocketAddress(socket.inetAddress, socket.port)

        logger?.info("Pleasantries exchanged with $peer")
    }

    fun sendEverything() {
        senderThread.submit(Runnable {
            logger?.fine("Sending all data to $peer")
            outputStream.writeUTF("sData")
            val data = cloud.sData.map { (_, prop) -> prop.value }
            outputStream.writeObject(data)
            outputStream.flush()
        })
    }

    fun sendSyncUpdate(data: SynchronizedData) {
        senderThread.submit(Runnable {
            logger?.fine("Sending synchronized to $peer: $data")
            outputStream.writeUTF("s")
            outputStream.writeObject(data)
            outputStream.flush()
        })
    }

    fun handleSingleInput() {
        val objType = inputStream.readUTF()
        if (objType == "sData") {
            try {
                val data = inputStream.readObject() as List<*>
                logger?.fine("Received synchronized data from $peer. Total objects: ${data.size}")
                for (value in data) {
                    cloud.pushSynchronizedImpl(value as SynchronizedData, false)
                }
            } catch (exc: Exception) {
                logger?.warning("Failed to receive synchronized data from $peer: $exc")
            }
        }else if (objType == "s") {
            try {
                val data = inputStream.readObject() as SynchronizedData
                logger?.fine("Received synchronized data from $peer: $data")
                cloud.pushSynchronizedImpl(data, false)
            } catch (exc: Exception) {
                logger?.warning("Failed to receive synchronized data from $peer: $exc")
            }
        } else {
            logger?.warning("Received unknown input from $peer: $objType")
        }
    }

    fun startHandlingInput() {
        Executors.newFixedThreadPool(1).submit(Runnable {
            while (true) handleSingleInput()
//            logger?.info("Input pipe from $peer going offline.")
        })
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