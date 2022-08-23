package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.core.AbstractConfigEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.Optional;

public abstract class HotKeyActionType<V, A extends HotKeyAction<V>> {
	private final String tagName;
	private final Tag tag;
	private final Icon icon;
	
	public HotKeyActionType(String tagName, Icon icon) {
		this.tagName = tagName;
		this.tag = new Tag("!action." + tagName);
		this.icon = icon;;
	}
	
	public String getTranslationKey() {
		return tagName;
	}
	public String getTagName() {
		return tagName;
	}
	public Tag getTag() {
		return tag;
	}
	public Icon getIcon() {
		return icon;
	}
	public Component getDisplayName() {
		return new TranslatableComponent("simpleconfig.hotkey.type.name." + getTranslationKey());
	}
	
	public Component formatAction(A action) {
		return new TranslatableComponent("simpleconfig.hotkey.type.action." + getTranslationKey());
	}
	
	public @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> A create(E entry, Object value) {
		return deserialize(entry, value);
	}
	public abstract @Nullable <T, C, E extends AbstractConfigEntry<T, C, V>> A deserialize(E entry, Object value);
	public abstract <T, C, E extends AbstractConfigEntry<T, C, V>> @Nullable Object serialize(E entry, A action);
	
	public <T, C, E extends AbstractConfigEntry<T, C, V>> Optional<Component> getActionError(E entry, Object value) {
		return Optional.empty();
	}
}
