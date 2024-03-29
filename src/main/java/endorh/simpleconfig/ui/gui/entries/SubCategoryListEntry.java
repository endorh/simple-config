package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.SubCategoryListEntry.VoidEntry;
import endorh.simpleconfig.ui.hotkey.HotKeyAction;
import endorh.simpleconfig.ui.hotkey.HotKeyActionType;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SubCategoryListEntry extends CaptionedSubCategoryListEntry<Void, VoidEntry> {
	public SubCategoryListEntry(
	  Component title, List<AbstractConfigListEntry<?>> entries
	) {
		super(title, entries, null);
	}
	
	@Override public HotKeyActionType<Void, ?> getHotKeyActionType() {
		return entries.stream().allMatch(e -> e.getHotKeyActionType() == HotKeyActionTypes.ASSIGN)
		  ? HotKeyActionTypes.ASSIGN.cast() : null;
	}
	
	@Override public void setHotKeyActionType(HotKeyActionType<Void, ?> type) {
		if (type == HotKeyActionTypes.ASSIGN.<Void>cast()) {
			entries.forEach(e -> e.setHotKeyActionType(HotKeyActionTypes.ASSIGN.cast()));
		} else if (type == null) {
			entries.forEach(e -> e.setHotKeyActionType(null));
		}
	}
	
	@Override public @Nullable HotKeyAction<Void> createHotKeyAction() {
		return null;
	}
	
	@Internal public static abstract class VoidEntry extends AbstractConfigListEntry<Void> implements IChildListEntry {
		private VoidEntry(Component title) {
			super(title);
		}
		@Override public void renderChildEntry(
         GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY, float delta
		) {}
		@Override public Rectangle getRowArea() {
			return null;
		}
		@Override public int getScrollY() {
			return 0;
		}
		@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
			return Collections.emptyList();
		}
	}
	
	@Internal public static abstract class VoidEntryBuilder
	  extends FieldBuilder<Void, VoidEntry, VoidEntryBuilder> {
		protected VoidEntryBuilder(ConfigFieldBuilder builder, Component name, Void value) {
			super(VoidEntry.class, builder, name, value);
		}
	}
}
