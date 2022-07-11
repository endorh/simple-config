lexer grammar SNBTLexer;
@header {
package endorh.simpleconfig.grammar.nbt;
}

CompoundStart : '{';
CompoundEnd : '}';
COMMA : ',';
COLON : ':';
ByteArrayStart : '[B;';
IntArrayStart : '[I;';
LongArrayStart : '[L;';
ListStart : '[';
ListEnd : ']';
TRUE : 'true';
FALSE : 'false';
Byte : '-'? [0-9]+ [bB];
Short : '-'? [0-9]+ [sS];
Int : '-'? [0-9]+;
Long : '-'? [0-9]+ [lL];
Float : '-'? ([0-9]* '.')? [0-9]+ ([eE] '-'? [0-9]+)? [fF];
Double : '-'? ([0-9]* '.')? [0-9]+ ([eE] '-'? [0-9]+)?;
Name : [a-zA-Z_][a-zA-Z0-9_]*;
DoubleStringStart : '"' -> pushMode(DOUBLE_STRING);
SingleStringStart : '\'' -> pushMode(SINGLE_STRING);

WS : [ \t\r\n]+ -> channel(HIDDEN);

mode DOUBLE_STRING;
    DoubleStringEnd : '"' -> popMode;
    DoubleStringEscape : '\\"';
    DoubleStringInvalidEscape : '\\' .;
    DoubleStringPart : ~["\\]+;

mode SINGLE_STRING;
    SingleStringEnd : '\'' -> popMode;
    SingleStringEscape : '\\\'';
    SingleStringInvalidEscape : '\\' .;
    SingleStringPart : ~['\\]+;