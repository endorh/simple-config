package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ClientConfig.confirm;
import endorh.simpleconfig.SimpleConfigMod.KeyBindings;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.api.ConfigBuilder.IConfigSnapshotHandler.IExternalChangeHandler;
import endorh.simpleconfig.ui.gui.ExternalChangesDialog.ExternalChangeResponse;
import endorh.simpleconfig.ui.gui.entries.KeyCodeEntry;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.SearchBarWidget;
import endorh.simpleconfig.ui.impl.EditHistory;
import endorh.simpleconfig.ui.math.Rectangle;
import endorh.simpleconfig.core.SimpleConfigTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.util.InputMappings.Input;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.SimpleConfigMod.CLIENT_CONFIG;
import static endorh.simpleconfig.core.SimpleConfigTextUtil.splitTtc;

public abstract class AbstractConfigScreen extends Screen
  implements ConfigScreen, IExtendedDragAwareNestedGuiEventHandler, ScissorsScreen,
             IExternalChangeHandler, IOverlayCapableScreen, IEntryHolder, IDialogCapableScreen {
	private static final Logger LOGGER = LogManager.getLogger();
	protected final ResourceLocation backgroundLocation;
	protected final Screen parent;
	protected final List<Tooltip> tooltips = Lists.newArrayList();
	protected boolean confirmUnsaved = confirm.discard;
	protected boolean confirmSave = confirm.save;
	protected boolean alwaysShowTabs = false;
	protected boolean transparentBackground = false;
	@Nullable protected ConfigCategory defaultFallbackCategory = null;
	protected boolean editable = true;
	protected KeyCodeEntry focusedBinding;
	protected ModifierKeyCode startedKeyCode = null;
	@Nullable protected Runnable savingRunnable = null;
	@Nullable protected Consumer<Screen> afterInitConsumer = null;
	protected Pair<Integer, IGuiEventListener> dragged = null;
	protected Map<String, ConfigCategory> serverCategoryMap;
	protected Map<String, ConfigCategory> categoryMap;
	protected boolean insideTransparentAction = false;
	protected ExternalChangesDialog externalChangesDialog;
	
	protected ConfigCategory selectedCategory;
	protected List<ConfigCategory> sortedClientCategories;
	protected List<ConfigCategory> sortedServerCategories;
	protected List<ConfigCategory> sortedCategories;
	
	protected SortedOverlayCollection sortedOverlays = new SortedOverlayCollection();
	
	protected EditHistory history;
	
	private final List<AbstractDialog> dialogs = Lists.newArrayList();
	protected @Nullable ConfigBuilder.IConfigSnapshotHandler snapshotHandler;
	
	protected AbstractConfigScreen(
	  Screen parent, ITextComponent title, ResourceLocation backgroundLocation,
	  Collection<ConfigCategory> clientCategories, Collection<ConfigCategory> serverCategories
	) {
		super(title);
		this.parent = parent;
		this.backgroundLocation = backgroundLocation;
		this.categoryMap = clientCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		this.sortedClientCategories = clientCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		this.serverCategoryMap = serverCategories.stream()
		  .collect(Collectors.toMap(ConfigCategory::getName, c -> c, (a, b) -> a));
		this.sortedServerCategories = serverCategories.stream()
		  .sorted(Comparator.comparingInt(ConfigCategory::getSortingOrder)).collect(Collectors.toList());
		this.sortedCategories = new ArrayList<>(sortedClientCategories);
		sortedCategories.addAll(sortedServerCategories);
		history = new EditHistory();
		history.setOwner(this);
	}
	
	public static void fillGradient(
	  MatrixStack mStack, double xStart, double yStart, double xEnd, double yEnd,
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
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		bb.begin(7, DefaultVertexFormats.POSITION_COLOR);
		// @formatter:off
		final Matrix4f m = mStack.last().pose();
		bb.vertex(m, (float) xEnd,   (float) yStart, (float) blitOffset).color(fr, fg, fb, fa).endVertex();
		bb.vertex(m, (float) xStart, (float) yStart, (float) blitOffset).color(fr, fg, fb, fa).endVertex();
		bb.vertex(m, (float) xStart, (float) yEnd,   (float) blitOffset).color(tr, tg, tb, ta).endVertex();
		bb.vertex(m, (float) xEnd,   (float) yEnd,   (float) blitOffset).color(tr, tg, tb, ta).endVertex();
		// @formatter:on
		tessellator.end();
	}
	
	@Override public void setSavingRunnable(@Nullable Runnable savingRunnable) {
		this.savingRunnable = savingRunnable;
	}
	
	@Override public void setAfterInitConsumer(@Nullable Consumer<Screen> afterInitConsumer) {
		this.afterInitConsumer = afterInitConsumer;
	}
	
	@Override public ResourceLocation getBackgroundLocation() {
		ResourceLocation background = selectedCategory.getBackground();
		return background != null? background : backgroundLocation;
	}
	
	@Override public boolean isRequiresRestart() {
		for (ConfigCategory cat : categoryMap.values()) {
			for (AbstractConfigEntry<?> entry : cat.getHeldEntries()) {
				if (entry.hasError() || !entry.isEdited() ||
				    !entry.isRequiresRestart()) continue;
				return true;
			}
		}
		return false;
	}
	
	public void selectNextCategory(boolean forward) {
		int i = sortedCategories.indexOf(selectedCategory);
		if (i == -1) throw new IllegalStateException();
		i = (i + (forward? 1 : -1) + sortedCategories.size()) % sortedCategories.size();
		setSelectedCategory(sortedCategories.get(i));
	}
	
	public ConfigCategory getSelectedCategory() {
		return selectedCategory;
	}
	
	public void setSelectedCategory(String name) {
		if (categoryMap.containsKey(name))
			setSelectedCategory(categoryMap.get(name));
		else if (serverCategoryMap.containsKey(name))
			setSelectedCategory(serverCategoryMap.get(name));
		else throw new IllegalArgumentException("Unknown category name: " + name);
	}
	
	public void setSelectedCategory(ConfigCategory category) {
		if (!categoryMap.containsValue(category) && !serverCategoryMap.containsValue(category))
			throw new IllegalArgumentException("Unknown category");
		selectedCategory = category;
	}
	
	@Override public boolean isEdited() {
		for (ConfigCategory cat : sortedCategories) {
			for (AbstractConfigEntry<?> entry : cat.getHeldEntries()) {
				if (!entry.isEdited()) continue;
				return true;
			}
		}
		return false;
	}
	
	public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		return null;
	}
	
	public ModConfig.Type currentConfigType() {
		return isSelectedCategoryServer()? ModConfig.Type.SERVER : ModConfig.Type.CLIENT;
	}
	
	public boolean isSelectedCategoryServer() {
		return serverCategoryMap.containsValue(selectedCategory);
	}
	
	public boolean isShowingTabs() {
		return isAlwaysShowTabs() || (isSelectedCategoryServer()? serverCategoryMap : categoryMap).size() > 1;
	}
	
	/**
	 * Run an atomic action, which is committed to the history as a single step.
	 * Atomic actions triggered within the action will also be joined in the same step.
	 * @param action Action to run
	 */
	public void runAtomicTransparentAction(Runnable action) {
		runAtomicTransparentAction(null, action);
	}
	
	/**
	 * Run an atomic action, which is committed to the history as a single step.
	 * Atomic actions triggered within the action will also be joined in the same step.
	 * @param focus Focus entry of the action, or {@code null}
	 * @param action Action to run
	 */
	public void runAtomicTransparentAction(@Nullable AbstractConfigEntry<?> focus, Runnable action) {
		getHistory().runAtomicTransparentAction(focus, action);
	}
	
	public Set<AbstractConfigEntry<?>> getSelectedEntries() {
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
	
	public ConfigCategory getFallbackCategory() {
		return defaultFallbackCategory != null
		       ? defaultFallbackCategory
		       : (categoryMap.isEmpty()? serverCategoryMap : categoryMap).values().iterator().next();
	}
	
	@Internal public void setFallbackCategory(@Nullable ConfigCategory defaultFallbackCategory) {
		this.defaultFallbackCategory = defaultFallbackCategory;
		selectedCategory = defaultFallbackCategory;
	}
	
	@Override public void saveAll(boolean openOtherScreens) {
		saveAll(openOtherScreens, false, false);
	}
	
	public void saveAll(boolean openOtherScreens, boolean forceConfirm, boolean forceOverwrite) {
		if (hasErrors() || !isEdited()) return;
		boolean external = !forceOverwrite && confirm.overwrite_external && hasConflictingExternalChanges();
		boolean remote = !forceOverwrite && confirm.overwrite_remote && hasConflictingRemoteChanges();
		if (external || remote) {
			addDialog(ConfirmDialog.create(
			  this, new TranslationTextComponent("simpleconfig.ui.confirm_overwrite"), d -> {
				  List<ITextComponent> body = splitTtc(
					 "simpleconfig.ui.confirm_overwrite.msg."
					 + (external ? remote ? "both" : "external" : "remote"));
				  body.addAll(SimpleConfigTextUtil.splitTtc(
					 "simpleconfig.ui.confirm_overwrite.msg.overwrite"));
				  d.setBody(body);
				  CheckboxButton[] checkBoxes = Stream.concat(
					 Stream.of(
						CheckboxButton.of(!confirm.overwrite_external, new TranslationTextComponent(
						  "simpleconfig.ui.confirm_overwrite.do_not_ask_external"
						))).filter(p -> external),
					 Stream.of(CheckboxButton.of(!confirm.overwrite_remote, new TranslationTextComponent(
						"simpleconfig.ui.confirm_overwrite.do_not_ask_remote"
					 ))).filter(p -> remote)).toArray(CheckboxButton[]::new);
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
						  doSaveAll(openOtherScreens);
					  }
				  }, checkBoxes);
				  d.setConfirmText(new TranslationTextComponent(
					 "simpleconfig.ui.confirm_overwrite.overwrite"));
				  d.setConfirmButtonTint(0x80603070);
			  }
			));
		} else if (confirmSave || forceConfirm) {
			addDialog(ConfirmDialog.create(
			  this, new TranslationTextComponent("simpleconfig.ui.confirm_save"), d -> {
				  d.withCheckBoxes((b, s) -> {
					  if (b) {
						  if (s[0]) {
							  final String CONFIRM_SAVE = "confirm.save";
							  if (CLIENT_CONFIG.hasGUI()) {
								  CLIENT_CONFIG.setGUI(CONFIRM_SAVE, false);
								  confirm.save = false;
							  } else CLIENT_CONFIG.set(CONFIRM_SAVE, false);
						  }
						  doSaveAll(openOtherScreens);
					  }
				  }, CheckboxButton.of(
					 false, new TranslationTextComponent("simpleconfig.ui.do_not_ask_again")));
				  d.setBody(splitTtc("simpleconfig.ui.confirm_save.msg"));
				  d.setConfirmText(new TranslationTextComponent("text.cloth-config.save_and_done"));
				  d.setConfirmButtonTint(0x8042BD42);
			  }));
		} else doSaveAll(openOtherScreens);
	}
	
	protected void doSaveAll(boolean openOtherScreens) {
		if (hasErrors()) return;
		for (ConfigCategory cat : sortedCategories)
			for (AbstractConfigEntry<?> entry : cat.getHeldEntries())
				entry.save();
		save();
		if (openOtherScreens && minecraft != null) {
			if (isRequiresRestart())
				minecraft.setScreen(new ClothRequiresRestartScreen(parent));
			else minecraft.setScreen(parent);
		}
	}
	
	protected void save() {
		Optional.ofNullable(savingRunnable).ifPresent(Runnable::run);
	}
	
	public boolean hasConflictingExternalChanges() {
		return sortedClientCategories.stream().flatMap(c -> c.getAllMainEntries().stream())
		  .anyMatch(AbstractConfigEntry::hasConflictingExternalDiff);
	}
	
	public boolean hasConflictingRemoteChanges() {
		return sortedServerCategories.stream().flatMap(c -> c.getAllMainEntries().stream())
		  .anyMatch(AbstractConfigEntry::hasConflictingExternalDiff);
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
			startedKeyCode.setKeyCodeAndModifier(InputMappings.UNKNOWN, Modifier.none());
		} else {
			startedKeyCode = null;
		}
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (focusedBinding != null && startedKeyCode != null &&
		    !startedKeyCode.isUnknown() && focusedBinding.isAllowMouse()) {
			focusedBinding.setDisplayedValue(startedKeyCode);
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
		if (handleDialogsMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleDialogsMouseScrolled(mouseX, mouseY, delta)) return true;
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	public boolean keyReleased(int int_1, int int_2, int int_3) {
		if (getDragged() != null)
			return true;
		if (focusedBinding != null && startedKeyCode != null &&
		    focusedBinding.isAllowKey()) {
			focusedBinding.setDisplayedValue(startedKeyCode);
			setFocusedBinding(null);
			return true;
		}
		return super.keyReleased(int_1, int_2, int_3);
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		SearchBarWidget searchBar = getSearchBar();
		if (searchBar.isMouseOver(mouseX, mouseY)) setFocused(searchBar);
		if (getDragged() != null
		    || handleDialogsMouseClicked(mouseX, mouseY, button)
		    || handleOverlaysMouseClicked(mouseX, mouseY, button))
			return true;
		if (focusedBinding != null && startedKeyCode != null &&
		    focusedBinding.isAllowMouse()) {
			if (startedKeyCode.isUnknown()) {
				startedKeyCode.setKeyCode(Type.MOUSE.getOrCreate(button));
			} else if (focusedBinding.isAllowModifiers() &&
			           startedKeyCode.getType() == Type.KEYSYM) {
				int code = startedKeyCode.getKeyCode().getValue();
				if (Minecraft.ON_OSX ? code == 343 || code == 347
				                     : code == 341 || code == 345
				) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
					startedKeyCode.setKeyCode(Type.MOUSE.getOrCreate(button));
					return true;
				}
				if (code == 344 || code == 340) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
					startedKeyCode.setKeyCode(Type.MOUSE.getOrCreate(button));
					return true;
				}
				if (code == 342 || code == 346) {
					Modifier modifier = startedKeyCode.getModifier();
					startedKeyCode.setModifier(
					  Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
					startedKeyCode.setKeyCode(Type.MOUSE.getOrCreate(button));
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
					startedKeyCode.setKeyCode(InputMappings.getKey(keyCode, scanCode));
				} else if (focusedBinding.isAllowModifiers()) {
					if (startedKeyCode.getType() == Type.KEYSYM) {
						int code = startedKeyCode.getKeyCode().getValue();
						if (Minecraft.ON_OSX ? code == 343 || code == 347
						                                : code == 341 || code == 345) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(modifier.hasAlt(), true, modifier.hasShift()));
							startedKeyCode.setKeyCode(InputMappings.getKey(
							  keyCode, scanCode));
							return true;
						}
						if (code == 344 || code == 340) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(modifier.hasAlt(), modifier.hasControl(), true));
							startedKeyCode.setKeyCode(InputMappings.getKey(
							  keyCode, scanCode));
							return true;
						}
						if (code == 342 || code == 346) {
							Modifier modifier = startedKeyCode.getModifier();
							startedKeyCode.setModifier(
							  Modifier.of(true, modifier.hasControl(), modifier.hasShift()));
							startedKeyCode.setKeyCode(InputMappings.getKey(
							  keyCode, scanCode));
							return true;
						}
					}
					if (Minecraft.ON_OSX ? keyCode == 343 || keyCode == 347
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
				focusedBinding.setDisplayedValue(ModifierKeyCode.unknown());
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
		  SimpleSound.forUI(SimpleConfigMod.UI_TAP, volume));
	}
	
	protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		Input key = InputMappings.getKey(keyCode, scanCode);
		if (KeyBindings.NEXT_PAGE.isActiveAndMatches(key)) {
			selectNextCategory(true);
			recomputeFocus();
			playFeedbackTap(1F);
			return true;
		} else if (KeyBindings.PREV_PAGE.isActiveAndMatches(key)) {
			selectNextCategory(false);
			recomputeFocus();
			playFeedbackTap(1F);
			return true;
		} else if (KeyBindings.SAVE.isActiveAndMatches(key)) {
			if (canSave()) saveAll(true, false, false);
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
			  this, new TranslationTextComponent("text.cloth-config.quit_config"), d -> {
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
					 false, new TranslationTextComponent("simpleconfig.ui.do_not_ask_again")));
				  d.setBody(splitTtc("text.cloth-config.quit_config_sure"));
				  d.setConfirmText(new TranslationTextComponent("text.cloth-config.quit_discard"));
				  d.setConfirmButtonTint(0x80BD2424);
			  }));
		} else {
			minecraft.setScreen(parent);
		}
		return true;
	}
	
	public void tick() {
		super.tick();
		boolean edited = isEdited();
		Optional.ofNullable(getQuitButton()).ifPresent(button -> button.setMessage(
		  edited ? new TranslationTextComponent("text.cloth-config.cancel_discard")
		         : new TranslationTextComponent("gui.cancel")));
		for (IGuiEventListener child : children()) {
			if (!(child instanceof ITickableTileEntity)) continue;
			((ITickableTileEntity) child).tick();
		}
	}
	
	@Nullable protected Widget getQuitButton() {
		return null;
	}
	
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final boolean hasDialog = hasDialogs();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		super.render(mStack, suppressHover ? -1 : mouseX, suppressHover ? -1 : mouseY, delta);
		renderOverlays(mStack, hasDialog ? -1 : mouseX, hasDialog ? -1 : mouseY, delta);
		if (hasDialog) tooltips.clear();
		renderDialogs(mStack, mouseX, mouseY, delta);
		renderTooltips(mStack, mouseX, mouseY, delta);
	}
	
	protected void renderTooltips(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		for (Tooltip tooltip : tooltips) {
			int ty = tooltip.getY();
			if (ty <= 24) ty += 16;
			renderTooltip(mStack, tooltip.getText(), tooltip.getX(), ty);
		}
		tooltips.clear();
	}
	
	@Override public void addTooltip(Tooltip tooltip) {
		tooltips.add(tooltip);
	}
	
	@Override public List<Tooltip> getTooltips() {
		return tooltips;
	}
	
	@Override public boolean removeTooltips(Rectangle area) {
		final List<Tooltip> removed =
		  tooltips.stream().filter(t -> area.contains(t.getPoint())).collect(Collectors.toList());
		return tooltips.removeAll(removed);
	}
	
	@Override @Nullable public Rectangle handleScissor(@Nullable Rectangle area) {
		return area;
	}
	
	protected void overlayBackground(
	  MatrixStack mStack, Rectangle rect, int r, int g, int b
	) {
		overlayBackground(mStack, rect, r, g, b, 255, 255);
	}
	
	@SuppressWarnings("SameParameterValue") protected void overlayBackground(
	  MatrixStack mStack, Rectangle rect, int r, int g, int b, int startAlpha, int endAlpha
	) {
		if (minecraft == null || isTransparentBackground()) return;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		minecraft.getTextureManager().bind(getBackgroundLocation());
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		final Matrix4f m = mStack.last().pose();
		// @formatter:off
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.vertex(m, (float) rect.getMinX(), (float) rect.getMaxY(), 0.0f).uv((float) rect.getMinX() / 32.0f, (float) rect.getMaxY() / 32.0f).color(r, g, b, endAlpha).endVertex();
		buffer.vertex(m, (float) rect.getMaxX(), (float) rect.getMaxY(), 0.0f).uv((float) rect.getMaxX() / 32.0f, (float) rect.getMaxY() / 32.0f).color(r, g, b, endAlpha).endVertex();
		buffer.vertex(m, (float) rect.getMaxX(), (float) rect.getMinY(), 0.0f).uv((float) rect.getMaxX() / 32.0f, (float) rect.getMinY() / 32.0f).color(r, g, b, startAlpha).endVertex();
		buffer.vertex(m, (float) rect.getMinX(), (float) rect.getMinY(), 0.0f).uv((float) rect.getMinX() / 32.0f, (float) rect.getMinY() / 32.0f).color(r, g, b, startAlpha).endVertex();
		// @formatter:on
		tessellator.end();
	}
	
	public void renderComponentHoverEffect(
	  @NotNull MatrixStack matrices, Style style, int x, int y
	) {
		super.renderComponentHoverEffect(matrices, style, x, y);
	}
	
	public boolean handleComponentClicked(@Nullable Style style) {
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
				addDialog(ConfirmLinkDialog.create(clickEvent.getValue(), this, true));
			} catch (URISyntaxException e) {
				LOGGER.error("Can't open url for {}", clickEvent, e);
			}
			return true;
		}
		return super.handleComponentClicked(style);
	}
	
	public boolean hasClient() {
		return !sortedClientCategories.isEmpty();
	}
	
	public boolean hasServer() {
		return !sortedServerCategories.isEmpty();
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
		this.history.setOwner(this);
	}
	
	public void commitHistory() {
		history.saveState();
		final AbstractConfigEntry<?> entry = getFocusedEntry();
		if (entry != null) entry.preserveState();
	}
	
	public void undo() {
		history.apply(false);
	}
	
	public void redo() {
		history.apply(true);
	}
	
	@Override public List<AbstractDialog> getDialogs() {
		return dialogs;
	}
	
	public @Nullable ConfigBuilder.IConfigSnapshotHandler getSnapshotHandler() {
		return snapshotHandler;
	}
	
	public void setSnapshotHandler(@Nullable ConfigBuilder.IConfigSnapshotHandler snapshotHandler) {
		this.snapshotHandler = snapshotHandler;
		if (snapshotHandler != null) snapshotHandler.setExternalChangeHandler(this);
	}
	
	public @Nullable AbstractConfigEntry<?> getFocusedEntry() {
		return null;
	}
	
	public boolean isSelecting() {
		return false;
	}
	
	public void updateSelection() {}
	
	@Override public void handleExternalChange(ModConfig.Type type) {
		// Changes sometimes arrive in batches
		if (externalChangesDialog != null) externalChangesDialog.cancel(false);
		externalChangesDialog = ExternalChangesDialog.create(type, this, response -> {
			handleExternalChangeResponse(response);
			externalChangesDialog = null;
		});
		addDialog(externalChangesDialog);
	}
	
	public void handleExternalChangeResponse(ExternalChangeResponse response) {
		if (response == ExternalChangeResponse.ACCEPT_ALL) {
			runAtomicTransparentAction(() -> getAllMainEntries()
			  .forEach(AbstractConfigEntry::acceptExternalValue));
		} else if (response == ExternalChangeResponse.ACCEPT_NON_CONFLICTING) {
			runAtomicTransparentAction(() -> getAllMainEntries().stream()
			  .filter(e -> !e.isEdited())
			  .forEach(AbstractConfigEntry::acceptExternalValue));
		} // else do nothing
	}
	
	public static void fill(
	  MatrixStack mStack, ResourceLocation texture, float tw, float th,
	  float x, float y, float w, float h, int tint
	) {
		float r = tint >> 16 & 0xFF, g = tint >> 8 & 0xFF, b = tint & 0xFF, a = tint >> 24;
		Minecraft.getInstance().getTextureManager().bind(texture);
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		Matrix4f m = mStack.last().pose();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		// @formatter:off
		buffer.vertex(m,     x,     y, 0F).uv(     x  / tw,      y  / th).color(r, g, b, a).endVertex();
		buffer.vertex(m, x + w,     y, 0F).uv((x + w) / tw,      y  / th).color(r, g, b, a).endVertex();
		buffer.vertex(m, x + w, y + h, 0F).uv((x + w) / tw, (y + h) / th).color(r, g, b, a).endVertex();
		buffer.vertex(m,     x, y + h, 0F).uv(     x  / tw, (y + h) / th).color(r, g, b, a).endVertex();
		// @formatter:on
		tessellator.end();
	}
}
