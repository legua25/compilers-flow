grammar Flow;

// parser

prog
  : Nl* stat (semi stat)* Nl*
  ;

stat
  : def
  | expr
  ;

expr
  : '{' block '}'
  | If '(' expr ')' Nl? expr (Nl? Else Nl? expr)?
  | While '(' expr ')' Nl? expr
  | literal
  | Id
  ;

block
  : Nl* blockStat (semi blockStat)* Nl*
  |
  ;

blockStat
  : valDef
  | expr
  ;

def
  : valDef
  | defDef
  ;

valDef
  : Var defRest
  | Val defRest
  ;

defDef
  : Def defSig typeAnn? '=' Nl? expr
  | External Def defSig typeAnn
  ;

defSig
  : Id paramClause?
  ;

defRest
  : defPat '=' expr
  ;

defPat
  : declIds
  | '(' declIds ')'
  ;

declIds
  : Id (',' Id)* typeAnn?
  ;

paramClause
  : '(' params? ')'
  ;

params
  : param (',' param)*
  ;

param
  : Id typeAnn
  ;

typeAnn
  : ':' Id
  ;

literal
  : BoolLiteral
  | CharLiteral
  | StringLiteral
  | '-'? IntLiteral
  | '-'? FloatLiteral
  ;

semi
  : ';'
  | Nl+
  ;


// lexer

Var
  : 'var'
  ;

Val
  : 'val'
  ;

Def
  : 'def'
  ;

External
  : 'external'
  ;

If
  : 'if'
  ;

Else
  : 'else'
  ;

While
  : 'while'
  ;

BoolLiteral
  : 'true'
  | 'false'
  ;

CharLiteral
  : '\'' CharElem '\''
  ;

StringLiteral
  : '"' StringElem* '"'
  ;

IntLiteral
  : Number
  ;

FloatLiteral
  : Number ('.' Digit*)? ('e' Number)? 'f'?
  ;

Id
  : [a-zA-Z_][a-zA-Z_0-9]*
  ;

Paren
  : '(' | ')' | '[' | ']' | '{' | '}'
  ;

Delim
  : '\'' | '"' | '.' | ';' | ','
  ;

Nl
  : '\r'? '\n'
  | '\r'
  ;

Ws
  : [ \t]+ -> skip
  ;

Comment
  : ('/*' .*? '*/'
  | '//' .*? Nl) -> skip
  ;

fragment CharElem         : ~'\'' | CharEscapeSeq;
fragment StringElem       : ~('"' | '\n' | '\r') | CharEscapeSeq;
fragment CharEscapeSeq    : '\\' ('b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\');
fragment Number           : '0' | NonZeroDigit Digit*;
fragment NonZeroDigit     : '1' .. '9';
fragment Digit            : '0' .. '9';
