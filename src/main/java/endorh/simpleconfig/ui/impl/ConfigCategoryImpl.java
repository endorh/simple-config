package endorh.simpleconfig.ui.impl;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigBuilder;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ConfigCategoryImpl implements ConfigCategory {
	protected final ConfigBuilder builder;
	protected final List<AbstractConfigEntry<?>> entries;
	protected final String name;
	protected ITextComponent title;
	protected int sortingOrder = 0;
	protected @Nullable ResourceLocation background;
	protected @Nullable Supplier<Optional<ITextComponent[]>> description = Optional::empty;
	protected @Nullable Path containingFile;
	protected boolean isServer;
	protected Icon icon = Icon.EMPTY;
	protected int color = 0;
	
	ConfigCategoryImpl(ConfigBuilder builder, String name, boolean isServer) {
		this.builder = builder;
		this.entries = Lists.newArrayList();
		this.name = name;
		this.title = new StringTextComponent(name);
		this.isServer = isServer;
	}
	
	@Override public ITextComponent getTitle() {
		return this.title;
	}
	@Override public void setTitle(ITextComponent name) {
		this.title = name;
	}
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
		return this.entries;
	}
	
	@Override public String getName() {
		return name;
	}
	
	@Override public ConfigCategory addEntry(AbstractConfigListEntry<?> entry) {
		this.entries.add(entry);
		return this;
	}
	@Override public ConfigCategory setCategoryBackground(ResourceLocation background) {
		if (this.builder.hasTransparentBackground()) throw new IllegalStateException(
		  "Cannot set category background if screen is using transparent background.");
		this.background = background;
		return this;
	}
	@Override public void removeCategory() {
		this.builder.removeCategory(this.name, isServer);
	}
	
	@Override public Optional<Path> getContainingFile() {
		return Optional.ofNullable(containingFile);
	}
	
	@Override public void setContainingFile(@Nullable Path file) {
		containingFile = file;
	}
	
	@Override public @Nullable ResourceLocation getBackground() {
		return this.background;
	}
	@Override public void setBackground(@Nullable ResourceLocation background) {
		this.background = background;
	}
	
	@Override public boolean isServer() {
		return isServer;
	}
	
	@Override public void setColor(int color) {
		this.color = color;
	}
	@Override public int getColor() {
		return color;
	}
	
	@Override public void setIcon(Icon icon) {
		this.icon = icon;
	}
	@Override public Icon getIcon() {
		return icon;
	}
	
	@Override public int getSortingOrder() {
		return sortingOrder;
	}
	@Override public void setSortingOrder(int order) {
		sortingOrder = order;
	}
	
	@Override public @Nullable Supplier<Optional<ITextComponent[]>> getDescription() {
		return this.description;
	}
	@Override public void setDescription(@Nullable Supplier<Optional<ITextComponent[]>> description) {
		this.description = description;
	}
}

