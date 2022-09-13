package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.api.ui.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PointTooltip implements Tooltip {
	private Font font = Minecraft.getInstance().font;
	private final Point location;
	private final List<FormattedCharSequence> text;
	private boolean fromKeyboard = false;
	
	protected PointTooltip(Point location, List<FormattedCharSequence> text) {
		this.location = location;
		this.text = Collections.unmodifiableList(text);
	}
	
	public static PointTooltip create(Point location, List<Component> text) {
		//noinspection unchecked
		return new PointTooltip(location, Language.getInstance().getVisualOrder(
		  (List<FormattedText>) (List<?>) text));
	}
	
	public static PointTooltip create(Point location, Component... text) {
		return PointTooltip.create(location, Arrays.asList(text));
	}
	
	public static PointTooltip create(Point location, FormattedCharSequence... text) {
		return new PointTooltip(location, Arrays.asList(text));
	}
	
	public static PointTooltip create(Point location, FormattedText... text) {
		return new PointTooltip(location, Language.getInstance().getVisualOrder(Arrays.asList(text)));
	}
	
	@Override public Font getFont() {
		return font;
	}
	@Override public PointTooltip withFont(Font font) {
		this.font = font;
		return this;
	}
	@Override public Point getPoint() {
		return location;
	}
	@Override public List<FormattedCharSequence> getText() {
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

