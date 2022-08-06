package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.ui.api.AbstractConfigEntry;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigTabButton extends MultiFunctionIconButton {
	protected final SimpleConfigScreen screen;
	protected final ConfigCategory category;
	
	public ClothConfigTabButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y, ITextComponent title
	) {
		this(screen, category, x, y, title, null);
	}
	
	public ClothConfigTabButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y,
	  ITextComponent title, @Nullable Supplier<Optional<ITextComponent[]>> descriptionSupplier
	) {
		super(x, y, 20, 200, Icon.EMPTY, ButtonAction.of(() -> {
			if (category != null) screen.setSelectedCategory(category);
		}).tint(category.getColor())
		  .icon(category.getIcon())
		  .tooltip(
			 descriptionSupplier != null
			 ? () -> Arrays.stream(descriptionSupplier.get().orElse(new ITextComponent[0]))
			   .collect(Collectors.toList())
			 : Collections::emptyList
		  ).active(() -> screen.selectedCategory != category)
		  .title(() -> screen.isSelecting() ? title.deepCopy().append(new StringTextComponent(
		    " [" + category.getAllMainEntries().stream().filter(AbstractConfigEntry::isSelected).count() + "]"
		  ).mergeStyle(TextFormatting.AQUA)) : title));
		this.category = category;
		this.screen = screen;
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return super.isMouseOver(mouseX, mouseY) && mouseX >= 20.0 && mouseX <= screen.width - 20;
	}
}