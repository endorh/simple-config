package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.gui.entries.AbstractTextFieldListListEntry.AbstractTextFieldListCell;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.TextFieldWidgetEx;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractTextFieldListListEntry<T, C extends AbstractTextFieldListCell<T, C, Self>, Self extends AbstractTextFieldListListEntry<T, C, Self>>
  extends AbstractListListEntry<T, C, Self> {
	
	@Internal public AbstractTextFieldListListEntry(
	  Component fieldName, List<T> value, Function<Self, C> cellFactory
	) {
		super(fieldName, value, cellFactory);
	}
	
	@Internal
	public static abstract class AbstractTextFieldListCell<
	  T, Self extends AbstractTextFieldListCell<T, Self, ListEntry>,
	  ListEntry extends AbstractTextFieldListListEntry<T, Self, ListEntry>
	> extends AbstractListListEntry.AbstractListCell<T, Self, ListEntry> {
		protected TextFieldWidgetEx widget;
		
		public AbstractTextFieldListCell(ListEntry listListEntry) {
			super(listListEntry);
			// T finalValue = this.substituteDefault(value);
			widget = new TextFieldWidgetEx(
			  Minecraft.getInstance().font, 0, 0, 100, 18,
			  GameNarrator.NO_TITLE
			) {
				@Override
				public void render(@NotNull GuiGraphics matrices, int mouseX, int mouseY, float delta) {
					setFocused(isSelected);
					super.render(matrices, mouseX, mouseY, delta);
				}
			};
			widget.setFilter(this::isValidText);
			widget.setMaxLength(Integer.MAX_VALUE);
			widget.setBordered(false);
			// this.widget.setText(Objects.toString(finalValue));
			widget.moveCaretToStart();
			widget.setResponder(s -> widget.setTextColor(getPreferredTextColor()));
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
         ((GuiEventListener) widget).setFocused(isSelected);
      }
		
		protected abstract boolean isValidText(@NotNull String var1);
		
		@Override public int getCellHeight() {
			return 20;
		}
		
		@Override public int getCellAreaOffset() {
			return -4;
		}
		
		@Override protected String seekableText() {
			return widget.getValue();
		}
		
		@Override public void renderCell(
         GuiGraphics gg, int index, int x, int y, int cellWidth, int cellHeight, int mouseX,
         int mouseY, boolean isSelected, float delta
		) {
			super.renderCell(gg, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
			Font font = Minecraft.getInstance().font;
			ListEntry listEntry = getListEntry();
			
			final boolean editable = listEntry.shouldRenderEditable();
			int fieldWidth = listEntry.getFieldWidth();
			int fieldX = font.isBidirectional() ? x : x + cellWidth - fieldWidth;
			widget.setWidth(fieldWidth);
			widget.setX(fieldX);
			widget.setY(y + 1);
			widget.setEditable(editable);
			
			widget.render(gg, mouseX, mouseY, delta);
			if (isSelected && editable) gg.fill(
            fieldX, y + 12, x + cellWidth, y + 13,
            hasError() ? 0xFFFF5555 : 0xFFE0E0E0);
			
			if (matchedText != null) gg.fill(
            fieldX, y - 2, x + fieldWidth, y + cellHeight - 2,
            isFocusedMatch() ? 0x64FFBD42 : 0x64FFFF42);
		}
		
		@Override public @NotNull List<? extends GuiEventListener> children() {
			return Collections.singletonList(widget);
		}
		
		@Override public void setFocusedMatch(boolean isFocusedMatch) {
			super.setFocusedMatch(isFocusedMatch);
			if (isFocusedMatch) {
				final ListEntry listEntry = getListEntry();
				listEntry.expandParents();
				listEntry.setExpanded(true);
				listEntry.claimFocus();
				scrollToSelf();
				listEntry.setFocused(this);
				widget.setFocused(true);
			}
		}
		
		@Override public void navigate() {
			final ListEntry listEntry = getListEntry();
			listEntry.expandParents();
			listEntry.claimFocus();
			listEntry.setExpanded(true);
			listEntry.setFocused(this);
			setFocused(widget);
			widget.setFocused(true);
			scrollToSelf();
			listEntry.getEntryList().setSelectedTarget(this);
		}
		
		protected void scrollToSelf() {
			final ListEntry listEntry = getListEntry();
			//noinspection SuspiciousMethodCalls
			final int j = listEntry.cells.indexOf(this);
			int y = 24;
			for (int i = 0; i < j; i++)
				y += listEntry.cells.get(i).getCellHeight();
			final DynamicEntryListWidget<?> parent = listEntry.getEntryList();
			int listY = listEntry.getScrollY();
			double listTarget = parent.scrollFor(listY, listEntry.getItemHeight());
			double target = parent.scrollFor(listY + y, getCellHeight());
			parent.scrollTo(target);
		}
	}
}

