package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public interface ConfigCategory extends IEntryHolder {
	ITextComponent getTitle();
	void setTitle(ITextComponent name);
	
	@Override @Internal List<AbstractConfigField<?>> getHeldEntries();
	
	String getName();
	ConfigCategory addEntry(AbstractConfigListEntry<?> entry);
	@Internal void removeEntry(String name);
	
	void setBackground(@Nullable ResourceLocation background);
	@Nullable ResourceLocation getBackground();
	EditType getType();
	
	boolean isEditable();
	void setEditable(boolean editable);
	
	@Nullable CompletableFuture<Boolean> getLoadingFuture();
	boolean isLoaded();
	
	int getColor();
	Icon getIcon();
	int getSortingOrder();
	
	@Nullable Supplier<Optional<ITextComponent[]>> getDescription();
	Optional<Path> getContainingFile();
	
	void finishLoadingEntries();
}
