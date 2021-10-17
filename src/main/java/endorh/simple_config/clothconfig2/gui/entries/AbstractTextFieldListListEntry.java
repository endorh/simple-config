package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.entries.AbstractTextFieldListListEntry.AbstractTextFieldListCell;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.gui.INavigableTarget;
import net.minecraft.client.Minecraft;
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
	public static abstract class AbstractTextFieldListCell<T, Self extends AbstractTextFieldListCell<T, Self, ListEntry>, ListEntry extends AbstractTextFieldListListEntry<T, Self, ListEntry>>
	  extends AbstractListListEntry.AbstractListCell<T, Self, ListEntry> {
		protected TextFieldWidget widget;
		
		public AbstractTextFieldListCell(ListEntry listListEntry) {
			super(listListEntry);
			// T finalValue = this.substituteDefault(value);
			widget = new TextFieldWidget(Minecraft.getInstance().fontRenderer, 0, 0, 100, 18,
			                                  NarratorChatListener.EMPTY) {
				
				public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
					setFocused(isSelected);
					super.render(matrices, mouseX, mouseY, delta);
				}
			};
			widget.setValidator(this::isValidText);
			widget.setMaxStringLength(Integer.MAX_VALUE);
			widget.setEnableBackgroundDrawing(false);
			// this.widget.setText(Objects.toString(finalValue));
			widget.setCursorPositionZero();
			widget.setResponder(s -> widget.setTextColor(getPreferredTextColor()));
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			forceSetFocus(widget, isSelected);
		}
		
		protected abstract boolean isValidText(@NotNull String var1);
		
		@Override public int getCellHeight() {
			return 20;
		}
		
		@Override public int getCellDecorationOffset() {
			return -4;
		}
		
		@Override protected String seekableText() {
			return widget.getText();
		}
		
		@Override public void render(
		  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
		  int mouseY, boolean isSelected, float delta
		) {
			super.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
			widget.setWidth(entryWidth - 12);
			widget.x = x;
			widget.y = y + 1;
			final boolean editable = getListEntry().isEditable();
			widget.setEnabled(editable);
			widget.render(mStack, mouseX, mouseY, delta);
			if (isSelected && editable) {
				AbstractTextFieldListCell.fill(
				  mStack, x, y + 12, x + entryWidth - 12, y + 13,
				  hasErrors() ? 0xffff5555 : 0xffe0e0e0);
			}
			if (matchedText != null) {
				fill(
				  mStack, x - 32, y - 2, x + entryWidth, y + entryHeight - 8,
				  isFocusedMatch()? 0x64FFBD42 : 0x64FFFF42);
			}
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
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
				listEntry.setListener(this);
				widget.setFocused2(true);
			}
		}
		
		@Override public List<INavigableTarget> getNavigableChildren() {
			return Lists.newArrayList(this);
		}
		
		@Override public void onNavigate() {
			final ListEntry listEntry = getListEntry();
			listEntry.expandParents();
			listEntry.claimFocus();
			listEntry.setExpanded(true);
			listEntry.setListener(this);
			setListener(widget);
			widget.setFocused2(true);
			scrollToSelf();
			listEntry.getParent().setSelectedTarget(this);
		}
		
		protected void scrollToSelf() {
			final ListEntry listEntry = getListEntry();
			//noinspection SuspiciousMethodCalls
			final int j = listEntry.cells.indexOf(this);
			int y = 24;
			for (int i = 0; i < j; i++)
				y += listEntry.cells.get(i).getCellHeight();
			final DynamicEntryListWidget<?> parent = listEntry.getParent();
			final int half = (parent.bottom - parent.top) / 2;
			int target = max(
			  0, listEntry.getScrollY() - parent.top - half
			     + min(half, listEntry.getCaptionHeight() / 2) + y);
			parent.scrollTo(target);
		}
	}
	
	@Override public String seekableValueText() {
		return "";
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

