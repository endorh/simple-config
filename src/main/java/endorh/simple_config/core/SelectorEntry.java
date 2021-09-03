package endorh.simple_config.core;

import com.google.common.collect.Lists;
import endorh.simple_config.SimpleConfigMod.ClientConfig.advanced;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.widget.ComboBoxWidget.ITypeWrapper;
import endorh.simple_config.clothconfig2.impl.builders.ComboBoxFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.SelectorBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SelectorEntry<V, C, G, E extends AbstractConfigEntry<V, C, G, E> & IKeyEntry<C, G>>
  extends AbstractConfigEntry<V, C, V, SelectorEntry<V, C, G, E>> implements IKeyEntry<C, G> {
	
	protected E entry;
	protected Function<V, ITextComponent> nameProvider;
	protected V[] choices;
	protected @Nullable Boolean useComboBox;
	
	protected SelectorEntry(
	  ISimpleConfigEntryHolder parent, String name, V value,
	  V[] choices, E entry
	) {
		super(parent, name, value);
		this.choices = choices;
		this.entry = entry;
	}
	
	public static class Builder<V, C, G, E extends AbstractConfigEntry<V, C, G, E> & IKeyEntry<C, G>>
	  extends AbstractConfigEntryBuilder<V, C, V, SelectorEntry<V, C, G, E>, Builder<V, C, G, E>> {
		
		protected final AbstractConfigEntryBuilder<V, C, ?, E, ?> builder;
		protected final V[] choices;
		protected Function<V, ITextComponent> nameProvider = null;
		protected @Nullable Boolean useComboBox = null;
		
		public Builder(AbstractConfigEntryBuilder<V, C, ?, E, ?> builder, V[] choices) {
			//noinspection deprecation
			super(
			  AbstractConfigEntryBuilder.getValue(builder),
			  commonSuperClass(choices = add(AbstractConfigEntryBuilder.getValue(builder), choices)));
			this.builder = builder.copy();
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
		
		public Builder<V, C, G, E> nameProvider(Function<V, ITextComponent> provider) {
			Builder<V, C, G, E> copy = copy();
			copy.nameProvider = provider;
			return copy;
		}
		
		public Builder<V, C, G, E> useComboBox() { return useComboBox(true); }
		
		public Builder<V, C, G, E> useComboBox(boolean comboBox) {
			Builder<V, C, G, E> copy = copy();
			copy.useComboBox = comboBox;
			return copy;
		}
		
		@Override protected SelectorEntry<V, C,G, E> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final E inner = DummyEntryHolder.build(parent, builder);
			final SelectorEntry<V, C, G, E> entry = new SelectorEntry<>(parent, name, value, choices, inner);
			entry.nameProvider =
			  nameProvider != null ? nameProvider :
			  v -> new StringTextComponent(String.valueOf(inner.forConfig(v)));
			entry.useComboBox = useComboBox;
			return entry;
		}
		
		@Override protected Builder<V, C, G, E> createCopy() {
			final Builder<V, C, G, E> copy = new Builder<>(builder, choices.clone());
			copy.nameProvider = nameProvider;
			copy.useComboBox = useComboBox;
			return copy;
		}
	}
	
	@Override public C forConfig(V value) {
		return entry.forConfig(value);
	}
	
	@Nullable @Override public V fromConfig(@Nullable C value) {
		return entry.fromConfig(value);
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(
	  ForgeConfigSpec.Builder builder
	) {
		return Optional.of(decorate(builder).define(name, forConfig(value), configValidator()));
	}
	
	@Override protected ForgeConfigSpec.Builder decorate(
	  ForgeConfigSpec.Builder builder
	) {
		builder = super.decorate(builder);
		final int max = advanced.max_options_in_config_comment;
		String omittedSuffix =
		  choices.length > max? ", ... (omitted " + (choices.length - max) + " more)" : "";
		Function<V, String> serializer = c -> entry.serializeStringKey(forConfig(c));
		builder.comment(
		  " Allowed values: " + Arrays.stream(ArrayUtils.subarray(choices, 0, max))
		    .map(c -> "\"" + serializer.apply(c) + "\"").collect(Collectors.joining(", "))
		  + omittedSuffix);
		return builder;
	}
	
	@Override public Optional<AbstractConfigListEntry<V>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (useComboBox != null? useComboBox : advanced.prefer_combo_box < choices.length) {
			final List<V> choices = Lists.newArrayList(this.choices);
			final ComboBoxFieldBuilder<V> valBuilder =
			  builder.startComboBox(getDisplayName(), new TypeWrapper<>(
					choices, v -> entry.serializeStringKey(entry.forConfig(v)),
					nameProvider), get()
				 ).setSuggestionMode(false)
				 .setSuggestions(choices);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final SelectorBuilder<V> valBuilder = builder
			  .startSelector(getDisplayName(), choices, get())
			  .setNameProvider(nameProvider);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	public static class TypeWrapper<V> implements ITypeWrapper<V> {
		protected List<V> choices;
		protected Function<V, String> nameProvider;
		protected Function<V, ITextComponent> formattedNameProvider;
		
		public TypeWrapper(
		  List<V> choices, Function<V, String> nameProvider,
		  Function<V, ITextComponent> formattedNameProvider
		) {
			this.choices = choices;
			this.nameProvider = nameProvider;
			this.formattedNameProvider = formattedNameProvider;
		}
		
		@Override public Pair<Optional<V>, Optional<ITextComponent>> parseElement(@NotNull String text) {
			final Optional<V> opt = choices.stream().filter(c -> text.equals(nameProvider.apply(c))).findFirst();
			Optional<ITextComponent> error = opt.isPresent()? Optional.empty() : Optional.of(new TranslationTextComponent(
			  "simple-config.config.error.unknown_value"));
			return Pair.of(opt, error);
		}
		
		@Override public ITextComponent getDisplayName(@NotNull V element) {
			return formattedNameProvider.apply(element);
		}
		
		@Override public String getName(@NotNull V element) {
			return nameProvider.apply(element);
		}
	}
	
	@Override public Optional<C> deserializeStringKey(@NotNull String key) {
		return entry.deserializeStringKey(key);
	}
	
	@Override public String serializeStringKey(@NotNull C key) {
		return entry.serializeStringKey(key);
	}
}
