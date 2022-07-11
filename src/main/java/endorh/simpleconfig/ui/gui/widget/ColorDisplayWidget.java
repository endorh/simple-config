package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorDisplayWidget extends Widget {
	protected static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	
	protected TextFieldWidgetEx textFieldWidget;
	protected int color;
	protected int size;
	@Nullable public Runnable onClick = null;
	
	public ColorDisplayWidget(TextFieldWidgetEx textFieldWidget, int x, int y, int size, int color) {
		super(x, y, size, size, NarratorChatListener.NO_TITLE);
		this.textFieldWidget = textFieldWidget;
		this.color = color;
		this.size = size;
	}
	
	@Override public void renderButton(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		this.fillGradient(
		  mStack, this.x, this.y, this.x + this.size, this.y + this.size,
		  this.textFieldWidget.isFocused() ? -1 : -6250336,
		  this.textFieldWidget.isFocused() ? -1 : -6250336);
		Minecraft.getInstance().getTextureManager().bind(CONFIG_TEX);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		SimpleConfigIcons.CHESS_BOARD.renderStretch(mStack, x + 1, y + 1, size - 2, size - 2);
		fillGradient(mStack, x + 1, y + 1, x + size - 1, y + size - 1, color, color);
	}
	
	@Override public void onClick(double mouseX, double mouseY) {
		if (onClick != null)
			onClick.run();
	}
	
	@Override public boolean changeFocus(boolean focus) {
		return false;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
}

