package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget.EntryDragAction.SelectionDragAction;
import endorh.simpleconfig.ui.gui.entries.BeanListEntry;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigListEntry<T> extends AbstractConfigField<T>
  implements IOverlayRenderer {
	protected final Rectangle entryArea = new Rectangle();
	protected final Rectangle fieldArea = new Rectangle();
	protected final Rectangle rowArea = new Rectangle();
	protected final List<IGuiEventListener> previewListeners = new ArrayList<>();
	protected final Rectangle previewOverlayRectangle = new Rectangle();
	protected final Rectangle previewOverlayCaptionRectangle = new Rectangle();
	protected final Rectangle flagsRectangle = new Rectangle();
	protected final MultiFunctionImageButton mergeButton;
	protected final MultiFunctionImageButton acceptButton;
	protected final CheckboxButton selectionCheckbox; // Not a listener (shouldn't be focused)
	private boolean headless;
	private INavigableTarget lastSelectedSubTarget;
	
	protected int externalPreviewShadowColor = 0xA0242424;
	protected int externalPreviewBgColor = 0xE0424242;
	protected int externalPreviewBorderColor = 0xE0956ED3;
	protected int externalPreviewTextColor = 0xE0A48CD3;
	
	public AbstractConfigListEntry(ITextComponent name) {
		super(name);
		setName(name.getString().replace(".", "")); // Default name
		selectionCheckbox = new CheckboxButton(
		  false, 0, 0, 18, 18,
		  SimpleConfigIcons.Widgets.CHECKBOX_FLAT, null, this::setSelected);
		acceptButton = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Buttons.MERGE_ACCEPT, ButtonAction.of(
		  this::acceptExternalValue
		).active(() -> hasExternalDiff() && !hasAcceptedExternalDiff())
		  .tooltip(() -> Lists.newArrayList(
			 new TranslationTextComponent(
				"simpleconfig.ui.merge.accept." + (getScreen().isEditingServer()? "remote" : "external"))
			   .withStyle(TextFormatting.LIGHT_PURPLE))));
		mergeButton = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Entries.MERGE_CONFLICT, ButtonAction.of(
			 () -> setPreviewingExternal(true)
		  ).active(() -> !isPreviewingExternal() && hasConflictingExternalDiff())
		  .tooltip(() -> Lists.newArrayList(
			 new TranslationTextComponent(
			   "simpleconfig.ui.view_" + (getScreen().isEditingServer()? "remote" : "external") + "_changes")
			   .withStyle(TextFormatting.GOLD)))
		) {
			@Override public void renderToolTip(@NotNull MatrixStack mStack, int mouseX, int mouseY) {
				if (advanced.show_ui_tips) super.renderToolTip(mStack, mouseX, mouseY - 16);
			}
		}.on(MultiFunctionImageButton.Modifier.NONE, ButtonAction.of(() -> {})
		  .active(() -> !isPreviewingExternal() && hasAcceptedExternalDiff())
		  .icon(SimpleConfigIcons.Entries.MERGE_ACCEPTED)
		  .tooltip(() -> Lists.newArrayList(
			 new TranslationTextComponent(
			   "simpleconfig.ui.accepted_" + (getScreen().isEditingServer()? "remote" : "external") + "_changes")
			   .withStyle(TextFormatting.DARK_GREEN)))
		).on(MultiFunctionImageButton.Modifier.NONE, ButtonAction.of(
		  () -> setPreviewingExternal(false)
		).active(this::isPreviewingExternal)
		  .icon(SimpleConfigIcons.Entries.CLOSE_X)
		  .tooltip(Collections::emptyList));
		previewListeners.add(acceptButton);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused) {
			final ResetButton resetButton = getResetButton();
			if (resetButton != null) resetButton.setFocused(false);
		}
	}
	
	@Override public void setPreviewingExternal(boolean previewing) {
		super.setPreviewingExternal(previewing);
		if (isPreviewingExternal()) {
			updateFocused(false);
			getScreen().addOverlay(previewOverlayRectangle, this, 10);
		}
	}
	
	public final int getPreferredTextColor() {
		if (isEditingHotKeyAction())
			return getHotKeyActionType() == null? 0xFF808080 : hasError()? 0xFFFF5555 : 0xFFFFFFFF;
		return shouldRenderEditable() ? hasError() ? 0xFFFF5555 : 0xFFFFFFFF : 0xFFA0A0A0;
	}
	
	@Override public boolean isShown() {
		SearchBarWidget bar = getScreen().getSearchBar();
		AbstractConfigField<?> parent = getParentEntry();
		return isSubEntry()? parent != null && parent.isShown()
		                   : !bar.isExpanded() || !bar.isFilter() || matchesSearch()
		                     || parent != null && parent.shouldShowChildren() || bar.isEmpty();
	}
	
	public CheckboxButton getSelectionCheckbox() {
		return selectionCheckbox;
	}
	
	public Rectangle getSelectionArea() {
		return rowArea;
	}
	
	@Override public Rectangle getRowArea() {
		return rowArea;
	}
	
	@Override public boolean handleModalClicks(double mouseX, double mouseY, int button) {
		if (isPreviewingExternal()) {
			for (IGuiEventListener listener : previewListeners)
				if (listener.mouseClicked(mouseX, mouseY, button)) return true;
			return true;
		}
		return super.handleModalClicks(mouseX, mouseY, button);
	}
	
	@Override public boolean handleModalKeyPress(int keyCode, int scanCode, int modifiers) {
		if (isPreviewingExternal()) {
			for (IGuiEventListener listener : previewListeners)
				if (listener.keyPressed(keyCode, scanCode, modifiers)) return true;
			return true;
		}
		return super.handleModalKeyPress(keyCode, scanCode, modifiers);
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (isSelectable() && selectionCheckbox.mouseClicked(mouseX, mouseY, button)) {
			final DynamicEntryListWidget<?> parent = getEntryList();
			if (parent instanceof ListWidget) ((ListWidget<?>) parent).startDragAction(
			  new SelectionDragAction(isSelected()));
			setDragging(true);
			return true;
		}
		Optional<ImageButton> opt = getMarginButton();
		if (opt.isPresent()) {
			if (opt.get().mouseClicked(mouseX, mouseY, button))
				return true;
		}
		return super.onMouseClicked(mouseX, mouseY, button);
	}
	
	public int getFieldWidth() {
		return getEntryList().getFieldWidth();
	}
	
	public int getKeyFieldWidth() {
		return getEntryList().getKeyFieldWidth();
	}
	
	public int getFieldHeight() {
		return 20;
	}
	
	protected boolean shouldRenderTitle() {
		return !isSubEntry() || getParentEntry() instanceof BeanListEntry;
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		entryArea.setBounds(x, y, entryWidth, entryHeight);
		final DynamicEntryListWidget<?> parent = getEntryList();
		rowArea.setBounds(parent.left, entryArea.y, parent.right - parent.left, getFieldHeight());
		selectionCheckbox.x = parent.left + 2;
		selectionCheckbox.y = y + 12 - 10;
		selectionCheckbox.setToggle(isSelected());
		if (isSelectable() && (
		  getScreen().isSelecting()
		  || selectionCheckbox.isMouseOver(mouseX, selectionCheckbox.y + 1)
		     && getEntryList().getArea().contains(mouseX, mouseY))) {
			selectionCheckbox.render(mStack, mouseX, mouseY, delta);
		}
		ResetButton resetButton = getResetButton();
		FontRenderer font = Minecraft.getInstance().font;
		int fieldWidth = getFieldWidth();
		int fieldHeight = getFieldHeight();
		int fieldX = font.isBidirectional()? x : x + entryWidth - fieldWidth;
		if (shouldRenderTitle()) {
			ITextComponent title = getDisplayedTitle();
			float textX = (float) (font.isBidirectional() ? x + entryWidth - font.width(title) : x);
			renderTitle(
			  mStack, title, textX, index, x, y, entryWidth, entryHeight,
			  mouseX, mouseY, isHovered, delta);
		}
		
		int resetButtonOffset;
		boolean ctrlDown = Screen.hasControlDown();
		if ((isEditingHotKeyAction() || ctrlDown) && !getHotKeyActionTypes().isEmpty()) {
			sideButtonReference.setTarget(hotKeyActionButton);
			resetButtonOffset = hotKeyActionButton.getWidth() + 2;
			fieldWidth -= resetButtonOffset;
			hotKeyActionButton.y = y;
			hotKeyActionButton.x = font.isBidirectional()? x : x + entryWidth - hotKeyActionButton.getWidth();
			fieldX += font.isBidirectional()? hotKeyActionButton.getWidth() : 0;
			hotKeyActionButton.render(mStack, mouseX, mouseY, delta);
		} else if (resetButton != null && !ctrlDown) {
			sideButtonReference.setTarget(this.resetButton);
			resetButtonOffset = resetButton.getWidth() + 2;
			fieldWidth -= resetButtonOffset;
			resetButton.y = y;
			resetButton.x = font.isBidirectional()? x : x + entryWidth - resetButton.getWidth();
			fieldX += font.isBidirectional()? resetButton.getWidth() : 0;
			if (isDisplayingValue())
				resetButton.render(mStack, mouseX, mouseY, delta);
		}
		Optional<ImageButton> opt = getMarginButton();
		ImageButton marginButton = isPreviewingExternal()? mergeButton : opt.orElse(mergeButton);
		marginButton.x = x + entryWidth + 8;
		marginButton.y = y;
		if (opt.isPresent())
			marginButton.render(mStack, mouseX, mouseY, delta);
		if (hasExternalDiff()) {
			if (isPreviewingExternal()) {
				int cH = max(0, getCaptionHeight());
				int pWidth = getPreviewFieldWidth();
				int pX = font.isBidirectional() ? x - 36 : x + entryWidth - pWidth - 8;
				previewOverlayRectangle.setBounds(
				  pX, y - 32 - 2, pWidth + 44, entryHeight + 32 + 4);
			}
		} else {
			if (isPreviewingExternal()) setPreviewingExternal(false);
		}
		fieldArea.setBounds(fieldX, y, fieldWidth, fieldHeight);
		renderField(mStack, fieldX, y, fieldWidth, fieldHeight, x, y, entryWidth, entryHeight, index, mouseX, mouseY, delta);
		Rectangle selectionArea = getSelectionArea();
		if (selectionArea.contains(mouseX, mouseY)) {
			if (parent instanceof SimpleConfigScreen.ListWidget) {
				((ListWidget<?>) parent).thisTimeTarget = selectionArea;
			}
		}
	}
	
	protected Optional<ImageButton> getMarginButton() {
		return hasExternalDiff() ? Optional.of(mergeButton) : Optional.empty();
	}
	
	@Override public Rectangle getNavigableArea() {
		return fieldArea;
	}
	
	@Override public @Nullable INavigableTarget getLastSelectedNavigableSubTarget() {
		return lastSelectedSubTarget;
	}
	
	@Override public void setLastSelectedNavigableSubTarget(@Nullable INavigableTarget target) {
		lastSelectedSubTarget = target;
	}
	
	protected int getPreviewCaptionOffset() {
		return 20;
	}
	
	protected boolean isFieldFullWidth() {
		return false;
	}
	
	public boolean isHeadless() {
		return headless;
	}
	
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}
	
	protected int getPreviewFieldWidth() {
		return isFieldFullWidth()? entryArea.width + 24 : getFieldWidth();
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (area == previewOverlayRectangle) {
			if (!isPreviewingExternal()) return false;
			FontRenderer font = Minecraft.getInstance().font;
			TranslationTextComponent caption = new TranslationTextComponent(
			  "simpleconfig.ui." + (getScreen().isEditingServer()? "remote_changes" : "external_changes"));
			
			final int captionWidth = font.width(caption);
			final int l = area.x + 4, t = area.y + 32 + 2 - 4, r = area.getMaxX() - 4, b = area.getMaxY() - 4 + 2;
			
			// Caption
			int cH = getPreviewCaptionOffset();
			int cY = t - cH;
			int fW = getFieldWidth();
			int cW = min(fW - resetButton.getWidth() - 2, captionWidth + 8) + 8;
			int cX = entryArea.getMaxX() - fW - 8 + 4;
			previewOverlayCaptionRectangle.setBounds(cX, cY, cW, cH);
			
			final DynamicEntryListWidget<?> entryList = getEntryList();
			ScissorsHandler.INSTANCE.pushScissor(entryList.getArea()); {
				// Shadow
				if (cH > 0) {
					fill(mStack, l - 2, t - 2, cX, t, externalPreviewShadowColor);
					fill(mStack, cX + cW, t - 2, r + 2, t, externalPreviewShadowColor);
					fill(mStack, cX - 2, cY, cX, t - 2, externalPreviewShadowColor);
					fill(mStack, cX + cW, cY, cX + cW + 2, t - 2, externalPreviewShadowColor);
					fill(mStack, cX - 2, cY - 2, cX + cW + 2, cY, externalPreviewShadowColor);
				} else fill(mStack, l - 2, t - 2, r + 2, t, externalPreviewShadowColor);
				fill(mStack, l - 2, b, r + 2, b + 2, externalPreviewShadowColor);
				fill(mStack, l - 2, t, l, b, externalPreviewShadowColor);
				fill(mStack, r, t, r + 2, b, externalPreviewShadowColor);
				
				// Outer border
				if (cH > 0) {
					fill(mStack, l, t, cX + 2, t + 2, externalPreviewBorderColor);
					fill(mStack, cX + cW - 2, t, r, t + 2, externalPreviewBorderColor);
					fill(mStack, cX, cY + 2, cX + 2, t, externalPreviewBorderColor);
					fill(mStack, cX + cW - 2, cY + 2, cX + cW, t, externalPreviewBorderColor);
					fill(mStack, cX, cY, cX + cW, cY + 2, externalPreviewBorderColor);
				} else fill(mStack, l, t, r, t + 2, externalPreviewBorderColor);
				fill(mStack, l, b - 2, r, b, externalPreviewBorderColor);
				fill(mStack, l, t + 2, l + 2, b - 2, externalPreviewBorderColor);
				fill(mStack, r - 2, t + 2, r, b - 2, externalPreviewBorderColor);
				
				// Inner border
				fill(mStack, l + 2, t + 2, r - 2, t + 4, externalPreviewBgColor);
				fill(mStack, l + 2, b - 4, r - 2, b - 2, externalPreviewBgColor);
				fill(mStack, l + 2, t + 4, l + 4, b - 4, externalPreviewBgColor);
				fill(mStack, r - 32, t + 4, r - 2, b - 4, externalPreviewBgColor);
				
				// Caption
				fill(mStack, cX + 2, cY + 2, cX + cW - 2, t + 2, externalPreviewBgColor);
				font.drawShadow(
				  mStack, font.split(caption, fW - 16).get(0), cX + 6, cY + 8,
				  externalPreviewTextColor);
				
				// Controls
				acceptButton.x = resetButton.x;
				acceptButton.y = resetButton.y;
				acceptButton.render(mStack, mouseX, mouseY, delta);
				mergeButton.render(mStack, mouseX, mouseY, delta);
			} ScissorsHandler.INSTANCE.popScissor();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		if (area != previewOverlayRectangle)
			return IOverlayRenderer.super.overlayMouseClicked(area, mouseX, mouseY, button);
		if (!getEntryList().getArea().contains(mouseX, mouseY)) return false;
		if (!isPreviewingExternal()) return false;
		if (mouseY < entryArea.y - 4 && !previewOverlayCaptionRectangle.contains(mouseX, mouseY)) {
			setPreviewingExternal(false);
			return false;
		}
		for (IGuiEventListener listener : previewListeners)
			if (listener.mouseClicked(mouseX, mouseY, button)) return true;
		mergeButton.mouseClicked(mouseX, mouseY, button);
		return true;
	}
	
	@Override public void overlayMouseClickedOutside(Rectangle area, double mouseX, double mouseY, int button) {
		IOverlayRenderer.super.overlayMouseClickedOutside(area, mouseX, mouseY, button);
		if (area == previewOverlayRectangle && isPreviewingExternal())
			setPreviewingExternal(false);
	}
	
	@Override public void renderBg(
	  MatrixStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		if (isPreviewingExternal()) {
			fill(
			  mStack, previewOverlayRectangle.x + 8, previewOverlayRectangle.y + 32 + 2,
			  previewOverlayRectangle.getMaxX() - 36, previewOverlayRectangle.getMaxY() - 4 + 2,
			  externalPreviewBgColor);
		}
		super.renderBg(mStack, index, x, y, w, h, mouseX, mouseY, isHovered, delta);
	}
	
	protected void renderField(
	  MatrixStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight,
	  int x, int y, int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		if (this instanceof IChildListEntry) {
			((IChildListEntry) this).renderChild(
			  mStack, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
		}
	}
	
	@Override protected void renderSelectionOverlay(
	  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
	  boolean isHovered, float delta
	) {
		Rectangle area = getSelectionArea();
		fill(mStack, area.x, area.y - 2, area.getMaxX(), area.getMaxY() + 2, selectionColor);
	}
	
	protected void renderTitle(
	  MatrixStack mStack, ITextComponent title, float textX, int index, int x, int y,
	  int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		final FontRenderer font = Minecraft.getInstance().font;
		font.drawShadow(
		  mStack, title.getVisualOrderText(), textX, (float) y + 6, getPreferredTextColor());
		final NavigableSet<EntryTag> entryFlags = getEntryTags();
		if (!entryFlags.isEmpty()) {
			final int textW = font.width(title);
			int flagsX =
			  font.isBidirectional()
			  ? max((int) textX - textW - 4 - 14 * entryFlags.size(), x + getFieldWidth() + 16)
			  : min((int) textX + textW + 4, x + entryWidth - getFieldWidth() - 16);
			int flagsY = y + 6 + font.lineHeight / 2 - 7;
			flagsRectangle.setBounds(flagsX, flagsY, 14 * entryFlags.size(), 14);
			int xx = flagsX;
			for (EntryTag entryFlag : font.isBidirectional()? entryFlags.descendingSet() : entryFlags) {
				entryFlag.getIcon().renderCentered(mStack, xx, flagsY, 14, 14);
				xx += 14;
			}
		}
	}
	
	protected boolean isMouseOverFlags(int mouseX, int mouseY) {
		return flagsRectangle.contains(mouseX, mouseY);
	}
	
	public boolean isMouseOverRow(double mouseX, double mouseY) {
		return rowArea.contains(mouseX, mouseY);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return entryArea.contains(mouseX, mouseY);
	}
	
	@Override public int getScrollY() {
		DynamicEntryListWidget<?> entryList = getEntryList();
		return (int) round(entryArea.y - entryList.top + entryList.getScroll());
	}
	
	@Override public final @NotNull List<? extends IGuiEventListener> children() {
		if (isPreviewingExternal())
			return previewListeners;
		return getEntryListeners();
	}
	
	protected abstract @NotNull List<? extends IGuiEventListener> getEntryListeners();
}