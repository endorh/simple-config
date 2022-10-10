package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface CaptionedMapEntryBuilder<
  K, V, KC, C, KG, G, MB extends EntryMapEntryBuilder<K, V, KC, C, KG, G, ?, ?>,
  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>
  > extends ConfigEntryBuilder<Pair<CV, Map<K, V>>, Pair<CC, Map<KC, C>>, Pair<CG, List<Pair<KG, G>>>, CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB>> {
	/**
	 * Bind a field to the caption entry.
	 *
	 * @see #mapField(String)
	 * @see #mapField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true)
	CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> captionField(String name);
	
	/**
	 * Bind a field to the map entry.<br>
	 * You may also omit the name to replace the default field with a field for the map.
	 *
	 * @see #captionField(String)
	 * @see #mapField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) @NotNull CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> mapField(
	  String name
	);
	
	/**
	 * Replace the default field with a field for the map value.
	 *
	 * @see #captionField(String)
	 * @see #mapField(String)
	 * @see #splitFields(String)
	 */
	@Contract(pure=true)
	CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> mapField();
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the map value.
	 *
	 * @see #captionField(String)
	 * @see #mapField(String)
	 * @see #mapField()
	 */
	@Contract(pure=true)
	CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> splitFields(
	  String captionSuffix
	);
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the map value.
	 *
	 * @param fullFieldName {@code true} if the caption field name should be treated
	 *   as the full name of the field. {@code false} (default) if it should be
	 *   treated as a camelCase suffix to this entry's name.
	 * @see #captionField(String)
	 * @see #mapField(String)
	 * @see #mapField()
	 */
	@Contract(pure=true)
	CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> splitFields(
	  String captionField, boolean fullFieldName
	);
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the map value.
	 *
	 * @see #captionField(String)
	 * @see #mapField(String)
	 * @see #mapField()
	 */
	@Contract(pure=true)
	CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> split_fields(
	  String caption_suffix
	);
}
