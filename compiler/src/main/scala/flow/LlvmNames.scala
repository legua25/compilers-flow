package flow

trait LlvmNames {

  def isAllowedChar(c: Char) =
    'a' <= c && c < 'z' || 'A' <= c && c < 'Z' || '0' <= c && c < '9'

  def safeNameFrom(name: String) = {
    name flatMap {
      case c if isAllowedChar(c) => c.toString
      case '!'                   => "$bang"
      case '#'                   => "$hash"
      case '$'                   => "$dollar"
      case '%'                   => "$percent"
      case '&'                   => "$amp"
      case '*'                   => "$times"
      case '+'                   => "$plus"
      case '-'                   => "$minus"
      case '/'                   => "$div"
      case ':'                   => "$colon"
      case '<'                   => "$less"
      case '='                   => "$equal"
      case '>'                   => "$greater"
      case '?'                   => "$qmark"
      case '@'                   => "$at"
      case '\\'                  => "$bslash"
      case '^'                   => "$up"
      case '_'                   => "$under"
      case '|'                   => "$bar"
      case '~'                   => "$tilde"
      case c                     => f"$$${c.toInt}%04x"
    }
  }

}
