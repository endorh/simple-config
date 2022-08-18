package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.ui.icon.Icon;
import endorh.simpleconfig.ui.impl.ConfigCategoryImpl;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public interface ConfigCategoryBuilder {
	EditType getType();
	String getName();
	
	ConfigCategoryBuilder addEntry(FieldBuilder<?, ?, ?> entry);
	ConfigCategoryBuilder addEntry(int index, FieldBuilder<?, ?, ?> entry);
	
	ITextComponent getTitle();
	void setTitle(ITextComponent title);
	
	int getSortingOrder();
	void setSortingOrder(int sortingOrder);
	
	@Nullable ResourceLocation getBackground();
	void setBackground(@Nullable ResourceLocation background);
	
	@Nullable Supplier<Optional<ITextComponent[]>> getDescription();
	void setDescription(
	  @Nullable Supplier<Optional<ITextComponent[]>> description
	);
	
	@Nullable Path getContainingFile();
	void setContainingFile(@Nullable Path containingFile);
	
	boolean isEditable();
	void setEditable(boolean editable);
	
	Icon getIcon();
	void setIcon(Icon icon);
	
	int getColor();
	void setColor(int color);
	
	ConfigCategoryImpl build();
}
