parser grammar RegexParser;
options { tokenVocab = RegexLexer; }
@header {
package endorh.simpleconfig.grammar.regex;
}

root: pattern EOF;

pattern: expr* (PIPE expr*)?;

expr: atom quantifier?;

quantifier: possessiveQuantifier | reluctantQuantifier | greedyQuantifier;
    possessiveQuantifier: quantifierBody PLUS;
    reluctantQuantifier: quantifierBody QUESTION;
    greedyQuantifier: quantifierBody;
        quantifierBody: QUESTION | STAR | PLUS | QuantifierStart quantity EndQuantifier;
        quantity: minQuantity | rangeQuantity | exactQuantity;
            minQuantity: QuantifierAmount COMMA;
            rangeQuantity: QuantifierAmount COMMA QuantifierAmount;
            exactQuantity: QuantifierAmount;

atom: characterClass | escape | anchor | flagSwitch | group | literal;
    escape: MetaEscape | OctalEscape | HexadecimalEscape | ControlCharacter | ShortUnicodeClass | ShortNegatedUnicodeClass | unicodeClass | negatedUnicodeClass | WILDCARD;
    anchor: Anchor | lookAhead | lookBehind;
    lookAhead: positiveLookAhead | negativeLookAhead;
        positiveLookAhead: LookAheadStart pattern GroupEnd;
        negativeLookAhead: NegativeLookAheadStart pattern GroupEnd;
    lookBehind: positiveLookBehind | negativeLookBehind;
        positiveLookBehind: LookBehindStart pattern GroupEnd;
        negativeLookBehind: NegativeLookBehindStart pattern GroupEnd;
    flagSwitch: FlagSwitch;
    group: namedGroup | atomicGroup | nonCapturingGroup | capturingGroup;
        namedGroup: NamedGroupStart GroupName GroupNameEnd pattern GroupEnd;
        atomicGroup: AtomicGroupStart pattern GroupEnd;
        nonCapturingGroup: NonCapturingGroupStart pattern GroupEnd;
        capturingGroup: CapturingGroupStart pattern GroupEnd;
    literal: Literal;
    unicodeClass: UnicodeClassStart unicodeClassBody UnicodeClassEnd;
    negatedUnicodeClass: NegatedUnicodeClassStart unicodeClassBody UnicodeClassEnd;
        unicodeClassBody: UnicodeClassName (EQUALS UnicodeClassName)?;

    characterClass: positiveCharacterClass | negativeCharacterClass;
        positiveCharacterClass: CharacterClassStart characterClassBody CharacterClassEnd;
        negativeCharacterClass: NegatedCharacterClassStart characterClassBody CharacterClassEnd;

    innerCharacterClass: innerPositiveCharacterClass | innerNegativeCharacterClass;
        innerPositiveCharacterClass: InnerCharacterClassStart characterClassBody CharacterClassEnd;
        innerNegativeCharacterClass: InnerNegatedCharacterClassStart characterClassBody CharacterClassEnd;

characterClassBody: characterClassBody INTERSECTION innerCharacterClass
                  | characterClassEscape
                  | characterClassRange
                  | innerCharacterClass
                  | characterClassLiteral;
    characterClassRange: characterClassLiteral RANGE characterClassLiteral;
    characterClassEscape: ClassMetaCharacter | ClassMetaEscape | ClassOctalEscape | ClassHexadecimalEscape | ClassControlCharacter | ClassShortUnicodeClass | ClassShortNegatedUnicodeClass | characterClassUnicodeClass | characterClassNegatedUnicodeClass;
    characterClassUnicodeClass: ClassUnicodeClassStart unicodeClassBody UnicodeClassEnd;
    characterClassNegatedUnicodeClass: ClassNegatedUnicodeClassStart unicodeClassBody UnicodeClassEnd;
    characterClassLiteral: ClassLiteral | ClassMetaCharacter;

