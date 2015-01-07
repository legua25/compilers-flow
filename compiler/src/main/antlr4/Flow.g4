grammar Flow;

// parser

program
  : statement (semi statement)* NL* EOF
  ;

statement
  : TYPE ID '=' '{' NL* (memberDefinition (NL+ memberDefinition)*)? NL* '}'     # TypeDefinition
  | definition                                                                  # DefinitionLabel
  | complexExpression                                                           # ComplexExpressionLabel
  |                                                                             # EmptyStatement
  ;
  
memberDefinition
  : definition                                                                  # DefinitionLabel2
  | STATIC defn                                                                 # StaticDef
  | variableDefinition                                                          # VarDefLabel
  ;

definition
  : defn                                                                        # DefnLabel
  | EXTERNAL DEF ID parameterClause typeAnn                                     # ExternalFun
  ;

defn
  : DEF ID parameterClause? typeAnn '=' NL? expression
  ;

variableDefinition
  : kw=(VAR|VAL) ID typeAnn '=' expression
  ;

complexExpression
  : expression
  | variableDefinition
  ;

expression
  : expression '(' arguments? ')'                                               # Application
  | IF cond=expression NL? THEN NL? thn=expression
    (NL? ELSE NL? els=expression)?                                              # If
  | WHILE cond=expression NL? DO NL? body=expression                            # While
  | '{' NL* (complexExpression (semi complexExpression)*)? NL* '}'              # Block
  | ID                                                                          # Id
  | expression '.' ID                                                           # Selection
  //| ID expression                                                               # PrefixExpression //'!' | '+' | '-' | '~';
  | expression ID expression                                                    # InfixExpression
  | literal                                                                     # LiteralExpression
  | ID '=' expression                                                           # IdAssignment
  | expression '.' ID '=' expression                                            # SelectionAssignment
  | '(' expression ')'                                                          # Parenthesized
  ;

typeAnn
  : ':' ID
  ;

parameterClause
  : '(' parameters? ')'
  ;

parameters
  : parameter (',' parameter)*
  ;

parameter
  : ID typeAnn
  ;

arguments
  : expression (',' expression)*
  ;

literal
  : BOOL                                                                        # Bool
  | CHAR                                                                        # Char
  | STRING                                                                      # String
  | intt                                                                        # Int
  | FLOAT                                                                       # Float
  ;

semi
  : ';'
  | NL
  ;

intt
  : DECIMAL                                                                     # Decimal
  | HEXADECIMAL                                                                 # HexaDecimal
  | OCTAL                                                                       # Octal
  | BINARY                                                                      # Binary
  ;


// lexer

TYPE     : 'type';
THIS     : 'this';
VAL      : 'val';
VAR      : 'var';
DEF      : 'def';
EXTERNAL : 'external';
STATIC   : 'static';
IF       : 'if';
THEN     : 'then';
ELSE     : 'else';
WHILE    : 'while';
DO       : 'do';

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
  : DECIMAL ('.' DIGIT+)? ('e' DECIMAL)?
  ;

ID
  : IDSTART IDREST*
  | OPCHAR+
  ;

WHITESPACE
  : [ \t]+ -> skip
  ;

COMMENT
  : ('/*' .*? '*/'
  | '//' (~('\n'))*) -> skip
  ;

NL
  : '\r'? '\n'
  | '\r'
  ;

fragment LETTER           : 'a' .. 'z' | 'A' .. 'Z' | '\u00C0' .. '\uFFFF';
fragment NONZERODIGIT     : '1' .. '9';
fragment DIGIT            : '0' .. '9';
fragment HEXDIGIT         : '0' .. '9' | 'a' .. 'f' | 'A' .. 'F';
fragment OCTDIGIT         : '0' .. '7';
fragment BINDIGIT         : '0' .. '1';

fragment IDSTART          : LETTER | '_';
fragment IDREST           : IDSTART | DIGIT;
fragment PREFIXOPCHAR     : '!' | '+' | '-' | '~';
fragment OPCHAR           : [!#$%&*+\-/:<=>?@\\^_|~] | '\u00A1' .. '\u00AC' | '\u00AE' .. '\u00BF';

fragment CHARELEM         : ~'\'' | CHARESCAPESEQ;
fragment STRINGELEM       : ~('"' | '\n' | '\r') | CHARESCAPESEQ;
fragment CHARESCAPESEQ    : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
