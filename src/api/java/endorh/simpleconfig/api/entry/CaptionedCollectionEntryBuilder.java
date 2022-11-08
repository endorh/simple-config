package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CaptionedCollectionEntryBuilder<
  V, C, G, B extends ConfigEntryBuilder<V, C, List<G>, B>,
  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
> extends ConfigEntryBuilder<
  @NotNull Pair<@NotNull CV, @NotNull V>, Pair<CC, C>, Pair<CG, List<G>>,
  CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB>
> {
	/**
	 * Bind a field to the caption entry.
	 *
	 * @see #collectionField(String)
	 * @see #collectionField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> captionField(String name);
	
	/**
	 * Bind a field to the list/set/map/collection entry.<br>
	 * You may also omit the name to replace the default field with a field for the list.
	 *
	 * @see #captionField(String)
	 * @see #collectionField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> collectionField(String name);
	
	/**
	 * Replace the default field with a field for the list/set/map/collection value.
	 *
	 * @see #captionField(String)
	 * @see #collectionField(String)
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> collectionField();
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the list value.
	 *
	 * @see #captionField(String)
	 * @see #collectionField(String)
	 * @see #collectionField()
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> splitFields(
	  String captionSuffix
	);
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the list value.
	 *
	 * @param fullFieldName {@code true} if the caption field name should be treated
	 *   as the full name of the field. {@code false} (default) if it should be
	 *   treated as a camelCase suffix to this entry's name.
	 * @see #captionField(String)
	 * @see #collectionField(String)
	 * @see #collectionField()
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> splitFields(
	  String captionField, boolean fullFieldName
	);
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the list value.
	 *
	 * @see #captionField(String)
	 * @see #collectionField(String)
	 * @see #collectionField()
	 */
	@Contract(pure=true) @NotNull CaptionedCollectionEntryBuilder<V, C, G, B, CV, CC, CG, CB> split_fields(
	  String caption_suffix
	);
}
