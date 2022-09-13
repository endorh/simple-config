package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AreaTooltip extends PointTooltip {
	private final Rectangle area;
	
	protected AreaTooltip(Rectangle area, Point point, List<FormattedCharSequence> text) {
		super(point, text);
		this.area = area;
	}
	
	protected static Point inferPoint(Rectangle area) {
		Screen screen = Minecraft.getInstance().screen;
		if (screen == null) return Point.of(area.getMaxX(), area.getY());
		return Point.of(
		  area.getCenterX() > screen.width / 2? area.getX() : area.getMaxX(),
		  area.getCenterY() > screen.height / 2? area.getY() : area.getMaxY());
	}
	
	public static AreaTooltip create(Rectangle area, Point point, List<Component> text) {
		//noinspection unchecked
		return new AreaTooltip(area, point, Language.getInstance().getVisualOrder(
		  (List<FormattedText>) (List<?>) text));
	}
	
	public static AreaTooltip create(Rectangle area, List<Component> text) {
		return create(area, inferPoint(area), text);
	}
	
	public static AreaTooltip create(Rectangle area, Point point, Component... text) {
		return new AreaTooltip(area, point, Language.getInstance().getVisualOrder(Arrays.asList(text)));
	}
	
	public static AreaTooltip create(Rectangle area, Component... text) {
		return create(area, inferPoint(area), text);
	}
	
	public static AreaTooltip create(Rectangle area, Point point, FormattedCharSequence... text) {
		return new AreaTooltip(area, point, Arrays.asList(text));
	}
	
	public static AreaTooltip create(Rectangle area, FormattedCharSequence... text) {
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
