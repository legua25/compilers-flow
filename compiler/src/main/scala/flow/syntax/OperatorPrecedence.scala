package flow.syntax

trait OperatorPrecedence {

  sealed trait Associativity

  case object Left extends Associativity

  case object Right extends Associativity

  def precedenceOf(operator: String) = operator.head match {
    case c if c != '=' && operator.last == '=' => 0
    case c if c.isLetter                       => 1
    case '|'                                   => 2
    case '^'                                   => 3
    case '&'                                   => 4
    case '<' | '>'                             => 5
    case '=' | '!'                             => 6
    case ':'                                   => 7
    case '+' | '-'                             => 8
    case '*' | '/' | '%'                       => 9
    case _                                     => 10
  }

  def associativityOf(operator: String): Associativity = operator.last match {
    case ':' => Right
    case _   => Left
  }

  def reparented(expression: Expression): Expression = expression match {
    case e @ InfixExpression(InfixExpression(e00, op0, e01), op1, e11) =>
      val p0 = precedenceOf(op0)
      val p1 = precedenceOf(op1)
      val a0 = associativityOf(op0)
      val a1 = associativityOf(op1)

      if (p0 > p1 || p0 == p1 && a1 == Left)
        e
      else
        InfixExpression(e00, op0, reparented(InfixExpression(e01, op1, e11)))
    case e: Expression => e
  }

}
