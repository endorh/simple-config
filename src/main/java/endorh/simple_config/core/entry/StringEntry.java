package endorh.simple_config.core.entry;

import com.google.common.collect.Lists;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.TextFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StringEntry
  extends AbstractConfigEntry<String, String, String, StringEntry>
  implements IAbstractStringKeyEntry<String> {
	protected List<String> choices;
	protected boolean restrict;
	
	@Internal public StringEntry(ISimpleConfigEntryHolder parent, String name, String value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<String, String, String, StringEntry, Builder> {
		protected List<String> choices = null;
		protected boolean restrict = false;
		public Builder(String value) {
			super(value, String.class);
		}
		
		public Builder suggest(String... suggestions) {
			return suggest(Arrays.stream(suggestions).collect(Collectors.toList()));
		}
		
		public Builder suggest(List<String> suggestions) {
			this.choices = suggestions;
			restrict = false;
			return self();
		}
		
		public Builder restrict(String first, String... choices) {
			return restrict(Lists.newArrayList(ArrayUtils.insert(0, choices, first)));
		}
		
		public Builder restrict(List<String> choices) {
			if (choices.isEmpty())
				throw new IllegalArgumentException("At least one choice must be specified");
			this.choices = choices;
			restrict = true;
			return self();
		}
		
		@Override protected StringEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final StringEntry entry = new StringEntry(parent, name, value);
			entry.choices = choices;
			entry.restrict = restrict;
			return entry;
		}
	}
	
	@Nullable @Override protected String fromConfig(@Nullable String value) {
		if (value == null)
			return null;
		if (choices != null && restrict && !choices.contains(value))
			return null;
		return value;
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	@Override protected ForgeConfigSpec.Builder decorate(
	  ForgeConfigSpec.Builder builder
	) {
		builder = super.decorate(builder);
		if (choices != null && restrict) {
			builder = builder.comment(" Allowed values: " + choices.stream()
			  .map(s -> "\"" + s + "\"")
			  .collect(Collectors.joining(", ")));
		}
		return builder;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<String>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (choices == null) {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final DropdownMenuBuilder<String> valBuilder =
			  builder.startStringDropdownMenu(getDisplayName(), get())
			    .setSelections(choices)
			    .setSuggestionMode(!restrict)
				 .setDefaultValue(value)
				 .setSaveConsumer(saveConsumer())
				 .setTooltipSupplier(this::supplyTooltip)
				 .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public String serializeStringKey(String key) {
		return key;
	}
	
	@Override public Optional<String> deserializeStringKey(String key) {
		return supplyError(key).isPresent()? Optional.empty() : Optional.of(key);
	}
	
	@Override public Optional<ITextComponent> stringKeyError(String key) {
		return supplyError(key);
	}
}
