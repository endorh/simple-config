package endorh.simpleconfig.ui.impl;

import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ConfigCategoryImpl implements ConfigCategory {
	protected final List<AbstractConfigField<?>> entries;
	protected final String name;
	protected Component title;
	protected int sortingOrder = 0;
	protected @Nullable ResourceLocation background;
	protected @Nullable Supplier<Optional<Component[]>> description = Optional::empty;
	protected @Nullable Path containingFile;
	protected EditType type;
	protected boolean isEditable = true;
	protected Icon icon = Icon.EMPTY;
	protected int color = 0;
	
	@Internal public ConfigCategoryImpl(
	  String name, EditType type, List<AbstractConfigField<?>> entries,
	  Component title, int sortingOrder, @Nullable ResourceLocation background,
	  @Nullable Supplier<Optional<Component[]>> description, @Nullable Path containingFile,
	  boolean isEditable, Icon icon, int color
	) {
		this.entries = entries;
		this.name = name;
		this.title = title;
		this.type = type;
		this.sortingOrder = sortingOrder;
		this.background = background;
		this.description = description;
		this.containingFile = containingFile;
		this.isEditable = isEditable;
		this.icon = icon;
		this.color = color;
	}
	
	@Override public Component getTitle() {
		return title;
	}
	@Override public void setTitle(Component name) {
		title = name;
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return entries;
	}
	
	@Override public String getName() {
		return name;
	}
	
	@Override public ConfigCategory addEntry(AbstractConfigListEntry<?> entry) {
		entries.add(entry);
		return this;
	}
	
	@Override public Optional<Path> getContainingFile() {
		return Optional.ofNullable(containingFile);
	}
	
	@Override public @Nullable ResourceLocation getBackground() {
		return background;
	}
	@Override public void setBackground(@Nullable ResourceLocation background) {
		this.background = background;
	}
	
	@Override public EditType getType() {
		return type;
	}
	
	@Override public boolean isEditable() {
		return isEditable;
	}
	
	@Override public void setEditable(boolean editable) {
		isEditable = editable;
	}
	
	@Override public int getColor() {
		return color;
	}
	
	@Override public Icon getIcon() {
		return icon;
	}
	
	@Override public int getSortingOrder() {
		return sortingOrder;
	}
	
	@Override public @Nullable Supplier<Optional<Component[]>> getDescription() {
		return description;
	}
}

