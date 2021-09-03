package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ConfigCategoryImpl implements ConfigCategory {
	private final ConfigBuilder builder;
	private final List<AbstractConfigEntry<?>> data;
	@Nullable
	private ResourceLocation background;
	private final ITextComponent categoryKey;
	@Nullable
	private Supplier<Optional<ITextProperties[]>> description = Optional::empty;
	private String name;
	
	ConfigCategoryImpl(ConfigBuilder builder, ITextComponent categoryKey) {
		this.builder = builder;
		this.data = Lists.newArrayList();
		this.categoryKey = categoryKey;
		this.name = categoryKey.getString();
	}
	
	@Override
	public ITextComponent getCategoryKey() {
		return this.categoryKey;
	}
	
	@Override
	public List<AbstractConfigEntry<?>> getEntries() {
		return this.data;
	}
	
	@Override public void setName(String name) {
		this.name = name;
	}
	
	@Override public String getName() {
		return name;
	}
	
	@Override
	public ConfigCategory addEntry(AbstractConfigListEntry<?> entry) {
		this.data.add(entry);
		return this;
	}
	
	@Override
	public ConfigCategory setCategoryBackground(ResourceLocation identifier) {
		if (this.builder.hasTransparentBackground()) {
			throw new IllegalStateException(
			  "Cannot set category background if screen is using transparent background.");
		}
		this.background = identifier;
		return this;
	}
	
	@Override
	public void removeCategory() {
		this.builder.removeCategory(this.categoryKey);
	}
	
	@Override
	public void setBackground(@Nullable ResourceLocation background) {
		this.background = background;
	}
	
	@Override
	@Nullable
	public ResourceLocation getBackground() {
		return this.background;
	}
	
	@Override
	@Nullable
	public Supplier<Optional<ITextProperties[]>> getDescription() {
		return this.description;
	}
	
	@Override
	public void setDescription(@Nullable Supplier<Optional<ITextProperties[]>> description) {
		this.description = description;
	}
}

