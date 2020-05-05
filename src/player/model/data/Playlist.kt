package player.model.data

import cloud.DFile
import cloud.SynchronizedData


/**
 * This data object specifies the state of a PlaylistPlayer.
 * It is sufficient to generate a Program.
 */
data class Playlist(val files: List<DFile>, val looping: Boolean, val shuffled: Boolean) : SynchronizedData()

//    @Throws(IllegalArgumentException::class)
//    fun getNext(current: Optional<DFile>, loop: Boolean): Optional<DFile> {
//        if (isEmpty) return Optional.empty()
//        if (!current.isPresent) return first()
//        val index = files.indexOf(current.get())
//        if (index < 0) throw IllegalArgumentException("media ID " + current.get() + " is not contained in playlist.")
//        return if (index < size() - 1) Optional.of(files[index + 1])!! else if (loop) first() else Optional.empty()
//    }
//
//    @Throws(IllegalArgumentException::class)
//    fun getPrevious(mediaID: Optional<DFile>, loop: Boolean): Optional<DFile> {
//        if (isEmpty) return Optional.empty()
//        if (!mediaID.isPresent) return first()
//        val index = files.indexOf(mediaID.get())
//        if (index < 0) throw IllegalArgumentException("media ID " + mediaID.get() + " is not contained in playlist.")
//        return if (index > 0) Optional.of(files[index + -1])!! else if (loop) last() else Optional.empty()
//    }
//
//    fun setAll(files: List<DFile?>, returnIDIndex: Int, shuffle: Boolean, firstStayFirst: Boolean): DFile? {
//        _clear()
//        var returnID: DFile? = null
//        if (!files.isEmpty()) {
//            this.files.clear()
//            this.files.addAll(files)
//            returnID = if (returnIDIndex >= 0) this.files[returnIDIndex] else this.files[0]
//            if (shuffle) {
//                if (firstStayFirst) _shuffle(Optional.of(returnID!!)) else _shuffle(Optional.empty())
//            }
//            returnID = this.files[0]
//        }
//        fireChangedLocally()
//        return returnID
//    }
//
//    fun addAll(files: List<DFile?>, returnIDIndex: Int, shuffle: Boolean, shuffleToFirst: Optional<DFile>): DFile? {
//        this.files.addAll(files)
//        val returnID = if (returnIDIndex >= 0) files[returnIDIndex] else null
//        if (shuffle) _shuffle(shuffleToFirst)
//        fireChangedLocally()
//        return returnID
//    }
//
//    fun shuffle(makeFirst: Optional<DFile>) {
//        _shuffle(makeFirst)
//        fireChangedLocally()
//    }
//
//    private fun _shuffle(makeFirst: Optional<DFile>) {
//        Collections.shuffle(files)
//        makeFirst.ifPresent { first: DFile? -> if (files.remove(first)) files.add(0, first) else throw IllegalArgumentException("makeFirst is not contained in playlist") }
//    }
//
//    fun add(file: DFile?) {
//        files.add(file)
//        fireChangedLocally()
//    }
//
//    private fun _clear() {
//        files.clear()
//    }
//
//    fun clear() {
//        _clear()
//        fireChangedLocally()
//    }
