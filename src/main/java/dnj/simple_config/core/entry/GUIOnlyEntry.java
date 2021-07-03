package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class GUIOnlyEntry<V, Gui, Self extends GUIOnlyEntry<V, Gui, Self>>
  extends AbstractConfigEntry<V, Void, Gui, Self> {
	public GUIOnlyEntry(V value, Class<?> typeClass) {
		super(value, typeClass);
	}
	
	@Override protected Void forConfig(V value) {
		return null;
	}
	
	@Override protected V fromConfig(@Nullable Void value) {
		return null;
	}
	
	@Override
	protected final V get(ConfigValue<?> spec) {
		return get();
	}
	
	@Override
	protected final void set(ConfigValue<?> spec, V value) {
		set(value);
	}
	
	protected abstract V get();
	protected abstract void set(V value);
	
	@Override
	protected boolean canBeNested() {
		return false;
	}
	
	@Override
	protected Optional<ITextComponent[]> supplyTooltip(Gui value) {
		return Optional.of(super.supplyTooltip(value).map(tooltip -> {
			final List<ITextComponent> t = Arrays.stream(tooltip).collect(Collectors.toList());
			t.add(new TranslationTextComponent(
			  "simple-config.config.help.not_persistent_entry"
			).mergeStyle(TextFormatting.GRAY));
			return t.toArray(new ITextComponent[0]);
		}).orElse(new ITextComponent[]{new TranslationTextComponent(
		  "simple-config.config.help.not_persistent_entry"
		).mergeStyle(TextFormatting.GRAY)}));
	}
	
	@Override
	protected final void buildConfig(Builder builder, Map<String, ConfigValue<?>> specValues) {
		if (parent.getRoot().type != Type.CLIENT) {
			throw new IllegalArgumentException(
			  "Attempt to declare non persistent config entry " + getPath() + " on a server config");
		}
		specValues.put(name, null);
	}
	
	@Override
	protected final Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		return Optional.empty();
	}
	
	@Override
	protected Consumer<Gui> saveConsumer(ISimpleConfigEntryHolder c) {
		return g -> {
			markDirty();
			c.markDirty().set(name, fromGuiOrDefault(g));
		};
	}
}
