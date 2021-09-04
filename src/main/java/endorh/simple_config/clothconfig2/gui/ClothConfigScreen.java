package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.widget.*;
import endorh.simple_config.clothconfig2.gui.widget.SearchBarWidget.ISearchHandler;
import endorh.simple_config.clothconfig2.impl.EasingMethod;
import endorh.simple_config.clothconfig2.math.Point;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigScreen
  extends AbstractTabbedConfigScreen implements ISearchHandler {
	private final ScrollingContainer tabsScroller = new ScrollingContainer() {
		@Override public Rectangle getBounds() {
			return new Rectangle(0, 0, 1, width - 40);
		}
		
		@Override public int getMaxScrollHeight() {
			return (int) getTabsMaximumScrolled();
		}
		
		@Override public void updatePosition(float delta) {
			super.updatePosition(delta);
			scrollAmount = clamp(scrollAmount, 0.0);
		}
	};
	public ListWidget<AbstractConfigEntry<?>> listWidget;
	public ListWidget<AbstractConfigEntry<?>> resizingListWidget = null;
	private final List<Pair<ConfigCategory, Integer>> tabs;
	protected TintedButton quitButton;
	protected TintedButton saveButton;
	protected Widget buttonLeftTab;
	protected Widget buttonRightTab;
	protected MultiFunctionImageButton undoButton;
	protected MultiFunctionImageButton redoButton;
	protected Rectangle tabsBounds;
	protected Rectangle tabsLeftBounds;
	protected Rectangle tabsRightBounds;
	protected double tabsMaximumScrolled = -1.0;
	protected final List<ClothConfigTabButton> tabButtons = Lists.newArrayList();
	protected TooltipSearchBarWidget searchBar;
	
	@Internal public ClothConfigScreen(
	  Screen parent, ITextComponent title, Map<String, ConfigCategory> categories,
	  ResourceLocation backgroundLocation
	) {
		super(parent, title, backgroundLocation, categories);
		categories.forEach((categoryName, category) -> {
			for (AbstractConfigEntry<?> entry : category.getEntries()) {
				entry.setCategory(category);
				entry.setScreen(this);
			}
			if (category.getBackground() != null)
				registerCategoryBackground(categoryName, category.getBackground());
		});
		tabs = sortedCategories.stream().map(c -> Pair.of(
		  c, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(c.getTitle()) + 8
		)).collect(Collectors.toList());
		this.categoryMap = categories;
		final WeakReference<ClothConfigScreen> weakSelf = new WeakReference<>(this);
		searchBar = new TooltipSearchBarWidget(this, 0, 0, 256, weakSelf::get);
	}

	protected static final Pattern COLON = Pattern.compile(":");
	protected static final Pattern DOT = Pattern.compile("\\.");
	
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
		return categoryMap.values().stream().flatMap(c -> c.getEntries().stream())
		  .collect(Collectors.toList());
	}
	
	@Override public String getSelectedCategory() {
		return sortedCategories.get(selectedCategoryIndex).getName();
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
		tabButtons.clear();
		searchBar.w = width;
		if (resizingListWidget == null) {
			this.listWidget = new ListWidget<>(
			  this, minecraft, width, height, isShowingTabs() ? 50 : 24,
			  height - 28, getBackgroundLocation());
			if (categoryMap.size() > selectedCategoryIndex)
				this.listWidget.getEntries().addAll(sortedCategories.get(selectedCategoryIndex).getEntries());
		} else {
			listWidget = resizingListWidget;
			listWidget.resize(width, height, isShowingTabs()? 50 : 24, height - 28);
			resizingListWidget = null;
		}
		children.add(listWidget);
		int buttonWidths = Math.min(200, (width - 50 - 12) / 3);
		quitButton =
		  new TintedButton(
			 width / 2 - buttonWidths - 3, height - 24, buttonWidths, 20,
			 isEdited() ? new TranslationTextComponent(
				"text.cloth-config.cancel_discard")
			            : new TranslationTextComponent("gui.cancel"),
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
						if (entry.getConfigError().isPresent()) {
							hasErrors = true;
							break;
						}
					}
					if (hasErrors) break;
				}
				active = isEdited() && !hasErrors;
				setMessage(
				  hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save")
				            : new TranslationTextComponent("text.cloth-config.save_and_done"));
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
			buttonLeftTab = new Button(4, 25, 12, 18, NarratorChatListener.EMPTY,
			                                button -> tabsScroller.scrollTo(0.0, true)) {
				
				public void renderButton(
				  @NotNull MatrixStack matrices, int mouseX, int mouseY, float delta
				) {
					minecraft.getTextureManager().bindTexture(AbstractConfigScreen.CONFIG_TEX);
					RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
					int int_3 = getYImage(isHovered());
					RenderSystem.enableBlend();
					RenderSystem.blendFuncSeparate(770, 771, 0, 1);
					RenderSystem.blendFunc(770, 771);
					blit(matrices, x, y, 12, 18 * int_3, width, height);
				}
			};
			children.add(buttonLeftTab);
			int j = 0;
			for (Pair<ConfigCategory, Integer> tab : tabs) {
				tabButtons.add(new ClothConfigTabButton(
				  this, j, -100, 26, tab.getValue(), 20, tab.getKey().getTitle(),
				  categoryMap.get(tab.getKey().getName()).getDescription()));
				++j;
			}
			children.addAll(tabButtons);
			buttonRightTab = new Button(width - 16, 25, 12, 18, NarratorChatListener.EMPTY,
			                                 button -> tabsScroller.scrollTo(
			                                   tabsScroller.getMaxScroll(), true)) {
				
				public void renderButton(
				  @NotNull MatrixStack matrices, int mouseX, int mouseY, float delta
				) {
					minecraft.getTextureManager()
					  .bindTexture(AbstractConfigScreen.CONFIG_TEX);
					RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
					int int_3 = getYImage(isHovered());
					RenderSystem.enableBlend();
					RenderSystem.blendFuncSeparate(770, 771, 0, 1);
					RenderSystem.blendFunc(770, 771);
					blit(matrices, x, y, 0, 18 * int_3, width, height);
				}
			};
			children.add(buttonRightTab);
			addListener(searchBar);
		} else {
			tabsLeftBounds = tabsRightBounds = new Rectangle();
			tabsBounds = tabsRightBounds;
		}
		undoButton = new MultiFunctionImageButton(
		  2, 2, 20, 20, 80, 128, CONFIG_TEX, (w, b) -> {
			  if (b == 0) {
				  undo();
				  setListener(listWidget);
			  }
			  return true;
		  });
		undoButton.setActivePredicate(w -> history.canUndo(this));
		addButton(undoButton);
		redoButton = new MultiFunctionImageButton(
		  22, 2, 20, 20, 100, 128, CONFIG_TEX, (w, b) -> {
			if (b == 0) {
				  redo();
				  setListener(listWidget);
			  }
			return true;
		});
		redoButton.setActivePredicate(w -> history.canRedo(this));
		addButton(redoButton);
		Optional.ofNullable(afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
	}
	
	@Override public void setSelectedCategory(int index) {
		if (selectedCategoryIndex != index) {
			getHistory().saveState(this);
			super.setSelectedCategory(index);
			int x = 0;
			for (int i = 0; i < index; i++)
				x += tabButtons.get(i).getWidth() + 2;
			x += tabButtons.get(index).getWidth() / 2;
			x -= tabsScroller.getBounds().width / 2;
			tabsScroller.scrollTo(x, true, 250);
			if (searchBar.isExpanded()) {
				searchBar.refresh();
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
		if (searchBar.isExpanded() && dialogs.isEmpty()) {
			setListener(searchBar);
		}
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == 264 || keyCode == 265) {
			setListener(listWidget);
			return true;
		}
		return false;
	}
	
	@Override protected boolean screenKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (super.screenKeyPressed(keyCode, scanCode, modifiers)) return true;
		if (Screen.hasControlDown()) {
			switch (keyCode) {
				case 89: // Ctrl + F
					if (!searchBar.isExpanded())
						searchBar.open();
					setListener(searchBar);
					return true;
				case 47: // Ctrl + Z
					undo();
					return true;
				case 84: // Ctrl + Y
					redo();
					return true;
			}
		}
		return false;
	}
	
	@Override public void resize(@NotNull Minecraft mc, int width, int height) {
		resizingListWidget = listWidget;
		super.resize(mc, width, height);
		resizingListWidget = null;
	}
	
	public double getTabsMaximumScrolled() {
		if (tabsMaximumScrolled == -1.0) {
			int[] i = new int[]{0};
			for (Pair<ConfigCategory, Integer> pair : tabs)
				i[0] = i[0] + (pair.getValue() + 2);
			tabsMaximumScrolled = i[0];
		}
		return tabsMaximumScrolled + 6.0;
	}
	
	public void resetTabsMaximumScrolled() {
		tabsMaximumScrolled = -1.0;
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
			buttonLeftTab.active = tabsScroller.scrollAmount > 0.0;
			buttonRightTab.active = tabsScroller.scrollAmount <
			                             getTabsMaximumScrolled() - (double) width + 40.0;
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
			Rectangle r = new Rectangle(tabsBounds.x + 20, tabsBounds.y, tabsBounds.width - 40, tabsBounds.height);
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
		} else {
			ClothConfigScreen.drawCenteredString(
			  mStack, minecraft.fontRenderer, title, width / 2, 8, 0xffffffff);
		}
		if (isEditable()) {
			List<ITextComponent> errors = Lists.newArrayList();
			for (ConfigCategory cat : sortedCategories) {
				for (AbstractConfigEntry<?> entry : cat.getEntries())
					entry.getConfigError().ifPresent(errors::add);
			}
			if (errors.size() > 0) {
				minecraft.getTextureManager().bindTexture(CONFIG_TEX);
				RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				String text =
				  "\u00a7c" + (errors.size() == 1 ? errors.get(0).copyRaw().getString() : I18n.format(
					 "text.cloth-config.multi_error"));
				if (isTransparentBackground()) {
					int stringWidth = minecraft.fontRenderer.getStringWidth(text);
					Objects.requireNonNull(minecraft.fontRenderer);
					fillGradient(mStack, 8 + 40, 6, 20 + 40 + stringWidth, 14 + 6, 0xA0000000, 0xA0000000);
				}
				blit(mStack, 10 + 40, 7, 24, 36, 3, 11);
				ClothConfigScreen.drawString(
				  mStack, minecraft.fontRenderer, text, 18 + 40, 9, 0xffffffff);
				if (errors.size() > 1) {
					int stringWidth = minecraft.fontRenderer.getStringWidth(text);
					if (smX >= 10 + 40 && smY >= 6 && smX <= 18 + 40 + stringWidth && smY <= 14 + 6) {
						Objects.requireNonNull(minecraft.fontRenderer);
						addTooltip(Tooltip.of(new Point(mouseX, mouseY), errors.toArray(new ITextComponent[0])));
					}
				}
			}
		} else if (!isEditable()) {
			minecraft.getTextureManager().bindTexture(CONFIG_TEX);
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
			String text = "\u00a7c" + I18n.format("text.cloth-config.not_editable");
			if (isTransparentBackground()) {
				int stringWidth = minecraft.fontRenderer.getStringWidth(text);
				Objects.requireNonNull(minecraft.fontRenderer);
				fillGradient(mStack, 8, 9, 20 + stringWidth, 14 + 9, 0x68000000, 0x68000000);
			}
			blit(mStack, 10, 10, 24, 36, 3, 11);
			ClothConfigScreen.drawString(mStack, minecraft.fontRenderer, text, 18, 12, -1);
		}
		searchBar.render(mStack, hasDialog? -1 : mouseX, hasDialog? -1 : mouseY, delta);
		if (listWidget.isScrollingNow())
			removeTooltips(new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.height));
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	@SuppressWarnings("SameParameterValue") private void drawTabsShades(
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
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(
		  matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMinY() + 4),
		  0.0f).tex(0.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
		  matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMinY() + 4),
		  0.0f).tex(1.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
			 matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMinY(), 0.0f)
		  .tex(1.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
			 matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMinY(), 0.0f)
		  .tex(0.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(
			 matrix, (float) (tabsBounds.getMinX() + 20), (float) tabsBounds.getMaxY(), 0.0f)
		  .tex(0.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
			 matrix, (float) (tabsBounds.getMaxX() - 20), (float) tabsBounds.getMaxY(), 0.0f)
		  .tex(1.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
		  matrix, (float) (tabsBounds.getMaxX() - 20), (float) (tabsBounds.getMaxY() - 4),
		  0.0f).tex(1.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
		  matrix, (float) (tabsBounds.getMinX() + 20), (float) (tabsBounds.getMaxY() - 4),
		  0.0f).tex(0.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		tessellator.draw();
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
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			if (item instanceof AbstractConfigEntry)
				((AbstractConfigEntry<?>) item).updateSelected(getFocused() == item);
			super.renderItem(matrices, item, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
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
				currentX = (int) ScrollingContainer.ease(
				  currentX, target.x, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentY = (int) ScrollingContainer.ease(
				  currentY, target.y, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentWidth = (int) ScrollingContainer.ease(
				  currentWidth, target.width, Math.min((double) timePast / duration * delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
				currentHeight = (int) ScrollingContainer.ease(
				  currentHeight, target.height, Math.min(
				    (double) timePast / (double) duration * (double) delta * 3.0, 1.0), EasingMethod.EasingMethodImpl.LINEAR);
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
			float f = (float) (j >> 24 & 0xFF) / 255.0f;
			float g = (float) (j >> 16 & 0xFF) / 255.0f;
			float h = (float) (j >> 8 & 0xFF) / 255.0f;
			float l = (float) (j & 0xFF) / 255.0f;
			float m = (float) (k >> 24 & 0xFF) / 255.0f;
			float n = (float) (k >> 16 & 0xFF) / 255.0f;
			float o = (float) (k >> 8 & 0xFF) / 255.0f;
			float p = (float) (k & 0xFF) / 255.0f;
			bufferBuilder.pos(matrix4f, (float) xEnd, (float) yStart, (float) i).color(g, h, l, f)
			  .endVertex();
			bufferBuilder.pos(matrix4f, (float) xStart, (float) yStart, (float) i).color(g, h, l, f)
			  .endVertex();
			bufferBuilder.pos(matrix4f, (float) xStart, (float) yEnd, (float) i).color(n, o, p, m)
			  .endVertex();
			bufferBuilder.pos(matrix4f, (float) xEnd, (float) yEnd, (float) i).color(n, o, p, m)
			  .endVertex();
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
		final ArrayList<String> names = new ArrayList<>(categoryMap.keySet());
		final String name = names.get(selectedCategoryIndex);
		final Map<String, Integer> search = categoryMap.entrySet().stream()
		  .filter(p -> !p.getKey().equals(name)).collect(Collectors.toMap(
		    Entry::getKey,
			 p -> (int) p.getValue().getEntries().stream().mapToLong(e -> e.search(query).size()).sum()));
		for (ClothConfigTabButton tabButton : tabButtons) {
			if (tabButton.index >= 0 && tabButton.index < names.size()) {
				int count = search.getOrDefault(names.get(tabButton.index), 0);
				tabButton.setTintColor(count > 0? 0x80BDBD42 : 0x00000000);
			}
		}
		return result;
	}
	
	@SuppressWarnings("RegExpUnexpectedAnchor") protected static final Pattern NO_MATCH = Pattern.compile("$^");
	@Override public void dismissQuery() {
		listWidget.search(NO_MATCH);
	}
	
	@Override public void selectMatch(int idx) {
		listWidget.changeFocusedMatch(idx);
	}
	
	public static class TooltipSearchBarWidget extends SearchBarWidget {
		protected static ITextComponent[] TOOLTIP_SEARCH_TOOLTIP = new ITextComponent[] {
		  new TranslationTextComponent("simple-config.ui.search.tooltip"),
		  new TranslationTextComponent("modifier.cloth-config.alt", "T").mergeStyle(TextFormatting.GRAY)};
		
		protected ToggleImageButton tooltipButton;
		protected boolean searchTooltips = true;
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, Supplier<IOverlayCapableScreen> screen
		) { this(handler, x, y, w, 24, screen); }
		
		public TooltipSearchBarWidget(
		  ISearchHandler handler, int x, int y, int w, int h, Supplier<IOverlayCapableScreen> screen
		) {
			super(handler, x, y, w, h, screen);
			tooltipButton = new ToggleImageButton(searchTooltips, 0, 0, 18, 18, 202, 188, CONFIG_TEX, b -> updateModifiers());
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
		
		@Override public boolean renderOverlay(
		  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
		) {
			x += 18;
			w -= 18;
			final boolean r = super.renderOverlay(mStack, area, mouseX, mouseY, delta);
			x -= 18;
			w += 18;
			if (!r) return false;
			tooltipButton.x = 4;
			tooltipButton.y = y + 3;
			tooltipButton.render(mStack, mouseX, mouseY, delta);
			return true;
		}
		
		@Override protected void drawBackground(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			x -= 18;
			w += 18;
			super.drawBackground(mStack, mouseX, mouseY, delta);
			x += 18;
			w -= 18;
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
}
