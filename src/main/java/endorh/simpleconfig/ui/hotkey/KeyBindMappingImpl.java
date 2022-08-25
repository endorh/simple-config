package endorh.simpleconfig.ui.hotkey;

import com.google.common.base.Splitter;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettingsBuilder;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation.*;

public class KeyBindMappingImpl implements KeyBindMapping {
	private final IntList requiredKeys;
	private final @Nullable Int2ObjectMap<String> charMap;
	private final ExtendedKeyBindSettings settings;
	
	public static KeyBindMappingImpl unset() {
		return new KeyBindMappingImpl(
		  new IntArrayList(), null, ExtendedKeyBindSettings.ingame().build());
	}
	
	public static KeyBindMappingImpl unset(ExtendedKeyBindSettings settings) {
		return new KeyBindMappingImpl(new IntArrayList(), null, settings);
	}
	
	public KeyBindMappingImpl(
	  IntList requiredKeys, @Nullable Int2ObjectMap<String> charMap,
	  ExtendedKeyBindSettings settings
	) {
		this.requiredKeys = requiredKeys;
		this.charMap = charMap;
		this.settings = settings;
	}
	
	@Override public IntList getRequiredKeys() {
		return requiredKeys;
	}
	@Override public @Nullable Int2ObjectMap<String> getCharMap() {
		return charMap;
	}
	@Override public ExtendedKeyBindSettings getSettings() {
		return settings;
	}
	
	@Override public boolean isUnset() {
		return requiredKeys.isEmpty();
	}
	
	@Override public boolean overlaps(KeyBindMapping other) {
		ExtendedKeyBindSettings settings = getSettings();
		ExtendedKeyBindSettings otherSettings = other.getSettings();
		if (
		  !settings.getContext().conflictsWith(otherSettings.getContext())
		  || isUnset() || other.isUnset()
		) return false;
		
		IntList keys = getRequiredKeys();
		IntList otherKeys = other.getRequiredKeys();
		int keysSize = keys.size();
		int otherKeysSize = otherKeys.size();
		
		if (keysSize < otherKeysSize) return false; // return other.overlaps(this);
		
		KeyBindActivation act = settings.getActivation();
		KeyBindActivation otherAct = otherSettings.getActivation();
		
		boolean isRelease = (act == RELEASE || act == TOGGLE_RELEASE) && settings.isExclusive();
		boolean otherIsRelease =
		  (otherAct == RELEASE || otherAct == TOGGLE_RELEASE) && otherSettings.isExclusive();
		if (isRelease != otherIsRelease && act != BOTH && otherAct != BOTH) return false;
		
		boolean matchByChar = settings.isMatchByChar();
		boolean otherMatchByChar = otherSettings.isMatchByChar();
		Int2ObjectMap<String> charMap = getCharMap();
		Int2ObjectMap<String> otherCharMap = other.getCharMap();
		boolean compareByChar = matchByChar || otherMatchByChar;
		
		assert !matchByChar || charMap != null;
		assert !otherMatchByChar || otherCharMap != null;
		
		if (settings.isOrderSensitive() && otherSettings.isOrderSensitive()) {
			boolean strict = !otherSettings.isAllowExtraKeys();
			int j = 0;
			match:for (int i = 0; i < otherKeysSize; i++) {
				int otherKey = otherKeys.getInt(i);
				while (j <= i + keysSize - otherKeysSize) {
					int key = keys.getInt(j++);
					if (compareByChar) {
						String ch = matchByChar? charMap.get(key) : Keys.getCharFromKey(key);
						String otherCh = otherMatchByChar? otherCharMap.get(otherKey) : Keys.getCharFromKey(otherKey);
						if (ch != null? ch.equals(otherCh) : otherCh == null && key == otherKey) continue match;
					} else if (!Keys.isVirtualKey(key) && key == otherKey) continue match;
					if (strict) return false;
				}
				return false;
			}
			return true;
		} else {
			IntOpenHashSet set = new IntOpenHashSet(keys);
			if (compareByChar) {
				Set<String> charSet =
				  charMap != null? new HashSet<>(charMap.values())
				                 : keys.stream().map(Keys::getCharFromKey).filter(Objects::nonNull).collect(Collectors.toSet());
				return otherKeys.stream().allMatch(otherKey -> {
					String otherChar = otherMatchByChar? otherCharMap.get((int) otherKey) : Keys.getCharFromKey(otherKey);
					return otherChar != null? charSet.contains(otherChar) : set.contains((int) otherKey);
				});
			} else return set.containsAll(otherKeys);
		}
	}
	
