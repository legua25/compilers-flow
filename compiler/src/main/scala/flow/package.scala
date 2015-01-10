import java.io.File

package object flow {

  def error(message: String) =
    throw new CompilerException(message)

  def illegal(message: String) =
    throw new IllegalStateException(message)

  def ??? =
    throw new NotImplementedError("not implemented yet")

  implicit class FileOps(val file: File) extends AnyVal {

    def name = file.getName()

    def path = file.getPath()

    def absolutePath = file.getAbsolutePath()

    def parts: Seq[String] = {
      path.split("/")
    }

    def components: (String, String) = {
      val string = path
      string.lastIndexOf('.') match {
        case -1 => (string, "")
        case i  => (string.substring(0, i), string.substring(i))
      }
    }

    def extension: String = components._2

    def withExtension(extension: String, replace: Boolean = true): File = {
      if (extension.startsWith(".")) {
        if (this.extension == extension) {
          file
        }
        else if (replace) {
          val (prefix, _) = components
          new File(prefix + extension)
        }
        else {
          new File(file.getPath() + extension)
        }
      }
      else {
        withExtension("." + extension, replace)
      }
    }

    def files = file.listFiles().toList

  }

}

package flow {

  class CompilerException(message: String) extends RuntimeException(message)

}
