lexer grammar IIMLLexer;

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

// New Keywords
SIZE    : 'size';
BACKGROUND  : 'background';
THICKNESS : 'thickness';
INTENSITY   : 'intensity';
STAR   : 'star';
CIRCLE  : 'circle';
RADIUS  : 'radius';
WIDTH   : 'width';
HEIGHT  : 'height';
RECTANGLE   : 'rect';
CROSS   : 'cross'; 
PLUS    : 'plus';
PLACE   : 'place';
AT      : 'at';
WITH    : 'with';
XAXIS   : 'xaxis';
YAXIS   : 'yaxis';
LINE    :  'line';



// Operators
ATR     : 'is';
SUB   : '-';
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

// Percentage
PERCENTAGE  : DIGIT+ ('.' DIGIT+)? '%';

// Characters
STRING  : '"' (ESC | .)*? '"';

// Identifier names
ID  : LETTER (LETTER | DIGIT)*;

fragment LETTER: [a-zA-Z_\u00C0-\u00FF];
fragment DIGIT: [0-9];
fragment ESC: '\\"' | '\\\\';

NEWLINE
    : '\r'? '\n'
    ;

LINE_COMMENT: '//' .*? '\n' -> skip;
COMMENT: '/*' ( . | '\r' | '\n' )*? '*/' -> skip;
WS: [ \t\r]+ -> skip;

// ensure no lexical errors
ERROR: .;