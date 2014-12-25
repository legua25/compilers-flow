grammar Flow;

// parser

program
  : statement (semi statement)*
  ;

statement
  : expression
  |
  ;

expression
  : expression ID expression                                                    # InfixExpr
  | ID '(' arguments? ')'                                                       # Call
  | ID                                                                          # Id
  | literal                                                                     # LiteralExpr
  | '(' expression ')'                                                          # Parenthesized
  ;

infixExpression
  : expression ID expression
  ;

arguments
  : expression (',' expression)*
  ;

literal
  : BOOL                                                                        # Bool
  | CHAR                                                                        # Char
  | STRING                                                                      # String
  | '-'? intt                                                                   # Int
  | '-'? FLOAT                                                                  # Float
  ;

semi
  : ';'
  | NL+
  ;

intt
  : DECIMAL                                                                     # Decimal
  | HEXADECIMAL                                                                 # HexaDecimal
  | OCTAL                                                                       # Octal
  | BINARY                                                                      # Binary
  ;


// lexer

BOOL
  : 'true'
  | 'false'
  ;

CHAR
  : '\'' CHARELEM '\''
  ;

STRING
  : '"' STRINGELEM* '"'
  ;

DECIMAL
  : '0'
  | NONZERODIGIT DIGIT*
  ;

HEXADECIMAL
  : '0x' HEXDIGIT+
  ;

OCTAL
  : '0' OCTDIGIT+
  ;

BINARY
  : '0b' BINDIGIT+
  ;

FLOAT
  : DECIMAL ('.' DIGIT*)? ('e' DECIMAL)? 'f'?
  ;

ID
  : IDSTART IDREST*
  | OP
  ;

OP
  : OPCHAR+
  ;

NL
  : '\r'? '\n'
  | '\r'
  ;

WHITESPACE
  : [ \t]+ -> skip
  ;

COMMENT
  : ('/*' .*? '*/'
  | '//' .*? NL) -> skip
  ;

fragment LETTER           : 'a' .. 'z' | 'A .. Z' | '\u00C0' .. '\uFFFF';
fragment NONZERODIGIT     : '1' .. '9';
fragment DIGIT            : '0' .. '9';
fragment HEXDIGIT         : '0' .. '9' | 'a' .. 'f' | 'A' .. 'F';
fragment OCTDIGIT         : '0' .. '7';
fragment BINDIGIT         : '0' .. '1';

fragment IDSTART          : LETTER | '_';
fragment IDREST           : IDSTART | DIGIT;
fragment OPCHAR           : [!#$%&*+\-/:<=>?@\\^_|~] | '\u00A1' .. '\u00AC' | '\u00AE' .. '\u00BF';

fragment CHARELEM         : ~'\'' | CHARESCAPESEQ;
fragment STRINGELEM       : ~('"' | '\n' | '\r') | CHARESCAPESEQ;
fragment CHARESCAPESEQ    : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
