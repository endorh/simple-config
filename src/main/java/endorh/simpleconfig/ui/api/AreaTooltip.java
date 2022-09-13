package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AreaTooltip extends PointTooltip {
	private final Rectangle area;
	
	protected AreaTooltip(Rectangle area, Point point, List<IReorderingProcessor> text) {
		super(point, text);
		this.area = area;
	}
	
	protected static Point inferPoint(Rectangle area) {
		Screen screen = Minecraft.getInstance().currentScreen;
		if (screen == null) return Point.of(area.getMaxX(), area.getY());
		return Point.of(
		  area.getCenterX() > screen.width / 2? area.getX() : area.getMaxX(),
		  area.getCenterY() > screen.height / 2? area.getY() : area.getMaxY());
	}
	
	public static AreaTooltip create(Rectangle area, Point point, List<ITextComponent> text) {
		//noinspection unchecked
		return new AreaTooltip(area, point, LanguageMap.getInstance().func_244260_a(
		  (List<ITextProperties>) (List<?>) text));
	}
	
	public static AreaTooltip create(Rectangle area, List<ITextComponent> text) {
		return create(area, inferPoint(area), text);
	}
	
	public static AreaTooltip create(Rectangle area, Point point, ITextComponent... text) {
		return new AreaTooltip(area, point, LanguageMap.getInstance().func_244260_a(Arrays.asList(text)));
	}
	
	public static AreaTooltip create(Rectangle area, ITextComponent... text) {
		return create(area, inferPoint(area), text);
	}
	
	public static AreaTooltip create(Rectangle area, Point point, IReorderingProcessor... text) {
		return new AreaTooltip(area, point, Arrays.asList(text));
	}
	
	public static AreaTooltip create(Rectangle area, IReorderingProcessor... text) {
		return create(area, inferPoint(area), text);
	}
	
	public Rectangle getArea() {
		return area;
	}
	
	@Override public void adjustForScreen(int tooltipWidth, int tooltipHeight, int width, int height) {
		Point point = getPoint();
		Rectangle area = getArea();
		
		int enforcedMargin = 6;
		
		Point initial = point.clone();
		int defaultOffset = 12;
		if (point.x + tooltipWidth + defaultOffset < width - enforcedMargin || point.x < width / 2) {
			point.x += defaultOffset;
		} else point.x -= tooltipWidth + defaultOffset;
		if (point.y - tooltipHeight - defaultOffset > enforcedMargin || point.y > height / 2) {
			point.y -= tooltipHeight + defaultOffset;
		} else point.y += defaultOffset;
		
		if (point.x + tooltipWidth > width - enforcedMargin)
			point.x = width - tooltipWidth - enforcedMargin;
		if (point.x < area.getMaxX() + enforcedMargin) {
			point.x = max(enforcedMargin, area.getX() - tooltipWidth - defaultOffset);
			if (point.x + tooltipWidth > area.getX()) {
				point.x = initial.getX();
				if (point.x + tooltipWidth + defaultOffset < width - enforcedMargin || point.x < width / 2) {
					point.x += defaultOffset;
				} else point.x -= tooltipWidth + defaultOffset;
				if (point.x + tooltipWidth > width - enforcedMargin) {
					point.x = max(enforcedMargin, width - tooltipWidth - enforcedMargin);
				} else if (point.x < enforcedMargin) point.x = enforcedMargin;
				point.y = area.getY() - tooltipHeight - defaultOffset;
				if (point.y < enforcedMargin && area.getCenterY() < height / 2)
					point.y = area.getMaxY() + defaultOffset;
				point.y = max(enforcedMargin, min(height - tooltipHeight - enforcedMargin, point.y));
				return;
			}
		}
		
		if (point.y + tooltipHeight > height - enforcedMargin) {
			point.y = max(enforcedMargin, height - tooltipHeight - enforcedMargin);
		} else if (point.y < enforcedMargin) point.y = enforcedMargin;
	}
}
