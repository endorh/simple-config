package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class MultiFunctionImageButton extends ImageButton {
	public int u;
	public int v;
	protected ResourceLocation texture;
	protected int texWidth;
	protected int texHeight;
	protected BiPredicate<ImageButton, Integer> pressAction;
	protected Predicate<ImageButton> activePredicate = w -> true;
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, int u, int v,
	  ResourceLocation tex, BiPredicate<ImageButton, Integer> pressAction
	) {
		this(x, y, width, height, u, v, tex, 256, 256, pressAction);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, int u, int v,
	  ResourceLocation texture, int texWidth, int texHeight,
	  BiPredicate<ImageButton, Integer> pressAction
	) {
		this(x, y, width, height, u, v, texture, texWidth, texHeight, pressAction, NarratorChatListener.EMPTY);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, int u, int v,
	  ResourceLocation texture, int texWidth, int texHeight,
	  BiPredicate<ImageButton, Integer> pressAction, ITextComponent title
	) {
		this(x, y, width, height, u, v, texture, texWidth, texHeight, pressAction, field_238486_s_, title);
	}
	
	public MultiFunctionImageButton(
	  int x, int y, int width, int height, int u, int v,
	  ResourceLocation texture, int texWidth, int texHeight,
	  BiPredicate<ImageButton, Integer> pressAction, ITooltip tooltip, ITextComponent title
	) {
		super(
		  x, y, width, height, u, v, height, texture, texWidth, texHeight, b -> {}, tooltip, title);
		this.u = u;
		this.v = v;
		this.texture = texture;
		this.texWidth = texWidth;
		this.texHeight = texHeight;
		this.pressAction = pressAction;
	}
	
	public void setActivePredicate(Predicate<ImageButton> pred) {
		this.activePredicate = pred;
	}
	
	public boolean onPress(int button) {
		return pressAction.test(this, button);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible && clicked(mouseX, mouseY) && onPress(button)) {
			playDownSound(Minecraft.getInstance().getSoundHandler());
			return true;
		}
		return false;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getTextureManager().bindTexture(texture);
		this.active = activePredicate.test(this);
		int v = this.v;
		if (this.active) {
			v += this.height;
			if (this.isHovered())
				v += this.height;
		}
		
		RenderSystem.enableDepthTest();
		blit(mStack, x, y, (float)u, (float)v, width, height, texWidth, texHeight);
		if (this.isHovered())
			this.renderToolTip(mStack, mouseX, mouseY);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) { // !(Enter | Space | NumPadEnter)
				return false;
			} else {
				int button = Screen.hasControlDown() ? 2 : Screen.hasShiftDown() ? 1 : 0;
				if (onPress(button)) {
					this.playDownSound(Minecraft.getInstance().getSoundHandler());
					return true;
				} else if (button != 0 && onPress(0)) {
					this.playDownSound(Minecraft.getInstance().getSoundHandler());
					return true;
				}
			}
		}
		return false;
	}
}
