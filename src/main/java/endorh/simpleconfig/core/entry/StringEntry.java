package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.TextFieldBuilder;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofString;
import static java.lang.Math.min;

public class StringEntry
  extends AbstractConfigEntry<String, String, String>
  implements IKeyEntry<String> {
	protected Supplier<List<String>> choiceSupplier;
	protected SimpleComboBoxModel<String> suggestionProvider;
	protected boolean restrict;
	protected int maxLength;
	protected int minLength;
	
	@Internal public StringEntry(ISimpleConfigEntryHolder parent, String name, String value) {
		super(parent, name, value);
		suggestionProvider = new SimpleComboBoxModel<>(
		  () -> choiceSupplier != null? choiceSupplier.get() : Lists.newArrayList());
	}
	
	public static class Builder
	  extends AbstractConfigEntryBuilder<String, String, String, StringEntry, Builder> {
		protected Supplier<List<String>> choiceSupplier = null;
		protected boolean restrict = false;
		protected int maxLength = Integer.MAX_VALUE;
		protected int minLength = 0;
		public Builder(String value) {
			super(value, String.class);
		}
		
		/**
		 * Suggest possible values in a combo-box.<br>
		 * To restrict values to the suggestions, use {@link Builder#restrict} instead.<br>
		 * For suggestions, it's possible to provide instead a suggestion supplier,
		 * to provide dynamic suggestions instead. This is not possible with restrictions.
		 */
		@Contract(pure=true) public Builder suggest(String... suggestions) {
			return suggest(Arrays.stream(suggestions).collect(Collectors.toList()));
		}
		
		/**
		 * Suggest possible values in a combo-box.<br>
		 * To restrict values to the suggestions, use {@link Builder#restrict} instead.<br>
		 * For suggestions, it's possible to provide instead a suggestion supplier,
		 * to provide dynamic suggestions instead. This is not possible with restrictions.
		 */
		@Contract(pure=true)public Builder suggest(@NotNull List<String> suggestions) {
			Builder copy = copy();
			Objects.requireNonNull(suggestions);
			copy.choiceSupplier = () -> suggestions;
			copy.restrict = false;
			return copy;
		}
		
		/**
		 * Suggest possible values in a combo-box dynamically.<br>
		 * To restrict values to the suggestions, use {@link Builder#restrict},
		 * although this method can only supply a fixed set of choices.
		 */
		@Contract(pure=true) public Builder suggest(Supplier<List<String>> suggestionSupplier) {
			Builder copy = copy();
			copy.choiceSupplier = suggestionSupplier;
			copy.restrict = false;
			return copy;
		}
		
		/**
		 * Restrict the values of this entry to a finite set
		 * of options, displayed in a combo box.<br>
		 * Unlike {@link Builder#suggest}, this method does not accept
		 * a {@link Supplier} of choices, since delayed choice
		 * computation would result in the entry's value being reset
		 * before the choices can be determined. Consider using
		 * suggestions instead when they cannot be determined at
		 * start-up time
		 */
		@Contract(pure=true) public Builder restrict(String first, String... choices) {
			return restrict(Lists.newArrayList(ArrayUtils.insert(0, choices, first)));
		}
		
		/**
		 * Restrict the values of this entry to a finite set
		 * of options, displayed in a combo box.<br>
		 * Unlike {@link Builder#suggest}, this method does not accept
		 * a {@link Supplier} of choices, since delayed choice
		 * computation would result in the entry's value being reset
		 * before the choices can be determined. Consider using
		 * suggestions instead when they cannot be determined at
		 * start-up time
		 */
		@Contract(pure=true) public Builder restrict(@NotNull List<String> choices) {
			Builder copy = copy();
			if (choices.isEmpty())
				throw new IllegalArgumentException("At least one choice must be specified");
			Objects.requireNonNull(choices);
			copy.choiceSupplier = () -> choices;
			copy.restrict = true;
			return copy;
		}
		
		@Contract(pure=true) public Builder maxLength(@Range(from = 0, to = Integer.MAX_VALUE) int maxLength) {
			Builder copy = copy();
			copy.maxLength = maxLength;
			return copy;
		}
		
		@Contract(pure=true) public Builder minLength(@Range(from = 0, to = Integer.MAX_VALUE) int minLength) {
			Builder copy = copy();
			copy.minLength = minLength;
			return copy;
		}
		
		@Contract(pure=true) public Builder notEmpty() {
			return minLength(1);
		}
		
		@Override protected StringEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final StringEntry entry = new StringEntry(parent, name, value);
			if (value.length() > maxLength)
				throw new IllegalArgumentException(
				  "Config entry's default value is longer than its max length: " + entry.getGlobalPath());
			// if (value.length() < minLength)
			// 	throw new IllegalArgumentException(
			// 	  "Config entry's default value is shorter than its min length: " + entry.getPath());
			entry.choiceSupplier = choiceSupplier;
			entry.restrict = restrict;
			entry.maxLength = maxLength;
			entry.minLength = minLength;
			return entry;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.choiceSupplier = choiceSupplier;
			copy.restrict = restrict;
			copy.maxLength = maxLength;
			copy.minLength = minLength;
			return copy;
		}
	}
	
	public List<String> getChoices() {
		return choiceSupplier != null? choiceSupplier.get() : null;
	}
	
	@Nullable @Override public String fromConfig(@Nullable String value) {
		if (value == null)
			return null;
		if (restrict) {
			final List<String> choices = getChoices();
			if (choices != null && !choices.contains(value))
				return null;
		}
		return value;
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(String value) {
		final Optional<ITextComponent> opt = super.getErrorFromGUI(value);
		if (opt.isPresent()) return opt;
		if (value.length() < minLength)
			return Optional.of(
			  minLength == 1
			  ? new TranslationTextComponent("simpleconfig.config.error.string.empty")
			  : new TranslationTextComponent(
				 "simpleconfig.config.error.string.min_length", coloredNumber(minLength)));
		if (value.length() > maxLength)
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.string.max_length", coloredNumber(maxLength)));
		return Optional.empty();
	}
	
	protected static IFormattableTextComponent coloredNumber(int number) {
		return new StringTextComponent(String.valueOf(number))
		  .mergeStyle(TextFormatting.DARK_AQUA);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		boolean added = false;
		if (choiceSupplier != null) {
			List<String> choices = getChoices();
			if (!choices.isEmpty()) {
				int max = advanced.max_options_in_config_comment;
				String omittedSuffix = choices.size() > max
				                       ? ", ... (omitted " + (choices.size() - max) + " more)" : "";
				String prefix = restrict? "Options: " : "Suggestions: ";
				tooltips.add(prefix + choices.subList(0, min(max, choices.size())).stream()
				  .map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + omittedSuffix);
				added = true;
			}
		}
		if (!added) tooltips.add("Text");
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<String, ?, ?>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (choiceSupplier == null) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), get())
			  .setMaxLength(maxLength);
			return Optional.of(decorate(valBuilder));
		} else {
			final ComboBoxFieldBuilder<String> valBuilder =
			  builder.startComboBox(getDisplayName(), ofString(), forGui(get()))
				 .setSuggestionMode(!restrict)
			    .setSuggestionProvider(suggestionProvider)
			    .setMaxLength(maxLength);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
