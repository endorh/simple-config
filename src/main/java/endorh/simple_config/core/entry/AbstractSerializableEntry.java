package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class AbstractSerializableEntry
  <V, Self extends AbstractSerializableEntry<V, Self>>
  extends AbstractConfigEntry<V, String, String, Self>
  implements IKeyEntry<String, String> {
	
	public AbstractSerializableEntry(
	  ISimpleConfigEntryHolder parent, String name, V value, Class<?> typeClass
	) {
		super(parent, name, value);
		this.typeClass = typeClass;
	}
	
	public static abstract class Builder<V, Entry extends AbstractSerializableEntry<V, Entry>,
	  Self extends Builder<V, Entry, Self>>
	  extends AbstractConfigEntryBuilder<V, String, String, Entry, Self> {
		
		public Builder(V value) {
			super(value, value.getClass());
		}
		
		public Builder(V value, Class<?> typeClass) {
			super(value, typeClass);
		}
	}
	
	protected abstract String serialize(V value);
	protected abstract @Nullable V deserialize(String value);
	
	@Override
	public String forGui(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override public V fromGui(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	@Override public String forConfig(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override public V fromConfig(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	protected Optional<ITextComponent> getErrorMessage(String value) {
		return Optional.of(new TranslationTextComponent(
		  "simple-config.config.error.invalid_value_generic"));
	}
	
	@Override
	public Optional<ITextComponent> supplyError(String value) {
		final Optional<ITextComponent> opt = super.supplyError(value);
		if (!opt.isPresent() && fromGui(value) == null && value != null) {
			return getErrorMessage(value);
		} else return opt;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(builder.define(name, forConfig(value), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<String>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final TextFieldBuilder valBuilder = builder
		  .startTextField(getDisplayName(), forGui(get()));
		return Optional.of(decorate(valBuilder).build());
	}
	
	@Override public Optional<String> deserializeStringKey(@NotNull String key) {
		return Optional.of(key);
	}
}
