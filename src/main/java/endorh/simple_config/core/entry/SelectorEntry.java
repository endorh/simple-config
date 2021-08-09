package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.DummyEntryHolder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SelectorBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SelectorEntry<V> extends AbstractConfigEntry<V, String, V, SelectorEntry<V>> {
	
	protected IAbstractStringKeyEntry<V> entry;
	protected Function<V, ITextComponent> nameProvider;
	protected V[] choices;
	
	protected SelectorEntry(
	  ISimpleConfigEntryHolder parent, String name, V value,
	  V[] choices, IAbstractStringKeyEntry<V> entry
	) {
		super(parent, name, value);
		this.choices = choices;
		this.entry = entry;
	}
	
	public static class Builder<V, E extends AbstractConfigEntry<V, ?, ?, E> & IAbstractStringKeyEntry<V>>
	  extends AbstractConfigEntryBuilder<V, String, V, SelectorEntry<V>, Builder<V, E>> {
		
		protected final AbstractConfigEntryBuilder<V, ?, ?, E, ?> builder;
		protected final V[] choices;
		protected Function<V, ITextComponent> nameProvider = null;
		
		public Builder(AbstractConfigEntryBuilder<V, ?, ?, E, ?> builder, V[] choices) {
			//noinspection deprecation
			super(
			  AbstractConfigEntryBuilder.getValue(builder),
			  commonSuperClass(choices = add(AbstractConfigEntryBuilder.getValue(builder), choices)));
			this.builder = builder;
			this.choices = choices;
		}
		
		protected static <V> V[] add(V value, V[] choices) {
			return ArrayUtils.contains(choices, value) ? choices :
			       ArrayUtils.insert(0, choices, value);
		}
		
		protected static Class<?> commonSuperClass(Object[]... objects) {
			Class<?> cls = null;
			for (Object obj : objects) {
				if (cls != null) {
					if (cls.isAssignableFrom(obj.getClass()))
						continue;
					else if (!obj.getClass().isAssignableFrom(cls))
						return null;
				}
				cls = obj.getClass();
			}
			return cls;
		}
		
		public Builder<V, E> nameProvider(Function<V, ITextComponent> provider) {
			this.nameProvider = provider;
			return self();
		}
		
		@Override protected SelectorEntry<V> buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final E str = DummyEntryHolder.build(parent, builder);
			final SelectorEntry<V> entry = new SelectorEntry<>(parent, name, value, choices, str);
			entry.nameProvider =
			  nameProvider != null ? nameProvider :
			  v -> new StringTextComponent(str.serializeStringKey(v));
			return entry;
		}
	}
	
	@Override protected String forConfig(V value) {
		return entry.serializeStringKey(value);
	}
	
	@Nullable @Override protected V fromConfig(@Nullable String value) {
		return value == null ? null :
		       entry.deserializeStringKey(value)
		         .filter(v -> Arrays.asList(choices).contains(v))
		         .orElse(null);
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(
	  ForgeConfigSpec.Builder builder
	) {
		return Optional.of(
		  decorate(builder)
		    .comment(" Allowed values: " + Arrays.stream(choices)
		      .map(c -> "\"" + forConfig(c) + "\"")
		      .collect(Collectors.joining(", ")))
		    .define(name, forConfig(value), configValidator()));
	}
	
	@Override public Optional<AbstractConfigListEntry<V>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final SelectorBuilder<V> valBuilder = builder
		  .startSelector(getDisplayName(), choices, get())
			 .setDefaultValue(value)
			 .setNameProvider(nameProvider)
			 .setSaveConsumer(saveConsumer())
			 .setErrorSupplier(this::supplyError)
			 .setTooltipSupplier(this::supplyTooltip);
		return Optional.of(decorate(valBuilder).build());
	}
}
