package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PointTooltip implements Tooltip {
	private FontRenderer font = Minecraft.getInstance().fontRenderer;
	private final Point location;
	private final List<IReorderingProcessor> text;
	private boolean fromKeyboard = false;
	
	protected PointTooltip(Point location, List<IReorderingProcessor> text) {
		this.location = location;
		this.text = Collections.unmodifiableList(text);
	}
	
	public static PointTooltip create(Point location, List<ITextComponent> text) {
		//noinspection unchecked
		return new PointTooltip(location, LanguageMap.getInstance().func_244260_a(
		  (List<ITextProperties>) (List<?>) text));
	}
	
	public static PointTooltip create(Point location, ITextComponent... text) {
		return PointTooltip.create(location, Arrays.asList(text));
	}
	
	public static PointTooltip create(Point location, IReorderingProcessor... text) {
		return new PointTooltip(location, Arrays.asList(text));
	}
	
	public static PointTooltip create(Point location, ITextProperties... text) {
		return new PointTooltip(location, LanguageMap.getInstance().func_244260_a(Arrays.asList(text)));
	}
	
	@Override public FontRenderer getFont() {
		return font;
	}
	@Override public PointTooltip withFont(FontRenderer font) {
		this.font = font;
		return this;
	}
	@Override public Point getPoint() {
		return location;
	}
	@Override public List<IReorderingProcessor> getText() {
		return text;
	}
	@Override public boolean isFromKeyboard() {
		return fromKeyboard;
	}
	@Override public PointTooltip asKeyboardTooltip(boolean fromKeyboard) {
		this.fromKeyboard = fromKeyboard;
		return this;
	}
}

