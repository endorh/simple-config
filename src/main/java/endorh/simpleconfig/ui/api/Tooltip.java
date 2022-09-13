package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.minecraftforge.fml.client.gui.GuiUtils.drawGradientRect;

public interface Tooltip {
	static Tooltip of(Point location, List<ITextComponent> text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, ITextComponent... text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, IReorderingProcessor... text) {
		return PointTooltip.create(location, text);
	}
	
	static Tooltip of(Rectangle area, Point point, List<ITextComponent> text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, List<ITextComponent> text) {
		return AreaTooltip.create(area, text);
	}
	
	static Tooltip of(Rectangle area, Point point, ITextComponent... text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, ITextComponent... text) {
		return AreaTooltip.create(area, text);
	}
	
	static Tooltip of(Rectangle area, Point point, IReorderingProcessor... text) {
		return AreaTooltip.create(area, point, text);
	}
	
	static Tooltip of(Rectangle area, IReorderingProcessor... text) {
		return AreaTooltip.create(area, text);
	}
	
	Point getPoint();
	
	default int getX() {
		return getPoint().getX();
	}
	default int getY() {
		return getPoint().getY();
	}
	FontRenderer getFont();
	boolean isFromKeyboard();
	default Tooltip asKeyboardTooltip() {
		return asKeyboardTooltip(true);
	}
	Tooltip asKeyboardTooltip(boolean fromKeyboard);
	Tooltip withFont(FontRenderer font);
	
	List<IReorderingProcessor> getText();
	
	/**
	 * Is called from {@link #render(Screen, MatrixStack)}.<br>
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
	
	default void render(Screen screen, MatrixStack mStack) {
		List<IReorderingProcessor> text = getText();
		if (text.isEmpty()) return;
		
		FontRenderer font = getFont();
		Minecraft mc = Minecraft.getInstance();
		
		int w = text.stream().mapToInt(font::func_243245_a).max().orElse(0);
		int h = text.size() * (font.FONT_HEIGHT + 1);
		
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
		
		mStack.push();
		Matrix4f m = mStack.getLast().getMatrix();
		// @formatter:off
		drawGradientRect(m, bo, x - 3,     y - 4,     x + w + 3, y - 3,         bgSt, bgSt);
		drawGradientRect(m, bo, x - 3,     y + h + 3, x + w + 3, y + h + 4,     bgEd, bgEd);
		drawGradientRect(m, bo, x - 3,     y - 3,     x + w + 3, y + h + 3,     bgSt, bgEd);
		drawGradientRect(m, bo, x - 4,     y - 3,     x - 3,     y + h + 3,     bgSt, bgEd);
		drawGradientRect(m, bo, x + w + 3, y - 3,     x + w + 4, y + h + 3,     bgSt, bgEd);
		drawGradientRect(m, bo, x - 3,     y - 3 + 1, x - 3 + 1, y + h + 3 - 1, bdSt, bdEd);
		drawGradientRect(m, bo, x + w + 2, y - 3 + 1, x + w + 3, y + h + 3 - 1, bdSt, bdEd);
		drawGradientRect(m, bo, x - 3,     y - 3,     x + w + 3, y - 3 + 1,     bdSt, bdEd);
		drawGradientRect(m, bo, x - 3,     y + h + 2, x + w + 3, y + h + 3,     bdEd, bdEd);
		// @formatter:on
		
		IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
		mStack.translate(0D, 0D, bo);
		
		int lineY = y;
		for (int l = 0; l < text.size(); l++) {
			IReorderingProcessor line = text.get(l);
			font.func_238416_a_(
			  line, x, lineY, 0xFFFFFFFF, true, m,
			  renderType, false, 0x0, 0xF000F0);
			lineY += font.FONT_HEIGHT + 1 + (l == 0? 2 : 0);
		}
		
		renderType.finish();
		mStack.pop();
	}
}

