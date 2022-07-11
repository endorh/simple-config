parser grammar SNBTParser;
options { tokenVocab = SNBTLexer; }
@header {
package endorh.simpleconfig.grammar.nbt;
}

root: value EOF;

value: byteValue
     | shortValue
     | intValue
     | longValue
     | floatValue
     | doubleValue
     | string
     | list
     | compound
     | byteArray
     | intArray
     | longArray
     | booleanValue;

byteValue: Byte;
shortValue: Short;
intValue: Int;
longValue: Long;
floatValue: Float;
doubleValue: Double;

string: doubleString | singleString;
    doubleString: DoubleStringStart (DoubleStringEscape | DoubleStringPart)* DoubleStringEnd;
    singleString: SingleStringStart (SingleStringEscape | SingleStringPart)* SingleStringEnd;

list: byteList
    | shortList
    | intList
    | longList
    | floatList
    | doubleList
    | stringList
    | listList
    | compoundList
    | intArrayList
    | longArrayList
    | byteArrayList;
    byteList: ListStart ((byteValue | booleanValue) (COMMA (byteValue | booleanValue))*)? ListEnd;
    shortList: ListStart (shortValue (COMMA shortValue)*)? ListEnd;
    intList: ListStart (intValue (COMMA intValue)*)? ListEnd;
    longList: ListStart (longValue (COMMA longValue)*)? ListEnd;
    floatList: ListStart (floatValue (COMMA floatValue)*)? ListEnd;
    doubleList: ListStart (doubleValue (COMMA doubleValue)*)? ListEnd;
    stringList: ListStart (string (COMMA string)*)? ListEnd;
    listList: ListStart (list (COMMA list)*)? ListEnd;
    compoundList: ListStart (compound (COMMA compound)*)? ListEnd;
    intArrayList: ListStart (intArray (COMMA intArray)*)? ListEnd;
    longArrayList: ListStart (longArray (COMMA longArray)*)? ListEnd;
    byteArrayList: ListStart (byteArray (COMMA byteArray)*)? ListEnd;

compound: CompoundStart (pair (COMMA pair)*)? CompoundEnd;
    pair: tag COLON value;
    tag: Name | string;

byteArray: ByteArrayStart (byteValue (COMMA byteValue)*)? ListEnd;
intArray: IntArrayStart (intValue (COMMA intValue)*)? ListEnd;
longArray: LongArrayStart (longValue (COMMA longValue)*)? ListEnd;

booleanValue: TRUE | FALSE;
