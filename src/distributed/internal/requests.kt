package distributed.internal

import distributed.DFile
import java.io.Serializable

class ListDirRequest(val dir: DFile) : Serializable

