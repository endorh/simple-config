package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.entries.AbstractTextFieldListListEntry.AbstractTextFieldListCell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractTextFieldListListEntry<T, C extends AbstractTextFieldListCell<T, C, SELF>, SELF extends AbstractTextFieldListListEntry<T, C, SELF>>
  extends AbstractListListEntry<T, C, SELF> {
	@ApiStatus.Internal
	public AbstractTextFieldListListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, SELF, C> createNewCell
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, createNewCell);
	}
	
	@ApiStatus.Internal
	public static abstract class AbstractTextFieldListCell<T, SELF extends AbstractTextFieldListCell<T, SELF, OUTER_SELF>, OUTER_SELF extends AbstractTextFieldListListEntry<T, SELF, OUTER_SELF>>
	  extends AbstractListListEntry.AbstractListCell<T, SELF, OUTER_SELF> {
		protected TextFieldWidget widget;
		private boolean isSelected;
		
		public AbstractTextFieldListCell(@Nullable T value, OUTER_SELF listListEntry) {
			super(value, listListEntry);
			T finalValue = this.substituteDefault(value);
			this.widget = new TextFieldWidget(Minecraft.getInstance().fontRenderer, 0, 0, 100, 18,
			                                  NarratorChatListener.EMPTY) {
				
				public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
					this.setFocused(isSelected);
					super.render(matrices, mouseX, mouseY, delta);
				}
			};
			this.widget.setValidator(this::isValidText);
			this.widget.setMaxStringLength(Integer.MAX_VALUE);
			this.widget.setEnableBackgroundDrawing(false);
			this.widget.setText(Objects.toString(finalValue));
			this.widget.setCursorPositionZero();
			this.widget.setResponder(s -> this.widget.setTextColor(this.getPreferredTextColor()));
		}
		
		@Override
		public void updateSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}
		
		@Nullable
		protected abstract T substituteDefault(@Nullable T var1);
		
		protected abstract boolean isValidText(@NotNull String var1);
		
		@Override
		public int getCellHeight() {
			return 20;
		}
		
		@Override
		public void render(
		  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
		  int mouseY, boolean isSelected, float delta
		) {
			this.widget.setWidth(entryWidth - 12);
			this.widget.x = x;
			this.widget.y = y + 1;
			this.widget.setEnabled(this.listListEntry.isEditable());
			this.widget.render(matrices, mouseX, mouseY, delta);
			if (isSelected && this.listListEntry.isEditable()) {
				AbstractTextFieldListCell.fill(matrices, x, y + 12, x + entryWidth - 12, y + 13,
				                               this.getConfigError().isPresent() ? -43691 : -2039584);
			}
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.singletonList(this.widget);
		}
	}
}

