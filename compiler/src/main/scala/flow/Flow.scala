package flow

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter

import scala.sys.process._

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

import flow.{ syntax => ast }

object Flow extends Compiler {

  val name = "flowc"
  val version = "0.1"
  val extension = ".flow"
  val libraryFolder = new File("../built-in")

  case class Config(
    run: Boolean = false,
    verbose: Boolean = true,
    outputFile: Option[File] = None,
    inputFile: File = null)

  val argumentParser = new scopt.OptionParser[Config](name) {
    head(s"${Flow.name} ${Flow.version}")
    opt[Unit]('r', "run")
      .action { (_, c) => c.copy(run = true) }
      .text("run complied program")
    opt[Unit]('v', "verbose")
      .action { (_, c) => c.copy(verbose = true) }
    opt[File]("output")
      .action { (f, c) => c.copy(outputFile = Some(f)) }
    arg[File]("source")
      .action { (f, c) => c.copy(inputFile = f) }
      .text("source to compile")
  }

  val libraryFiles = {
    libraryFolder
      .files
      .filter(_.extension == extension)
  }

  def main(args: Array[String]): Unit = {
    argumentParser.parse(args, Config()) match {
      case Some(Config(run, verbose, outputFileOpt, inputFile)) =>
        val outputFile = outputFileOpt.getOrElse(inputFile.withExtension(".ll"))

        val libraries = libraryFiles.map(f => programFrom(f, verbose))
        val program = programFrom(inputFile, verbose)

        if (verbose)
          println(s"Compiling ${inputFile.name}.")

        val module = compile(inputFile.name, program, libraries)
        writeModuleTo(outputFile, module, verbose)

        if (run)
          s"./optnrun.sh $outputFile".!
      case None =>
    }
  }

  def programFrom(file: File, verbose: Boolean = false) = {
    val input = new FileInputStream(file)

    if (verbose)
      println(s"Parsing $file.")

    val antlrInput = new ANTLRInputStream(input)
    val lexer = new ast.FlowLexer(antlrInput)
    val tokens = new CommonTokenStream(lexer)
    val parser = new ast.FlowParser(tokens)
    val tree = parser.program()

    val program = new ast.AstVisitor().visit(tree).asInstanceOf[ast.Program]

    input.close()

    program
  }

  def writeModuleTo(file: File, module: llvm.Module, verbose: Boolean = false) = {
    val output = new FileOutputStream(file)
    val printer = new PrintWriter(output)

    if (verbose)
      println(s"Writing $file.")

    printer.println(module.llvm)
    printer.close()
    output.close()
  }

}
