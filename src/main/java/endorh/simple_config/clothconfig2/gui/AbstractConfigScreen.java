package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.SimpleConfigMod.ClientConfig.confirm;
import endorh.simple_config.SimpleConfigMod.KeyBindings;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.entries.KeyCodeEntry;
import endorh.simple_config.clothconfig2.gui.widget.CheckboxButton;
import endorh.simple_config.clothconfig2.gui.widget.SearchBarWidget;
import endorh.simple_config.clothconfig2.impl.EditHistory;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.InputMappings;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static endorh.simple_config.SimpleConfigMod.CLIENT_CONFIG;

public abstract class AbstractConfigScreen extends Screen
  implements ConfigScreen, IExtendedDragAwareNestedGuiEventHandler, ScissorsScreen,
             IOverlayCapableScreen, IEntryHolder, IDialogCapableScreen {
	protected static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	protected final ResourceLocation backgroundLocation;
	protected boolean confirmUnsaved = confirm.unsaved;
	protected boolean confirmSave = confirm.save;
	protected final Screen parent;
	protected boolean alwaysShowTabs = false;
	protected boolean transparentBackground = false;
	@Nullable protected String defaultFallbackCategory = null;
	public int selectedCategoryIndex = 0;
	protected boolean editable = true;
	protected KeyCodeEntry focusedBinding;
	protected ModifierKeyCode startedKeyCode = null;
	protected final List<Tooltip> tooltips = Lists.newArrayList();
	@Nullable protected Runnable savingRunnable = null;
	@Nullable protected Consumer<Screen> afterInitConsumer = null;
	protected Pair<Integer, IGuiEventListener> dragged = null;
	protected Map<String, ConfigCategory> categoryMap;
	protected List<ConfigCategory> sortedCategories;
	
	protected SortedOverlayCollection sortedOverlays = new SortedOverlayCollection();
	
	protected EditHistory history = new EditHistory();
	
	protected List<AbstractDialog> dialogs = Lists.newLinkedList();
	
	protected AbstractConfigScreen(
	  Screen parent, ITextComponent title, ResourceLocation backgroundLocation,
	  Map<String, ConfigCategory> categories
	) {
		super(title);
		this.parent = parent;
		this.backgroundLocation = backgroundLocation;
		this.categoryMap = categories;
		this.sortedCategories = categories.values().stream()
		  .sorted(Comparator.comparing(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
	}
	
	@Override public void setSavingRunnable(@Nullable Runnable savingRunnable) {
		this.savingRunnable = savingRunnable;
	}
	
	@Override public void setAfterInitConsumer(@Nullable Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
	}
	
	@Override public ResourceLocation getBackgroundLocation() {
		return backgroundLocation;
	}
	
	@Override public boolean isRequiresRestart() {
		for (ConfigCategory cat : categoryMap.values()) {
			for (AbstractConfigEntry<?> entry : cat.getEntries()) {
				if (entry.hasErrors() || !entry.isEdited() ||
				    !entry.isRequiresRestart()) continue;
				return true;
			}
		}
		return false;
	}
	
	public void setSelectedCategory(String name) {
		final ConfigCategory cat = categoryMap.get(name);
		if (cat == null) throw new IllegalArgumentException("Unknown category: " + name);
		setSelectedCategory(sortedCategories.indexOf(cat));
	}
	
	public void setSelectedCategory(int index) {
		if (index < 0 || index >= categoryMap.size())
			throw new IndexOutOfBoundsException("index: " + index + ", size: " + categoryMap.size());
		if (selectedCategoryIndex != index) {
			selectedCategoryIndex = index;
			init(Minecraft.getInstance(), width, height);
		}
	}
	
	public @Nullable String getSelectedCategoryName() {
		return sortedCategories.get(selectedCategoryIndex).getName();
	}
	
	@Override
	public boolean isEdited() {
		for (ConfigCategory cat : categoryMap.values()) {
			for (AbstractConfigEntry<?> entry : cat.getEntries()) {
				if (!entry.isEdited()) continue;
				return true;
			}
		}
		return false;
	}
	
	public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		return null;
	}
	
	public boolean isShowingTabs() {
		return isAlwaysShowTabs() || categoryMap.size() > 1;
	}
	
	public boolean isAlwaysShowTabs() {
		return alwaysShowTabs;
	}
	
	@Internal public void setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
	}
	
	public boolean isTransparentBackground() {
		return transparentBackground && Minecraft.getInstance().world != null;
	}
	
	@Internal public void setTransparentBackground(boolean transparentBackground) {
		this.transparentBackground = transparentBackground;
	}
	
	public String getFallbackCategory() {
		if (defaultFallbackCategory != null) return defaultFallbackCategory;
		return categoryMap.keySet().iterator().next();
	}
	
	@Internal public void setFallbackCategory(@Nullable String defaultFallbackCategory) {
		this.defaultFallbackCategory = defaultFallbackCategory;
		for (int i = 0; i < sortedCategories.size(); i++) {
			if (sortedCategories.get(i).getName().equals(defaultFallbackCategory)) {
				selectedCategoryIndex = i;
				break;
			}
		}
	}
	
	@Override public void saveAll(boolean openOtherScreens) {
		saveAll(openOtherScreens, false);
	}
	
	public void saveAll(boolean openOtherScreens, boolean forceConfirm) {
		if ((confirmSave || forceConfirm) && isEdited()) {
			final ConfirmDialog dialog = new ConfirmDialog(
			  (b, s) -> {
				  if (b) doSaveAll(openOtherScreens);
				  if (s[0]) CLIENT_CONFIG.set("confirm.save", false);
			  }, new TranslationTextComponent("simple-config.ui.confirm_save"),
			  Lists.newArrayList(new TranslationTextComponent("simple-config.ui.confirm_save.msg")),
			  DialogTexts.GUI_CANCEL, new TranslationTextComponent("text.cloth-config.save_and_done"), this,
			  new CheckboxButton(false, 0, 0, 100, new TranslationTextComponent("simple-config.ui.dont_confirm"), null));
			dialog.setConfirmButtonTint(0x8042BD42);
			addDialog(dialog);
		} else {
			doSaveAll(openOtherScreens);
		}
	}
	
	protected void doSaveAll(boolean openOtherScreens) {
		for (ConfigCategory cat : categoryMap.values()) {
			for (AbstractConfigEntry<?> entry : cat.getEntries()) entry.save();
		}
		save();
		if (openOtherScreens && minecraft != null) {
			if (isRequiresRestart())
				minecraft.displayGuiScreen(new ClothRequiresRestartScreen(parent));
			else minecraft.displayGuiScreen(parent);
		}
	}
	
	public void save() {
		Optional.ofNullable(savingRunnable).ifPresent(Runnable::run);
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	@Internal public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public KeyCodeEntry getFocusedBinding() {
		return focusedBinding;
	}
	
	@Internal public void setFocusedBinding(KeyCodeEntry focusedBinding) {
		this.focusedBinding = focusedBinding;
		if (focusedBinding != null) {
			startedKeyCode = this.focusedBinding.getValue();
			startedKeyCode.setKeyCodeAndModifier(InputMappings.INPUT_INVALID, Modifier.none());
		} else {
			startedKeyCode = null;
		}
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (focusedBinding != null && startedKeyCode != null &&
		    !startedKeyCode.isUnknown() && focusedBinding.isAllowMouse()) {
			focusedBinding.setValue(startedKeyCode);
			setFocusedBinding(null);
			return true;
		}
		if (handleOverlaysMouseReleased(mouseX, mouseY, button))
			return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY))
			return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta))
			return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	public boolean keyReleased(int int_1, int int_2, int int_3) {
		if (getDragged() != null)
			return true;
		if (focusedBinding != null && startedKeyCode != null &&
		    focusedBinding.isAllowKey()) {
			focusedBinding.setValue(startedKeyCode);
			setFocusedBinding(null);
			return true;
		}
		return super.keyReleased(int_1, int_2, int_3);
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (getDragged() != null
		    || handleDialogsMouseClicked(mouseX, mouseY, button)
		    || handleOverlaysMouseClicked(mouseX, mouseY, button))
			return true;
		if (focusedBinding != null && startedKeyCode != null &&
		    focusedBinding.isAllowMouse()) {
			if (startedKeyCode.isUnknown()) {
				startedKeyCode.setKeyCode(InputMappings.Type.MOUSE.getOrMakeInput(button));
			} else if (focusedBinding.isAllowModifiers() &&
			           startedKeyCode.getType() == InputMappings.Type.KEYSYM) {
				int code = startedKeyCode.getKeyCode().getKeyCode();
				if (Minecraft.IS_RUNNING_ON_MAC ? code == 343 || code == 347
				                                : code == 341 || code == 345) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
					startedKeyCode.setKeyCode(InputMappings.Type.MOUSE.getOrMakeInput(button));
					return true;
				}
				if (code == 344 || code == 340) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
					startedKeyCode.setKeyCode(InputMappings.Type.MOUSE.getOrMakeInput(button));
					return true;
				}
				if (code == 342 || code == 346) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
					startedKeyCode.setKeyCode(InputMappings.Type.MOUSE.getOrMakeInput(button));
					return true;
				}
			}
			return true;
		}
		if (focusedBinding != null) {
			return true;
		}
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseClicked(mouseX, mouseY, button);
	}
	
	protected void recomputeFocus() {}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (handleDialogsCharTyped(codePoint, modifiers)) return true;
		return super.charTyped(codePoint, modifiers);
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (handleDialogsKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (getDragged() != null)
			return true; // Suppress
		if (focusedBinding != null && (focusedBinding.isAllowKey() || keyCode == 256)) {
			if (keyCode != 256) {
				if (startedKeyCode.isUnknown()) {
					startedKeyCode.setKeyCode(InputMappings.getInputByCode(keyCode, scanCode));
				} else if (focusedBinding.isAllowModifiers()) {
					if (startedKeyCode.getType() == InputMappings.Type.KEYSYM) {
						int code = startedKeyCode.getKeyCode().getKeyCode();
						if (Minecraft.IS_RUNNING_ON_MAC ? code == 343 || code == 347
						                                : code == 341 || code == 345) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
							startedKeyCode.setKeyCode(InputMappings.getInputByCode(
							  keyCode, scanCode));
							return true;
						}
						if (code == 344 || code == 340) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
							startedKeyCode.setKeyCode(InputMappings.getInputByCode(
							  keyCode, scanCode));
							return true;
						}
						if (code == 342 || code == 346) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
							startedKeyCode.setKeyCode(InputMappings.getInputByCode(
							  keyCode, scanCode));
							return true;
						}
					}
					if (Minecraft.IS_RUNNING_ON_MAC ? keyCode == 343 || keyCode == 347
					                                : keyCode == 341 || keyCode == 345) {
						Modifier modifier = startedKeyCode.getModifier();
						startedKeyCode.setModifier(
						  Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
						return true;
					}
					if (keyCode == 344 || keyCode == 340) {
						Modifier modifier = startedKeyCode.getModifier();
						startedKeyCode.setModifier(
						  Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
						return true;
					}
					if (keyCode == 342 || keyCode == 346) {
						Modifier modifier = startedKeyCode.getModifier();
						startedKeyCode.setModifier(
						  Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
						return true;
					}
				}
			} else {
				focusedBinding.setValue(ModifierKeyCode.unknown());
				setFocusedBinding(null);
			}
			return true;
		}
		if (focusedBinding != null)
			return true;
		if (keyCode == 256) { // Escape key
			if (handleOverlaysEscapeKey())
				return true;
			if (shouldCloseOnEsc()) {
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
				return quit();
			}
		}
		if (screenKeyPressed(keyCode, scanCode, modifiers))
			return true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	protected boolean canSave() {
		return isEdited();
	}
	
	protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		InputMappings.Input key = InputMappings.getInputByCode(keyCode, scanCode);
		if (KeyBindings.NEXT_PAGE.isActiveAndMatches(key)) {
			int i = selectedCategoryIndex - 1;
			if (i < 0) i = categoryMap.size() - 1;
			setSelectedCategory(i);
			recomputeFocus();
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			return true;
		} else if (KeyBindings.PREV_PAGE.isActiveAndMatches(key)) {
			int i = selectedCategoryIndex + 1;
			if (i >= categoryMap.size()) i = 0;
			setSelectedCategory(i);
			recomputeFocus();
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			return true;
		} else if (KeyBindings.SAVE.isActiveAndMatches(key)) {
			if (canSave()) saveAll(true, true);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			return true;
		}
		// if (Screen.hasControlDown()) {
		// 	int i;
		// 	switch (keyCode) {
		// 		case 266: // Page Up
		// 			i = selectedCategoryIndex - 1;
		// 			if (i < 0) i = categoryMap.size() - 1;
		// 			setSelectedCategory(i);
		// 			recomputeFocus();
		// 			Minecraft.getInstance().getSoundHandler().play(
		// 			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
		// 			return true;
		// 		case 267: // Page Down
		// 			i = selectedCategoryIndex + 1;
		// 			if (i >= categoryMap.size()) i = 0;
		// 			setSelectedCategory(i);
		// 			recomputeFocus();
		// 			Minecraft.getInstance().getSoundHandler().play(
		// 			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
		// 			return true;
		// 		case 59: // Ctrl + S
		// 			if (canSave()) saveAll(true, true);
		// 			Minecraft.getInstance().getSoundHandler().play(
		// 			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
		// 			return true;
		// 	}
		// }
		return false;
	}
	
	protected final boolean quit() {
		if (minecraft == null) return false;
		if (confirmUnsaved && isEdited()) {
			final ConfirmDialog dialog = new ConfirmDialog(
			  (b, s) -> {
				  if (s[0]) CLIENT_CONFIG.set("confirm.unsaved", false);
				  if (b) minecraft.displayGuiScreen(parent);
			  },
			  new TranslationTextComponent("text.cloth-config.quit_config"),
			  Util.<List<ITextComponent>>make(
				 new ArrayList<>(),
				 l -> l.add(new TranslationTextComponent("text.cloth-config.quit_config_sure"))),
			  new TranslationTextComponent("gui.cancel"),
			  new TranslationTextComponent("text.cloth-config.quit_discard"), this,
			  new CheckboxButton(false, 0, 0, 100, new TranslationTextComponent("simple-config.ui.dont_confirm"), null));
			dialog.setConfirmButtonTint(0x80BD2424);
			addDialog(dialog);
		} else minecraft.displayGuiScreen(parent);
		return true;
	}
	
	public void tick() {
		super.tick();
		boolean edited = isEdited();
		Optional.ofNullable(getQuitButton()).ifPresent(button -> button.setMessage(
		  edited ? new TranslationTextComponent("text.cloth-config.cancel_discard")
		         : new TranslationTextComponent("gui.cancel")));
		for (IGuiEventListener child : getEventListeners()) {
			if (!(child instanceof ITickableTileEntity)) continue;
			((ITickableTileEntity) child).tick();
		}
	}
	
	@Nullable protected Widget getQuitButton() {
		return null;
	}
	
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final boolean hasDialog = !dialogs.isEmpty();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		super.render(mStack, suppressHover? -1 : mouseX, suppressHover? -1 : mouseY, delta);
		renderOverlays(mStack, hasDialog? -1 : mouseX, hasDialog? -1 : mouseY, delta);
		if (hasDialog) tooltips.clear();
		renderDialogs(mStack, mouseX, mouseY, delta);
		for (Tooltip tooltip : tooltips)
			renderTooltip(mStack, tooltip.getText(), tooltip.getX(), tooltip.getY());
		tooltips.clear();
	}
	
	@Override public void addTooltip(Tooltip tooltip) {
		tooltips.add(tooltip);
	}
	
	@Override public boolean removeTooltips(Rectangle area) {
		final List<Tooltip> removed = tooltips.stream().filter(t -> area.contains(t.getPoint())).collect(Collectors.toList());
		return tooltips.removeAll(removed);
	}
	
	@Override @Nullable public Rectangle handleScissor(@Nullable Rectangle area) {
		return area;
	}
	
	protected void overlayBackground(
	  MatrixStack mStack, Rectangle rect, int red, int green, int blue
	) {
		overlayBackground(mStack.getLast().getMatrix(), rect, red, green, blue, 255, 255);
	}
	
	@SuppressWarnings("SameParameterValue") protected void overlayBackground(
	  Matrix4f matrix, Rectangle rect, int red, int green, int blue, int startAlpha, int endAlpha
	) {
		if (minecraft == null || isTransparentBackground()) return;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		minecraft.getTextureManager().bindTexture(getBackgroundLocation());
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) rect.getMinX(), (float) rect.getMaxY(), 0.0f)
		  .tex((float) rect.getMinX() / 32.0f, (float) rect.getMaxY() / 32.0f)
		  .color(red, green, blue, endAlpha).endVertex();
		buffer.pos(matrix, (float) rect.getMaxX(), (float) rect.getMaxY(), 0.0f)
		  .tex((float) rect.getMaxX() / 32.0f, (float) rect.getMaxY() / 32.0f)
		  .color(red, green, blue, endAlpha).endVertex();
		buffer.pos(matrix, (float) rect.getMaxX(), (float) rect.getMinY(), 0.0f)
		  .tex((float) rect.getMaxX() / 32.0f, (float) rect.getMinY() / 32.0f)
		  .color(red, green, blue, startAlpha).endVertex();
		buffer.pos(matrix, (float) rect.getMinX(), (float) rect.getMinY(), 0.0f)
		  .tex((float) rect.getMinX() / 32.0f, (float) rect.getMinY() / 32.0f)
		  .color(red, green, blue, startAlpha).endVertex();
		tessellator.draw();
	}
	
	public void renderComponentHoverEffect(
	  @NotNull MatrixStack matrices, Style style, int x, int y
	) {
		super.renderComponentHoverEffect(matrices, style, x, y);
	}
	
	public boolean handleComponentClicked(@Nullable Style style) {
		if (style == null) {
			return false;
		}
		ClickEvent clickEvent = style.getClickEvent();
		if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
			try {
				URI uri = new URI(clickEvent.getValue());
				String string = uri.getScheme();
				if (string == null) {
					throw new URISyntaxException(clickEvent.getValue(), "Missing protocol");
				}
				if (!string.equalsIgnoreCase("http") && !string.equalsIgnoreCase("https")) {
					throw new URISyntaxException(
					  clickEvent.getValue(), "Unsupported protocol: " + string.toLowerCase(Locale.ROOT));
				}
				addDialog(new ConfirmLinkDialog(clickEvent.getValue(), this, true));
			} catch (URISyntaxException e) {
				ClothConfigInitializer.LOGGER.error("Can't open url for {}", clickEvent, e);
			}
			return true;
		}
		return super.handleComponentClicked(style);
	}
	
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	@Override public void claimRectangle(
	  Rectangle area, IOverlayRenderer overlayRenderer, int priority
	) {
		sortedOverlays.add(area, overlayRenderer, priority);
	}
	
	@Override public SortedOverlayCollection getSortedOverlays() {
		return sortedOverlays;
	}
	
	public abstract SearchBarWidget getSearchBar();
	
	public EditHistory getHistory() {
		return history;
	}
	
	public void setHistory(EditHistory previous) {
		this.history = new EditHistory(previous);
	}
	
	public void undo() {
		history.apply(this, false);
	}
	
	public void redo() {
		history.apply(this, true);
	}
	
	@Override public List<AbstractDialog> getDialogs() {
		return dialogs;
	}
}
