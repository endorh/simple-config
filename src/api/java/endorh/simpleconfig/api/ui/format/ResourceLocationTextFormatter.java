package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceLocationTextFormatter implements ITextFormatter {
	private Style namespaceStyle = Style.EMPTY.setColor(Color.fromInt(0xA0A0A0));
	private Style colonStyle = Style.EMPTY.setColor(Color.fromInt(0x808080));
	private Style pathStyle = Style.EMPTY.setColor(Color.fromInt(0x80FFA0));
	private Style slashStyle = Style.EMPTY.setColor(Color.fromInt(0x4280FF));
	private Style errorStyle = Style.EMPTY.setFormatting(TextFormatting.RED).func_244282_c(true);
	
	private static final Pattern RESOURCE_LOCATION_PATTERN = Pattern.compile(
	  "^(?<ls>\\s*+)(?<pre>(?<name>[a-zA-Z\\d_.-]*):)?(?<path>[a-zA-Z\\d_./-]+)(?<rs>\\s*+)$");
	private static final Pattern SLASH = Pattern.compile("/");
	private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
	
	@Override public IFormattableTextComponent formatText(String text) {
		if (text.isEmpty()) return StringTextComponent.EMPTY.deepCopy();
		Matcher m = RESOURCE_LOCATION_PATTERN.matcher(text);
		if (!m.matches()) return new StringTextComponent(text).mergeStyle(errorStyle);
		IFormattableTextComponent res = new StringTextComponent(m.group("ls"));
		if (m.group("pre") != null) {
			res.append(highlightUppercase(m.group("name"), namespaceStyle));
			res.append(new StringTextComponent(":").mergeStyle(colonStyle));
		}
		String path = m.group("path");
		Matcher s = SLASH.matcher(path);
		int idx = 0;
		while (s.find()) {
			res.append(highlightUppercase(path.substring(idx, s.start()), pathStyle));
			res.append(new StringTextComponent(s.group()).mergeStyle(slashStyle));
			idx = s.end();
		}
		if (idx < path.length())
			res.append(highlightUppercase(path.substring(idx), pathStyle));
		res.append(new StringTextComponent(m.group("rs")));
		return res;
	}
	
	private IFormattableTextComponent highlightUppercase(String text, Style style) {
		IFormattableTextComponent res = null;
		Matcher m = UPPERCASE.matcher(text);
		int last = 0;
		while (m.find()) {
			res =
			  append(res, new StringTextComponent(text.substring(last, m.start())).mergeStyle(style));
			res = append(res, new StringTextComponent(m.group()).mergeStyle(errorStyle));
			last = m.end();
		}
		if (last < text.length())
			res = append(res, new StringTextComponent(text.substring(last)).mergeStyle(style));
		return res != null ? res : StringTextComponent.EMPTY.deepCopy();
	}
	
	private IFormattableTextComponent append(IFormattableTextComponent c, ITextComponent append) {
		return c == null ? append.deepCopy() : c.append(append);
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
		return ITextFormatter.filterCharacters(
		  text.toLowerCase(),
		  c -> c >= 'a' && c <= 'z' || c == '/' || c == '_' || c == '-' || c == ':');
	}
}
