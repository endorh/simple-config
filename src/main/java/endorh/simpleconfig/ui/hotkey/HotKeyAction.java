package endorh.simpleconfig.ui.hotkey;

import com.electronwill.nightconfig.core.CommentedConfig;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class HotKeyAction<V> {
	private final HotKeyActionType<V, ?> type;
	
	public HotKeyAction(HotKeyActionType<V, ?> type) {
		this.type = type;
	}
	
	public HotKeyActionType<V, ?> getType() {
		return type;
	}
	
	public abstract <T, C, E extends AbstractConfigEntry<T, C, V>>
	@Nullable ITextComponent apply(String path, E entry, CommentedConfig result);
	
	public @Nullable ITextComponent apply(
	  SimpleConfigImpl config, String path, CommentedConfig result
	) {
		try {
			AbstractConfigEntry<?, ?, V> entry = config.getEntryOrNull(path);
			if (entry != null) return apply(entry.getPath(), entry, result);
		} catch (ClassCastException ignored) {}
		return null;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HotKeyAction<?> that = (HotKeyAction<?>) o;
		return type.equals(that.type);
	}
	
	@Override public int hashCode() {
		return Objects.hash(type);
	}
}
