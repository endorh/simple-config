package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.TextFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;

public class StringEntry
  extends AbstractConfigEntry<String, String, String, StringEntry> implements
                                                                   IAbstractStringKeyEntry<String> {
	@Internal public StringEntry(ISimpleConfigEntryHolder parent, String name, String value) {
		super(parent, name, value);
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<String, String, String, StringEntry, Builder> {
		
		public Builder(String value) {
			super(value, String.class);
		}
		
		@Override
		protected StringEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new StringEntry(parent, name, value);
		}
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, value, configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<String>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final TextFieldBuilder valBuilder = builder
		  .startTextField(getDisplayName(), get())
		  .setDefaultValue(value)
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
	
	@Override
	public String serializeStringKey(String key) {
		return key;
	}
	
	@Override
	public Optional<String> deserializeStringKey(String key) {
		return supplyError(key).isPresent()? Optional.empty() : Optional.of(key);
	}
	
	@Override
	public Optional<ITextComponent> stringKeyError(String key) {
		return supplyError(key);
	}
}
