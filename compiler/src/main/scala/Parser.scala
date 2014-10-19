import java.io.FileNotFoundException
import java.util.Arrays

import scala.util.control.NonFatal

import org.antlr.v4.runtime.ANTLRFileStream
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.gui.TreeViewer

import javax.swing.UIManager

object Flow extends ANTLRClassLoader {
  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  
  def main(args: Array[String]): Unit = {
    val argMap = args.zipWithIndex.map(t => (t._2, t._1)).toMap

    val grammar = "Flow"
    val rule = argMap.getOrElse(0, "")
    val input = argMap.getOrElse(1, "")

    try {
      val stream = streamFor(input)
      val lexer = lexerFor(grammar)(stream)
      val tokens = new CommonTokenStream(lexer)
      val parser = parserFor(grammar)(tokens)

      val tree = treeFor(parser, rule)

      println(tree.toStringTree(parser))

      val treeViewer = new TreeViewer(Arrays.asList(parser.getRuleNames: _*), tree)
      treeViewer.open()
    }
    catch {
      case NonFatal(e) =>
        e.printStackTrace()
    }
  }
}

trait ANTLRClassLoader {
  def streamFor(input: String): CharStream = {
    try {
      new ANTLRFileStream(input)
    }
    catch {
      case e: FileNotFoundException =>
        new ANTLRInputStream(input)
    }
  }

  def lexerFor(grammar: String): CharStream => TokenSource = {
    val constructor = Class.forName(grammar + "Lexer").getConstructor(classOf[CharStream])
    (charStream) => constructor.newInstance(charStream).asInstanceOf[TokenSource]
  }

  def parserFor(grammar: String): TokenStream => Parser = {
    val constructor = Class.forName(grammar + "Parser").getConstructor(classOf[TokenStream])
    (tokenStream) => constructor.newInstance(tokenStream).asInstanceOf[Parser]
  }

  def treeFor(parser: Parser, rule: String): ParseTree = {
    val method = parser.getClass.getMethod(rule)
    method.invoke(parser).asInstanceOf[ParseTree]
  }
}
