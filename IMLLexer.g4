lexer grammar IMLLexer;

// Separators

LPAREN  : '(';
RPAREN  : ')';
LBRACK  : '[';
RBRACK  : ']';
COMMA   : ',' ;

// Types 
NUMBERT         : 'number';
IMAGE           : 'image';
PERCENTAGET     : 'percentage';
STRINGT         : 'string'; 
BOOLEANT        : 'boolean';
LISTT            : 'list';


// Keywords
PRINT   : 'output';
DRAW    : 'draw';
LOAD    : 'load';
FROM    : 'from';
STORE   : 'store';
INTO    : 'into';
COLUMNS : 'columns';
ROWS    : 'rows';
OF      : 'of';
IF      : 'if';
THEN    : 'then';
DONE    : 'done';
ELSE    : 'else';
IN      : 'in';
PIXEL   : 'pixel';
TOP     : 'top';
BLACK   : 'black';
HAT     : 'hat';
RUN     : 'run';
READ    : 'read';
BY      : 'by';
UNTIL   : 'until';
DO      : 'do';
FOR     : 'for';
WITHIN  : 'within';
APPEND  : 'append';
COUNT   : 'count';
REMOVE  : 'remove';
POP     : 'pop';

// Operators

ATR     : 'is';
HSCALE  : '-*';
VSCALE  : '|*';
SCALE   : '+*';
PXPLUS  : '.+';
PXMINUS : '.-';
PXDIV   : './';
PXMUL   : '.*';
PXGT    : '.>';
PXLT    : '.<';
PXGTE   : '.>=';
PXLTE   : '.<=';
PXEQ    : '.==';
PXNEQ   : '.!=';
VFLIP   : '-';
HFLIP   : '|';
FLIP    : '+';
ERODE   : 'erode';
DILATE  : 'dilate';
OPEN    : 'open';
CLOSE   : 'close';
GT      : '>';
LT      : '<';
GTE     : '>=';
LTE     : '<=';
EQ      : '==';
NEQ     : '!=';
DIV     : '/';
MUL     : '*';
ANY     : 'any';
ALL     : 'all';
AND     : 'and';
OR      : 'or';
NOT     : 'not';

// Boolean
BOOLEAN : 'True' | 'False';

// Number        
NUMBER : DIGIT+ ([.] DIGIT+ ([eE][+-]?DIGIT+)?)?;

// percentage

PERCENTAGE  : DIGIT+ ('.' DIGIT+)? '%';

// Characters
STRING  : '"' (ESC | .)*? '"';

// Identifier names
ID  : LETTER (LETTER | DIGIT)*;

fragment LETTER: [a-zA-Z_\u00C0-\u00FF];
fragment DIGIT: [0-9];
fragment ESC: '\\"' | '\\\\';

LINE_COMMENT: '//' .*? '\n' -> skip;
COMMENT: '/*' ( . | '\r' | '\n' )*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;

// ensure no lexical errors
ERROR: .;