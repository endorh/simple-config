package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.api.ConfigCategoryBuilder;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder;
import endorh.simpleconfig.ui.impl.ConfigCategoryImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ConfigCategoryBuilderImpl implements ConfigCategoryBuilder {
	protected final ConfigScreenBuilder builder;
	protected final String name;
	protected EditType type;
	protected final List<FieldBuilder<?, ?, ?>> entries = new ArrayList<>();
	protected Component title;
	protected int sortingOrder;
	protected @Nullable ResourceLocation background;
	protected @Nullable Supplier<Optional<Component[]>> description = Optional::empty;
	protected @Nullable Path containingFile;
	protected boolean isEditable = true;
	protected Icon icon = Icon.EMPTY;
	protected int color = 0;
	protected @Nullable CompletableFuture<Function<ConfigCategory, Boolean>> loadingFuture = null;
	
	public ConfigCategoryBuilderImpl(ConfigScreenBuilder builder, String name, EditType type) {
		this.builder = builder;
		this.name = name;
		this.type = type;
	}
	
	@Override public String getName() {
		return name;
	}
	
	@Override public EditType getType() {
		return type;
	}
	
	@Override public ConfigCategoryBuilder addEntry(FieldBuilder<?, ?, ?> entry) {
		entries.add(entry);
		return this;
	}
	
	@Override public ConfigCategoryBuilder addEntry(int index, FieldBuilder<?, ?, ?> entry) {
		entries.add(index, entry);
		return this;
	}
	
	@Override public Component getTitle() {
		return title != null? title : Component.literal(name);
	}
	@Override public void setTitle(Component title) {
		this.title = title;
	}
	
	@Override public int getSortingOrder() {
		return sortingOrder;
	}
	@Override public void setSortingOrder(int sortingOrder) {
		this.sortingOrder = sortingOrder;
	}
	
	@Override public @Nullable ResourceLocation getBackground() {
		return background;
	}
	@Override public void setBackground(@Nullable ResourceLocation background) {
		this.background = background;
	}
	
	@Override public @Nullable Supplier<Optional<Component[]>> getDescription() {
		return description;
	}
	@Override public void setDescription(
	  @Nullable Supplier<Optional<Component[]>> description
	) {
		this.description = description;
	}
	
	@Override public @Nullable Path getContainingFile() {
		return containingFile;
	}
	@Override public void setContainingFile(@Nullable Path containingFile) {
		this.containingFile = containingFile;
	}
	
	@Override public boolean isEditable() {
		return isEditable;
	}
	@Override public void setEditable(boolean editable) {
		isEditable = editable;
	}
	
	@Override public @Nullable CompletableFuture<Function<ConfigCategory, Boolean>> getLoadingFuture() {
		return loadingFuture;
	}
	
	@Override public void setLoadingFuture(@Nullable CompletableFuture<Function<ConfigCategory, Boolean>> future) {
		loadingFuture = future;
	}
	
	@Override public Icon getIcon() {
		return icon;
	}
	@Override public void setIcon(Icon icon) {
		this.icon = icon;
	}
	
	@Override public int getColor() {
		return color;
	}
	@Override public void setColor(int color) {
		this.color = color;
	}
	
	@Override public ConfigCategoryImpl build() {
		List<AbstractConfigField<?>> builtEntries =
		  entries.stream().map(FieldBuilder::build).collect(Collectors.toList());
		return new ConfigCategoryImpl(
		  name, type, builtEntries, getTitle(), getSortingOrder(),
		  getBackground(), getDescription(), getContainingFile(), isEditable(),
		  getIcon(), getColor(), getLoadingFuture());
	}
}
