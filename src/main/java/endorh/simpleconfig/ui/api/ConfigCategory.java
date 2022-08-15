package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.core.SimpleConfig.EditType;
import endorh.simpleconfig.ui.gui.icon.Icon;
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
	
	@Override @Internal List<AbstractConfigEntry<?>> getHeldEntries();
	
	String getName();
	ConfigCategory addEntry(AbstractConfigListEntry<?> var1);
	
	void setBackground(@Nullable ResourceLocation background);
	@Nullable ResourceLocation getBackground();
	EditType getType();
	
	boolean isEditable();
	void setEditable(boolean editable);
	
	int getColor();
	Icon getIcon();
	int getSortingOrder();
	
	@Nullable Supplier<Optional<ITextComponent[]>> getDescription();
	Optional<Path> getContainingFile();
}

