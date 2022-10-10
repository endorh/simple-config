package endorh.simpleconfig.ui.hotkey;

import com.google.common.base.Splitter;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettings;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindSettingsBuilder;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simpleconfig.api.ui.hotkey.KeyBindMapping.KeyBindActivation.*;

@SuppressWarnings("ClassCanBeRecord")
public class KeyBindMappingImpl implements KeyBindMapping {
	private final @NotNull IntList requiredKeys;
	private final @Nullable Int2ObjectMap<String> charMap;
	private final @NotNull ExtendedKeyBindSettings settings;
	
	public static @NotNull KeyBindMappingImpl unset() {
		return new KeyBindMappingImpl(
		  new IntArrayList(), null, ExtendedKeyBindSettings.ingame().build());
	}
	
	public static @NotNull KeyBindMappingImpl unset(ExtendedKeyBindSettings settings) {
		return new KeyBindMappingImpl(new IntArrayList(), null, settings);
	}
	
	public KeyBindMappingImpl(
	  @NotNull IntList requiredKeys, @Nullable Int2ObjectMap<String> charMap,
	  @NotNull ExtendedKeyBindSettings settings
	) {
		this.requiredKeys = requiredKeys;
		this.charMap = charMap;
		this.settings = settings;
	}
	
	@Override public @NotNull IntList getRequiredKeys() {
		return requiredKeys;
	}
	@Override public @Nullable Int2ObjectMap<String> getCharMap() {
		return charMap;
	}
	@Override public @NotNull ExtendedKeyBindSettings getSettings() {
		return settings;
	}
	
	@Override public boolean isUnset() {
		return requiredKeys.isEmpty();
	}
	
	@Override public boolean overlaps(KeyBindMapping other) {
		ExtendedKeyBindSettings settings = getSettings();
		ExtendedKeyBindSettings otherSettings = other.getSettings();
		if (
		  !settings.context().conflictsWith(otherSettings.context())
		  || isUnset() || other.isUnset()
		) return false;
		
		IntList keys = getRequiredKeys();
		IntList otherKeys = other.getRequiredKeys();
		int keysSize = keys.size();
		int otherKeysSize = otherKeys.size();
		
		if (keysSize < otherKeysSize) return false; // return other.overlaps(this);
		
		KeyBindActivation act = settings.activation();
		KeyBindActivation otherAct = otherSettings.activation();
		
		boolean isRelease = (act == RELEASE || act == TOGGLE_RELEASE) && settings.exclusive();
		boolean otherIsRelease =
		  (otherAct == RELEASE || otherAct == TOGGLE_RELEASE) && otherSettings.exclusive();
		if (isRelease != otherIsRelease && act != BOTH && otherAct != BOTH) return false;
		
		boolean matchByChar = settings.matchByChar();
		boolean otherMatchByChar = otherSettings.matchByChar();
		Int2ObjectMap<String> charMap = getCharMap();
		Int2ObjectMap<String> otherCharMap = other.getCharMap();
		boolean compareByChar = matchByChar || otherMatchByChar;
		
		assert !matchByChar || charMap != null;
		assert !otherMatchByChar || otherCharMap != null;
		
		if (settings.orderSensitive() && otherSettings.orderSensitive()) {
			boolean strict = !otherSettings.allowExtraKeys();
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
				                 : keys.intStream().mapToObj(Keys::getCharFromKey).filter(Objects::nonNull).collect(Collectors.toSet());
				return otherKeys.intStream().allMatch(otherKey -> {
					String otherChar = otherMatchByChar? otherCharMap.get(otherKey) : Keys.getCharFromKey(otherKey);
					return otherChar != null? charSet.contains(otherChar) : set.contains(otherKey);
				});
			} else return set.containsAll(otherKeys);
		}
	}
	
	@Override public @NotNull KeyBindMapping copy() {
		return new KeyBindMappingImpl(
		  new IntArrayList(requiredKeys),
		  charMap == null ? null : new Int2ObjectOpenHashMap<>(charMap),
		  settings.copy());
	}
	
	@Override public @NotNull Component getDisplayName(Style style) {
		ExtendedKeyBindSettings settings = getSettings();
		MutableComponent joiner = Component.literal(settings.orderSensitive()? ">" : "+")
		  .withStyle(ChatFormatting.GRAY);
		if (requiredKeys.isEmpty()) return Component.empty();
		MutableComponent r = Component.literal("");
		int first = requiredKeys.getInt(0);
		boolean matchByChar = settings.matchByChar();
		String firstChar = charMap != null? charMap.get(first) : null;
		r.append((matchByChar && firstChar != null? formatKey(firstChar) : formatKey(first)).withStyle(style));
		for (int i = 1; i < requiredKeys.size(); i++) {
			int k = requiredKeys.getInt(i);
			String ch = charMap != null? charMap.get(k) : null;
			r.append(joiner).append((matchByChar && ch != null? formatKey(ch) : formatKey(k)).withStyle(style));
		}
		return r;
	}
	
	@Override public @NotNull String serialize() {
		ExtendedKeyBindSettings settings = getSettings();
		String joiner = settings.orderSensitive()? ">" : "+";
		boolean matchByChar = settings.matchByChar();
		String keys =
		  requiredKeys.isEmpty()
		  ? "unset"
		  : matchByChar
		    ? requiredKeys.intStream().mapToObj(k -> {
				 String ch = charMap != null? charMap.get(k) : null;
				 return ch != null? serializeKey(ch) : serializeKey(k);
			 }).collect(Collectors.joining(joiner))
		    : requiredKeys.intStream()
		      .mapToObj(KeyBindMappingImpl::serializeKey)
		      .collect(Collectors.joining(joiner));
		KeyBindActivation activation = settings.activation();
		KeyBindContext context = settings.context();
		String act = activation != PRESS? activation.serialize() : "";
		String ctx = context != VanillaKeyBindContext.GAME? context.serialize() : "";
		StringBuilder b =
		  new StringBuilder(keys.length() + act.length() + ctx.length() + 4);
		if (settings.matchByChar() && !keys.contains("\"")) b.append('@');
		if (settings.exclusive()) b.append('!');
		if (settings.orderSensitive() && requiredKeys.size() <= 1) b.append('>');
		b.append(keys);
		if (!settings.allowExtraKeys()) b.append("!");
		if (!settings.preventFurther()) b.append(">");
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
	public static @NotNull KeyBindMappingImpl parse(String serialized) {
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
	
	protected MutableComponent formatKey(int key) {
		return Component.literal(WordUtils.capitalize(
		  Keys.getDisplayNameForKey(key).getString()
		)).withStyle(s -> s.withColor(TextColor.fromRgb(
		  Keys.isMouseKey(key) || Keys.isScrollKey(key)
		  ? 0xAAAAFF : Keys.isScanCode(key)? 0xAAFFFF : Keys.isModifier(key)? 0xEEEEEE : 0xFFFFFF)));
	}
	
	protected MutableComponent formatKey(String key) {
		return Component.literal(WordUtils.capitalize(key))
		  .withStyle(ChatFormatting.ITALIC);
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
