package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;

import java.util.List;

public interface CaptionedListEntryBuilder<
  V, C, G, B extends ListEntryBuilder<V, C, G, B>,
  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>
  > extends ConfigEntryBuilder<Pair<CV, List<V>>, Pair<CC, List<C>>, Pair<CG, List<G>>, CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB>> {
	/**
	 * Bind a field to the caption entry.
	 *
	 * @see #listField(String)
	 * @see #listField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> captionField(String name);
	
	/**
	 * Bind a field to the list entry.<br>
	 * You may also omit the name to replace the default field with a field for the list.
	 *
	 * @see #captionField(String)
	 * @see #listField()
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> listField(String name);
	
	/**
	 * Replace the default field with a field for the list value.
	 *
	 * @see #captionField(String)
	 * @see #listField(String)
	 * @see #splitFields(String)
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> listField();
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the list value.
	 *
	 * @see #captionField(String)
	 * @see #listField(String)
	 * @see #listField()
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> splitFields(
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
	 * @see #listField(String)
	 * @see #listField()
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> splitFields(
	  String captionField, boolean fullFieldName
	);
	
	/**
	 * Bind a field to the caption entry and replace the default field with a field
	 * for the list value.
	 *
	 * @see #captionField(String)
	 * @see #listField(String)
	 * @see #listField()
	 */
	@Contract(pure=true) CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> split_fields(
	  String caption_suffix
	);
}
