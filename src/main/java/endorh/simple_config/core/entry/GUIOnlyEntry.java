package endorh.simple_config.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class GUIOnlyEntry<V, Gui, Self extends GUIOnlyEntry<V, Gui, Self>>
  extends AbstractConfigEntry<V, Void, Gui, Self> {
	protected boolean addNonPersistentTooltip;
	
	public GUIOnlyEntry(
	  ISimpleConfigEntryHolder parent, String name, V value, Class<?> typeClass
	) { this(parent, name, value, true, typeClass); }
	
	public GUIOnlyEntry(
	  ISimpleConfigEntryHolder parent, String name, V value,
	  boolean addNonPersistentTooltip, Class<?> typeClass
	) {
		super(parent, name, value);
		this.addNonPersistentTooltip = addNonPersistentTooltip;
		this.typeClass = typeClass;
		
		nonPersistent = true;
		this.actualValue = value;
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
			nonPersistent = true;
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
	
	@Override protected final void set(ConfigValue<?> spec, V value) {
		set(value);
		bakeField();
	}
	
	@Override protected boolean canBeNested() {
		return false;
	}
	
	@Override protected List<ITextComponent> supplyExtraTooltip(Gui value) {
		return addNonPersistentTooltip? super.supplyExtraTooltip(value) : Lists.newArrayList();
	}
	
	@Override
	protected final void buildConfig(ForgeConfigSpec.Builder builder, Map<String, ConfigValue<?>> specValues) {
		specValues.put(name, null);
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {}
	
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
	
	@Override protected void put(CommentedConfig config, Void value) {}
	@Override protected Void get(CommentedConfig config) {
		return null;
	}
}
