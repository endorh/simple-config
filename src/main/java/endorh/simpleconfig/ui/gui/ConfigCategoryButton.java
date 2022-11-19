package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ConfigCategoryButton extends MultiFunctionIconButton {
	protected final SimpleConfigScreen screen;
	protected final ConfigCategory category;
	protected final Supplier<Optional<ITextComponent[]>> descriptionSupplier;
	
	private int lastColor = 0;
	private int lastDark;
	private int lastLight;
	
	public ConfigCategoryButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y, ITextComponent title
	) {
		this(screen, category, x, y, title, null);
	}
	
	public ConfigCategoryButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y,
	  ITextComponent title, @Nullable Supplier<Optional<ITextComponent[]>> descriptionSupplier
	) {
		super(x, y, 20, 200, Icon.EMPTY, ButtonAction.of(() -> {
			if (category != null) screen.setSelectedCategory(category);
		}).icon(category.getIcon())
		  .title(() -> screen.isSelecting() ? title.deepCopy().append(new StringTextComponent(
		    " [" + category.getAllMainEntries().stream().filter(
			   AbstractConfigField::isSelected).count() + "]"
		  ).mergeStyle(TextFormatting.AQUA)) : title)
		  .active(category::isLoaded));
		this.descriptionSupplier = descriptionSupplier;
		this.category = category;
		this.screen = screen;
		lastColor = category.getColor();
		updateColors();
	}
	
	@Override public List<ITextComponent> getTooltip() {
		if (descriptionSupplier != null && (!isSelected() || isHovered))
			return Lists.newArrayList(descriptionSupplier.get().orElse(new ITextComponent[0]));
		return Collections.emptyList();
	}
	
	private void updateColors() {
		int c = lastColor;
		lastDark = c == 0? 0x64242424 :
		           new java.awt.Color(c, true).darker().darker().getRGB() & 0xFFFFFF
		           | (int) ((c >> 24 & 0xFF) * 0.6F) << 24;
		lastLight = c == 0? 0x32E0E0E0 : c;
	}
	
	@Override public ITextComponent getTitle() {
		ITextComponent title = super.getTitle();
		if (!isSelected()) title = title.deepCopy().modifyStyle(s -> s.setColor(Color.fromInt(0xAADBDBDB)));
		return title;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		int c = category.getColor();
		if (c != lastColor) {
			lastColor = c;
			updateColors();
		}
		if (isSelected()) {
			defaultTint = lastLight;
		} else defaultTint = lastDark;
		super.renderButton(mStack, mouseX, mouseY, partialTicks);
	}
	
	public boolean isSelected() {
		return screen.getSelectedCategory() == category;
	}
	
	@Override public boolean isHovered() {
		return super.isHovered() || isSelected();
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return super.isMouseOver(mouseX, mouseY) && mouseX >= 20.0 && mouseX <= screen.width - 20;
	}
}