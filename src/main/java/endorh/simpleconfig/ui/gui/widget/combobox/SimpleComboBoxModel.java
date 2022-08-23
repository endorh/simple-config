package endorh.simpleconfig.ui.gui.widget.combobox;

import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleComboBoxModel<T> extends AbstractComboBoxModel<T> {
	@NotNull protected Supplier<List<T>> suggestions;
	protected long lastUpdate = 0L;
	protected long updateCooldown = 250L;
	protected @Nullable Function<String, Component> placeholder = null;
	
	public SimpleComboBoxModel(
	  @NotNull List<T> suggestions
	) {
		this.suggestions = () -> suggestions;
	}
	
	public SimpleComboBoxModel(
	  @NotNull Supplier<List<T>> suggestionSupplier
	) {
		this.suggestions = suggestionSupplier;
	}
	
	@Override public Optional<List<T>> updateSuggestions(
	  ITypeWrapper<T> typeWrapper, String query
	) {
		final long time = System.currentTimeMillis();
		if (time - lastUpdate < updateCooldown)
			return Optional.empty();
		lastUpdate = time;
		return Optional.of(new ArrayList<>(this.suggestions.get()));
	}
	
	@Override public Optional<Component> getPlaceHolder(
	  ITypeWrapper<T> typeWrapper, String query
	) {
		return placeholder != null ? Optional.of(placeholder.apply(query))
		                           : super.getPlaceHolder(typeWrapper, query);
	}
	
	/**
	 * Set the minimum cooldown between suggestion update checks.<br>
	 * Defaults to 500ms.
	 */
	public void setUpdateCooldown(long cooldownMs) {
		this.updateCooldown = cooldownMs;
	}
	
	public void setPlaceholder(@Nullable Function<String, Component> getter) {
		this.placeholder = getter;
	}
	
	public void setPlaceholder(Component placeholder) {
		setPlaceholder(s -> placeholder);
	}
}
