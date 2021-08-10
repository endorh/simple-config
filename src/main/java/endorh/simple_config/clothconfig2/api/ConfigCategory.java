package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.api.AbstractConfigEntry.EntryError;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigCategory extends IEntryHolder {
	ITextComponent getTitle();
	
	void setTitle(ITextComponent name);
	
	@Internal List<AbstractConfigEntry<?>> getEntries();
	
	String getName();
	
	ConfigCategory addEntry(AbstractConfigListEntry<?> var1);
	
	ConfigCategory setCategoryBackground(ResourceLocation background);
	void setBackground(@Nullable ResourceLocation background);
	@Nullable ResourceLocation getBackground();
	
	boolean isServer();
	void setIsServer(boolean isServer);
	
	void setColor(int color);
	int getColor();
	
	int getSortingOrder();
	void setSortingOrder(int order);
	
	@Nullable Supplier<Optional<ITextProperties[]>> getDescription();
	void setDescription(@Nullable Supplier<Optional<ITextProperties[]>> var1);
	
	default void setDescription(@Nullable ITextProperties[] description) {
		this.setDescription(() -> Optional.ofNullable(description));
	}
	
	void removeCategory();
	
	default List<EntryError> getErrors() {
		return getEntries().stream().flatMap(e -> e.getErrors().stream())
		  .collect(Collectors.toCollection(LinkedList::new));
	}
	
	Optional<Path> getContainingFile();
	void setContainingFile(Path path);
}

