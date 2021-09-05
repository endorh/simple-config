package endorh.simple_config.core.entry;

import com.google.common.collect.Lists;
import endorh.simple_config.SimpleConfigMod.ClientConfig.advanced;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.ITypeWrapper;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.SimpleSortedSuggestionProvider;
import endorh.simple_config.clothconfig2.impl.builders.ComboBoxFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.TextFieldBuilder;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.simple_config.clothconfig2.impl.builders.ComboBoxFieldBuilder.ofString;

public class StringEntry
  extends AbstractConfigEntry<String, String, String, StringEntry>
  implements IKeyEntry<String, String> {
	protected Supplier<List<String>> choiceSupplier;
	protected SupplierSuggestionProvider<String> suggestionProvider;
	protected boolean restrict;
	protected int maxLength;
	protected int minLength;
	
	@Internal public StringEntry(ISimpleConfigEntryHolder parent, String name, String value) {
		super(parent, name, value);
		suggestionProvider = new SupplierSuggestionProvider<>(
		  () -> choiceSupplier != null? choiceSupplier.get() : Lists.newArrayList());
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<String, String, String, StringEntry, Builder> {
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
		public Builder suggest(String... suggestions) {
			return suggest(Arrays.stream(suggestions).collect(Collectors.toList()));
		}
		
		/**
		 * Suggest possible values in a combo-box.<br>
		 * To restrict values to the suggestions, use {@link Builder#restrict} instead.<br>
		 * For suggestions, it's possible to provide instead a suggestion supplier,
		 * to provide dynamic suggestions instead. This is not possible with restrictions.
		 */
		public Builder suggest(@NotNull List<String> suggestions) {
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
		public Builder suggest(Supplier<List<String>> suggestionSupplier) {
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
		public Builder restrict(String first, String... choices) {
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
		public Builder restrict(@NotNull List<String> choices) {
			Builder copy = copy();
			if (choices.isEmpty())
				throw new IllegalArgumentException("At least one choice must be specified");
			Objects.requireNonNull(choices);
			copy.choiceSupplier = () -> choices;
			copy.restrict = true;
			return copy;
		}
		
		public Builder maxLength(@Range(from = 0, to = Integer.MAX_VALUE) int maxLength) {
			Builder copy = copy();
			copy.maxLength = maxLength;
			return copy;
		}
		
		public Builder minLength(@Range(from = 0, to = Integer.MAX_VALUE) int minLength) {
			Builder copy = copy();
			copy.minLength = minLength;
			return copy;
		}
		
		public Builder notEmpty() {
			return minLength(1);
		}
		
		@Override protected StringEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final StringEntry entry = new StringEntry(parent, name, value);
			if (value.length() > maxLength)
				throw new IllegalArgumentException(
				  "Config entry's default value is longer than its max length: " + entry.getPath());
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
	
	@Override public Optional<ITextComponent> supplyError(String value) {
		final Optional<ITextComponent> opt = super.supplyError(value);
		if (opt.isPresent()) return opt;
		if (value.length() < minLength)
			return Optional.of(
			  minLength == 1
			  ? new TranslationTextComponent("simple-config.config.error.string.empty")
			  : new TranslationTextComponent("simple-config.config.error.string.min_length", minLength));
		if (value.length() > maxLength)
			return Optional.of(new TranslationTextComponent("simple-config.config.error.string.max_length", maxLength));
		return Optional.empty();
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	@Override protected ForgeConfigSpec.Builder decorate(
	  ForgeConfigSpec.Builder builder
	) {
		builder = super.decorate(builder);
		if (choiceSupplier != null) {
			final List<String> choices = getChoices();
			if (!choices.isEmpty()) {
				int max = advanced.max_options_in_config_comment;
				String omittedSuffix =
				  choices.size() > max ? ", ... (omitted " + (choices.size() - max) + " more)" : "";
				if (restrict) {
					builder = builder.comment(
					  " Allowed values: " + choices.subList(0, max).stream()
						 .map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))
					  + omittedSuffix);
				} else {
					builder = builder.comment(
					  " Suggested values: " + choices.subList(0, max).stream()
						 .map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))
					  + omittedSuffix);
				}
			}
		}
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<String>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (choiceSupplier == null) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), get())
			  .setMaxLength(maxLength);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final ComboBoxFieldBuilder<String> valBuilder =
			  builder.startComboBox(getDisplayName(), ofString(), forGui(get()))
				 .setSuggestionMode(!restrict)
			    .setSuggestionProvider(suggestionProvider)
			    .setMaxLength(maxLength);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public Optional<String> deserializeStringKey(@NotNull String key) {
		return Optional.of(key);
	}
	
	public static class SupplierSuggestionProvider<T> extends SimpleSortedSuggestionProvider<T> {
		protected Supplier<List<T>> supplier;
		
		public SupplierSuggestionProvider(@NotNull Supplier<List<T>> supplier) {
			super(Lists.newArrayList());
			this.supplier = supplier;
		}
		
		public Supplier<List<T>> getSupplier() {
			return supplier;
		}
		
		public void setSupplier(Supplier<List<T>> supplier) {
			this.supplier = supplier;
		}
		
		@Override public Pair<List<T>, List<ITextComponent>> provideDecoratedSuggestions(
		  ITypeWrapper<T> typeWrapper, String query
		) {
			suggestions = supplier.get();
			return super.provideDecoratedSuggestions(typeWrapper, query);
		}
		
		@Override public List<T> provideSuggestions(ITypeWrapper<T> typeWrapper, String query) {
			suggestions = supplier.get();
			return super.provideSuggestions(typeWrapper, query);
		}
	}
}