	@Override public KeyBindMapping copy() {
		return new KeyBindMappingImpl(
		  new IntArrayList(requiredKeys),
		  charMap == null ? null : new Int2ObjectOpenHashMap<>(charMap),
		  settings.copy());
	}
	
	@Override public ITextComponent getDisplayName(Style style) {
		ExtendedKeyBindSettings settings = getSettings();
		IFormattableTextComponent joiner = new StringTextComponent(
		  settings.isOrderSensitive()? ">" : "+"
		).mergeStyle(TextFormatting.GRAY);
		if (requiredKeys.isEmpty()) return StringTextComponent.EMPTY;
		IFormattableTextComponent r = new StringTextComponent("");
		int first = requiredKeys.getInt(0);
		boolean matchByChar = settings.isMatchByChar();
		String firstChar = charMap != null? charMap.get(first) : null;
		r.append((matchByChar && firstChar != null? formatKey(firstChar) : formatKey(first)).mergeStyle(style));
		for (int i = 1; i < requiredKeys.size(); i++) {
			int k = requiredKeys.getInt(i);
			String ch = charMap != null? charMap.get(k) : null;
			r.append(joiner).append((matchByChar && ch != null? formatKey(ch) : formatKey(k)).mergeStyle(style));
		}
		return r;
	}
	
	@Override public String serialize() {
		ExtendedKeyBindSettings settings = getSettings();
		String joiner = settings.isOrderSensitive()? ">" : "+";
		boolean matchByChar = settings.isMatchByChar();
		String keys =
		  requiredKeys.isEmpty()
		  ? "unset"
		  : matchByChar
		    ? requiredKeys.stream().map(k -> {
				 String ch = charMap != null? charMap.get((int) k) : null;
				 return ch != null? serializeKey(ch) : serializeKey(k);
			 }).collect(Collectors.joining(joiner))
		    : requiredKeys.stream()
		      .map(KeyBindMappingImpl::serializeKey)
		      .collect(Collectors.joining(joiner));
		KeyBindActivation activation = settings.getActivation();
		KeyBindContext context = settings.getContext();
		String act = activation != PRESS? activation.serialize() : "";
		String ctx = context != VanillaKeyBindContext.GAME? context.serialize() : "";
		StringBuilder b =
		  new StringBuilder(keys.length() + act.length() + ctx.length() + 4);
		if (settings.isMatchByChar() && !keys.contains("\"")) b.append('@');
		if (settings.isExclusive()) b.append('!');
		if (settings.isOrderSensitive() && requiredKeys.size() <= 1) b.append('>');
		b.append(keys);
		if (!settings.isAllowExtraKeys()) b.append("!");
		if (!settings.isPreventFurther()) b.append(">");
		if (!act.isEmpty()) {
			b.append(':');
			b.append(act);
		}
		if (!ctx.isEmpty()) {
			b.append('#');
			b.append(ctx);
		}
		return b.toString();
	}
	
