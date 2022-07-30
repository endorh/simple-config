package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.ui.gui.Icon;
import endorh.simpleconfig.ui.hotkey.SimpleHotKeyActionType.SimpleHotKeyAction;
import net.minecraft.util.text.*;
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
	
	@Override public ITextComponent formatAction(SimpleHotKeyAction<V, S> action) {
		return new TranslationTextComponent(
		  "simpleconfig.hotkey.type.action." + getTranslationKey(), formatStorage(action.getStorage()));
	}
	
	public ITextComponent formatStorage(S storage) {
		String value;
		if (storage instanceof Float) {
			value = String.format("%.2f", storage);
		} else if (storage instanceof Double) {
			value = String.format("%.3f", storage);
		} else {
			value = String.valueOf(storage);
		}
		return new StringTextComponent(value).mergeStyle(TextFormatting.DARK_AQUA);
	}
	
	@SuppressWarnings("unchecked") @Override public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V, ?>> SimpleHotKeyAction<V, S> deserialize(
	  E entry, Object value
	) {
		if (value == null) return null;
		return new SimpleHotKeyAction<>(this, entry, action, (S) value);
	}
	
	@Override public <T, C, E extends AbstractConfigEntry<T, C, V, ?>> Object serialize(
	  E entry, SimpleHotKeyAction<V, S> action
	) {
		return action.getStorage();
	}
	
	@Override
	public <T, C, E extends AbstractConfigEntry<T, C, V, ?>> Optional<ITextComponent> getActionError(
	  E entry, Object value
	) {
		try {
			//noinspection unchecked
			return error != null? error.getError(entry, (S) value) : Optional.empty();
		} catch (ClassCastException e) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_type_generic"));
		}
	}
	
	public static class SimpleHotKeyAction<V, S> extends HotKeyAction<V> {
		private final AbstractConfigEntry<?, ?, V, ?> entry;
		private final ISimpleHotKeyAction<V, S> action;
		private final S storage;
		
		public SimpleHotKeyAction(
		  SimpleHotKeyActionType<V, S> type, AbstractConfigEntry<?, ?, V, ?> entry,
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
		
		public AbstractConfigEntry<?, ?, V, ?> getEntry() {
			return entry;
		}
		
		@Override public <T, C, E extends AbstractConfigEntry<T, C, V, ?>>
		@Nullable ITextComponent apply(E entry) {
			ITextComponent prev = formatValue(entry.getForCommand());
			T prevValue = entry.get();
			boolean success = entry.trySet(entry.fromGui(applyValue(entry.forGui(prevValue))));
			ITextComponent set = formatValue(entry.getForCommand());
			boolean change = success && !Objects.equals(prevValue, entry.get());
			return formatPath(entry.getPath()).appendString(" ")
			  .append(new TranslationTextComponent(
				 "simpleconfig.hotkey.type.report." + getType().getTranslationKey(),
				 getType().formatStorage(getStorage()), prev, set)
			            .mergeStyle(change? TextFormatting.WHITE : TextFormatting.GRAY));
		}
		
		private IFormattableTextComponent formatPath(String path) {
			if (path.length() > 60)
				path = path.substring(0, 10) + "..." + path.substring(path.length() - 47);
			return new StringTextComponent("[" + path + "]").mergeStyle(TextFormatting.LIGHT_PURPLE);
		}
		
		private static final Pattern FLOATING_NUMBER = Pattern.compile(
		  "^[+-]?(?:\\d*\\.\\d+|\\d*\\.)$");
		private ITextComponent formatValue(String value) {
			if (FLOATING_NUMBER.matcher(value).matches()) {
				double v = Double.parseDouble(value);
				value = String.format("%.3f", v);
			}
			return new StringTextComponent(value).mergeStyle(TextFormatting.DARK_AQUA);
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
			       Objects.equals(storage, that.storage);
		}
		
		@Override public int hashCode() {
			return Objects.hash(super.hashCode(), entry, action, storage);
		}
	}
	
	@FunctionalInterface public interface ISimpleHotKeyAction<V, S> {
		@Nullable V applyValue(AbstractConfigEntry<?, ?, V, ?> entry, V value, S serialized);
	}
	
	@FunctionalInterface public interface ISimpleHotKeyError<V, S> {
		Optional<ITextComponent> getError(AbstractConfigEntry<?, ?, V, ?> entry, S storage);
	}
}