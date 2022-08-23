package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.ui.hotkey.SimpleHotKeyActionType.SimpleHotKeyAction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class SimpleHotKeyActionType<V, S> extends HotKeyActionType<V, SimpleHotKeyAction<V, S>> {
	protected final String translationKey;
	protected final ISimpleHotKeyAction<V, S> action;
	protected final @Nullable ISimpleHotKeyError<V, S> error;
	
	public SimpleHotKeyActionType(
	  String tagName, Icon icon, ISimpleHotKeyAction<V, S> action
	) {
		this(tagName, icon, action, null);
	}
	
	private static final Pattern COLON = Pattern.compile(":");
	public SimpleHotKeyActionType(
	  String tagName, Icon icon, ISimpleHotKeyAction<V, S> action,
	  @Nullable ISimpleHotKeyError<V, S> error
	) {
		super(tagName.replace(':', '.'), icon);
		this.action = action;
		this.error = error;
		if (tagName.contains(":")) {
			String[] split = COLON.split(tagName, 2);
			translationKey = split[1];
		} else translationKey = tagName;
	}
	
	@Override public String getTranslationKey() {
		return translationKey;
	}
	
	@Override public Component formatAction(SimpleHotKeyAction<V, S> action) {
		return new TranslatableComponent(
		  "simpleconfig.hotkey.type.action." + getTranslationKey(), formatStorage(action.getStorage()));
	}
	
	public Component formatStorage(S storage) {
		String value;
		if (storage instanceof Float) {
			value = String.format("%.2f", storage);
		} else if (storage instanceof Double) {
			value = String.format("%.3f", storage);
		} else {
			value = String.valueOf(storage);
		}
		return new TextComponent(value).withStyle(ChatFormatting.DARK_AQUA);
	}
	
	@SuppressWarnings("unchecked") @Override public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> SimpleHotKeyAction<V, S> deserialize(
	  E entry, Object value
	) {
		if (value == null) return null;
		return new SimpleHotKeyAction<>(this, entry, action, (S) value);
	}
	
	@Override public <T, C, E extends AbstractConfigEntry<T, C, V>> Object serialize(
	  E entry, SimpleHotKeyAction<V, S> action
	) {
		return action.getStorage();
	}
	
	@Override
	public <T, C, E extends AbstractConfigEntry<T, C, V>> Optional<Component> getActionError(
	  E entry, Object value
	) {
		try {
			//noinspection unchecked
			return error != null? error.getError(entry, (S) value) : Optional.empty();
		} catch (ClassCastException e) {
			return Optional.of(new TranslatableComponent(
			  "simpleconfig.command.error.set.invalid_type_generic"));
		}
	}
	
	public static class SimpleHotKeyAction<V, S> extends HotKeyAction<V> {
		private static final Logger LOGGER = LogManager.getLogger();
		private final AbstractConfigEntry<?, ?, V> entry;
		private final ISimpleHotKeyAction<V, S> action;
		private final S storage;
		
		public SimpleHotKeyAction(
		  SimpleHotKeyActionType<V, S> type, AbstractConfigEntry<?, ?, V> entry,
		  ISimpleHotKeyAction<V, S> action, S storage
		) {
			super(type);
			this.entry = entry;
			this.action = action;
			this.storage = storage;
		}
		
		@SuppressWarnings("unchecked") @Override public SimpleHotKeyActionType<V, S> getType() {
			return (SimpleHotKeyActionType<V, S>) super.getType();
		}
		
		public S getStorage() {
			return storage;
		}
		
		public AbstractConfigEntry<?, ?, V> getEntry() {
			return entry;
		}
		
		@Override public <T, C, E extends AbstractConfigEntry<T, C, V>>
		@Nullable Component apply(String path, E entry, CommentedConfig result) {
			Component prev = formatValue(entry.getForCommand());
			T prevValue = entry.get();
			T newValue = entry.fromGui(applyValue(entry.forGui(prevValue)));
			boolean success = entry.isValidValue(newValue);
			if (!success) {
				LOGGER.error(entry.getGlobalPath() + ": Error applying config hotkey, result value is not a valid value: " + newValue);
				newValue = entry.defValue;
			}
			T v = newValue;
			Component set = formatValue(entry.forCommand(v));
			boolean change = success && !Objects.equals(prevValue, v);
			if (success) result.set(path, entry.apply(e -> e.forActualConfig(e.forConfig(v))));
			MutableComponent report = formatPath(entry.getPath()).append(" ")
			  .append(new TranslatableComponent(
				 "simpleconfig.hotkey.type.report." + getType().getTranslationKey(),
				 getType().formatStorage(getStorage()), prev, set)
				         .withStyle(change? ChatFormatting.WHITE : ChatFormatting.GRAY));
			if (!success) report.append(" ").append(new TranslatableComponent(
			  "simpleconfig.hotkey.report.failure").withStyle(ChatFormatting.RED));
			return report;
		}
		
		private MutableComponent formatPath(String path) {
			if (path.length() > 60)
				path = path.substring(0, 10) + "..." + path.substring(path.length() - 47);
			return new TextComponent("[" + path + "]").withStyle(ChatFormatting.LIGHT_PURPLE);
		}
		
		private static final Pattern FLOATING_NUMBER = Pattern.compile(
		  "^[+-]?(?:\\d*\\.\\d+|\\d*\\.)$");
		private Component formatValue(String value) {
			if (FLOATING_NUMBER.matcher(value).matches()) {
				double v = Double.parseDouble(value);
				value = String.format("%.3f", v);
			}
			return new TextComponent(value).withStyle(ChatFormatting.DARK_AQUA);
		}
		
		public @Nullable V applyValue(V value) {
			try {
				return action.applyValue(entry, value, storage);
			} catch (ClassCastException e) {
				return null;
			}
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			SimpleHotKeyAction<?, ?> that = (SimpleHotKeyAction<?, ?>) o;
			return Objects.equals(entry, that.entry) &&
			       Objects.equals(action, that.action) &&
			       equalsStorage(storage, that.storage);
		}
		
		protected boolean equalsStorage(Object a, Object b) {
			if (a instanceof Number && b instanceof Number)
				return Float.compare(((Number) a).floatValue(), ((Number) b).floatValue()) == 0;
			return Objects.equals(a, b);
		}
		
		@Override public int hashCode() {
			return Objects.hash(super.hashCode(), entry, action, storage);
		}
	}
	
	@FunctionalInterface public interface ISimpleHotKeyAction<V, S> {
		@Nullable V applyValue(AbstractConfigEntry<?, ?, V> entry, V value, S serialized);
	}
	
	@FunctionalInterface public interface ISimpleHotKeyError<V, S> {
		Optional<Component> getError(AbstractConfigEntry<?, ?, V> entry, S storage);
	}
}