package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class GUIOnlyEntry<V, Gui, Self extends GUIOnlyEntry<V, Gui, Self>>
  extends AbstractConfigEntry<V, Void, Gui, Self> {
	protected V actualValue;
	protected boolean addNonPersistentTooltip;
	
	public GUIOnlyEntry(
	  ISimpleConfigEntryHolder parent, String name, V value, Class<?> typeClass
	) { this(parent, name, value, true, typeClass); }
	
	public GUIOnlyEntry(
	  ISimpleConfigEntryHolder parent, String name, V value,
	  boolean addNonPersistentTooltip, Class<?> typeClass
	) {
		super(parent, name, value);
		this.actualValue = value;
		this.addNonPersistentTooltip = addNonPersistentTooltip;
		this.typeClass = typeClass;
	}
	
	public static abstract class Builder<V, Gui,
	  Entry extends GUIOnlyEntry<V, Gui, Entry>,
	  Self extends Builder<V, Gui, Entry, Self>>
	  extends AbstractConfigEntryBuilder<V, Void, Gui, Entry, Self> {
		public Builder(V value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		@Override
		protected Entry build(ISimpleConfigEntryHolder parent, String name) {
			/*if (parent.getRoot().type != Type.CLIENT)
				throw new IllegalArgumentException(
				  "Attempt to declare non persistent config entry in a non-client config");*/
			return super.build(parent, name);
		}
	}
	
	@Override public Void forConfig(V value) {
		return null;
	}
	
	@Override public V fromConfig(@Nullable Void value) {
		return null;
	}
	
	@Override
	protected final V get(ConfigValue<?> spec) {
		return get();
	}
	
	@Override
	protected final void set(ConfigValue<?> spec, V value) {
		set(value);
		bakeField();
	}
	
	protected V get() {
		return actualValue;
	}
	protected void set(V value) {
		this.actualValue = value;
	}
	
	@Override
	protected boolean canBeNested() {
		return false;
	}
	
	@Override
	protected Optional<ITextComponent[]> supplyTooltip(Gui value) {
		if (addNonPersistentTooltip)
			return Optional.of(super.supplyTooltip(value).map(tooltip -> {
				final List<ITextComponent> t = Arrays.stream(tooltip).collect(Collectors.toList());
				t.add(new TranslationTextComponent(
				  "simple-config.config.help.not_persistent_entry"
				).mergeStyle(TextFormatting.GRAY));
				return t.toArray(new ITextComponent[0]);
			}).orElse(new ITextComponent[]{new TranslationTextComponent(
			  "simple-config.config.help.not_persistent_entry"
			).mergeStyle(TextFormatting.GRAY)}));
		else return super.supplyTooltip(value);
	}
	
	@Override
	protected final void buildConfig(ForgeConfigSpec.Builder builder, Map<String, ConfigValue<?>> specValues) {
		specValues.put(name, null);
	}
	
	@Override
	protected final Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	@Override
	protected Consumer<Gui> saveConsumer() {
		return g -> {
			dirty();
			parent.markDirty().set(name, fromGuiOrDefault(g));
		};
	}
}
