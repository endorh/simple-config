package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Matrix4f;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;

import static endorh.simpleconfig.ui.gui.AbstractConfigScreen.fillGradient;
import static java.lang.Math.max;
import static java.lang.Math.min;

public interface Tooltip {
	static Tooltip of(Point location, List<Component> text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, Component... text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, FormattedCharSequence... text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Rectangle area, Point point, List<Component> text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, List<Component> text) {
		return AreaTooltip.create(area, text);
	}
	
	static Tooltip of(Rectangle area, Point point, Component... text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, Component... text) {
		return AreaTooltip.create(area, text);
	}
	
	static Tooltip of(Rectangle area, Point point, FormattedCharSequence... text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, FormattedCharSequence... text) {
		return AreaTooltip.create(area, text);
	}
	
	Point getPoint();
	
	default int getX() {
		return getPoint().getX();
	}
	default int getY() {
		return getPoint().getY();
	}
	Font getFont();
	boolean isFromKeyboard();
	default Tooltip asKeyboardTooltip() {
		return asKeyboardTooltip(true);
	}
	Tooltip asKeyboardTooltip(boolean fromKeyboard);
	Tooltip withFont(Font font);
	
	List<FormattedCharSequence> getText();
	
	/**
	 * Is called from {@link #render(Screen, PoseStack)}.<br>
	 * Not pure, can only be called once.
	 */
	@Internal default void adjustForScreen(
	  int tooltipWidth, int tooltipHeight, int width, int height
	) {
		Point point = getPoint();
		
		int enforcedMargin = 6;
		
		int defaultOffset = 12;
		if (point.x + tooltipWidth + defaultOffset < width - enforcedMargin || point.x < width / 2) {
			point.x += defaultOffset;
		} else point.x -= tooltipWidth + defaultOffset;
		if (point.y - tooltipHeight - defaultOffset > enforcedMargin || point.y > height / 2) {
			point.y -= tooltipHeight + defaultOffset;
		} else point.y += defaultOffset;
		
		if (point.x + tooltipWidth > width - enforcedMargin) {
			point.x = max(enforcedMargin, width - tooltipWidth - enforcedMargin);
		} else if (point.x < enforcedMargin) point.x = enforcedMargin;
		
		if (point.y + tooltipHeight > height - enforcedMargin) {
			point.y = max(enforcedMargin, height - tooltipHeight - enforcedMargin);
		} else if (point.y < enforcedMargin) point.y = enforcedMargin;
	}
	
	default void render(Screen screen, PoseStack mStack) {
		List<ClientTooltipComponent> text = getText().stream()
		  .map(ClientTooltipComponent::create).toList();
		if (text.isEmpty()) return;
		
		Font font = getFont();
		Minecraft mc = Minecraft.getInstance();
		ItemRenderer itemRenderer = mc.getItemRenderer();
		
		int w = text.stream().mapToInt(t -> t.getWidth(font)).max().orElse(0);
		int h = text.stream().mapToInt(ClientTooltipComponent::getHeight).sum();
		
		adjustForScreen(w, h, screen.width, screen.height);
		int x = getX();
		int y = getY();
		
		// Last resort clamp, prioritizing the top left corner to be visible
		x = max(0, min(screen.width - w, x));
		y = max(0, min(screen.height - h, y));
		
		int bgSt = 0xF0100010;
		int bgEd = bgSt;
		int bdSt = 0x505000FF;
		int bdEd = 0x5028007F;
		int bo = 400;
		float prevIRBlitOffset = itemRenderer.blitOffset;
		itemRenderer.blitOffset = 400F;
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bb = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		
		mStack.pushPose();
		Matrix4f m = mStack.last().pose();
		// @formatter:off
		fillGradient(m, bb, x - 3,     y - 4,     x + w + 3, y - 3,         bo, bgSt, bgSt);
		fillGradient(m, bb, x - 3,     y + h + 3, x + w + 3, y + h + 4,     bo, bgEd, bgEd);
		fillGradient(m, bb, x - 3,     y - 3,     x + w + 3, y + h + 3,     bo, bgSt, bgEd);
		fillGradient(m, bb, x - 4,     y - 3,     x - 3,     y + h + 3,     bo, bgSt, bgEd);
		fillGradient(m, bb, x + w + 3, y - 3,     x + w + 4, y + h + 3,     bo, bgSt, bgEd);
		fillGradient(m, bb, x - 3,     y - 3 + 1, x - 3 + 1, y + h + 3 - 1, bo, bdSt, bdEd);
		fillGradient(m, bb, x + w + 2, y - 3 + 1, x + w + 3, y + h + 3 - 1, bo, bdSt, bdEd);
		fillGradient(m, bb, x - 3,     y - 3,     x + w + 3, y - 3 + 1,     bo, bdSt, bdEd);
		fillGradient(m, bb, x - 3,     y + h + 2, x + w + 3, y + h + 3,     bo, bdEd, bdEd);
		// @formatter:on
		RenderSystem.enableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		bb.end();
		BufferUploader.end(bb);
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();
		BufferSource bSrc = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		
		mStack.translate(0D, 0D, bo);
		int lineY = y;
		for (int l = 0; l < text.size(); l++) {
			ClientTooltipComponent line = text.get(l);
			line.renderText(font, x, lineY, m, bSrc);
			lineY += line.getHeight() + (l == 0? 2 : 0);
		}
		bSrc.endBatch();
		mStack.popPose();
		
		lineY = y;
		TextureManager tm = mc.getTextureManager();
		for (int l = 0; l < text.size(); ++l) {
			ClientTooltipComponent line = text.get(l);
			line.renderImage(font, x, lineY, mStack, itemRenderer, 400, tm);
			lineY += line.getHeight() + (l == 0? 2 : 0);
		}
		itemRenderer.blitOffset = prevIRBlitOffset;
	}
}

