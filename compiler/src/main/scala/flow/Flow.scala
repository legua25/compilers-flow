package flow

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Arrays

import scala.sys.process._

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.gui.TreeViewer

import ast._

object Flow extends Compiler {

  def namePrefixOf(fileName: String) = {
    fileName.lastIndexOf('.') match {
      case -1 => fileName
      case i  => fileName.substring(0, i)
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.size == 0) {
      println("Input file not specified")
      sys.exit()
    }

    val inputFile = args(0)
    val namePrefix = namePrefixOf(inputFile)
    val outputFile = namePrefix + ".ll"

    val input = new FileInputStream(inputFile)
    val output = new FileOutputStream(outputFile)

    println(s"Parsing $inputFile")

    val antlrInput = new ANTLRInputStream(input)
    val lexer = new FlowLexer(antlrInput)
    val tokens = new CommonTokenStream(lexer)
    val parser = new FlowParser(tokens)
    val tree = parser.program()

    val program = new AstVisitor().visit(tree).asInstanceOf[Program]

    //    new TreeViewer(Arrays.asList(parser.getRuleNames: _*), tree).open()
    //    println(program)

    println(s"Compiling to $outputFile")

    val module = compile(inputFile, program)

    val printer = new PrintWriter(output)

    printer.println(module.llvm)
    printer.close()

    s"./optnrun.sh $outputFile".!
  }

}
