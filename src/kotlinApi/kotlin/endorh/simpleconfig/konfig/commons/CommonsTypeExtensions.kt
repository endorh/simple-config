/**
 * Contains type extensions to unify the API from Apache Commons
 * tuple types with the Kotlin stdlib types, as well as methods
 * to perform conversions between the two.
 */
package endorh.simpleconfig.konfig.commons

import org.apache.commons.lang3.tuple.Pair
import org.apache.commons.lang3.tuple.Triple

/**
 * First value (left)
 */
inline val <A> Pair<A, *>.first: A get() = left
/**
 * Second value (right)
 */
inline val <B> Pair<*, B>.second: B get() = right

operator fun <A> Pair<A, *>.component1(): A = left
operator fun <B> Pair<*, B>.component2(): B = right
fun <A, B> Pair<A, B>.copy(first: A = left, second: B = right) =
  Pair.of(first, second)!!

/**
 * Converts this pair into a list.
 */
fun <T> Pair<T, T>.toList(): List<T> = listOf(left, right)

/**
 * Converts this Apache Commons [Pair] into a [kotlin.Pair]
 */
fun <K, V> Pair<K, V>.toPair() = left to right
/**
 * Converts this [kotlin.Pair] into an Apache Commons [Pair]
 */
fun <K, V> kotlin.Pair<K, V>.toCommonsPair() = Pair.of(first, second)!!

/**
 * First value (left)
 */
inline val <A> Triple<A, *, *>.first: A get() = left
/**
 * Second value (middle)
 */
inline val <B> Triple<*, B, *>.second: B get() = middle
/**
 * Third value (right)
 */
inline val <C> Triple<*, *, C>.third: C get() = right

operator fun <A> Triple<A, *, *>.component1(): A = left
operator fun <B> Triple<*, B, *>.component2(): B = middle
operator fun <C> Triple<*, *, C>.component3(): C = right
fun <A, B, C> Triple<A, B, C>.copy(first: A = left, second: B = middle, third: C = right) =
  Triple.of(first, second, third)!!

/**
 * Converts this triple into a list.
 */
fun <T> Triple<T, T, T>.toList(): List<T> = listOf(left, middle, right)

/**
 * Converts this Apache Commons [Triple] into a [kotlin.Triple]
 */
fun <L, M, R> Triple<L, M, R>.toTriple(): kotlin.Triple<L, M, R> = Triple(left, middle, right)
/**
 * Converts this [kotlin.Triple] into an Apache Commons [Triple]
 */
fun <L, M, R> kotlin.Triple<L, M, R>.toCommonsTriple() = Triple.of(first, second, third)!!

// Pairs to map

/**
 * @see kotlin.collections.mapOf
 */
fun <K, V> mapOf(vararg pairs: Pair<K, V>) = mapOf(*pairs.map { it.toPair() }.toTypedArray())
/**
 * @see kotlin.collections.mutableMapOf
 */
fun <K, V> mutableMapOf(vararg pairs: Pair<K, V>) = mutableMapOf(*pairs.map { it.toPair() }.toTypedArray())
/**
 * @see kotlin.collections.linkedMapOf
 */
fun <K, V> linkedMapOf(vararg pairs: Pair<K, V>) = linkedMapOf(*pairs.map { it.toPair() }.toTypedArray())
/**
 * @see kotlin.collections.sortedMapOf
 */
fun <K: Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>) = sortedMapOf(*pairs.map { it.toPair() }.toTypedArray())
/**
 * @see kotlin.collections.sortedMapOf
 */
fun <K, V> sortedMapOf(comparator: Comparator<K>, vararg pairs: Pair<K, V>) = sortedMapOf(comparator, *pairs.map { it.toPair() }.toTypedArray())

/**
 * @see kotlin.collections.toMap
 */
fun <K, V> Array<out Pair<K, V>>.toMap() = mapOf(*this)
/**
 * @see kotlin.collections.toMap
 */
fun <K, V, M: MutableMap<K, V>> Array<out Pair<K, V>>.toMap(destination: M) =
  map { it.toPair() }.toTypedArray().toMap(destination)

/**
 * @see kotlin.collections.toMap
 */
fun <K, V> Sequence<Pair<K, V>>.toMap() = map { it.toPair() }.toMap()
/**
 * @see kotlin.collections.toMap
 */
fun <K, V, M: MutableMap<K, V>> Sequence<Pair<K, V>>.toMap(destination: M) =
  map { it.toPair() }.toMap(destination)

/**
 * @see kotlin.collections.toMap
 */
fun <K, V> Iterable<Pair<K, V>>.toMap() = associate { it.toPair() }
/**
 * @see kotlin.collections.toMap
 */
fun <K, V, M: MutableMap<K, V>> Iterable<Pair<K, V>>.toMap(destination: M) =
  associateTo(destination) { it.toPair() }