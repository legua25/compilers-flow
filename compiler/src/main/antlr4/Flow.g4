grammar Flow;

// parser

prog
  : Nl* stat (semi stat)* Nl*
  ;

stat
  : expr
  | defDef
  ;

expr
  : varDef                                                           # varDefExpr
  | '{' block '}'                                                    # blockExpr
  | If '(' cond=expr ')' Nl? thenE=expr (Nl? Else Nl? elseE=expr)?   # branch
  | While '(' cond=expr ')' Nl? body=expr                            # while
  | Id '=' expr                                                      # assignment
  | Id '(' args? ')'                                                 # call
  | literal                                                          # literalExpr
  | Id                                                               # id
  ;

block
  : Nl* expr (semi expr)* Nl*
  |
  ;

varDef
  : Var defRest                                                      # varVarDef
  | Val defRest                                                      # valVarDef
  ;

defRest
  : defPat '=' expr
  ;

defPat
  : declIds
//  | '(' declIds ')'
  ;

declIds
  : Id (',' Id)* typeAnn?
  ;

defDef
  : External Def defSig typeAnn                                      # externalDef
  | Def defSig typeAnn? '=' Nl? expr                                 # internalDef
  ;

defSig
  : Id paramClause?
  ;

paramClause
  : '(' params? ')'
  ;

params
  : param (',' param)*
  ;

param
  : Id typeAnn Star?
  ;

typeAnn
  : ':' Id
  ;

args
  : expr (',' expr)*
  ;

literal
  : BoolLiteral                                                      # bool
  | CharLiteral                                                      # Char
  | StringLiteral                                                    # String
  | '-'? IntLiteral                                                  # Int
  | '-'? FloatLiteral                                                # Float
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

Star
  : '*'
  ;

External
  : 'external'
  ;

Type
  : 'type'
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
