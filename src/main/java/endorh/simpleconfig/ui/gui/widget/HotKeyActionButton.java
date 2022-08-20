package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractConfigField;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.Modifier;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons.Actions;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HotKeyActionButton<T> extends MultiFunctionIconButton {
	private final AbstractConfigField<T> entry;
	
	public HotKeyActionButton(AbstractConfigField<T> entry) {
		super(0, 0, 20, 20, Actions.NONE, ButtonAction.of(b -> {}));
		actions.clear();
		defaultAction = ButtonAction.of(i -> {
			if (!entry.isEditingHotKeyAction())
				entry.getScreen().setEditedConfigHotKey(new ConfigHotKey(), null);
			if (i == -1) i = Screen.hasShiftDown()? 1 : 0;
			changeAction(i == 0? 1 : i == 1? -1 : 0);
		}).tooltip(this::getTypeTooltip)
		  .build();
		actions.add(Pair.of(Modifier.NONE, defaultAction));
		setTintColor(0x80242424);
		this.entry = entry;
	}
	
	protected List<ITextComponent> getTypeTooltip() {
		HotKeyActionType<T, ?> type = entry.getHotKeyActionType();
		return Lists.newArrayList(
		  type == null? new TranslationTextComponent("simpleconfig.hotkey.type:help")
		              : type.getDisplayName());
	}
	
	protected AbstractConfigScreen getScreen() {
		return entry.getScreen();
	}
	
	protected <V> @Nullable V getNext(V current, List<V> values, boolean forward) {
		if (values.isEmpty()) return null;
		int index = values.indexOf(current), size = values.size();
		if (index == -1) return values.get(forward? 0 : size - 1);
		int next = (size + 1 + index + (forward? 1 : -1)) % (size + 1);
		return next == size? null : values.get(next);
	}
	
	protected void changeAction(int direction) {
		if (direction != 0) {
			HotKeyActionType<T, ?> type = entry.getHotKeyActionType();
			List<HotKeyActionType<T, ?>> types = entry.getHotKeyActionTypes();
			HotKeyActionType<T, ?> next = getNext(type, types, direction > 0);
			entry.setHotKeyActionType(next);
		} else entry.setHotKeyActionType(null);
	}
	
	public void tick() {
		HotKeyActionType<T, ?> type = entry.getHotKeyActionType();
		setDefaultIcon(type != null? type.getIcon() : Actions.NONE);
		setTintColor(type != null? 0 : 0x80242424);
	}
}
