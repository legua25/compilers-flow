package flow

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import scala.sys.process._
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import flow.{ syntax => ast }
import flow.utils._
import org.antlr.v4.runtime.tree.gui.TreeViewer
import java.util.Arrays

object Flow extends Compiler {

  val name = "flowc"
  val version = "0.1"
  val extension = ".flow"
  val libraryFolder = new File("../built-in")

  case class Config(
    run: Boolean = false,
    verbose: Boolean = false,
    debug: Boolean = false,
    tree: Boolean = false,
    outputFile: Option[File] = None,
    inputFile: Option[File] = None,
    sourceFile: File = null)

  val argumentParser = new scopt.OptionParser[Config](name) {
    head(s"${Flow.name} ${Flow.version}")
    opt[Unit]('r', "run")
      .action { (_, c) => c.copy(run = true) }
      .text("run complied program")
    opt[Unit]('v', "verbose")
      .action { (_, c) => c.copy(verbose = true) }
    opt[Unit]('d', "debug")
      .action { (_, c) => c.copy(debug = true) }
      .text("print debug messages")
    opt[Unit]('t', "tree")
      .action { (_, c) => c.copy(tree = true) }
      .text("show parse tree")
    opt[File]('o', "output")
      .action { (f, c) => c.copy(outputFile = Some(f)) }
      .text("compiled output")
    opt[File]('i', "input")
      .action { (f, c) => c.copy(inputFile = Some(f)) }
      .text("input to program when run")
    arg[File]("source")
      .action { (f, c) => c.copy(sourceFile = f) }
      .text("source file to compile")
  }

  val libraryFiles = {
    libraryFolder
      .files
      .filter(_.extension == extension)
  }

  def main(args: Array[String]): Unit = {
    argumentParser.parse(args, Config()) match {
      case Some(Config(run, verbose, debug, tree, outputFileOpt, inputFile, sourceFile)) =>
        flow.debugMode = debug

        try {
          val outputFile = outputFileOpt.getOrElse(sourceFile.withExtension(".ll"))

          val libraries = libraryFiles.map(f => programFrom(f, verbose))
          val program = programFrom(sourceFile, verbose, tree)

          if (verbose)
            println(s"Compiling ${sourceFile.name}.")

          val module = compile(sourceFile.name, program, libraries)
          writeModuleTo(outputFile, module, verbose)

          if (run) {
            inputFile match {
              case Some(file) =>
                s"./run.sh $outputFile < $file".!
              case None =>
                s"./run.sh $outputFile".!
            }
          }
        }
        catch {
          case e: CompilerException if !debug =>
            println(e.getMessage())
            sys.exit(1)
        }
      case None =>
    }
  }

  def programFrom(file: File, verbose: Boolean, showTree: Boolean = false) = {
    val input = new FileInputStream(file)

    if (verbose)
      println(s"Parsing $file.")

    val antlrInput = new ANTLRInputStream(input)
    val lexer = new ast.FlowLexer(antlrInput)
    val tokens = new CommonTokenStream(lexer)
    val parser = new ast.FlowParser(tokens)
    val tree = parser.program()

    if (showTree)
      new TreeViewer(Arrays.asList(parser.getRuleNames(): _*), tree).open()

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
