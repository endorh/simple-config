package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource location formatter, with different styles for
 * the namespace, colon, path, slashes in the path and errors.<br>
 * Also prevents the user from inputting invalid characters, and
 * forces inputted characters to be lowercase.
 */
public class ResourceLocationTextFormatter implements TextFormatter {
	private Style namespaceStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xA0A0A0));
	private Style colonStyle = Style.EMPTY.withColor(TextColor.fromRgb(0x808080));
	private Style pathStyle = Style.EMPTY.withColor(TextColor.fromRgb(0x80FFA0));
	private Style slashStyle = Style.EMPTY.withColor(TextColor.fromRgb(0x4280FF));
	private Style errorStyle = Style.EMPTY.withColor(ChatFormatting.RED).withUnderlined(true);
	
	private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile(
	  "^(?<ls>\\s*+)(?<pre>(?<name>[a-zA-Z\\d_.-]*):)?(?<path>[a-zA-Z\\d_./-]+)(?<rs>\\s*+)$");
	private static final Pattern SLASH = Pattern.compile("/");
	private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
	
	@Override public MutableComponent formatText(String text) {
		if (text.isEmpty()) return Component.empty();
		Matcher m = RESOURCE_LOCATION_PATTERN.matcher(text);
		if (!m.matches()) return Component.literal(text).withStyle(errorStyle);
		MutableComponent res = Component.literal(m.group("ls"));
		if (m.group("pre") != null) {
			res.append(highlightUppercase(m.group("name"), namespaceStyle));
			res.append(Component.literal(":").withStyle(colonStyle));
		}
		String path = m.group("path");
		Matcher s = SLASH.matcher(path);
		int idx = 0;
		while (s.find()) {
			res.append(highlightUppercase(path.substring(idx, s.start()), pathStyle));
			res.append(Component.literal(s.group()).withStyle(slashStyle));
			idx = s.end();
		}
		if (idx < path.length())
			res.append(highlightUppercase(path.substring(idx), pathStyle));
		res.append(Component.literal(m.group("rs")));
		return res;
	}
	
	private MutableComponent highlightUppercase(String text, Style style) {
		MutableComponent res = null;
		Matcher m = UPPERCASE.matcher(text);
		int last = 0;
		while (m.find()) {
			res =
			  append(res, Component.literal(text.substring(last, m.start())).withStyle(style));
			res = append(res, Component.literal(m.group()).withStyle(errorStyle));
			last = m.end();
		}
		if (last < text.length())
			res = append(res, Component.literal(text.substring(last)).withStyle(style));
		return res != null ? res : Component.empty();
	}
	
	private MutableComponent append(MutableComponent c, Component append) {
		return c == null ? append.copy() : c.append(append);
	}
	
	public void setNamespaceStyle(Style namespaceStyle) {
		this.namespaceStyle = namespaceStyle;
	}
	
	public void setColonStyle(Style colonStyle) {
		this.colonStyle = colonStyle;
	}
	
	public void setPathStyle(Style pathStyle) {
		this.pathStyle = pathStyle;
	}
	
	public void setSlashStyle(Style slashStyle) {
		this.slashStyle = slashStyle;
	}
	
	public void setErrorStyle(Style errorStyle) {
		this.errorStyle = errorStyle;
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
		return TextFormatter.filterCharacters(
		  text.toLowerCase(),
		  c -> c >= 'a' && c <= 'z' || c == '/' || c == '_' || c == '-' || c == ':');
	}
}
