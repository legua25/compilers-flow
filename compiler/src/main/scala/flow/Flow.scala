package flow

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

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

    println(s"Compiling $inputFile to $outputFile")

    val antlrInput = new ANTLRInputStream(input)
    val lexer = new FlowLexer(antlrInput)
    val tokens = new CommonTokenStream(lexer)
    val parser = new FlowParser(tokens)
    val tree = parser.prog()

    val program = new AstVisitor().visit(tree).asInstanceOf[Program]
    val module = compile(inputFile, program)

    val printer = new PrintWriter(output)

    printer.println(module.llvm)
    printer.close()
  }
}
