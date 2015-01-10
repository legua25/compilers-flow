package flow

import java.io.File

package object utils {

  implicit def fileToFileOps(file: File): FileOps =
    new FileOps(file)

}
