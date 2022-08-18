package endorh.simpleconfig.core.entry;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.api.entry.GUIOnlyEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class GUIOnlyEntry<V, Gui, Self extends GUIOnlyEntry<V, Gui, Self>>
  extends AbstractConfigEntry<V, Void, Gui> {
	private static final Logger LOGGER = LogManager.getLogger();
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
	  Self extends GUIOnlyEntryBuilder<V, Gui, Self>,
	  SelfImpl extends Builder<V, Gui, Entry, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<V, Void, Gui, Entry, Self, SelfImpl>
	  implements GUIOnlyEntryBuilder<V, Gui, Self> {
		public Builder(V value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		@Override
		protected Entry build(@NotNull ISimpleConfigEntryHolder parent, String name) {
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
	
	@Override protected List<ITextComponent> addExtraTooltip(Gui value) {
		return addNonPersistentTooltip ? super.addExtraTooltip(value) : Lists.newArrayList();
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {}
	
	@Override
	protected final Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.empty();
	}
	
	@Override
	protected Consumer<Gui> createSaveConsumer() {
		return g -> {
			dirty();
			if (!trySet(fromGuiOrDefault(g)))
				LOGGER.warn("Unexpected error saving config entry \"" + getGlobalPath() + "\"");
		};
	}
	
	@Override protected void put(CommentedConfig config, Void value) {}
	@Override protected Void get(CommentedConfig config) {
		return null;
	}
}
