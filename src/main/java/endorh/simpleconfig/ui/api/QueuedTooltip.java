package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.math.Point;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QueuedTooltip implements Tooltip {
	private final Point location;
	private final List<IReorderingProcessor> text;
	
	private QueuedTooltip(Point location, List<IReorderingProcessor> text) {
		this.location = location;
		this.text = Collections.unmodifiableList(text);
	}
	
	public static QueuedTooltip create(Point location, List<ITextComponent> text) {
		//noinspection unchecked
		return new QueuedTooltip(location, LanguageMap.getInstance().getVisualOrder(
		  (List<ITextProperties>) (List<?>) text));
	}
	
	public static QueuedTooltip create(Point location, ITextComponent... text) {
		return QueuedTooltip.create(location, Arrays.asList(text));
	}
	
	public static QueuedTooltip create(Point location, IReorderingProcessor... text) {
		return new QueuedTooltip(location, Arrays.asList(text));
	}
	
	public static QueuedTooltip create(Point location, ITextProperties... text) {
		return new QueuedTooltip(location, LanguageMap.getInstance().getVisualOrder(Arrays.asList(text)));
	}
	
	@Override
	public Point getPoint() {
		return this.location;
	}
	
	@Override
	@ApiStatus.Internal
	public List<IReorderingProcessor> getText() {
		return this.text;
	}
}

