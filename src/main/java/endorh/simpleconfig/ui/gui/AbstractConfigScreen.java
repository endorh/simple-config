package endorh.simpleconfig.ui.gui;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Matrix4f;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.ConfigScreen;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.confirm;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigScreenGUIState;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler;
import endorh.simpleconfig.ui.api.ConfigScreenBuilder.IConfigSnapshotHandler.IExternalChangeHandler;
import endorh.simpleconfig.ui.gui.ExternalChangesDialog.ExternalChangeResponse;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget;
import endorh.simpleconfig.ui.hotkey.ConfigHotKey;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyManager.ConfigHotKeyGroup;
import endorh.simpleconfig.ui.impl.EditHistory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.SimpleConfigMod.CLIENT_CONFIG;
import static endorh.simpleconfig.api.SimpleConfigTextUtil.splitTtc;

public abstract class AbstractConfigScreen extends Screen
  implements ConfigScreen, IExtendedDragAwareNestedGuiEventHandler,
             IExternalChangeHandler, IEntryHolder, IDialogCapableScreen {
	private static final Logger LOGGER = LogManager.getLogger();
	protected final ResourceLocation backgroundLocation;
	protected final Screen parent;
	protected final String modId;
	protected final List<Tooltip> tooltips = Lists.newArrayList();
	protected boolean confirmUnsaved = confirm.discard;
	protected boolean confirmSave = confirm.save;
	protected boolean alwaysShowTabs = false;
	protected boolean transparentBackground = false;
	protected boolean editable = true;
	protected @Nullable IModalInputProcessor modalInputProcessor = null;
	protected @Nullable Runnable savingRunnable = null;
	protected @Nullable Runnable closingRunnable;
	protected @Nullable Consumer<Screen> afterInitConsumer = null;
	protected Pair<Integer, GuiEventListener> dragged = null;
	protected Map<String, Map<String, ConfigCategory>> categoryMap;
	protected ExternalChangesDialog externalChangesDialog;
	
	protected ConfigCategory selectedCategory;
	protected List<ConfigCategory> sortedCategories;
	protected EnumMap<EditType, List<ConfigCategory>> sortedCategoriesMap;
	
	protected SortedOverlayCollection sortedOverlays = new SortedOverlayCollection();
	
	protected EditHistory history;
	protected @Nullable ConfigHotKey editedConfigHotKey;
	protected Consumer<Boolean> hotKeySaver = null;
	
	private final SortedDialogCollection dialogs = new SortedDialogCollection();
	protected @Nullable IConfigSnapshotHandler snapshotHandler;
	protected @Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider;
	
	protected Map<EditType, CommentedConfig> remoteConfigs = new EnumMap<>(EditType.class);
	protected Set<EditType> loadedRemoteConfigs = Collections.newSetFromMap(
	  new EnumMap<>(EditType.class));
	
	protected AbstractConfigScreen(
	  Screen parent, String modId, Component title, ResourceLocation backgroundLocation,
	  Collection<ConfigCategory> clientCategories,
	  Collection<ConfigCategory> commonCategories,
	  Collection<ConfigCategory> serverCommonCategories,
	  Collection<ConfigCategory> serverCategories
	) {
		super(title);
		this.parent = parent;
		this.modId = modId;
		this.backgroundLocation = backgroundLocation;
		Map<String, ConfigCategory> clientCategoryMap = clientCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		List<ConfigCategory> sortedClientCategories = clientCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		Map<String, ConfigCategory> commonCategoryMap = commonCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		List<ConfigCategory> sortedCommonCategories = commonCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		Map<String, ConfigCategory> serverCommonCategoryMap = serverCommonCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		List<ConfigCategory> sortedServerCommonCategories = serverCommonCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		Map<String, ConfigCategory> serverCategoryMap = serverCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		List<ConfigCategory> sortedServerCategories = serverCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		categoryMap = Util.make(new HashMap<>(4), m -> {
			m.put(SimpleConfig.EditType.CLIENT.getAlias(), clientCategoryMap);
			m.put(SimpleConfig.EditType.COMMON.getAlias(), commonCategoryMap);
			m.put(SimpleConfig.EditType.SERVER_COMMON.getAlias(), serverCommonCategoryMap);
			m.put(SimpleConfig.EditType.SERVER.getAlias(), serverCategoryMap);
		});
		sortedCategoriesMap = Util.make(new EnumMap<>(EditType.class), m -> {
			m.put(SimpleConfig.EditType.CLIENT, sortedClientCategories);
			m.put(SimpleConfig.EditType.COMMON, sortedCommonCategories);
			m.put(SimpleConfig.EditType.SERVER_COMMON, sortedServerCommonCategories);
			m.put(SimpleConfig.EditType.SERVER, sortedServerCategories);
		});
		sortedCategories = Stream.of(
		  sortedClientCategories, sortedCommonCategories,
		  sortedServerCommonCategories, sortedServerCategories
		).flatMap(Collection::stream).collect(Collectors.toList());
		history = new EditHistory();
		history.setOwner(this);
	}
	
	public String getModId() {
		return modId;
	}
	
	public static void fillGradient(
	  PoseStack mStack, double xStart, double yStart, double xEnd, double yEnd,
	  int blitOffset, int from, int to
	) {
		float fa = (float) (from >> 24 & 0xFF) / 255F;
		float fr = (float) (from >> 16 & 0xFF) / 255F;
		float fg = (float) (from >> 8 & 0xFF) / 255F;
		float fb = (float) (from & 0xFF) / 255F;
		float ta = (float) (to >> 24 & 0xFF) / 255F;
		float tr = (float) (to >> 16 & 0xFF) / 255F;
		float tg = (float) (to >> 8 & 0xFF) / 255F;
		float tb = (float) (to & 0xFF) / 255F;
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		// @formatter:off
		final Matrix4f m = mStack.last().pose();
		bb.vertex(m, (float) xEnd,   (float) yStart, (float) blitOffset).color(fr, fg, fb, fa).endVertex();
		bb.vertex(m, (float) xStart, (float) yStart, (float) blitOffset).color(fr, fg, fb, fa).endVertex();
		bb.vertex(m, (float) xStart, (float) yEnd,   (float) blitOffset).color(tr, tg, tb, ta).endVertex();
		bb.vertex(m, (float) xEnd,   (float) yEnd,   (float) blitOffset).color(tr, tg, tb, ta).endVertex();
		// @formatter:on
		tessellator.end();
	}
	
	public static void fillGradient(
	  @NotNull Matrix4f m, BufferBuilder bb, int minX, int minY,
	  int maxX, int maxY, int blitOffset, int from, int to
	) {
		float fA = (from >> 24 & 255) / 255F;
		float fR = (from >> 16 & 255) / 255F;
		float fG = (from >> 8 & 255) / 255F;
		float fB = (from & 255) / 255F;
		float tA = (to >> 24 & 255) / 255F;
		float tR = (to >> 16 & 255) / 255F;
		float tG = (to >> 8 & 255) / 255F;
		float tB = (to & 255) / 255F;
		bb.vertex(m, maxX, minY, blitOffset).color(fR, fG, fB, fA).endVertex();
		bb.vertex(m, minX, minY, blitOffset).color(fR, fG, fB, fA).endVertex();
		bb.vertex(m, minX, maxY, blitOffset).color(tR, tG, tB, tA).endVertex();
		bb.vertex(m, maxX, maxY, blitOffset).color(tR, tG, tB, tA).endVertex();
	}
	
	public static void drawBorderRect(
	  PoseStack mStack, Rectangle area, int w, int color, int innerColor
	) {
		drawBorderRect(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), w, color, innerColor);
	}
	
	public static void drawBorderRect(
	  PoseStack mStack, int l, int t, int r, int b, int w, int color, int innerColor
	) {
		fill(mStack, l, t, r, t + w, color);
		fill(mStack, l, b - w, r, b, color);
		fill(mStack, l, t + w, l + w, b - w, color);
		fill(mStack, r - w, t + w, r, b - w, color);
		if (innerColor != 0)
			fill(mStack, l + w, t + w, r - w, b - w, innerColor);
	}
	
	@Override public void setSavingRunnable(@Nullable Runnable savingRunnable) {
		this.savingRunnable = savingRunnable;
	}
	@Override public void setClosingRunnable(@Nullable Runnable closingRunnable) {
		this.closingRunnable = closingRunnable;
	}
	
	@Override public void setAfterInitConsumer(@Nullable Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
	}
	
	@Override public ResourceLocation getBackgroundLocation() {
		ResourceLocation background = selectedCategory.getBackground();
		return background != null? background : backgroundLocation;
	}
	
	@Override public boolean isRequiresRestart() {
		return getAllMainEntries().stream().anyMatch(
		  e -> e.isRequiresRestart() && e.isEdited());
	}
	
	public boolean isShowingHelp() {
		return false;
	}
	
	public void selectNextCategory(boolean forward) {
		int i = sortedCategories.indexOf(selectedCategory);
		if (i == -1) throw new IllegalStateException("Unknown selected category: " + selectedCategory);
		i = (i + (forward? 1 : -1) + sortedCategories.size()) % sortedCategories.size();
		setSelectedCategory(sortedCategories.get(i));
	}
	
	public ConfigCategory getSelectedCategory() {
		return selectedCategory;
	}
	
	public void setSelectedCategory(ConfigCategory category) {
		if (categoryMap.values().stream().noneMatch(m -> m.containsKey(category.getName())))
			throw new IllegalStateException("Unknown category: " + category.getName());
		selectedCategory = category;
	}
	
	@Override public boolean isEdited() {
		for (ConfigCategory cat : sortedCategories) {
			for (AbstractConfigField<?> entry : cat.getHeldEntries()) {
				if (!entry.isEdited()) continue;
				return true;
			}
		}
		return false;
	}
	
	@Override public @Nullable AbstractConfigField<?> getEntry(String path) {
		return null;
	}
	
	public EditType getEditedType() {
		return getSelectedCategory().getType();
	}
	
	public List<ConfigCategory> getSortedTypeCategories() {
		return sortedCategoriesMap.get(getEditedType());
	}
	
	public Map<String, ConfigCategory> getTypeCategories(EditType type) {
		return categoryMap.get(type.getAlias());
	}
	
	public Map<String, ConfigCategory> getTypeCategories() {
		return getTypeCategories(getEditedType());
	}
	
	public boolean isEditingServer() {
		return getEditedType() == SimpleConfig.EditType.SERVER;
	}
	
	public boolean isShowingTabs() {
		return isAlwaysShowTabs() || getTypeCategories().size() > 1;
	}
	
	/**
	 * Perform a batch of modifications that won't be recorded in the history.
	 * <b>Only useful for loading purposes.</b> Instead, consider using
	 * {@link #runAtomicTransparentAction(Runnable)}.
	 *
	 * @param action Action to run.
	 */
	public void runUnrecordedAction(Runnable action) {
		getHistory().runUnrecordedAction(action);
	}
	
	/**
	 * Run an atomic action, which is committed to the history as a single step.
	 * Atomic actions triggered within the action will also be joined in the same step.
	 *
	 * @param action Action to run
	 */
	public void runAtomicTransparentAction(Runnable action) {
		runAtomicTransparentAction(null, action);
	}
	
	/**
	 * Run an atomic action, which is committed to the history as a single step.
	 * Atomic actions triggered within the action will also be joined in the same step.
	 *
	 * @param focus Focus entry of the action, or {@code null}
	 * @param action Action to run
	 */
	public void runAtomicTransparentAction(@Nullable AbstractConfigField<?> focus, Runnable action) {
		getHistory().runAtomicTransparentAction(focus, action);
	}
	
	public Set<AbstractConfigField<?>> getSelectedEntries() {
		return Collections.emptySet();
	}
	
	public boolean canSelectEntries() {
		return false;
	}
	
	public boolean isAlwaysShowTabs() {
		return alwaysShowTabs;
	}
	
	@Internal public void setAlwaysShowTabs(boolean alwaysShowTabs) {
		this.alwaysShowTabs = alwaysShowTabs;
	}
	
	public boolean isTransparentBackground() {
		return transparentBackground && Minecraft.getInstance().level != null;
	}
	
	@Internal public void setTransparentBackground(boolean transparentBackground) {
		this.transparentBackground = transparentBackground;
	}
	
	public void loadConfigScreenGUIState(@Nullable IConfigScreenGUIState state) {}
	public IConfigScreenGUIState saveConfigScreenGUIState() {
		return null;
	}
	
	@Override public void saveAll(boolean openOtherScreens) {
		saveAll(openOtherScreens, false, false, false);
	}
	
	public void saveAll(
	  boolean openOtherScreens, boolean skipConfirm,
	  boolean forceOverwrite, boolean forceSaveWithErrors
	) {
		if (hasErrors() && !forceSaveWithErrors || !isEdited()) return;
		boolean external = !forceOverwrite && confirm.overwrite_external && hasConflictingExternalChanges();
		boolean remote = !forceOverwrite && confirm.overwrite_remote && hasConflictingRemoteChanges();
		if (external || remote) {
			addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.ui.confirm_overwrite"), d -> {
				  List<Component> body = splitTtc(
					 "simpleconfig.ui.confirm_overwrite.msg."
					 + (external ? remote ? "both" : "external" : "remote"));
				  body.addAll(splitTtc(
					 "simpleconfig.ui.confirm_overwrite.msg.overwrite"));
				  d.setBody(body);
				  CheckboxButton[] checkBoxes = Stream.concat(
					 Stream.of(
						CheckboxButton.of(!confirm.overwrite_external, Component.translatable("simpleconfig.ui.confirm_overwrite.do_not_ask_external"))).filter(p -> external),
					 Stream.of(CheckboxButton.of(!confirm.overwrite_remote, Component.translatable("simpleconfig.ui.confirm_overwrite.do_not_ask_remote"))).filter(p -> remote)).toArray(CheckboxButton[]::new);
				  d.withCheckBoxes((b, s) -> {
					  if (b) {
						  if (external) {
							  String CONFIRM_OVERWRITE_EXTERNAL = "confirm.overwrite_external";
							  if (CLIENT_CONFIG.hasGUI()) {
								  CLIENT_CONFIG.setGUI(CONFIRM_OVERWRITE_EXTERNAL, !s[0]);
								  confirm.overwrite_external = !s[0];
							  } else CLIENT_CONFIG.set(CONFIRM_OVERWRITE_EXTERNAL, !s[0]);
						  }
						  String CONFIRM_OVERWRITE_REMOTE = "confirm.overwrite_remote";
						  if (remote) {
							  boolean c = !s[external ? 1 : 0];
							  if (CLIENT_CONFIG.hasGUI()) {
								  CLIENT_CONFIG.setGUI(CONFIRM_OVERWRITE_REMOTE, c);
								  confirm.overwrite_external = c;
							  } else CLIENT_CONFIG.set(CONFIRM_OVERWRITE_REMOTE, c);
						  }
						  saveAll(openOtherScreens, true, true, forceSaveWithErrors);
					  }
				  }, checkBoxes);
				  d.setConfirmText(
				    Component.translatable("simpleconfig.ui.confirm_overwrite.overwrite"));
				  d.setConfirmButtonTint(0x80603070);
			  }
			));
		} else if (confirmSave && !skipConfirm) {
			addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.ui.confirm_save"), d -> {
				  d.withCheckBoxes((b, s) -> {
					  if (b) {
						  if (s[0]) {
							  final String CONFIRM_SAVE = "confirm.save";
							  if (CLIENT_CONFIG.hasGUI()) {
								  CLIENT_CONFIG.setGUI(CONFIRM_SAVE, false);
								  confirm.save = false;
							  } else CLIENT_CONFIG.set(CONFIRM_SAVE, false);
						  }
						  saveAll(openOtherScreens, true, true, forceSaveWithErrors);
					  }
				  }, CheckboxButton.of(
					 false, Component.translatable("simpleconfig.ui.do_not_ask_again")));
				  d.setBody(splitTtc("simpleconfig.ui.confirm_save.msg"));
				  d.setConfirmText(Component.translatable("simpleconfig.ui.save"));
				  d.setConfirmButtonTint(0x8042BD42);
			  }));
		} else doSaveAll(openOtherScreens, forceSaveWithErrors);
	}
	
	protected void doSaveAll(boolean openOtherScreens, boolean allowErrors) {
		if (hasErrors() && !allowErrors) return;
		for (ConfigCategory cat : sortedCategories)
			for (AbstractConfigField<?> entry : cat.getHeldEntries())
				entry.save();
		if (remoteConfigProvider != null) for (EditType type: loadedRemoteConfigs) {
			boolean requiresRestart = sortedCategoriesMap.get(type).stream()
			  .flatMap(c -> c.getAllMainEntries().stream())
			  .anyMatch(e -> e.isEdited() && e.isRequiresRestart());
			remoteConfigProvider.saveRemoteConfig(type, requiresRestart);
		}
		save();
		if (openOtherScreens && minecraft != null) {
			if (closingRunnable != null) closingRunnable.run();
			minecraft.setScreen(parent);
		}
	}
	
	protected void save() {
		Optional.ofNullable(savingRunnable).ifPresent(Runnable::run);
	}
	
	protected void saveHotkey() {
		if (isOnlyEditingConfigHotKey()) {
			if (hotKeySaver != null) hotKeySaver.accept(true);
		} else {
			ConfigHotKeyGroup hotkeys = ConfigHotKeyManager.INSTANCE.getHotKeys();
			hotkeys.addEntry(editedConfigHotKey);
			ConfigHotKeyManager.INSTANCE.updateHotKeys(hotkeys);
			setEditedConfigHotKey(null, null);
		}
	}
	
	protected void discardHotkey() {
		if (isOnlyEditingConfigHotKey()) {
			if (hotKeySaver != null) hotKeySaver.accept(false);
		} else setEditedConfigHotKey(null, null);
	}
	
	public boolean hasConflictingExternalChanges() {
		return Arrays.stream(SimpleConfig.EditType.values()).filter(t -> !t.isRemote())
		  .map(sortedCategoriesMap::get)
		  .flatMap(Collection::stream)
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .anyMatch(AbstractConfigField::hasConflictingExternalDiff);
	}
	
	public boolean hasConflictingRemoteChanges() {
		return Arrays.stream(SimpleConfig.EditType.values()).filter(SimpleConfig.EditType::isRemote)
		  .map(sortedCategoriesMap::get)
		  .flatMap(Collection::stream)
		  .flatMap(c -> c.getAllMainEntries().stream())
		  .anyMatch(AbstractConfigField::hasConflictingExternalDiff);
	}
	
	public boolean isEditable() {
		return editable && getSelectedCategory().isEditable();
	}
	
	@Internal public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public @Nullable ConfigHotKey getEditedConfigHotKey() {
		return editedConfigHotKey;
	}
	
	public void setEditedConfigHotKey(@Nullable ConfigHotKey hotkey, Consumer<Boolean> hotKeySaver) {
		if (editedConfigHotKey == null && hotkey == null
		    && this.hotKeySaver == null && hotKeySaver == null
		) return;
		if (hotkey == null)
			getAllMainEntries().forEach(e -> e.setHotKeyActionType(null, null));
		editedConfigHotKey = hotkey;
		this.hotKeySaver = hotKeySaver;
		// Refresh layout
		init(Minecraft.getInstance(), width, height);
	}
	
	public boolean isEditingConfigHotKey() {
		return editedConfigHotKey != null;
	}
	public boolean isOnlyEditingConfigHotKey() {
		return hotKeySaver != null;
	}
	
	@Override public @Nullable IModalInputProcessor getModalInputProcessor() {
		return modalInputProcessor;
	}
	@Override public void setModalInputProcessor(@Nullable IModalInputProcessor processor) {
		modalInputProcessor = processor;
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		handleEndDrag(mouseX, mouseY, button);
		if (handleModalMouseReleased(mouseX, mouseY, button)) return true;
		if (handleOverlaysMouseReleased(mouseX, mouseY, button)) return true;
		if (handleDialogsMouseReleased(mouseX, mouseY, button)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (handleDialogsMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleModalMouseScrolled(mouseX, mouseY, delta)) return true;
		if (handleDialogsMouseScrolled(mouseX, mouseY, delta)) return true;
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (handleModalKeyReleased(keyCode, scanCode, modifiers)) return true;
		if (handleDialogsKeyReleased(keyCode, scanCode, modifiers)) return true;
		if (getDragged() != null) return true;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleModalMouseClicked(mouseX, mouseY, button)) return true;
		SearchBarWidget searchBar = getSearchBar();
		if (searchBar.isMouseOver(mouseX, mouseY)) setFocused(searchBar);
		if (handleDialogsMouseClicked(mouseX, mouseY, button)
		    || handleOverlaysMouseClicked(mouseX, mouseY, button)
		    || getDragged() != null)
			return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseClicked(mouseX, mouseY, button);
	}
	
	protected void recomputeFocus() {}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (handleModalCharTyped(codePoint, modifiers)) return true;
		if (handleDialogsCharTyped(codePoint, modifiers)) return true;
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (handleModalKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (handleDialogsKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (getDragged() != null) return true; // Suppress
		if ( keyCode == GLFW.GLFW_KEY_ESCAPE) {
			if (handleOverlaysEscapeKey())
				return true;
			if (isEditingConfigHotKey()) {
				discardHotkey();
				playFeedbackTap(1F);
				return true;
			} else if (shouldCloseOnEsc()) {
				playFeedbackTap(1F);
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
	
	protected void playFeedbackTap(float volume) {
		Minecraft.getInstance().getSoundManager().play(
		  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, volume));
	}
	
	protected void playFeedbackClick(float volume) {
		Minecraft.getInstance().getSoundManager().play(
		  SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, volume));
	}
	
	protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		Key key = InputConstants.getKey(keyCode, scanCode);
		if (KeyBindings.NEXT_PAGE.isActiveAndMatches(key)) {
			EditType prevType = getEditedType();
			selectNextCategory(true);
			recomputeFocus();
			if (prevType == getEditedType()) {
				playFeedbackTap(1F);
			} else playFeedbackClick(1F);
			return true;
		} else if (KeyBindings.PREV_PAGE.isActiveAndMatches(key)) {
			EditType prevType = getEditedType();
			selectNextCategory(false);
			recomputeFocus();
			if (prevType == getEditedType()) {
				playFeedbackTap(1F);
			} else playFeedbackClick(1F);
			return true;
		} else if (KeyBindings.SAVE.isActiveAndMatches(key)) {
			if (isEditingConfigHotKey()) {
				saveHotkey();
			} else if (canSave()) saveAll(true);
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}

	protected final boolean quit() {
		return quit(false);
	}
	
	protected final boolean quit(boolean skipConfirm) {
		if (minecraft == null) return false;
		if (!skipConfirm && confirmUnsaved && isEdited()) {
			addDialog(ConfirmDialog.create(
			  Component.translatable("simpleconfig.ui.discard.dialog"), d -> {
				  d.withCheckBoxes((b, s) -> {
					  if (b) {
						  if (s[0]) {
							  final String CONFIRM_UNSAVED = "confirm.unsaved";
							  if (CLIENT_CONFIG.hasGUI()) {
								  CLIENT_CONFIG.setGUI(CONFIRM_UNSAVED, false);
								  confirm.discard = false;
							  } else CLIENT_CONFIG.set(CONFIRM_UNSAVED, false);
						  }
						  quit(true);
					  }
				  }, CheckboxButton.of(
					 false, Component.translatable("simpleconfig.ui.do_not_ask_again")));
				  d.setBody(splitTtc("simpleconfig.ui.discard.confirm"));
				  d.setConfirmText(Component.translatable("simpleconfig.ui.discard"));
				  d.setConfirmButtonTint(0x80BD2424);
			  }));
		} else {
			if (closingRunnable != null) closingRunnable.run();
			minecraft.setScreen(parent);
		}
		return true;
	}
	
	@Override public void tick() {
		super.tick();
		tickDialogs();
		boolean edited = isEdited();
		Optional.ofNullable(getQuitButton()).ifPresent(button -> button.setMessage(
		  edited ? Component.translatable("simpleconfig.ui.discard") : CommonComponents.GUI_CANCEL));
	}
	
	@Nullable protected AbstractWidget getQuitButton() {
		return null;
	}
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		final boolean hasDialog = hasDialogs();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		super.render(mStack, suppressHover ? -1 : mouseX, suppressHover ? -1 : mouseY, delta);
		renderOverlays(mStack, mouseX, mouseY, delta);
		if (hasDialog) tooltips.clear();
		renderDialogs(mStack, mouseX, mouseY, delta);
		renderTooltips(mStack, mouseX, mouseY, delta);
	}
	
	protected void renderTooltips(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		for (Tooltip tooltip : tooltips)
			if (!tooltip.isFromKeyboard() || tooltips.size() == 1)
				tooltip.render(this, mStack);
		tooltips.clear();
	}
	
	@Override public List<Tooltip> getTooltips() {
		return tooltips;
	}
	
	@Override public boolean removeTooltips(Rectangle area) {
		final List<Tooltip> removed =
		  tooltips.stream().filter(t -> area.contains(t.getPoint())).toList();
		return tooltips.removeAll(removed);
	}
	
	@Override public void renderComponentHoverEffect(
	  @NotNull PoseStack matrices, Style style, int x, int y
	) {
		super.renderComponentHoverEffect(matrices, style, x, y);
	}
	
	@Override public boolean handleComponentClicked(@Nullable Style style) {
		if (style == null) return false;
		ClickEvent clickEvent = style.getClickEvent();
		if (clickEvent != null && clickEvent.getAction() == Action.OPEN_URL) {
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
				addDialog(ConfirmLinkDialog.create(clickEvent.getValue(), true));
			} catch (URISyntaxException e) {
				LOGGER.error("Can't open url for {}", clickEvent, e);
			}
			return true;
		}
		return super.handleComponentClicked(style);
	}
	
	public boolean hasType(EditType type) {
		return !sortedCategoriesMap.get(type).isEmpty()
		       && (!type.isOnlyRemote()
		           || remoteConfigProvider != null && remoteConfigs.containsKey(type));
	}
	
	public boolean mayHaveType(EditType type) {
		return sortedCategoriesMap.containsKey(type)
		       && (!type.isOnlyRemote()
		           || remoteConfigProvider != null
		              && remoteConfigProvider.mayHaveRemoteConfig(type));
	}
	
	public boolean hasLoadedType(EditType type) {
		return !type.isOnlyRemote() || loadedRemoteConfigs.contains(type);
	}
	
	@Override public Pair<Integer, GuiEventListener> getDragged() {
		return dragged;
	}
	
	@Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	@Override public SortedOverlayCollection getSortedOverlays() {
		return sortedOverlays;
	}
	
	public abstract SearchBarWidget getSearchBar();
	
	public EditHistory getHistory() {
		return history;
	}
	
	public void setHistory(EditHistory previous) {
		history = new EditHistory(previous);
		history.setOwner(this);
	}
	
	public void commitHistory() {
		history.saveState();
		final AbstractConfigField<?> entry = getFocusedEntry();
		if (entry != null) entry.preserveState();
	}
	
	public void undo() {
		history.apply(false);
	}
	
	public void redo() {
		history.apply(true);
	}
	
	@Override public SortedDialogCollection getDialogs() {
		return dialogs;
	}
	
	public @Nullable IConfigSnapshotHandler getSnapshotHandler() {
		return snapshotHandler;
	}
	
	public void setSnapshotHandler(@Nullable IConfigSnapshotHandler snapshotHandler) {
		this.snapshotHandler = snapshotHandler;
		if (snapshotHandler != null) snapshotHandler.setExternalChangeHandler(this);
	}
	
	public @Nullable ConfigScreenBuilder.IRemoteConfigProvider getRemoteConfigProvider() {
		return remoteConfigProvider;
	}
	
	public void setRemoteCommonConfigProvider(@Nullable ConfigScreenBuilder.IRemoteConfigProvider remoteConfigProvider) {
		this.remoteConfigProvider = remoteConfigProvider;
	}
	
	public @Nullable AbstractConfigField<?> getFocusedEntry() {
		return null;
	}
	
	public boolean isSelecting() {
		return false;
	}
	
	public void updateSelection() {}
	
	@Override public void handleExternalChange(EditType type) {
		// Changes sometimes arrive in batches
		if (externalChangesDialog != null) externalChangesDialog.cancel(false);
		externalChangesDialog = ExternalChangesDialog.create(type, response -> {
			handleExternalChangeResponse(response);
			externalChangesDialog = null;
		});
		addDialog(externalChangesDialog);
	}
	
	@Override public void handleRemoteConfigExternalChange(
	  EditType type, CommentedConfig remoteConfig
	) {
		if (remoteConfigProvider == null) return;
		if (!loadedRemoteConfigs.contains(type)) {
			remoteConfigs.put(type, remoteConfig);
		} else remoteConfigProvider.loadRemoteConfig(type, remoteConfig, true);
	}
	
	public void handleExternalChangeResponse(ExternalChangeResponse response) {
		if (response == ExternalChangeResponse.ACCEPT_ALL) {
			runAtomicTransparentAction(() -> getAllMainEntries()
			  .forEach(AbstractConfigField::acceptExternalValue));
		} else if (response == ExternalChangeResponse.ACCEPT_NON_CONFLICTING) {
			runAtomicTransparentAction(() -> getAllMainEntries().stream()
			  .filter(e -> !e.isEdited())
			  .forEach(AbstractConfigField::acceptExternalValue));
		} // else do nothing
	}
	
	public static void fill(
	  PoseStack mStack, ResourceLocation texture, float tw, float th,
	  float x, float y, float w, float h, int tint
	) {
		float r = tint >> 16 & 0xFF, g = tint >> 8 & 0xFF, b = tint & 0xFF, a = tint >> 24;
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		Matrix4f m = mStack.last().pose();
		buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		// @formatter:off
		buffer.vertex(m,     x,     y, 0F).uv(     x  / tw,      y  / th).color(r, g, b, a).endVertex();
		buffer.vertex(m, x + w,     y, 0F).uv((x + w) / tw,      y  / th).color(r, g, b, a).endVertex();
		buffer.vertex(m, x + w, y + h, 0F).uv((x + w) / tw, (y + h) / th).color(r, g, b, a).endVertex();
		buffer.vertex(m,     x, y + h, 0F).uv(     x  / tw, (y + h) / th).color(r, g, b, a).endVertex();
		// @formatter:on
		tessellator.end();
	}
}
