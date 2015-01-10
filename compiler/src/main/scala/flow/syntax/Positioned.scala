package flow.syntax

// TODO: actually use this
trait Positioned {

  var position: Position = NoPosition

  def positioned(position: Position): this.type = {
    this.position = position
    this
  }

  def positioned(line: Int, column: Int): this.type =
    positioned(SourcePosition(line, column))

}

sealed trait Position {

  def line: Int

  def column: Int

}

case object NoPosition extends Position {

  def line = 0

  def column = 0

  override def toString = "unknown position"

}

case class SourcePosition(line: Int, column: Int) extends Position {

  override def toString = s"line $line:$column"

}
