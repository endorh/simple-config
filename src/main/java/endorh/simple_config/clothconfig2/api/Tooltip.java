package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.math.Point;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;

import java.util.List;

public interface Tooltip {
	static Tooltip of(Point location, ITextComponent... text) {
		return QueuedTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, ITextProperties... text) {
		return QueuedTooltip.create(location, text);
	}
	
	static Tooltip of(Point location, IReorderingProcessor... text) {
		return QueuedTooltip.create(location, text);
	}
	
	Point getPoint();
	
	default int getX() {
		return this.getPoint().getX();
	}
	
	default int getY() {
		return this.getPoint().getY();
	}
	
	List<IReorderingProcessor> getText();
}

