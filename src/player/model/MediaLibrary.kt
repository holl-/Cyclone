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
import player.getConfigFile
import distributed.DFile

class MediaLibrary {
    val roots: ObservableList<DFile> = FXCollections.observableArrayList()
    val recentlyUsed: ObservableList<DFile> = FXCollections.observableArrayList()
    var recentlyUsedSize: Int = 10

    /**
     * Index of all isLocal files and directories that are part of the library.
     * Only media files are added, other files are ignored.
     */
    private val localIndex: ObservableList<DFile> = FXCollections.observableArrayList()
    private val indexingService: ExecutorService

    private val savefile: File = getConfigFile("library.txt")


    init {
        indexingService = Executors.newSingleThreadExecutor { r -> Thread(r, "Index Service") }
        roots.addListener{ e: ListChangeListener.Change<out DFile> -> updateIndex() }
        recentlyUsed.addListener {e: ListChangeListener.Change<out DFile> -> removeDuplicates(recentlyUsed, recentlyUsedSize); }
        load(savefile, true)
    }

    @Throws(IOException::class)
    private fun save(file: File) {
        TODO()
    }

    private fun load(file: File, elseLoadDefault: Boolean) {
        if (!file.exists()) {
            if (elseLoadDefault)
                addDefaultRoots()
        } else {
            TODO()
        }
    }

    private fun addDefaultRoots() {
        val music = File(System.getProperty("user.home"), "Music")
        if (music.exists() && music.isDirectory) {
            roots.add(DFile(music))
        }
    }

    fun startSearch(pattern: String): ObservableList<DFile> {
        val set = FXCollections.observableArrayList<DFile>()
        val lowerPattern = pattern.toLowerCase()
        Thread {
            val searchList = localIndex.stream().filter { file -> file.getPath().toLowerCase().contains(lowerPattern) }.collect(Collectors.toList())
            Platform.runLater{ set.addAll(searchList) }
        }.start()
        return set
    }

    fun isIndexed(file: DFile): Boolean {
        return localIndex.contains(file)
    }


    private fun updateIndex() {
        val newIndex = ArrayList<DFile>()
        indexingService.submit {
            for(root in roots) {
                if(root.originatesHere()) {
                    recursiveAdd(root, newIndex)
                }
            }
            localIndex.setAll(newIndex)
        }
    }

    private fun recursiveAdd(file: DFile, list: MutableCollection<DFile>) {
        if (file in list) return
        list.add(file)
        if(file.isDirectory()) {
            file.list().forEach { child -> recursiveAdd(child, list) }
        }
    }

    private fun<T> removeDuplicates(list: ObservableList<T>, maxSize: Int) {
        var result = list.stream().distinct().collect(Collectors.toList())
        if(result.size > maxSize) {
            result = result.subList(0, maxSize)
        }
        if(result.size < list.size) {
            list.setAll(result)
        }
    }

}
