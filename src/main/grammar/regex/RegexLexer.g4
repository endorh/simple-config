lexer grammar RegexLexer;
@header {
package endorh.simpleconfig.grammar.regex;
}

GroupEnd : ')';
WILDCARD : '.';
QUESTION : '?';
STAR : '*';
PLUS : '+';
PIPE : '|';
QuantifierStart : '{' -> pushMode(QUANTIFIER);
FlagSwitch : '(?' ([idmsuxU]+ | [idmsuxU]* '-' [idmsuxU]+) ')';
NamedGroupStart : '(?<' -> pushMode(GROUP_NAME);
AtomicGroupStart : '(?>';
NonCapturingGroupStart : '(?' [idmsuxU]* ('-' [idmsuxU]+)? ':';
LookAheadStart : '(?=';
NegativeLookAheadStart : '(?!';
LookBehindStart : '(?<=';
NegativeLookBehindStart : '(?<!';
CapturingGroupStart : '(';
Anchor : '\\' [bBAGZz] | '^' | '$';
MetaEscape : '\\' [tnrfae\\dDsSwW];
OctalEscape : '\\0' ([0-3] [0-7][0-7] | [0-7][0-7]?);
HexadecimalEscape : '\\x' [0-9a-fA-F][0-9a-fA-F] | '\\u' [0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F] | '\\x{' [0-9a-fA-F]+ '}';
ControlCharacter : '\\c' [0-9a-zA-Z];
ShortUnicodeClass : '\\p' [a-zA-Z];
ShortNegatedUnicodeClass : '\\P' [a-zA-Z];
UnicodeClassStart : '\\p{' -> pushMode(UNICODE_CLASS);
NegatedUnicodeClassStart : '\\P{' -> pushMode(UNICODE_CLASS);
Literal : ~[\\()[{?+*|.];
InvalidMetaCharacter : '\\' .;

CharacterClassStart : '[' -> pushMode(CHARACTER_CLASS);
NegatedCharacterClassStart : '[^' -> pushMode(CHARACTER_CLASS);

mode QUANTIFIER;
    EndQuantifier : '}' -> popMode;
    COMMA : ',';
    QuantifierAmount : [0-9]+;

mode CHARACTER_CLASS;
    CharacterClassEnd : ']' -> popMode;
    ClassMetaCharacter : '\\' [\-\][\\];
    InnerCharacterClassStart : CharacterClassStart;
    InnerNegatedCharacterClassStart : NegatedCharacterClassStart;
    RANGE : '-';
    INTERSECTION : '&&';
    ClassMetaEscape : '\\' [tnrfae\\dDsSwW];
    ClassOctalEscape : '\\0' ([0-3] [0-7][0-7] | [0-7][0-7]?);
    ClassHexadecimalEscape : '\\x' [0-9a-fA-F][0-9a-fA-F] | '\\u' [0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F] | '\\x{' [0-9a-fA-F]+ '}';
    ClassControlCharacter : '\\c' [0-9a-zA-Z];
    ClassShortUnicodeClass : '\\p' [a-zA-Z];
    ClassShortNegatedUnicodeClass : '\\P' [a-zA-Z];
    ClassUnicodeClassStart : '\\p{' -> pushMode(UNICODE_CLASS);
    ClassNegatedUnicodeClassStart : '\\P{' -> pushMode(UNICODE_CLASS);
    ClassLiteral : ~[\]\\];
    ClassInvalidMetaCharacter : '\\' .;

mode UNICODE_CLASS;
    UnicodeClassEnd : '}' -> popMode;
    EQUALS : '=';
    UnicodeClassName : [a-zA-Z0-9_]+;

mode GROUP_NAME;
    GroupNameEnd : '>' -> popMode;
    GroupName : [a-zA-Z0-9]+;
