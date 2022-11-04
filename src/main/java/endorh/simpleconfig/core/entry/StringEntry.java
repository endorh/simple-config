package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.StringEntryBuilder;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.TextFieldBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofString;
import static java.lang.Math.min;

public class StringEntry
  extends AbstractConfigEntry<String, String, String>
  implements AtomicEntry<String> {
	protected Supplier<List<String>> choiceSupplier;
	protected SimpleComboBoxModel<String> suggestionProvider;
	protected boolean restrict;
	protected int maxLength;
	protected int minLength;
	
	@Internal public StringEntry(ConfigEntryHolder parent, String name, String value) {
		super(parent, name, value);
		suggestionProvider = new SimpleComboBoxModel<>(
		  () -> choiceSupplier != null? choiceSupplier.get() : Lists.newArrayList());
	}
	
	public static class Builder
	  extends AbstractConfigEntryBuilder<String, String, String, StringEntry, StringEntryBuilder, Builder>
	  implements StringEntryBuilder {
		protected Supplier<List<String>> choiceSupplier = null;
		protected boolean restrict = false;
		protected int maxLength = Integer.MAX_VALUE;
		protected int minLength = 0;
		public Builder(String value) {
			super(value, String.class);
		}
		
		@Override public @NotNull StringEntryBuilder withValue(String value) {
			if (restrict && choiceSupplier != null) {
				List<String> choices = choiceSupplier.get();
				if (!choices.contains(value)) throw new IllegalArgumentException(
				  "New value for String entry (\"" + value + "\") is not in the choice list: [" +
				  choices.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]");
			}
			return super.withValue(value);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder suggest(String... suggestions) {
			return suggest(Arrays.stream(suggestions).collect(Collectors.toList()));
		}
		
		@Override @Contract(pure=true) public @NotNull Builder suggest(@NotNull List<String> suggestions) {
			Builder copy = copy();
			Objects.requireNonNull(suggestions);
			copy.choiceSupplier = () -> suggestions;
			copy.restrict = false;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder suggest(
		  Supplier<List<String>> suggestionSupplier
		) {
			Builder copy = copy();
			copy.choiceSupplier = suggestionSupplier;
			copy.restrict = false;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull StringEntryBuilder restrict(String first, String... choices) {
			List<String> list = new ArrayList<>(choices.length + 1);
			list.add(first);
			list.addAll(Arrays.asList(choices));
			if (!list.contains(value)) return withValue(first).restrict(list);
			return restrict(list);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder restrict(@NotNull List<String> choices) {
			Builder copy = copy();
			if (choices.isEmpty()) throw new IllegalArgumentException(
			  "At least one choice must be specified");
			Objects.requireNonNull(choices);
			if (!choices.contains(value)) throw new IllegalArgumentException(
			  "Default value is not included in choice list for String entry");
			copy.choiceSupplier = () -> choices;
			copy.restrict = true;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder maxLength(
		  @Range(from=0, to=Integer.MAX_VALUE) int maxLength
		) {
			Builder copy = copy();
			copy.maxLength = maxLength;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder minLength(
		  @Range(from=0, to=Integer.MAX_VALUE) int minLength
		) {
			Builder copy = copy();
			copy.minLength = minLength;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder notEmpty() {
			return minLength(1);
		}
		
		@Override protected StringEntry buildEntry(ConfigEntryHolder parent, String name) {
			final StringEntry entry = new StringEntry(parent, name, value);
			entry.choiceSupplier = choiceSupplier;
			entry.restrict = restrict;
			entry.maxLength = maxLength;
			entry.minLength = minLength;
			return entry;
		}
		
		@Override protected Builder createCopy(String value) {
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
	
	@Override public Optional<Component> getErrorFromGUI(String value) {
		final Optional<Component> opt = super.getErrorFromGUI(value);
		if (opt.isPresent()) return opt;
		if (value.length() < minLength)
			return Optional.of(
			  minLength == 1
			  ? Component.translatable("simpleconfig.config.error.string.empty")
			  : Component.translatable("simpleconfig.config.error.string.min_length", coloredNumber(minLength)));
		if (value.length() > maxLength)
			return Optional.of(Component.translatable("simpleconfig.config.error.string.max_length", coloredNumber(maxLength)));
		return Optional.empty();
	}
	
	protected static MutableComponent coloredNumber(int number) {
		return Component.literal(String.valueOf(number))
		  .withStyle(ChatFormatting.DARK_AQUA);
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
	  ConfigFieldBuilder builder
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
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		Optional<List<String>> opt = suggestionProvider.updateSuggestions(ofString(), "");
		if (opt.isPresent()) {
			List<String> suggestions = opt.get();
			String current = get();
			for (String s: suggestions) if (!current.equals(s) && !defValue.equals(s))
				builder.suggest(s);
		}
		return true;
	}
}
