package player.model

import javafx.application.Platform
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import cloud.CloudFile

class MediaLibrary {
    val roots: ObservableList<CloudFile> = FXCollections.observableArrayList()
    val recentlyUsed: ObservableList<CloudFile> = FXCollections.observableArrayList()
    var recentlyUsedSize: Int = 10

    /**
     * Index of all isLocal files and directories that are part of the library.
     * Only media files are added, other files are ignored.
     */
    private val localIndex: ObservableList<CloudFile> = FXCollections.observableArrayList()
    private val indexingService: ExecutorService


    init {
        indexingService = Executors.newSingleThreadExecutor { r -> Thread(r, "Index Service") }
        roots.addListener{ e: ListChangeListener.Change<out CloudFile> -> updateIndex() }
        recentlyUsed.addListener {e: ListChangeListener.Change<out CloudFile> -> removeDuplicates(recentlyUsed, recentlyUsedSize); }
    }

    @Throws(IOException::class)
    private fun save(file: File) {
        TODO()
    }

    fun startSearch(pattern: String): ObservableList<CloudFile> {
        val set = FXCollections.observableArrayList<CloudFile>()
        val lowerPattern = pattern.toLowerCase()
        Thread {
            val searchList = localIndex.stream().filter { file -> file.getPath().toLowerCase().contains(lowerPattern) }.collect(Collectors.toList())
            Platform.runLater{ set.addAll(searchList) }
        }.start()
        return set
    }

    fun isIndexed(file: CloudFile): Boolean {
        return localIndex.contains(file)
    }


    private fun updateIndex() {
        val newIndex = ArrayList<CloudFile>()
        indexingService.submit {
            for(root in roots) {
                if(root.originatesHere()) {
                    recursiveAdd(root, newIndex)
                }
            }
            localIndex.setAll(newIndex)
        }
    }

    private fun recursiveAdd(file: CloudFile, list: MutableCollection<CloudFile>) {
        if (file in list) return
        if(file.isDirectory() or AudioFiles.isAudioFile(file.getName()))
            list.add(file)
        if(file.isDirectory()) {
            File(file.getPath()).listFiles()!!.forEach { child -> recursiveAdd(CloudFile(child), list) }
        }
    }

    private fun<T> removeDuplicates(list: ObservableList<T>, maxSize: Int?) {
        var result = list.stream().distinct().collect(Collectors.toList())
        if(maxSize != null && result.size > maxSize) {
            result = result.subList(0, maxSize)
        }
        if(result.size < list.size) {
            list.setAll(result)
        }
    }

}
