/**
 * Package used to expose all the entry builders from the Simple Config API
 * adapted to Kotlin under a single wildcard import statement.<br><br>
 * 
 * Simple Konfig equivalent to the {@link endorh.simpleconfig.api.ConfigBuilderFactoryProxy}
 * import from the Java API. While it's possible to use the Java API directly,
 * this package provides a more Kotlin-friendly API, with Kotlin specific
 * entry types, default values, and some type conversions to Kotlin stdlib types
 *
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#bool
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#yesNo
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#enable
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#onOff
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#string
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#option
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#button
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#number
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#int
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#long
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#float
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#double
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#percent
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#fraction
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#volume
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#range
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#color
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#regex
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#pattern
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#entry
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#data
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#tag
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#compoundTag
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#resource
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#key
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#item
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#itemName
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#block
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#blockName
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#fluid
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#fluidName
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#stringList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#intList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#longList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#floatList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#doubleList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#list
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#set
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#map
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#linkedMap
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#pairList
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#caption
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#toCommonsPair
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#toPair
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#pair
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#toCommonsTriple
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#toTriple
 * @see endorh.simpleconfig.konfig.builders.SimpleKonfigBuildersKt#triple
 */
package endorh.simpleconfig.konfig.builders;