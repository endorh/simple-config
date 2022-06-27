package endorh.simpleconfig.clothconfig2.gui;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigCategory;
import endorh.simpleconfig.clothconfig2.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.clothconfig2.gui.widget.MultiFunctionImageButton.ButtonAction;
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
	protected final ClothConfigScreen screen;
	protected final ConfigCategory category;
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, ConfigCategory category, int x, int y, ITextComponent title
	) {
		this(screen, category, x, y, title, null);
	}
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, ConfigCategory category, int x, int y,
	  ITextComponent title, @Nullable Supplier<Optional<ITextComponent[]>> descriptionSupplier
	) {
		super(x, y, 20, 200, Icon.EMPTY, ButtonAction.of(() -> {
			if (category != null) screen.setSelectedCategory(category);
		}).tint(category.getColor())
		  .tooltip(
			 descriptionSupplier != null
			 ? () -> Arrays.stream(descriptionSupplier.get().orElse(new ITextComponent[0]))
			   .collect(Collectors.toList())
			 : Collections::emptyList
		  ).active(() -> screen.selectedCategory != category)
		  .title(() -> screen.isSelecting() ? title.copy().append(new StringTextComponent(
		    " [" + category.getAllMainEntries().stream().filter(AbstractConfigEntry::isSelected).count() + "]"
		  ).withStyle(TextFormatting.AQUA)) : title));
		this.category = category;
		this.screen = screen;
	}
	
	public boolean isMouseOver(double mouseX, double mouseY) {
		return super.isMouseOver(mouseX, mouseY) && mouseX >= 20.0 && mouseX <= screen.width - 20;
	}
}