	private static final Pattern KEY_BIND_PATTERN = Pattern.compile(
	  "^(?<char>@)?+(?<exclusive>!)?+(?<order>>)?+(?<keys>.*?)(?<extra>!)?+(?<prevent>>)?+" +
	  "(?::(?<activation>\\w++))?+(?:#(?<context>\\w++))?+$");
	private static final Pattern CHARS_SORTED_PATTERN = Pattern.compile(
	  "^(?:\"[^\"]++\"|\\w++(?:\\.\\w++)*+)(?:>(?:\"[^\"]++\"|\\w++(?:\\.\\w++)*+))*+$");
	private static final Pattern CHARS_UNSORTED_PATTERN = Pattern.compile(
	  "^(?:\"[^\"]++\"|\\w++(?:\\.\\w++)*+)(?:\\+(?:\"[^\"]++\"|\\w++(?:\\.\\w++)*+))*+$");
	private static final Pattern KEYS_SORTED_PATTERN = Pattern.compile(
	  "^\\w++(?:\\.\\w++)*+(?:>\\w++(?:\\.\\w++)*+)*+$");
	private static final Pattern KEYS_UNSORTED_PATTERN = Pattern.compile(
	  "^\\w++(?:\\.\\w++)*+(?:\\+\\w++(?:\\.\\w++)*+)*+$");
	private static final Splitter UNORDERED_SPLITTER = Splitter.on('+');
	private static final Splitter ORDERED_SPLITTER = Splitter.on('>');
	public static KeyBindMappingImpl parse(String serialized) {
		Matcher m = KeyBindMappingImpl.KEY_BIND_PATTERN.matcher(serialized);
		ExtendedKeyBindSettingsBuilder settings = new ExtendedKeyBindSettingsBuilder();
		if (!m.matches()) return new KeyBindMappingImpl(new IntArrayList(), null, settings.build());
		if (m.group("exclusive") != null) settings.setExclusive(true);
		boolean orderSensitive = m.group("order") != null || m.group("keys").contains(">");
		settings.setOrderSensitive(orderSensitive);
		settings.setAllowExtraKeys(m.group("extra") == null);
		settings.setPreventFurther(m.group("prevent") == null);
		String keys = m.group("keys");
		boolean matchByChar = m.group("char") != null || keys.contains("\"");
		settings.setMatchByChar(matchByChar);
		String activation = m.group("activation");
		settings.withActivation(
		  activation != null? deserialize(activation) : PRESS);
		String context = m.group("context");
		settings.withContext(
		  context != null? KeyBindContext.deserialize(context) : VanillaKeyBindContext.GAME);
		Splitter splitter =
		  orderSensitive? KeyBindMappingImpl.ORDERED_SPLITTER : KeyBindMappingImpl.UNORDERED_SPLITTER;
		Pattern pattern = matchByChar
		                  ? orderSensitive? KeyBindMappingImpl.CHARS_SORTED_PATTERN
		                                  : KeyBindMappingImpl.CHARS_UNSORTED_PATTERN
		                  : orderSensitive? KeyBindMappingImpl.KEYS_SORTED_PATTERN
		                                  : KeyBindMappingImpl.KEYS_UNSORTED_PATTERN;
		if (keys.equals("unset"))
			return new KeyBindMappingImpl(new IntArrayList(), matchByChar? new Int2ObjectOpenHashMap<>() : null, settings.build());
		if (!pattern.matcher(keys).matches())
			return new KeyBindMappingImpl(new IntArrayList(), null, settings.build());
		if (matchByChar) {
			IntList list = new IntArrayList();
			Int2ObjectMap<String> chars = new Int2ObjectOpenHashMap<>();
			int charIndex = Keys.FIRST_UNASSIGNED_KEY;
			for (String ch: splitter.split(keys)) {
				if (ch.startsWith("\"")) {
					list.add(charIndex);
					chars.put(charIndex++, deserializeChar(ch));
				} else list.add(deserializeKey(ch));
			}
			return new KeyBindMappingImpl(list, chars, settings.build());
		} else {
			IntArrayList list = new IntArrayList();
			Iterable<String> iter = splitter.split(keys);
			iter.forEach(s -> list.add(KeyBindMappingImpl.deserializeKey(s)));
			return new KeyBindMappingImpl(list, null, settings.build());
		}
	}
	
	protected static String serializeKey(int key) {
		return Keys.getNameForKey(key);
	}
	
	protected static String serializeKey(String key) {
		return "\"" + key + "\"";
	}
	
	protected static int deserializeKey(String key) {
		return Keys.getKeyFromName(key);
	}
	protected static String deserializeChar(String key) {
		return key.substring(1, key.length() - 1);
	}
	
	protected IFormattableTextComponent formatKey(int key) {
		return new StringTextComponent(WordUtils.capitalize(
		  Keys.getDisplayNameForKey(key).getString()
		)).modifyStyle(s -> s.setColor(Color.fromInt(
		  Keys.isMouseKey(key) || Keys.isScrollKey(key)
		  ? 0xAAAAFF : Keys.isScanCode(key)? 0xAAFFFF : Keys.isModifier(key)? 0xEEEEEE : 0xFFFFFF)));
	}
	
	protected IFormattableTextComponent formatKey(String key) {
		return new StringTextComponent(WordUtils.capitalize(key))
		  .mergeStyle(TextFormatting.ITALIC);
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KeyBindMappingImpl that = (KeyBindMappingImpl) o;
		int size = requiredKeys.size();
		if (size != that.requiredKeys.size()
		    || !settings.equals(that.settings)
		) return false;
		if (charMap == null || that.charMap == null)
			return requiredKeys.equals(that.requiredKeys);
		for (int i = 0; i < size; i++) {
			int k = requiredKeys.getInt(i);
			int tk = that.requiredKeys.getInt(i);
			String ch = charMap.get(k);
			String tch = that.charMap.get(tk);
			if ((ch == null) != (tch == null)) return false;
			if (ch == null? k != tk : !ch.equals(tch)) return false;
		}
		return true;
	}
	
	@Override public int hashCode() {
		if (charMap == null) return Objects.hash(requiredKeys, settings);
		int hash = Objects.hash(settings);
		for (int k: requiredKeys) {
			String ch = charMap.get(k);
			hash = 31 * hash + (ch == null? k : ch.hashCode());
		}
		return hash;
	}
	
	@Override public String toString() {
		return serialize();
	}
}
