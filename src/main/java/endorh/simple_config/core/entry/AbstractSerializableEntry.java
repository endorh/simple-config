package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.TextFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class AbstractSerializableEntry
  <V, Self extends AbstractSerializableEntry<V, Self>>
  extends AbstractConfigEntry<V, String, String, Self> {
	
	public AbstractSerializableEntry(V value, Class<?> typeClass) {
		super(value, typeClass);
	}
	
	protected abstract String serialize(V value);
	protected abstract @Nullable V deserialize(String value);
	
	@Override
	protected String forGui(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override
	protected V fromGui(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	@Override
	protected String forConfig(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override
	protected V fromConfig(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	protected Optional<ITextComponent> getErrorMessage(String value) {
		return Optional.of(new TranslationTextComponent(
		  "simple-config.config.error.invalid_value_generic"));
	}
	
	@Override
	protected Optional<ITextComponent> supplyError(String value) {
		final Optional<ITextComponent> opt = super.supplyError(value);
		if (!opt.isPresent() && fromGui(value) == null) {
			return getErrorMessage(value);
		} else return opt;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(builder.define(name, forConfig(value), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<String>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final TextFieldBuilder valBuilder = builder
		  .startTextField(getDisplayName(), forGui(c.get(name)))
		  .setDefaultValue(forGui(value))
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
