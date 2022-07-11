package endorh.simpleconfig.ui.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigCategory extends IEntryHolder {
	ITextComponent getTitle();
	
	void setTitle(ITextComponent name);
	
	@Internal List<AbstractConfigEntry<?>> getHeldEntries();
	
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
	
	@Nullable Supplier<Optional<ITextComponent[]>> getDescription();
	void setDescription(@Nullable Supplier<Optional<ITextComponent[]>> var1);
	
	default void setDescription(@Nullable ITextComponent[] description) {
		this.setDescription(() -> Optional.ofNullable(description));
	}
	
	void removeCategory();
	
	Optional<Path> getContainingFile();
	void setContainingFile(Path path);
}

