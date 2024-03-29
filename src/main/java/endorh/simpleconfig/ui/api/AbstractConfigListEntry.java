package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget.EntryDragAction.SelectionDragAction;
import endorh.simpleconfig.ui.gui.entries.BeanListEntry;
import endorh.simpleconfig.ui.gui.widget.*;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static endorh.simpleconfig.api.SimpleConfigTextUtil.subText;
import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigListEntry<T> extends AbstractConfigField<T>
  implements IOverlayRenderer {
	protected final Rectangle entryArea = new Rectangle();
	protected final Rectangle fieldArea = new Rectangle();
	protected final Rectangle rowArea = new Rectangle();
	protected final List<GuiEventListener> previewListeners = new ArrayList<>();
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
	
	public AbstractConfigListEntry(Component name) {
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
			 Component.translatable("simpleconfig.ui.merge.accept." + (getScreen().isEditingServer()? "remote" : "external"))
			   .withStyle(ChatFormatting.LIGHT_PURPLE))));
		mergeButton = new MultiFunctionImageButton(
		  0, 0, 20, 20, SimpleConfigIcons.Entries.MERGE_CONFLICT, ButtonAction.of(
			 () -> setPreviewingExternal(true)
		  ).active(() -> !isPreviewingExternal() && hasConflictingExternalDiff())
		  .tooltip(() -> Lists.newArrayList(
			 Component.translatable("simpleconfig.ui.view_" + (getScreen().isEditingServer()? "remote" : "external") + "_changes")
			   .withStyle(ChatFormatting.GOLD)))
		) {
			@Override public void setTooltip(@Nullable Tooltip tooltip) {
				if (advanced.show_ui_tips) super.setTooltip(tooltip);
			}
		}.on(MultiFunctionImageButton.Modifier.NONE, ButtonAction.of(() -> {})
		  .active(() -> !isPreviewingExternal() && hasAcceptedExternalDiff())
		  .icon(SimpleConfigIcons.Entries.MERGE_ACCEPTED)
		  .tooltip(() -> Lists.newArrayList(
			 Component.translatable("simpleconfig.ui.accepted_" + (getScreen().isEditingServer()? "remote" : "external") + "_changes")
			   .withStyle(ChatFormatting.DARK_GREEN)))
		).on(MultiFunctionImageButton.Modifier.NONE, ButtonAction.of(
		  () -> setPreviewingExternal(false)
		).active(this::isPreviewingExternal)
		  .icon(SimpleConfigIcons.Entries.CLOSE_X)
		  .tooltip(Collections::emptyList));
		previewListeners.add(acceptButton);
	}

	@Override
	public @Nullable ComponentPath getCurrentFocusPath() {
		return wrapPath(super.getCurrentFocusPath());
	}

	@Override
	public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
		return wrapPath(super.nextFocusPath(e));
	}

	public static ComponentPath wrapPath(ComponentPath path) {
		if (path instanceof ComponentPath.Path p) {
			GuiEventListener entry = path.component();
			if (entry instanceof AbstractConfigListEntry<?>)
				return EntryPath.entryPath((AbstractConfigListEntry<?>) entry, p.childPath());
		} else if (path instanceof ComponentPath.Leaf l) {
			GuiEventListener entry = path.component();
			if (entry instanceof AbstractConfigListEntry<?>)
				return EntryLeafPath.entryLeaf((AbstractConfigListEntry<?>) entry);
		}
		return path;
	}

	public record EntryPath(
		@NotNull AbstractConfigListEntry<?> entry,
		@NotNull ComponentPath childPath
	) implements ComponentPath {
		public static EntryPath entryPath(@NotNull AbstractConfigListEntry<?> entry, @NotNull ComponentPath childPath) {
			return new EntryPath(entry, childPath);
		}

		@Override
		public @NotNull GuiEventListener component() {
			return entry;
		}

		@Override
		public void applyFocus(boolean focused) {
			if (focused) {
				if (!(childPath.component() instanceof AbstractConfigListEntry<?>)) {
					entry.navigate();
					ComponentPath curr = entry.getCurrentFocusPath();
					if (curr != null) curr.applyFocus(false);
				} else entry.setFocused(true);
				entry.setFocused(childPath.component());
			} else entry.setFocused(false);
			childPath.applyFocus(focused);
		}
	}

	// Adapter from Mojang's focus path to our `navigate` recursive focus update
	public record EntryLeafPath(@NotNull AbstractConfigListEntry<?> entry) implements ComponentPath {
		public static EntryLeafPath entryLeaf(@NotNull AbstractConfigListEntry<?> entry) {
			return new EntryLeafPath(entry);
		}

		@Override
		public @NotNull GuiEventListener component() {
			return entry;
		}

		@Override
		public void applyFocus(boolean focused) {
			if (focused) entry.navigate();
         else entry.setFocused(false);
		}
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
			for (GuiEventListener listener : previewListeners)
				if (listener.mouseClicked(mouseX, mouseY, button)) return true;
			return true;
		}
		return super.handleModalClicks(mouseX, mouseY, button);
	}
	
	@Override public boolean handleModalKeyPress(int keyCode, int scanCode, int modifiers) {
		if (isPreviewingExternal()) {
			for (GuiEventListener listener : previewListeners)
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
      GuiGraphics gg, int index, int x, int y, int entryWidth, int entryHeight,
      int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(gg, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		entryArea.setBounds(x, y, entryWidth, entryHeight);
		final DynamicEntryListWidget<?> parent = getEntryList();
		rowArea.setBounds(parent.left, entryArea.y, parent.right - parent.left, getFieldHeight());
		selectionCheckbox.setX(parent.left + 2);
		selectionCheckbox.setY(y + 12 - 10);
		selectionCheckbox.setToggle(isSelected());
		if (isSelectable() && (
		  getScreen().isSelecting()
		  || selectionCheckbox.isMouseOver(mouseX, selectionCheckbox.getY() + 1)
		     && getEntryList().getArea().contains(mouseX, mouseY))) {
			selectionCheckbox.render(gg, mouseX, mouseY, delta);
		}
		ResetButton resetButton = getResetButton();
		Font font = Minecraft.getInstance().font;
		int fieldWidth = getFieldWidth();
		int fieldHeight = getFieldHeight();
		int fieldX = font.isBidirectional()? x : x + entryWidth - fieldWidth;
		if (shouldRenderTitle()) {
			Component title = getDisplayedTitle();
			float textX = (float) (font.isBidirectional() ? x + entryWidth - font.width(title) : x);
			renderTitle(
			  gg, title, textX, index, x, y, entryWidth, entryHeight,
			  mouseX, mouseY, isHovered, delta);
		}
		
		int resetButtonOffset;
		boolean ctrlDown = Screen.hasControlDown();
		if ((isEditingHotKeyAction() || ctrlDown) && !getHotKeyActionTypes().isEmpty()) {
			sideButtonReference.setTarget(hotKeyActionButton);
			resetButtonOffset = hotKeyActionButton.getWidth() + 2;
			fieldWidth -= resetButtonOffset;
			hotKeyActionButton.setY(y);
			hotKeyActionButton.setX(font.isBidirectional() ? x : x + entryWidth - hotKeyActionButton.getWidth());
			fieldX += font.isBidirectional()? hotKeyActionButton.getWidth() : 0;
			hotKeyActionButton.render(gg, mouseX, mouseY, delta);
		} else if (resetButton != null && !ctrlDown) {
			sideButtonReference.setTarget(this.resetButton);
			resetButtonOffset = resetButton.getWidth() + 2;
			fieldWidth -= resetButtonOffset;
			resetButton.setY(y);
			resetButton.setX(font.isBidirectional() ? x : x + entryWidth - resetButton.getWidth());
			fieldX += font.isBidirectional()? resetButton.getWidth() : 0;
			if (isDisplayingValue())
				resetButton.render(gg, mouseX, mouseY, delta);
		}
		Optional<ImageButton> opt = getMarginButton();
		ImageButton marginButton = isPreviewingExternal()? mergeButton : opt.orElse(mergeButton);
		marginButton.setX(x + entryWidth + 8);
		marginButton.setY(y);
		if (opt.isPresent())
			marginButton.render(gg, mouseX, mouseY, delta);
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
		renderField(gg, fieldX, y, fieldWidth, fieldHeight, x, y, entryWidth, entryHeight, index, mouseX, mouseY, delta);
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
      GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (area == previewOverlayRectangle) {
			if (!isPreviewingExternal()) return false;
			Font font = Minecraft.getInstance().font;
			MutableComponent caption = Component.translatable(
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
					gg.fill(l - 2, t - 2, cX, t, externalPreviewShadowColor);
					gg.fill(cX + cW, t - 2, r + 2, t, externalPreviewShadowColor);
					gg.fill(cX - 2, cY, cX, t - 2, externalPreviewShadowColor);
					gg.fill(cX + cW, cY, cX + cW + 2, t - 2, externalPreviewShadowColor);
					gg.fill(cX - 2, cY - 2, cX + cW + 2, cY, externalPreviewShadowColor);
				} else gg.fill(l - 2, t - 2, r + 2, t, externalPreviewShadowColor);
				gg.fill(l - 2, b, r + 2, b + 2, externalPreviewShadowColor);
				gg.fill(l - 2, t, l, b, externalPreviewShadowColor);
				gg.fill(r, t, r + 2, b, externalPreviewShadowColor);
				
				// Outer border
				if (cH > 0) {
					gg.fill(l, t, cX + 2, t + 2, externalPreviewBorderColor);
					gg.fill(cX + cW - 2, t, r, t + 2, externalPreviewBorderColor);
					gg.fill(cX, cY + 2, cX + 2, t, externalPreviewBorderColor);
					gg.fill(cX + cW - 2, cY + 2, cX + cW, t, externalPreviewBorderColor);
					gg.fill(cX, cY, cX + cW, cY + 2, externalPreviewBorderColor);
				} else gg.fill(l, t, r, t + 2, externalPreviewBorderColor);
				gg.fill(l, b - 2, r, b, externalPreviewBorderColor);
				gg.fill(l, t + 2, l + 2, b - 2, externalPreviewBorderColor);
				gg.fill(r - 2, t + 2, r, b - 2, externalPreviewBorderColor);
				
				// Inner border
				gg.fill(l + 2, t + 2, r - 2, t + 4, externalPreviewBgColor);
				gg.fill(l + 2, b - 4, r - 2, b - 2, externalPreviewBgColor);
				gg.fill(l + 2, t + 4, l + 4, b - 4, externalPreviewBgColor);
				gg.fill(r - 32, t + 4, r - 2, b - 4, externalPreviewBgColor);
				
				// Caption
				gg.fill(cX + 2, cY + 2, cX + cW - 2, t + 2, externalPreviewBgColor);
				gg.drawString( // TODO: shadow
				  font, font.split(caption, fW - 16).get(0), cX + 6, cY + 8,
				  externalPreviewTextColor);
				
				// Controls
				acceptButton.setX(resetButton.getX());
				acceptButton.setY(resetButton.getY());
				acceptButton.render(gg, mouseX, mouseY, delta);
				mergeButton.render(gg, mouseX, mouseY, delta);
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
		for (GuiEventListener listener : previewListeners)
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
      GuiGraphics gg, int index, int x, int y, int w, int h, int mouseX, int mouseY,
      boolean isHovered, float delta
	) {
		if (isPreviewingExternal()) {
			gg.fill(
			  previewOverlayRectangle.x + 8, previewOverlayRectangle.y + 32 + 2,
			  previewOverlayRectangle.getMaxX() - 36, previewOverlayRectangle.getMaxY() - 4 + 2,
			  externalPreviewBgColor);
		}
		super.renderBg(gg, index, x, y, w, h, mouseX, mouseY, isHovered, delta);
	}
	
	protected void renderField(
      GuiGraphics gg, int fieldX, int fieldY, int fieldWidth, int fieldHeight,
      int x, int y, int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		if (this instanceof IChildListEntry) {
			((IChildListEntry) this).renderChild(
			  gg, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
		}
	}
	
	@Override protected void renderSelectionOverlay(
      GuiGraphics gg, int index, int y, int x, int w, int h, int mouseX, int mouseY,
      boolean isHovered, float delta
	) {
		Rectangle area = getSelectionArea();
		gg.fill(area.x, area.y - 2, area.getMaxX(), area.getMaxY() + 2, selectionColor);
	}
	
	protected void renderTitle(
      GuiGraphics gg, Component title, float textX, int index, int x, int y,
      int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		Font font = Minecraft.getInstance().font;
		NavigableSet<EntryTag> entryFlags = getEntryTags();
		int flagsW = 14 * entryFlags.size();
		int textW = entryWidth - getFieldWidth() - flagsW - 4;
		boolean hoveredTitle = mouseY >= y + 4 && mouseY < y + 16 && mouseX > textX && mouseX < textX + textW;
		if (hoveredTitle) textW = font.width(title);
		MutableComponent text = subText(title, 0, font.substrByWidth(title, textW).getString().length());
		gg.drawString(font, text, (int) textX, y + 6, getPreferredTextColor()); // TODO: Shadow
		if (!entryFlags.isEmpty()) {
			int tW = font.width(text);
			int flagsX;
			if (font.isBidirectional()) {
				flagsX = (int) textX - tW - 4 - flagsW;
				if (!hoveredTitle) flagsX = max(flagsX, x + getFieldWidth() + flagsW + 2);
			} else {
				flagsX = (int) textX + tW + 4;
				if (!hoveredTitle) flagsX = min(flagsX, x + entryWidth - getFieldWidth() - flagsW - 2);
			}
			int flagsY = y + 6 + font.lineHeight / 2 - 7;
			flagsRectangle.setBounds(flagsX, flagsY, flagsW, 14);
			int xx = flagsX;
			for (EntryTag entryFlag : font.isBidirectional()? entryFlags.descendingSet() : entryFlags) {
				entryFlag.getIcon().renderCentered(gg, xx, flagsY, 14, 14);
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
	
	@Override public final @NotNull List<? extends GuiEventListener> children() {
		if (isPreviewingExternal())
			return previewListeners;
		return getEntryListeners();
	}
	
	protected abstract @NotNull List<? extends GuiEventListener> getEntryListeners();
}