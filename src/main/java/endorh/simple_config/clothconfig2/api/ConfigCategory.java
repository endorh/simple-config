package endorh.simple_config.clothconfig2.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigCategory extends IEntryHolder {
	ITextComponent getTitle();
	
	void setTitle(ITextComponent name);
	
	@Internal List<AbstractConfigEntry<?>> getEntries();
	
	String getName();
	
	ConfigCategory addEntry(AbstractConfigListEntry<?> var1);
	
	ConfigCategory setCategoryBackground(ResourceLocation var1);
	
	void setBackground(@Nullable ResourceLocation var1);
	
	@Nullable ResourceLocation getBackground();
	
	int getSortingOrder();
	
	void setSortingOrder(int order);
	
	@Nullable Supplier<Optional<ITextProperties[]>> getDescription();
	
	void setDescription(@Nullable Supplier<Optional<ITextProperties[]>> var1);
	
	default void setDescription(@Nullable ITextProperties[] description) {
		this.setDescription(() -> Optional.ofNullable(description));
	}
	
	void removeCategory();
}

