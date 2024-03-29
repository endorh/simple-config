package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.api.ConfigCategory;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ConfigCategoryButton extends MultiFunctionIconButton {
	protected final SimpleConfigScreen screen;
	protected final ConfigCategory category;
	protected final Supplier<Optional<Component[]>> descriptionSupplier;
	
	private int lastColor;
	private int lastDark;
	private int lastLight;
	
	public ConfigCategoryButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y, Component title
	) {
		this(screen, category, x, y, title, null);
	}
	
	public ConfigCategoryButton(
	  SimpleConfigScreen screen, ConfigCategory category, int x, int y,
	  Component title, @Nullable Supplier<Optional<Component[]>> descriptionSupplier
	) {
		super(x, y, 20, 200, Icon.EMPTY, ButtonAction.of(() -> {
			if (category != null) screen.setSelectedCategory(category);
		}).icon(category.getIcon())
		  .title(() -> screen.isSelecting()? title.copy().append(
		    Component.literal(" [" + category.getAllMainEntries().stream().filter(
			   AbstractConfigField::isSelected).count() + "]").withStyle(ChatFormatting.AQUA)) : title)
		  .active(category::isLoaded));
		this.descriptionSupplier = descriptionSupplier;
		this.category = category;
		this.screen = screen;
		lastColor = category.getColor();
		updateColors();
	}
	
	@Override public List<Component> getTooltipContents() {
		if (descriptionSupplier != null && (!isSelected() || isHovered))
			return Lists.newArrayList(descriptionSupplier.get().orElse(new Component[0]));
		return Collections.emptyList();
	}
	
	private void updateColors() {
		int c = lastColor;
		lastDark = c == 0? 0x64242424 :
		           new Color(c, true).darker().darker().getRGB() & 0xFFFFFF
		           | (int) ((c >> 24 & 0xFF) * 0.6F) << 24;
		lastLight = c == 0? 0x32E0E0E0 : c;
	}
	
	@Override public Component getTitle() {
		Component title = super.getTitle();
		if (!isSelected()) title = title.copy().withStyle(s -> s.withColor(TextColor.fromRgb(0xAADBDBDB)));
		return title;
	}
	
	@Override public void renderWidget(
      @NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks
	) {
		int c = category.getColor();
		if (c != lastColor) {
			lastColor = c;
			updateColors();
		}
		if (isSelected()) {
			defaultTint = lastLight;
		} else defaultTint = lastDark;
		super.renderWidget(gg, mouseX, mouseY, partialTicks);
	}
	
	public boolean isSelected() {
		return screen.getSelectedCategory() == category;
	}
	
	@Override public boolean isHoveredOrFocused() {
		return super.isHoveredOrFocused() || isSelected();
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return super.isMouseOver(mouseX, mouseY) && mouseX >= 20.0 && mouseX <= screen.width - 20;
	}
}