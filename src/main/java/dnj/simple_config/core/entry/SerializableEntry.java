package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.TextFieldBuilder;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Doesn't have a GUI<br>
 * To create your custom type of entry with GUI,
 * extend this class or {@link AbstractConfigEntry} directly
 *
 * @param <V> Type of the value
 */
public class SerializableEntry<V>
  extends AbstractConfigEntry<V, String, String, SerializableEntry<V>> {
	public Function<V, String> serializer;
	public Function<String, Optional<V>> deserializer;
	
	public SerializableEntry(
	  V value, IConfigEntrySerializer<V> serializer
	) {
		this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
	}
	
	public SerializableEntry(
	  V value,
	  Function<V, String> serializer,
	  Function<String, Optional<V>> deserializer
	) {
		super(value);
		this.serializer = serializer;
		this.deserializer = deserializer;
	}
	
	@Override
	protected String forGui(V value) {
		return serializer.apply(value);
	}
	
	@Override
	protected @Nullable
	V fromGui(@Nullable String value) {
		return value != null ? deserializer.apply(value).orElse(null) : null;
	}
	
	@Override
	protected String forConfig(V value) {
		return serializer.apply(value);
	}
	
	@Override
	protected @Nullable
	V fromConfig(@Nullable String value) {
		return value != null ? deserializer.apply(value).orElse(null) : null;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.of(
		  builder.define(
			 name, serializer.apply(value),
			 s -> s instanceof String && deserializer.apply((String) s).isPresent()));
	}
	
	@Override
	protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
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
	
	public static class SerializableConfigEntry<T extends ISerializableConfigEntry<T>> extends SerializableEntry<T> {
		public SerializableConfigEntry(T value) {
			super(value, null, null);
			final IConfigEntrySerializer<T> serializer = value.getConfigSerializer();
			this.serializer = serializer::serializeConfigEntry;
			this.deserializer = serializer::deserializeConfigEntry;
		}
	}
}
