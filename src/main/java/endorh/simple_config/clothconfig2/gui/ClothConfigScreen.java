package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget;
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
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigScreen
  extends AbstractTabbedConfigScreen {
	private final ScrollingContainer tabsScroller = new ScrollingContainer() {
		
		@Override
		public Rectangle getBounds() {
			return new Rectangle(0, 0, 1, ClothConfigScreen.this.width - 40);
		}
		
		@Override
		public int getMaxScrollHeight() {
			return (int) ClothConfigScreen.this.getTabsMaximumScrolled();
		}
		
		@Override
		public void updatePosition(float delta) {
			super.updatePosition(delta);
			this.scrollAmount = this.clamp(this.scrollAmount, 0.0);
		}
	};
	public ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> listWidget;
	private final LinkedHashMap<ITextComponent, List<AbstractConfigEntry<?>>> categorizedEntries =
	  Maps.newLinkedHashMap();
	private final List<Tuple<ITextComponent, Integer>> tabs;
	private Widget quitButton;
	private Widget saveButton;
	private Widget buttonLeftTab;
	private Widget buttonRightTab;
	private Rectangle tabsBounds;
	private Rectangle tabsLeftBounds;
	private Rectangle tabsRightBounds;
	private double tabsMaximumScrolled = -1.0;
	private final List<ClothConfigTabButton> tabButtons = Lists.newArrayList();
	private final Map<ITextComponent, ConfigCategory> categoryMap;
	
	@ApiStatus.Internal
	public ClothConfigScreen(
	  Screen parent, ITextComponent title, Map<ITextComponent, ConfigCategory> categoryMap,
	  ResourceLocation backgroundLocation
	) {
		super(parent, title, backgroundLocation);
		categoryMap.forEach((categoryName, category) -> {
			List<AbstractConfigListEntry<?>> entries = Lists.newArrayList();
			for (Object object : category.getEntries()) {
				//noinspection unchecked
				AbstractConfigListEntry<?> entry =
				  object instanceof Tuple ? ((Tuple<?, AbstractConfigListEntry<?>>) object).getB()
				                          : (AbstractConfigListEntry<?>) object;
				entry.setScreen(this);
				entries.add(entry);
			}
			//noinspection unchecked
			this.categorizedEntries.put(
			  categoryName, (List<AbstractConfigEntry<?>>) (List<?>) entries);
			if (category.getBackground() != null) {
				this.registerCategoryBackground(categoryName, category.getBackground());
			}
		});
		this.tabs = this.categorizedEntries.keySet().stream().map(s -> new Tuple<>(
		  s, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(s) + 8)
		).collect(Collectors.toList());
		this.categoryMap = categoryMap;
	}
	
	@Override
	public ITextComponent getSelectedCategory() {
		return this.tabs.get(this.selectedCategoryIndex).getA();
	}
	
	@Override
	public Map<ITextComponent, List<AbstractConfigEntry<?>>> getCategorizedEntries() {
		return this.categorizedEntries;
	}
	
	@Override
	public boolean isEdited() {
		return super.isEdited();
	}
	
	@Override
	@Deprecated
	public void setEdited(boolean edited) {
		super.setEdited(edited);
	}
	
	@Override
	@Deprecated
	public void setEdited(boolean edited, boolean requiresRestart) {
		super.setEdited(edited, requiresRestart);
	}
	
	@Override
	public void saveAll(boolean openOtherScreens) {
		super.saveAll(openOtherScreens);
	}
	
	protected void init() {
		super.init();
		this.tabButtons.clear();
		this.listWidget = new ListWidget<>(
		  this, this.minecraft, this.width, this.height, this.isShowingTabs() ? 70 : 30,
		  this.height - 32, this.getBackgroundLocation());
		this.children.add(this.listWidget);
		if (this.categorizedEntries.size() > this.selectedCategoryIndex) {
			this.listWidget.getEventListeners().addAll(
			  (List) Lists.newArrayList(this.categorizedEntries.values())
				 .get(this.selectedCategoryIndex));
		}
		int buttonWidths = Math.min(200, (this.width - 50 - 12) / 3);
		this.quitButton =
		  new Button(this.width / 2 - buttonWidths - 3, this.height - 26, buttonWidths, 20,
		             this.isEdited() ? new TranslationTextComponent(
			            "text.cloth-config.cancel_discard")
		                             : new TranslationTextComponent("gui.cancel"),
		             widget -> this.quit());
		this.addButton(this.quitButton);
		this.saveButton = new Button(this.width / 2 + 3, this.height - 26, buttonWidths, 20,
		                             NarratorChatListener.EMPTY, button -> this.saveAll(true)) {
			
			public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
				boolean hasErrors = false;
				for (List<AbstractConfigEntry<?>> entries : Lists.newArrayList(
				  ClothConfigScreen.this.categorizedEntries.values())) {
					for (AbstractConfigEntry<?> entry : entries) {
						if (!entry.getConfigError().isPresent()) continue;
						hasErrors = true;
						break;
					}
					if (!hasErrors) continue;
					break;
				}
				this.active = ClothConfigScreen.this.isEdited() && !hasErrors;
				this.setMessage(
				  hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save")
				            : new TranslationTextComponent("text.cloth-config.save_and_done"));
				super.render(matrices, mouseX, mouseY, delta);
			}
		};
		this.addButton(this.saveButton);
		this.saveButton.active = this.isEdited();
		if (this.isShowingTabs()) {
			this.tabsBounds = new Rectangle(0, 41, this.width, 24);
			this.tabsLeftBounds = new Rectangle(0, 41, 18, 24);
			this.tabsRightBounds = new Rectangle(this.width - 18, 41, 18, 24);
			this.buttonLeftTab = new Button(4, 44, 12, 18, NarratorChatListener.EMPTY,
			                                button -> this.tabsScroller.scrollTo(0.0, true)) {
				
				public void renderButton(
				  @NotNull MatrixStack matrices, int mouseX, int mouseY, float delta
				) {
					ClothConfigScreen.this.minecraft.getTextureManager()
					  .bindTexture(AbstractConfigScreen.CONFIG_TEX);
					RenderSystem.color4f(1.0f, 1.0f, 1.0f, this.alpha);
					int int_3 = this.getYImage(this.isHovered());
					RenderSystem.enableBlend();
					RenderSystem.blendFuncSeparate(770, 771, 0, 1);
					RenderSystem.blendFunc(770, 771);
					this.blit(matrices, this.x, this.y, 12, 18 * int_3, this.width, this.height);
				}
			};
			this.children.add(this.buttonLeftTab);
			int j = 0;
			for (Tuple<ITextComponent, Integer> tab : this.tabs) {
				this.tabButtons.add(new ClothConfigTabButton(this, j, -100, 43, tab.getB(), 20,
				                                             tab.getA(),
				                                             this.categoryMap.get(tab.getA())
				                                               .getDescription()));
				++j;
			}
			this.children.addAll(this.tabButtons);
			this.buttonRightTab = new Button(this.width - 16, 44, 12, 18, NarratorChatListener.EMPTY,
			                                 button -> this.tabsScroller.scrollTo(
			                                   this.tabsScroller.getMaxScroll(), true)) {
				
				public void renderButton(
				  @NotNull MatrixStack matrices, int mouseX, int mouseY, float delta
				) {
					ClothConfigScreen.this.minecraft.getTextureManager()
					  .bindTexture(AbstractConfigScreen.CONFIG_TEX);
					RenderSystem.color4f(1.0f, 1.0f, 1.0f, this.alpha);
					int int_3 = this.getYImage(this.isHovered());
					RenderSystem.enableBlend();
					RenderSystem.blendFuncSeparate(770, 771, 0, 1);
					RenderSystem.blendFunc(770, 771);
					this.blit(matrices, this.x, this.y, 0, 18 * int_3, this.width, this.height);
				}
			};
			this.children.add(this.buttonRightTab);
		} else {
			this.tabsLeftBounds = this.tabsRightBounds = new Rectangle();
			this.tabsBounds = this.tabsRightBounds;
		}
		Optional.ofNullable(this.afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (this.tabsBounds.contains(mouseX, mouseY) &&
		    !this.tabsLeftBounds.contains(mouseX, mouseY) &&
		    !this.tabsRightBounds.contains(mouseX, mouseY) && amount != 0.0) {
			this.tabsScroller.offset(-amount * 16.0, true);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	public double getTabsMaximumScrolled() {
		if (this.tabsMaximumScrolled == -1.0) {
			int[] i = new int[]{0};
			for (Tuple<ITextComponent, Integer> pair : this.tabs) {
				i[0] = i[0] + (pair.getB() + 2);
			}
			this.tabsMaximumScrolled = i[0];
		}
		return this.tabsMaximumScrolled + 6.0;
	}
	
	public void resetTabsMaximumScrolled() {
		this.tabsMaximumScrolled = -1.0;
	}
	
	@Override
	public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (this.isShowingTabs()) {
			this.tabsScroller.updatePosition(delta * 3.0f);
			int xx = 24 - (int) this.tabsScroller.scrollAmount;
			for (ClothConfigTabButton tabButton : this.tabButtons) {
				tabButton.x = xx;
				xx += tabButton.getWidth() + 2;
			}
			this.buttonLeftTab.active = this.tabsScroller.scrollAmount > 0.0;
			this.buttonRightTab.active = this.tabsScroller.scrollAmount <
			                             this.getTabsMaximumScrolled() - (double) this.width + 40.0;
		}
		if (this.isTransparentBackground()) {
			this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
		} else {
			this.renderDirtBackground(0);
		}
		this.listWidget.render(matrices, mouseX, mouseY, delta);
		ScissorsHandler.INSTANCE.scissor(
		  new Rectangle(this.listWidget.left, this.listWidget.top, this.listWidget.width,
		                this.listWidget.bottom - this.listWidget.top));
		for (Object child : this.listWidget.getEventListeners()) {
			((AbstractConfigEntry<?>) child).lateRender(matrices, mouseX, mouseY, delta);
		}
		ScissorsHandler.INSTANCE.removeLastScissor();
		if (this.isShowingTabs()) {
			ClothConfigScreen.drawCenteredString(
			  matrices, this.minecraft.fontRenderer, this.title, this.width / 2, 18, -1);
			Rectangle onlyInnerTabBounds =
			  new Rectangle(this.tabsBounds.x + 20, this.tabsBounds.y, this.tabsBounds.width - 40,
			                this.tabsBounds.height);
			ScissorsHandler.INSTANCE.scissor(onlyInnerTabBounds);
			if (this.isTransparentBackground()) {
				this.fillGradient(
				  matrices, onlyInnerTabBounds.x, onlyInnerTabBounds.y, onlyInnerTabBounds.getMaxX(),
				  onlyInnerTabBounds.getMaxY(), 0x68000000, 0x68000000);
			} else {
				this.overlayBackground(matrices, onlyInnerTabBounds, 32, 32, 32, 255, 255);
			}
			this.tabButtons.forEach(widget -> widget.render(matrices, mouseX, mouseY, delta));
			this.drawTabsShades(matrices, 0, this.isTransparentBackground() ? 120 : 255);
			ScissorsHandler.INSTANCE.removeLastScissor();
			this.buttonLeftTab.render(matrices, mouseX, mouseY, delta);
			this.buttonRightTab.render(matrices, mouseX, mouseY, delta);
		} else {
			ClothConfigScreen.drawCenteredString(
			  matrices, this.minecraft.fontRenderer, this.title, this.width / 2, 12, -1);
		}
		if (this.isEditable()) {
			List<ITextComponent> errors = Lists.newArrayList();
			for (List<AbstractConfigEntry<?>> entries : Lists.newArrayList(
			  this.categorizedEntries.values())) {
				for (AbstractConfigEntry<?> entry : entries) {
					if (!entry.getConfigError().isPresent()) continue;
					errors.add(entry.getConfigError().get());
				}
			}
			if (errors.size() > 0) {
				this.minecraft.getTextureManager().bindTexture(CONFIG_TEX);
				RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				String text =
				  "\u00a7c" + (errors.size() == 1 ? errors.get(0).copyRaw().getString() : I18n.format(
					 "text.cloth-config.multi_error"));
				if (this.isTransparentBackground()) {
					int stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
					Objects.requireNonNull(this.minecraft.fontRenderer);
					this.fillGradient(matrices, 8, 9, 20 + stringWidth, 14 + 9, 0x68000000, 0x68000000);
				}
				this.blit(matrices, 10, 10, 0, 54, 3, 11);
				ClothConfigScreen.drawString(
				  matrices, this.minecraft.fontRenderer, text, 18, 12, -1);
				if (errors.size() > 1) {
					int stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
					if (mouseX >= 10 && mouseY >= 10 && mouseX <= 18 + stringWidth) {
						Objects.requireNonNull(this.minecraft.fontRenderer);
						if (mouseY <= 14 + 9) {
							this.addTooltip(Tooltip.of(new Point(mouseX, mouseY),
							                           errors.toArray(new ITextComponent[0])));
						}
					}
				}
			}
		} else if (!this.isEditable()) {
			this.minecraft.getTextureManager().bindTexture(CONFIG_TEX);
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
			String text = "\u00a7c" + I18n.format("text.cloth-config.not_editable");
			if (this.isTransparentBackground()) {
				int stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
				Objects.requireNonNull(this.minecraft.fontRenderer);
				this.fillGradient(matrices, 8, 9, 20 + stringWidth, 14 + 9, 0x68000000, 0x68000000);
			}
			this.blit(matrices, 10, 10, 0, 54, 3, 11);
			ClothConfigScreen.drawString(matrices, this.minecraft.fontRenderer, text, 18, 12, -1);
		}
		super.render(matrices, mouseX, mouseY, delta);
	}
	
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public void queueTooltip(QueuedTooltip queuedTooltip) {
		super.addTooltip(queuedTooltip);
	}
	
	private void drawTabsShades(MatrixStack matrices, int lightColor, int darkColor) {
		this.drawTabsShades(matrices.getLast().getMatrix(), lightColor, darkColor);
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
		  matrix, (float) (this.tabsBounds.getMinX() + 20), (float) (this.tabsBounds.getMinY() + 4),
		  0.0f).tex(0.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
		  matrix, (float) (this.tabsBounds.getMaxX() - 20), (float) (this.tabsBounds.getMinY() + 4),
		  0.0f).tex(1.0f, 1.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
			 matrix, (float) (this.tabsBounds.getMaxX() - 20), (float) this.tabsBounds.getMinY(), 0.0f)
		  .tex(1.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
			 matrix, (float) (this.tabsBounds.getMinX() + 20), (float) this.tabsBounds.getMinY(), 0.0f)
		  .tex(0.0f, 0.0f).color(0, 0, 0, darkColor).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(
			 matrix, (float) (this.tabsBounds.getMinX() + 20), (float) this.tabsBounds.getMaxY(), 0.0f)
		  .tex(0.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
			 matrix, (float) (this.tabsBounds.getMaxX() - 20), (float) this.tabsBounds.getMaxY(), 0.0f)
		  .tex(1.0f, 1.0f).color(0, 0, 0, darkColor).endVertex();
		buffer.pos(
		  matrix, (float) (this.tabsBounds.getMaxX() - 20), (float) (this.tabsBounds.getMaxY() - 4),
		  0.0f).tex(1.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		buffer.pos(
		  matrix, (float) (this.tabsBounds.getMinX() + 20), (float) (this.tabsBounds.getMaxY() - 4),
		  0.0f).tex(0.0f, 0.0f).color(0, 0, 0, lightColor).endVertex();
		tessellator.draw();
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	@Override
	public void save() {
		super.save();
	}
	
	@Override
	public boolean isEditable() {
		return super.isEditable();
	}
	
	public static class ListWidget<R extends DynamicElementListWidget.ElementEntry<R>>
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
			this.setRenderSelection(false);
			this.screen = screen;
		}
		
		@Override
		public int getItemWidth() {
			return this.width - 80;
		}
		
		@Override
		protected int getScrollbarPosition() {
			return this.left + this.width - 36;
		}
		
		@Override
		protected void renderItem(
		  MatrixStack matrices, R item, int index, int y, int x, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			if (item instanceof AbstractConfigEntry) {
				((AbstractConfigEntry<?>) item).updateSelected(this.getFocused() == item);
			}
			super.renderItem(
			  matrices, item, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		}
		
		@Override
		protected void renderList(
		  MatrixStack matrices, int startX, int startY, int int_3, int int_4, float delta
		) {
			long timePast;
			this.thisTimeTarget = null;
			if (this.hasCurrent) {
				timePast = System.currentTimeMillis() - this.lastTouch;
				int alpha = timePast <= 200L ? 255 : MathHelper.ceil(
				  255.0 - (double) (Math.min((float) (timePast - 200L), 500.0f) / 500.0f) * 255.0);
				alpha = alpha * 36 / 255 << 24;
				this.fillGradient(
				  matrices, this.currentX, this.currentY, this.currentX + this.currentWidth,
				  this.currentY + this.currentHeight, 0xFFFFFF | alpha, 0xFFFFFF | alpha);
			}
			super.renderList(matrices, startX, startY, int_3, int_4, delta);
			if (this.thisTimeTarget != null && this.isMouseOver(int_3, int_4)) {
				this.lastTouch = System.currentTimeMillis();
			}
			if (this.thisTimeTarget != null && !this.thisTimeTarget.equals(this.target)) {
				if (!this.hasCurrent) {
					this.currentX = this.thisTimeTarget.x;
					this.currentY = this.thisTimeTarget.y;
					this.currentWidth = this.thisTimeTarget.width;
					this.currentHeight = this.thisTimeTarget.height;
					this.hasCurrent = true;
				}
				this.target = this.thisTimeTarget.clone();
				this.start = this.lastTouch;
				this.duration = 40L;
			} else if (this.hasCurrent && this.target != null) {
				timePast = System.currentTimeMillis() - this.start;
				this.currentX = (int) ScrollingContainer.ease(this.currentX, this.target.x, Math.min(
				                                                (double) timePast / (double) this.duration * (double) delta * 3.0, 1.0),
				                                              EasingMethod.EasingMethodImpl.LINEAR);
				this.currentY = (int) ScrollingContainer.ease(this.currentY, this.target.y, Math.min(
				                                                (double) timePast / (double) this.duration * (double) delta * 3.0, 1.0),
				                                              EasingMethod.EasingMethodImpl.LINEAR);
				this.currentWidth = (int) ScrollingContainer.ease(this.currentWidth, this.target.width,
				                                                  Math.min((double) timePast /
				                                                           (double) this.duration *
				                                                           (double) delta * 3.0, 1.0),
				                                                  EasingMethod.EasingMethodImpl.LINEAR);
				this.currentHeight =
				  (int) ScrollingContainer.ease(this.currentHeight, this.target.height, Math.min(
					                               (double) timePast / (double) this.duration * (double) delta * 3.0, 1.0),
				                                EasingMethod.EasingMethodImpl.LINEAR);
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
			  this.getBlitOffset(), colorStart, colorEnd);
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
			this.updateScrollingState(mouseX, mouseY, button);
			if (!this.isMouseOver(mouseX, mouseY)) {
				return false;
			}
			for (R entry : this.getEventListeners()) {
				if (!entry.mouseClicked(mouseX, mouseY, button)) continue;
				this.setListener(entry);
				this.setDragging(true);
				return true;
			}
			if (button == 0) {
				this.clickedHeader(
				  (int) (mouseX - (double) (this.left + this.width / 2 - this.getItemWidth() / 2)),
				  (int) (mouseY - (double) this.top) + (int) this.getScroll() - 4);
				return true;
			}
			return this.scrolling;
		}
		
		@Override
		protected void renderBackBackground(
		  MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator
		) {
			if (!this.screen.isTransparentBackground()) {
				super.renderBackBackground(matrices, buffer, tessellator);
			} else {
				this.fillGradient(
				  matrices, this.left, this.top, this.right, this.bottom, 0x68000000, 0x68000000);
			}
		}
		
		@Override
		protected void renderHoleBackground(
		  MatrixStack matrices, int y1, int y2, int alpha1, int alpha2
		) {
			if (!this.screen.isTransparentBackground()) {
				super.renderHoleBackground(matrices, y1, y2, alpha1, alpha2);
			}
		}
	}
}

