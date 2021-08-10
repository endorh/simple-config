package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.SimpleConfigMod.KeyBindings;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry.EntryError;
import endorh.simple_config.clothconfig2.gui.widget.*;
import endorh.simple_config.clothconfig2.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simple_config.clothconfig2.gui.widget.SearchBarWidget.ISearchHandler;
import endorh.simple_config.clothconfig2.impl.EasingMethod;
import endorh.simple_config.clothconfig2.math.Point;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simple_config.core.SimpleConfigTextUtil.splitTtc;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigScreen
  extends AbstractTabbedConfigScreen implements ISearchHandler {
	private final ScrollingHandler tabsScroller = new ScrollingHandler() {
		@Override public Rectangle getBounds() {
			return new Rectangle(0, 0, 1, width - 40);
		}
		
		@Override public int getMaxScrollHeight() {
			return (int) tabsMaximumScrolled;
		}
		
		@Override public void updatePosition(float delta) {
			super.updatePosition(delta);
			scrollAmount = clamp(scrollAmount, 0.0);
		}
	};
	public Map<ConfigCategory, ListWidget<AbstractConfigEntry<?>>> listWidgets = new HashMap<>();
	public ListWidget<AbstractConfigEntry<?>> listWidget;
	protected TintedButton quitButton;
	protected TintedButton saveButton;
	protected MultiFunctionImageButton serverButton;
	protected PresetPickerWidget presetPickerWidget;
	protected Widget buttonLeftTab;
	protected Widget buttonRightTab;
	protected MultiFunctionImageButton undoButton;
	protected MultiFunctionImageButton redoButton;
	protected MultiFunctionImageButton editFileButton;
	protected Rectangle tabsBounds;
	protected Rectangle tabsLeftBounds;
	protected Rectangle tabsRightBounds;
	protected double tabsMaximumScrolled = -1.0;
	protected final List<ClothConfigTabButton> tabButtons = Lists.newArrayList();
	protected TooltipSearchBarWidget searchBar;
	protected ErrorDisplayWidget errorDisplayWidget;
	
	protected ConfigCategory lastClientCategory = null;
	protected ConfigCategory lastServerCategory = null;
	
	@Internal public ClothConfigScreen(
	  Screen parent, ITextComponent title, Collection<ConfigCategory> categories,
	  Collection<ConfigCategory> serverCategories, ResourceLocation backgroundLocation
	) {
		super(parent, title, backgroundLocation, categories, serverCategories);
		for (ConfigCategory category : sortedCategories) {
			for (AbstractConfigEntry<?> entry : category.getEntries()) {
				entry.setCategory(category);
				entry.setScreen(this);
			}
		}
		errorDisplayWidget = new ErrorDisplayWidget(this, 94, 6);
		searchBar = new TooltipSearchBarWidget(this, 0, 0, 256, this);
		presetPickerWidget = new PresetPickerWidget(this, 0, 0, 70);
		if (isSelectedCategoryServer())
			lastServerCategory = selectedCategory;
		else lastClientCategory = selectedCategory;
	}
	
	protected static final Pattern COLON = Pattern.compile(":" );
	protected static final Pattern DOT = Pattern.compile("\\." );
	
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		final String[] sp = COLON.split(path, 2);
		final Optional<ConfigCategory> opt =
		  categoryMap.values().stream().filter(c -> c.getName().equals(sp[0])).findFirst();
		if (!opt.isPresent()) return null;
		final ConfigCategory cat = opt.get();
		final String[] split = DOT.split(sp[1], 2);
		final Optional<AbstractConfigEntry<?>> first =
		  cat.getEntries().stream().filter(e -> e.getName().equals(split[0])).findFirst();
		if (!first.isPresent()) return null;
		AbstractConfigEntry<?> entry = first.get();
		if (split.length < 2) return entry;
		if (!(entry instanceof IEntryHolder))
			return null;
		return ((IEntryHolder) entry).getEntry(split[1]);
	}
	
	@Override public List<AbstractConfigEntry<?>> getEntries() {
		return sortedCategories.stream().flatMap(c -> c.getEntries().stream())
		  .collect(Collectors.toList());
	}
	
	@Override
	public boolean isEdited() {
		return super.isEdited();
	}
	
	@Override
	public void saveAll(boolean openOtherScreens) {
		history.saveState(this);
		super.saveAll(openOtherScreens);
	}
	
	protected void init() {
		super.init();
		searchBar.w = width;
		listWidget = listWidgets.computeIfAbsent(
		  selectedCategory, c -> {
			  final ListWidget<AbstractConfigEntry<?>> w =
				 new ListWidget<>(
					this, minecraft, width, height, isShowingTabs() ? 50 : 24,
					height - 28, getBackgroundLocation());
			  w.getEntries().addAll(c.getEntries());
			  return w;
		  });
		listWidget.resize(width, height, isShowingTabs()? 50 : 24, height - 28);
		children.add(listWidget);
		int buttonWidths = Math.min(200, (width - 50 - 12) / 3);
		quitButton =
		  new TintedButton(
			 width / 2 - buttonWidths - 3, height - 24, buttonWidths, 20,
			 isEdited() ? new TranslationTextComponent(
				"text.cloth-config.cancel_discard" )
			            : DialogTexts.GUI_CANCEL,
			 widget -> quit());
		// quitButton.setTintColor(0x80BD4242);
		addButton(quitButton);
		saveButton = new TintedButton(
		  width / 2 + 3, height - 24, buttonWidths, 20,
		  NarratorChatListener.EMPTY, button -> saveAll(true)) {
			
			public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
				boolean hasErrors = false;
				for (ConfigCategory cat : sortedCategories) {
					for (AbstractConfigEntry<?> entry : cat.getEntries()) {
						if (entry.hasErrors()) {
							hasErrors = true;
							break;
						}
					}
					if (hasErrors) break;
				}
				active = isEdited() && !hasErrors;
				setMessage(
				  hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save" )
				            : new TranslationTextComponent("text.cloth-config.save_and_done" ));
				super.render(matrices, mouseX, mouseY, delta);
			}
		};
		saveButton.setTintColor(0x8042BD42);
		addButton(saveButton);
		saveButton.active = isEdited();
		if (isShowingTabs()) {
			tabsBounds = new Rectangle(0, 24, width, 24);
			tabsLeftBounds = new Rectangle(0, 24, 18, 24);
			tabsRightBounds = new Rectangle(width - 18, 24, 18, 24);
			buttonLeftTab = new MultiFunctionImageButton(
			  4, 27, 12, 18, SimpleConfigIcons.LEFT_TAB,
			  ButtonAction.of(() -> tabsScroller.offset(-48, true))
				 .active(() -> tabsScroller.scrollAmount > 0.0));
			children.add(buttonLeftTab);
			tabButtons.clear();
			int ww = 0;
			for (ConfigCategory cat : (isSelectedCategoryServer() ? sortedServerCategories
			                                                      : sortedClientCategories)) {
				final int w = font.getStringPropertyWidth(cat.getTitle());
				ww += w + 2;
				tabButtons.add(new ClothConfigTabButton(
				  this, cat, -100, 26, w + 8,
				  20, cat.getTitle(), cat.getDescription()));
			}
			tabsMaximumScrolled = ww;
			children.addAll(tabButtons);
			buttonRightTab = new MultiFunctionImageButton(
			  width - 16, 27, 12, 18, SimpleConfigIcons.RIGHT_TAB,
			  ButtonAction.of(() -> tabsScroller.offset(48, true))
				 .active(
					() -> tabsScroller.scrollAmount < tabsMaximumScrolled - (double) width + 40.0));
			children.add(buttonRightTab);
		} else tabsBounds = tabsLeftBounds = tabsRightBounds = new Rectangle();
		addListener(searchBar);
		addListener(errorDisplayWidget);
		undoButton = new MultiFunctionImageButton(
		  48, 2, 20, 20, SimpleConfigIcons.UNDO, ButtonAction.of(() -> {
			undo();
			setListener(listWidget);
		}).active(() -> history.canUndo(this)));
		addButton(undoButton);
		redoButton = new MultiFunctionImageButton(
		  68, 2, 20, 20, SimpleConfigIcons.REDO, ButtonAction.of(() -> {
			redo();
			setListener(listWidget);
		}).active(() -> history.canRedo(this)));
		addButton(redoButton);
		editFileButton = new MultiFunctionImageButton(
		  24, 2, 20, 20, SimpleConfigIcons.EDIT_FILE, ButtonAction.of(
			 () -> selectedCategory.getContainingFile().ifPresent(
			   f -> addDialog(new EditConfigFileDialog(this, f.toAbsolutePath())))
		).active(() -> selectedCategory.getContainingFile().isPresent())
		  .tooltip(() -> Lists.newArrayList(new TranslationTextComponent(
			 "simple-config.file.open"))));
		addButton(editFileButton);
		if (hasClient() && hasServer()) {
			serverButton = new MultiFunctionImageButton(
			  width - 24, 26, 20, 20,
			  isSelectedCategoryServer() ? SimpleConfigIcons.CLIENT : SimpleConfigIcons.SERVER,
			  ButtonAction.of(() -> {
				  if (isSelectedCategoryServer() && hasClient())
					  setSelectedCategory(
						 lastClientCategory != null ? lastClientCategory : sortedClientCategories.get(0));
				  else if (!isSelectedCategoryServer() && hasServer())
					  setSelectedCategory(
						 lastServerCategory != null ? lastServerCategory : sortedServerCategories.get(0));
			  }).active(() -> isSelectedCategoryServer() ? hasClient() : hasServer())
			    .tooltip(() -> Lists.newArrayList(
					new TranslationTextComponent(
					  "simple-config.ui.switch."
					  + (isSelectedCategoryServer()? "client" : "server")))));
			children.add(serverButton);
			tabsMaximumScrolled -= 24;
			buttonRightTab.x -= 24;
			tabsBounds = tabsBounds.grow(0, 0, -24, 0);
			tabsRightBounds = tabsRightBounds.grow(-24, 0, -24, 0);
		}
		presetPickerWidget.x = width * 2 / 3;
		presetPickerWidget.y = 2;
		presetPickerWidget.w = width - presetPickerWidget.x - 2;
		addListener(presetPickerWidget);
		Optional.ofNullable(afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
	}
	
	@Override protected boolean canSave() {
		return isEdited() && sortedCategories.stream().anyMatch(
		  c -> c.getEntries().stream().anyMatch(INavigableTarget::hasErrors));
	}
	
	@Override public void setSelectedCategory(ConfigCategory category) {
		if (selectedCategory != category) {
			getHistory().saveState(this);
			if (isSelectedCategoryServer())
				lastServerCategory = selectedCategory;
			else lastClientCategory = selectedCategory;
			super.setSelectedCategory(category);
			final int index = getTabbedCategories().indexOf(category);
			int x = 0;
			for (int i = 0; i < index; i++)
				x += tabButtons.get(i).getWidth() + 2;
			x += tabButtons.get(index).getWidth() / 2;
			x -= tabsScroller.getBounds().width / 2;
			tabsScroller.scrollTo(x, true, 250L);
			init(Minecraft.getInstance(), width, height);
			searchBar.refresh();
			if (searchBar.isExpanded()) {
				setListener(searchBar);
			}
		}
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (tabsBounds.contains(mouseX, mouseY) &&
		    !tabsLeftBounds.contains(mouseX, mouseY) &&
		    !tabsRightBounds.contains(mouseX, mouseY) && amount != 0.0) {
			tabsScroller.offset(-amount * 16.0, true);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	@Override protected void recomputeFocus() {
		if (searchBar.isExpanded() && dialogs.isEmpty())
			setListener(searchBar);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == 264 || keyCode == 265) { // Up | Down
			setListener(listWidget);
			return listWidget.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	@Override protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		InputMappings.Input key = InputMappings.getInputByCode(keyCode, scanCode);
		if (KeyBindings.NEXT_ERROR.isActiveAndMatches(key)) { // F1
			focusNextError(true);
			return true;
		} else if (KeyBindings.PREV_ERROR.isActiveAndMatches(key)) {
			focusNextError(false);
			return true;
		} else if (KeyBindings.SEARCH.isActiveAndMatches(key)) {
			if (!searchBar.isExpanded())
				searchBar.open();
			setListener(searchBar);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			return true;
		} else if (KeyBindings.UNDO.isActiveAndMatches(key)) {
			if (getHistory().canUndo(this))
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			undo();
			return true;
		} else if (KeyBindings.REDO.isActiveAndMatches(key)) {
			if (getHistory().canRedo(this))
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			redo();
			return true;
		}
		return false;
	}
	
	@Override
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (minecraft == null) return;
		final boolean hasDialog = !dialogs.isEmpty();
		boolean suppressHover = hasDialog || shouldOverlaysSuppressHover(mouseX, mouseY);
		final int smX = suppressHover ? -1 : mouseX;
		final int smY = suppressHover ? -1 : mouseY;
		if (getListener() == null || getListener() == searchBar && !searchBar.isExpanded())
			setListener(listWidget);
		if (isShowingTabs()) {
			tabsScroller.updatePosition(delta * 3.0f);
			int xx = 24 - (int) tabsScroller.scrollAmount;
			for (ClothConfigTabButton tabButton : tabButtons) {
				tabButton.x = xx;
				xx += tabButton.getWidth() + 2;
			}
		}
		if (isTransparentBackground()) {
			fillGradient(mStack, 0, 0, width, height, 0xa0101010, 0xb0101010);
		} else {
			renderDirtBackground(0);
		}
		listWidget.render(mStack, smX, smY, delta);
		ScissorsHandler.INSTANCE.scissor(
		  new Rectangle(listWidget.left, listWidget.top, listWidget.width,
		                listWidget.bottom - listWidget.top));
		ScissorsHandler.INSTANCE.removeLastScissor();
		if (isShowingTabs()) {
			ClothConfigScreen.drawCenteredString(
			  mStack, minecraft.fontRenderer, title, width / 2, 8, 0xffffffff);
			Rectangle r =
			  new Rectangle(tabsBounds.x + 20, tabsBounds.y, tabsBounds.width - 40, tabsBounds.height);
			ScissorsHandler.INSTANCE.scissor(r);
			if (isTransparentBackground()) {
				fillGradient(mStack, r.x, r.y, r.getMaxX(), r.getMaxY(), 0x68000000, 0x68000000);
			} else {
				overlayBackground(mStack, r, 32, 32, 32);
			}
			tabButtons.forEach(widget -> widget.render(mStack, smX, smY, delta));
			drawTabsShades(mStack, 0, isTransparentBackground() ? 120 : 255);
			ScissorsHandler.INSTANCE.removeLastScissor();
			buttonLeftTab.render(mStack, smX, smY, delta);
			buttonRightTab.render(mStack, smX, smY, delta);
			if (hasClient() && hasServer())
				serverButton.render(mStack, mouseX, mouseY, delta);
		} else {
			ClothConfigScreen.drawCenteredString(
			  mStack, minecraft.fontRenderer, title, width / 2, 8, 0xffffffff);
		}
		editFileButton.render(mStack, mouseX, mouseY, delta);
		errorDisplayWidget.render(mStack, smX, smY);
		searchBar.render(mStack, hasDialog ? -1 : mouseX, hasDialog ? -1 : mouseY, delta);
		if (listWidget.isScrollingNow())
			removeTooltips(
			  new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.height));
		presetPickerWidget.render(mStack, mouseX, mouseY, delta);
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	public void focusNextError(boolean forwards) {
		final List<AbstractConfigEntry<?>> entries = getSelectedCategory().getAllEntries();
		// Most of the time less entries will have errors, so this traversal is better
		final Map<Integer, INavigableTarget> errors =
		  getSelectedCategory().getErrors().stream().collect(Collectors.toMap(
			 e -> entries.indexOf(e.getEntry()), EntryError::getSource, (a, b) -> a));
		errors.remove(-1); // There shouldn't be any outside of allEntries
		AbstractConfigEntry<?> entry = listWidget.getSelectedEntry();
		int index = entry == null ? forwards ? -1 : entries.size() : entries.indexOf(entry);
		if (!errors.isEmpty()) {
			Comparator<Integer> order = Comparator.naturalOrder();
			if (!forwards) order = order.reversed();
			//noinspection OptionalGetWithoutIsPresent
			int next = errors.keySet().stream().filter(forwards ? i -> i > index : i -> i < index)
			  .min(order).orElse(errors.keySet().stream().min(order).get());
			errors.get(next).onNavigate();
		} else {
			int s = sortedCategories.size();
			final int selectedIndex = sortedCategories.indexOf(selectedCategory);
			Function<Integer, Integer> step = forwards ? j -> (j + 1) % s : j -> (j - 1 + s) % s;
			for (int i = step.apply(selectedIndex); i != selectedIndex; i = step.apply(i)) {
				ConfigCategory cat = sortedCategories.get(i);
				final List<EntryError> catErrors = cat.getErrors();
				if (!catErrors.isEmpty()) {
					setSelectedCategory(cat.getName());
					catErrors.get(0).getSource().onNavigate();
					break;
				}
			}
		}
	}
	
	@SuppressWarnings("SameParameterValue" ) private void drawTabsShades(
	  MatrixStack mStack, int lightColor, int darkColor
	) {
		drawTabsShades(mStack.getLast().getMatrix(), lightColor, darkColor);
	}
	
	private void drawTabsShades(Matrix4f matrix, int lightColor, int darkColor) {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(770, 771, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		// @formatter:off
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMinY() + 4), 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMinY() + 4), 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMinY(), 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMinY(), 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMaxY(), 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMaxY(), 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMaxY() - 4), 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		tessellator.draw();
		// @formatter:on
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	public static class ListWidget<R extends DynamicElementListWidget.ElementEntry>
	  extends DynamicElementListWidget<R> {
		private final AbstractConfigScreen screen;
		private boolean hasCurrent;
		private double currentX;
		private double currentY;
		private double currentWidth;
		private double currentHeight;
		public Rectangle target;
		public Rectangle thisTimeTarget;
		public long lastTouch;
		public long start;
		public long duration;
		
		public ListWidget(
		  AbstractConfigScreen screen, Minecraft client, int width, int height, int top, int bottom,
		  ResourceLocation backgroundLocation
		) {
			super(client, width, height, top, bottom, backgroundLocation);
			setRenderSelection(false);
			this.screen = screen;
		}
		
		@Override public int getItemWidth() {
			return width - 80;
		}
		
		@Override protected int getScrollBarPosition() {
			return left + width - 36;
		}
		
		@Override protected void renderItem(
		  MatrixStack matrices, R item, int index, int y, int x, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isHovered, float delta
		) {
			if (item instanceof AbstractConfigEntry)
				((AbstractConfigEntry<?>) item).updateSelected(getFocused() == item);
			super.renderItem(matrices, item, index, y, x, entryWidth, entryHeight, mouseX, mouseY,
			                 isHovered, delta);
		}
		
		@Override protected void renderList(
		  MatrixStack mStack, int startX, int startY, int mouseX, int mouseY, float delta
		) {
			long timePast;
			thisTimeTarget = null;
			if (hasCurrent) {
				timePast = System.currentTimeMillis() - lastTouch;
				int alpha = timePast <= 200L ? 255 : MathHelper.ceil(
				  255.0 - (double) (Math.min((float) (timePast - 200L), 500.0f) / 500.0f) * 255.0);
				alpha = alpha * 36 / 255 << 24;
				fillGradient(
				  mStack, currentX, currentY, currentX + currentWidth,
				  currentY + currentHeight, 0xFFFFFF | alpha, 0xFFFFFF | alpha);
			}
			super.renderList(mStack, startX, startY, mouseX, mouseY, delta);
			if (thisTimeTarget != null && isMouseOver(mouseX, mouseY)) {
				lastTouch = System.currentTimeMillis();
			}
			if (thisTimeTarget != null && !thisTimeTarget.equals(target)) {
				if (!hasCurrent) {
					currentX = thisTimeTarget.x;
					currentY = thisTimeTarget.y;
					currentWidth = thisTimeTarget.width;
					currentHeight = thisTimeTarget.height;
					hasCurrent = true;
				}
				target = thisTimeTarget.clone();
				start = lastTouch;
				duration = 40L;
			} else if (hasCurrent && target != null) {
				timePast = System.currentTimeMillis() - start;
				// @formatter:off
				currentX = (int) ScrollingHandler.ease(currentX, target.x, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentY = (int) ScrollingHandler.ease(currentY, target.y, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentWidth = (int) ScrollingHandler.ease(currentWidth, target.width, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentHeight = (int) ScrollingHandler.ease(currentHeight, target.height, Math.min((double) timePast / (double) duration * (double) delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				// @formatter:on
			}
		}
		
		protected void fillGradient(
		  MatrixStack matrices, double xStart, double yStart, double xEnd, double yEnd,
		  int colorStart, int colorEnd
		) {
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.disableAlphaTest();
			RenderSystem.defaultBlendFunc();
			RenderSystem.shadeModel(7425);
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
			ListWidget.fillGradient(
			  matrices.getLast().getMatrix(), bufferBuilder, xStart, yStart, xEnd, yEnd,
			  getBlitOffset(), colorStart, colorEnd);
			tessellator.draw();
			RenderSystem.shadeModel(7424);
			RenderSystem.disableBlend();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableTexture();
		}
		
		protected static void fillGradient(
		  Matrix4f matrix4f, BufferBuilder bufferBuilder, double xStart, double yStart, double xEnd,
		  double yEnd, int i, int j, int k
		) {
			float f = (float) (j >> 24 & 0xFF) / 255F;
			float g = (float) (j >> 16 & 0xFF) / 255F;
			float h = (float) (j >> 8 & 0xFF) / 255F;
			float l = (float) (j & 0xFF) / 255F;
			float m = (float) (k >> 24 & 0xFF) / 255F;
			float n = (float) (k >> 16 & 0xFF) / 255F;
			float o = (float) (k >> 8 & 0xFF) / 255F;
			float p = (float) (k & 0xFF) / 255F;
			// @formatter:off
			bufferBuilder.pos(matrix4f, (float) xEnd, (float) yStart, (float) i).color(g, h, l, f).endVertex();
			bufferBuilder.pos(matrix4f, (float) xStart, (float) yStart, (float) i).color(g, h, l, f).endVertex();
			bufferBuilder.pos(matrix4f, (float) xStart, (float) yEnd, (float) i).color(n, o, p, m).endVertex();
			bufferBuilder.pos(matrix4f, (float) xEnd, (float) yEnd, (float) i).color(n, o, p, m).endVertex();
			// @formatter:on
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			updateScrollingState(mouseX, mouseY, button);
			if (!isMouseOver(mouseX, mouseY))
				return false;
			for (R entry : getEventListeners()) {
				if (!entry.mouseClicked(mouseX, mouseY, button)) continue;
				setListener(entry);
				if (!isDragging())
					setDragged(Pair.of(button, entry));
				setDragging(true);
				updateSelectedTarget();
				return true;
			}
			if (button == 0) {
				clickedHeader(
				  (int) (mouseX - (double) (left + width / 2 - getItemWidth() / 2)),
				  (int) (mouseY - (double) top) + (int) getScroll() - 4);
				return true;
			}
			return scrolling;
		}
		
		@Override
		protected void renderBackBackground(
		  MatrixStack mStack, BufferBuilder buffer, Tessellator tessellator
		) {
			if (!screen.isTransparentBackground()) {
				super.renderBackBackground(mStack, buffer, tessellator);
			} else {
				fillGradient(mStack, left, top, right, bottom, 0x68000000, 0x68000000);
			}
		}
		
		@Override
		protected void renderHoleBackground(
		  MatrixStack matrices, int y1, int y2, int alpha1, int alpha2
		) {
			if (!screen.isTransparentBackground()) {
				super.renderHoleBackground(matrices, y1, y2, alpha1, alpha2);
			}
		}
	}
	
	@Override public Pair<Integer, Integer> query(Pattern query) {
		final Pair<Integer, Integer> result = listWidget.search(query);
		final Map<ConfigCategory, Long> searches =
		  sortedCategories.stream().filter(p -> p != selectedCategory)
			 .collect(Collectors.toMap(c -> c, c -> c.getEntries().stream()
				.mapToLong(e -> e.search(query).size()).sum()));
		for (ClothConfigTabButton button : tabButtons)
			button.setTintColor(searches.getOrDefault(button.category, 0L) > 0 ? 0x80BDBD42 : 0);
		return result;
	}
	
	@SuppressWarnings("RegExpUnexpectedAnchor" ) protected static final Pattern NO_MATCH =
	  Pattern.compile("$^" );
	
	@Override public void dismissQuery() {
		listWidget.search(NO_MATCH);
		tabButtons.forEach(b -> b.setTintColor(0));
	}
	
	@Override public void selectMatch(int idx) {
		listWidget.changeFocusedMatch(idx);
	}
	
	public static class TooltipSearchBarWidget extends SearchBarWidget {
		protected static ITextComponent[] TOOLTIP_SEARCH_TOOLTIP = new ITextComponent[]{
		  new TranslationTextComponent("simple-config.ui.search.tooltip" ),
		  new TranslationTextComponent("modifier.cloth-config.alt", "T" ).mergeStyle(
			 TextFormatting.GRAY)};
		
		protected ToggleImageButton tooltipButton;
		protected boolean searchTooltips = true;
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, IOverlayCapableScreen screen
		) {this(handler, x, y, w, 24, screen);}
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, int h, IOverlayCapableScreen screen
		) {
			super(handler, x, y, w, h, screen);
			tooltipButton =
			  new ToggleImageButton(searchTooltips, 0, 0, 18, 18, SimpleConfigIcons.SEARCH_TOOLTIPS,
			                        b -> updateModifiers());
			expandedListeners.add(tooltipButton);
			regexListeners.add(tooltipButton);
		}
		
		@Override protected void updateModifiers() {
			searchTooltips = tooltipButton.getValue();
			super.updateModifiers();
		}
		
		@Override public Optional<ITextComponent[]> getTooltip(double mouseX, double mouseY) {
			if (isExpanded() && tooltipButton.isMouseOver(mouseX, mouseY))
				return Optional.of(TOOLTIP_SEARCH_TOOLTIP);
			return super.getTooltip(mouseX, mouseY);
		}
		
		@Override
		protected void positionExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			x += 18;
			w -= 18;
			super.positionExpanded(mStack, mouseX, mouseY, delta);
			x -= 18;
			w += 18;
			close.x = x + 2;
			tooltipButton.x = 24;
			tooltipButton.y = y + 3;
		}
		
		@Override
		protected void renderExpanded(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			super.renderExpanded(mStack, mouseX, mouseY, delta);
			tooltipButton.render(mStack, mouseX, mouseY, delta);
		}
		
		@Override public boolean charTyped(char codePoint, int modifiers) {
			if (Screen.hasAltDown()) {
				if (Character.toLowerCase(codePoint) == 't') {
					tooltipButton.setValue(!tooltipButton.getValue());
					return true;
				}
			}
			return super.charTyped(codePoint, modifiers);
		}
		
		public boolean isSearchTooltips() {
			return searchTooltips;
		}
	}
	
	@Override public TooltipSearchBarWidget getSearchBar() {
		return searchBar;
	}
	
	@Override public void undo() {
		super.undo();
	}
	
	@Override public void redo() {
		super.redo();
		if (listWidget.getSelectedTarget() instanceof AbstractConfigEntry)
			history.preserveState(((AbstractConfigEntry<?>) listWidget.getSelectedTarget()));
	}
	
	public static class ErrorDisplayWidget extends AbstractGui implements IGuiEventListener {
		protected ClothConfigScreen screen;
		protected int x, y, w, h;
		
		public ErrorDisplayWidget(ClothConfigScreen screen, int x, int y) {
			this.screen = screen;
			this.x = x;
			this.y = y;
			w = 20;
			h = 14;
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (!isMouseInside(mouseX, mouseY)) return false;
			screen.focusNextError(!Screen.hasShiftDown());
			return true;
		}
		
		public void render(MatrixStack mStack, int mouseX, int mouseY) {
			Minecraft mc = Minecraft.getInstance();
			FontRenderer font = mc.fontRenderer;
			if (screen.isEditable()) {
				List<ITextComponent> errors = Lists.newArrayList();
				for (ConfigCategory cat : screen.sortedClientCategories)
					cat.getErrors().forEach(e -> errors.add(e.getError()));
				if (errors.size() > 0) {
					mc.getTextureManager().bindTexture(CONFIG_TEX);
					RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
					String text =
					  "\u00a7c" +
					  (errors.size() == 1 ? errors.get(0).copyRaw().getString() : I18n.format(
						 "text.cloth-config.multi_error" ));
					w = 12 + font.getStringWidth(text);
					int bgColor = 0xBD000000;
					Objects.requireNonNull(font);
					fillGradient(mStack, x, y, x + w, y + h, 0xA0000000, 0xA0000000);
					blit(mStack, x + 2, y + 1, 24, 36, 3, 11);
					ClothConfigScreen.drawString(
					  mStack, font, text, x + 10, y + 3, 0xffffffff);
					if (errors.size() > 1) {
						if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) {
							Objects.requireNonNull(font);
							screen.addTooltip(Tooltip.of(
							  new Point(mouseX, mouseY), errors.toArray(new ITextComponent[0])));
						}
					}
				} else {
					w = 0;
				}
			} else {
				mc.getTextureManager().bindTexture(CONFIG_TEX);
				RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				String text = "\u00a7c" + I18n.format("text.cloth-config.not_editable" );
				w = 12 + mc.fontRenderer.getStringWidth(text);
				if (screen.isTransparentBackground()) {
					Objects.requireNonNull(mc.fontRenderer);
					fillGradient(mStack, x, y, x + w, y + h, 0x68000000, 0x68000000);
				}
				blit(mStack, x + 2, y + 3, 24, 36, 3, 11);
				ClothConfigScreen.drawString(mStack, mc.fontRenderer, text, x + 10, y + 3, -1);
			}
		}
		
		public boolean isMouseInside(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		}
	}
	
	public static class EditConfigFileDialog extends ConfirmDialog {
		protected TintedButton openAndExit;
		protected final Path file;
		
		public EditConfigFileDialog(
		  ClothConfigScreen screen, Path file
		) {
			super(screen, new TranslationTextComponent(
			  "simple-config.file.dialog.title"
			), b -> {
				if (b) open(file);
			}, splitTtc(
			  "simple-config.file.dialog.body",
			  new StringTextComponent(file.toString()).modifyStyle(s -> s
			    .setFormatting(TextFormatting.DARK_AQUA).setUnderlined(true)
			    .setHoverEvent(new HoverEvent(
					HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.copy.click")))
			    .setClickEvent(new ClickEvent(
					ClickEvent.Action.COPY_TO_CLIPBOARD, file.toString())))));
			setConfirmText(new TranslationTextComponent(
			  "simple-config.file.dialog.option.open_n_continue"));
			setConfirmButtonTint(0xAA8000AA);
			this.file = file;
			openAndExit = new TintedButton(0, 0, 0, 20, new TranslationTextComponent(
			  "simple-config.file.dialog.option.open_n_discard"), p -> {
				open(file);
				cancel(true);
				screen.quit(true);
			});
			openAndExit.setTintColor(0xAA904210);
			addButton(1, openAndExit);
		}
		
		protected static void open(Path path) {
			Util.getOSType().openFile(path.toFile());
		}
	}
}
