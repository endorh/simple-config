package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.gui.INavigableTarget;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.entries.AbstractTextFieldListListEntry.AbstractTextFieldListCell;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicEntryListWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractTextFieldListListEntry<T, C extends AbstractTextFieldListCell<T, C, Self>, Self extends AbstractTextFieldListListEntry<T, C, Self>>
  extends AbstractListListEntry<T, C, Self> {
	
	@Internal public AbstractTextFieldListListEntry(
	  ITextComponent fieldName, List<T> value, Function<Self, C> cellFactory
	) {
		super(fieldName, value, cellFactory);
	}
	
	@Internal
	public static abstract class AbstractTextFieldListCell<
	    T, Self extends AbstractTextFieldListCell<T, Self, ListEntry>,
	    ListEntry extends AbstractTextFieldListListEntry<T, Self, ListEntry>
	  > extends AbstractListListEntry.AbstractListCell<T, Self, ListEntry> {
		protected TextFieldWidget widget;
		
		public AbstractTextFieldListCell(ListEntry listListEntry) {
			super(listListEntry);
			// T finalValue = this.substituteDefault(value);
			widget = new TextFieldWidget(
			  Minecraft.getInstance().font, 0, 0, 100, 18,
			  NarratorChatListener.NO_TITLE
			) {
				public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
					setFocused(isSelected);
					super.render(matrices, mouseX, mouseY, delta);
				}
			};
			widget.setFilter(this::isValidText);
			widget.setMaxLength(Integer.MAX_VALUE);
			widget.setBordered(false);
			// this.widget.setText(Objects.toString(finalValue));
			widget.moveCursorToStart();
			widget.setResponder(s -> widget.setTextColor(getPreferredTextColor()));
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			WidgetUtils.forceSetFocus(widget, isSelected);
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
		  MatrixStack mStack, int index, int x, int y, int cellWidth, int cellHeight, int mouseX,
		  int mouseY, boolean isSelected, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
			FontRenderer font = Minecraft.getInstance().font;
			ListEntry listEntry = getListEntry();
			
			final boolean editable = listEntry.shouldRenderEditable();
			int fieldWidth = listEntry.getFieldWidth();
			int fieldX = font.isBidirectional() ? x : x + cellWidth - fieldWidth;
			widget.setWidth(fieldWidth);
			widget.x = fieldX;
			widget.y = y + 1;
			widget.setEditable(editable);
			
			widget.render(mStack, mouseX, mouseY, delta);
			if (isSelected && editable) {
				AbstractTextFieldListCell.fill(
				  mStack, fieldX, y + 12, x + cellWidth, y + 13,
				  hasError() ? 0xffff5555 : 0xffe0e0e0);
			}
			
			if (matchedText != null) {
				fill(
				  mStack, fieldX, y - 2, x + fieldWidth, y + cellHeight - 2,
				  isFocusedMatch()? 0x64FFBD42 : 0x64FFFF42);
			}
		}
		
		public @NotNull List<? extends IGuiEventListener> children() {
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
				widget.setFocus(true);
			}
		}
		
		@Override public List<INavigableTarget> getNavigableChildren() {
			return Lists.newArrayList(this);
		}
		
		@Override public void navigate() {
			final ListEntry listEntry = getListEntry();
			listEntry.expandParents();
			listEntry.claimFocus();
			listEntry.setExpanded(true);
			listEntry.setFocused(this);
			setFocused(widget);
			widget.setFocus(true);
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
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		if (expanded) {
			final List<INavigableTarget> targets = new LinkedList<>(cells);
			targets.add(0, this);
			return targets;
		}
		return super.getNavigableChildren();
	}
}

