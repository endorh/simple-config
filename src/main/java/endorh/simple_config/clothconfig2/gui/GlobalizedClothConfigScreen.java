package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.widget.SearchBarWidget;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;

// TODO: Drop or merge
@Deprecated public class GlobalizedClothConfigScreen
  extends AbstractConfigScreen implements ReferenceBuildingConfigScreen, IExpandable {
	public ClothConfigScreen.ListWidget<AbstractConfigEntry<?>> listWidget;
	private Widget cancelButton;
	private Widget exitButton;
	// private final LinkedHashMap<ITextComponent, List<AbstractConfigEntry<?>>> categorizedEntries = Maps.newLinkedHashMap();
	private final ScrollingContainer sideScroller = new ScrollingContainer() {
		
		@Override
		public Rectangle getBounds() {
			return new Rectangle(
			  4, 4, GlobalizedClothConfigScreen.this.getSideSliderPosition() - 14 - 4,
			  GlobalizedClothConfigScreen.this.height - 8);
		}
		
		@Override
		public int getMaxScrollHeight() {
			int i = 0;
			for (Reference reference : GlobalizedClothConfigScreen.this.references) {
				if (i != 0) {
					i = (int) ((float) i + 3.0f * reference.getScale());
				}
				float f = i;
				Objects.requireNonNull(GlobalizedClothConfigScreen.this.font);
				i = (int) (f + 9.0f * reference.getScale());
			}
			return i;
		}
	};
	private Reference lastHoveredReference = null;
	private final ScrollingContainer sideSlider = new ScrollingContainer() {
		private final Rectangle empty = new Rectangle();
		
		@Override
		public Rectangle getBounds() {
			return this.empty;
		}
		
		@Override
		public int getMaxScrollHeight() {
			return 1;
		}
	};
	private final List<Reference> references = Lists.newArrayList();
	private final LazyResettable<Integer> sideExpandLimit = new LazyResettable<>(() -> {
		int max = 0;
		for (Reference reference : this.references) {
			ITextComponent referenceText = reference.getText();
			int width = this.font.getStringPropertyWidth(
			  new StringTextComponent(
				 StringUtils.repeat("  ", reference.getIndent()) + "- ").append(referenceText));
			if (width <= max) continue;
			max = width;
		}
		return Math.min(max + 8, this.width / 4);
	});
	private boolean requestingReferenceRebuilding = false;
	
	@Override public List<AbstractConfigEntry<?>> getEntries() {
		return Lists.newArrayList();
	}
	
	@Internal
	public GlobalizedClothConfigScreen(
	  Screen parent, ITextComponent title, Map<String, ConfigCategory> categories,
	  ResourceLocation backgroundLocation
	) {
		super(parent, title, backgroundLocation, categories);
		for (Entry<String, ConfigCategory> e : categories.entrySet()) {
			for (AbstractConfigEntry<?> entry : e.getValue().getEntries())
				entry.setScreen(this);
		}
		this.sideSlider.scrollTo(0.0, false);
		cancelButton = new Button(0, 0, 0, 0, NarratorChatListener.EMPTY, p -> this.onClose());
		exitButton = new Button(0, 0, 0, 0, NarratorChatListener.EMPTY, p -> onClose());
	}
	
	@Override public void requestReferenceRebuilding() {
		this.requestingReferenceRebuilding = true;
	}
	
	protected void init() {
		super.init();
		this.sideExpandLimit.reset();
		this.references.clear();
		this.buildReferences();
		this.listWidget =
		  new ClothConfigScreen.ListWidget<>(
			 this, this.minecraft, this.width - 14, this.height, 30,
			 this.height - 32, this.getBackgroundLocation());
		this.children.add(this.listWidget);
		this.listWidget.setLeftPos(14);
		for (ConfigCategory cat : this.sortedCategories) {
			if (!this.listWidget.getEntries().isEmpty())
				this.listWidget.getEntries().add(new EmptyEntry(5));
			this.listWidget.getEntries().add(new EmptyEntry(4));
			this.listWidget.getEntries().add(new CategoryTextEntry(
			  cat.getTitle(), cat.getTitle().deepCopy().mergeStyle(TextFormatting.BOLD)));
			this.listWidget.getEntries().add(new EmptyEntry(4));
			this.listWidget.getEntries().addAll(cat.getEntries());
		}
		int buttonWidths = Math.min(200, (this.width - 50 - 12) / 3);
		this.cancelButton = new Button(0, this.height - 26, buttonWidths, 20,
		                               this.isEdited() ? new TranslationTextComponent(
		                                 "text.cloth-config.cancel_discard")
		                                               : new TranslationTextComponent("gui.cancel"),
		                               widget -> this.quit());
		this.addButton(this.cancelButton);
		this.exitButton =
		  new Button(0, this.height - 26, buttonWidths, 20, NarratorChatListener.EMPTY,
		             button -> this.saveAll(true)) {
			  
			  public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
				  boolean hasErrors = false;
				  entries:
				  for (ConfigCategory cat : sortedCategories) {
					  for (AbstractConfigEntry<?> entry : cat.getEntries()) {
						  if (entry.hasErrors()) {
							  hasErrors = true;
							  break entries;
						  }
					  }
				  }
				  this.active = GlobalizedClothConfigScreen.this.isEdited() && !hasErrors;
				  this.setMessage(
					 hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save")
					           : new TranslationTextComponent("text.cloth-config.save_and_done"));
				  super.render(matrices, mouseX, mouseY, delta);
			  }
		  };
		this.addButton(this.exitButton);
		Optional.ofNullable(this.afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
	}
	
	private void buildReferences() {
		for (ConfigCategory cat : sortedCategories) {
			references.add(new CategoryReference(cat.getTitle()));
			for (AbstractConfigEntry<?> entry : cat.getEntries())
				buildReferenceFor(entry, 1);
		}
	}
	
	private void buildReferenceFor(AbstractConfigEntry<?> entry, int layer) {
		List<ReferenceProvider> referencableEntries = entry.getReferenceProviderEntries();
		if (referencableEntries != null) {
			this.references.add(new ConfigEntryReference(entry, layer));
			for (ReferenceProvider referencableEntry : referencableEntries) {
				this.buildReferenceFor(referencableEntry.provideReferenceEntry(), layer + 1);
			}
		}
	}
	
	@Override
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		this.lastHoveredReference = null;
		if (this.requestingReferenceRebuilding) {
			this.references.clear();
			this.buildReferences();
			this.requestingReferenceRebuilding = false;
		}
		int sliderPosition = this.getSideSliderPosition();
		ScissorsHandler.INSTANCE.scissor(
		  new Rectangle(sliderPosition, 0, this.width - sliderPosition, this.height));
		if (this.isTransparentBackground()) {
			this.fillGradient(mStack, 14, 0, this.width, this.height, -1072689136, -804253680);
		} else {
			this.renderDirtBackground(0);
			this.overlayBackground(
			  mStack, new Rectangle(14, 0, this.width, this.height), 64, 64, 64);
		}
		this.listWidget.width = this.width - sliderPosition;
		this.listWidget.setLeftPos(sliderPosition);
		this.listWidget.render(mStack, mouseX, mouseY, delta);
		ScissorsHandler.INSTANCE.scissor(
		  new Rectangle(this.listWidget.left, this.listWidget.top, this.listWidget.width,
		                this.listWidget.bottom - this.listWidget.top));
		ScissorsHandler.INSTANCE.removeLastScissor();
		this.font.func_238407_a_(
		  mStack, this.title.func_241878_f(),
		  (float) sliderPosition + (float) (this.width - sliderPosition) / 2.0f -
		  (float) this.font.getStringPropertyWidth(this.title) / 2.0f, 12.0f, -1);
		ScissorsHandler.INSTANCE.removeLastScissor();
		this.cancelButton.x =
		  sliderPosition + (this.width - sliderPosition) / 2 - this.cancelButton.getWidth() - 3;
		this.exitButton.x = sliderPosition + (this.width - sliderPosition) / 2 + 3;
		super.render(mStack, mouseX, mouseY, delta);
		this.sideSlider.updatePosition(delta);
		this.sideScroller.updatePosition(delta);
		if (this.isTransparentBackground()) {
			this.fillGradient(mStack, 0, 0, sliderPosition, this.height, -1240461296, -972025840);
			this.fillGradient(
			  mStack, 0, 0, sliderPosition - 14, this.height, 0x68000000, 0x68000000);
		} else {
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			this.minecraft.getTextureManager().bindTexture(this.getBackgroundLocation());
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
			float f = 32.0f;
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(sliderPosition - 14, this.height, 0.0).tex(0.0f, (float) this.height / 32.0f).color(68, 68, 68, 255).endVertex();
			buffer.pos(sliderPosition, this.height, 0.0).tex(14 / 32.0F, (float) this.height / 32.0f).color(68, 68, 68, 255).endVertex();
			buffer.pos(sliderPosition, 0.0, 0.0).tex(14 / 32.0F, 0.0f).color(68, 68, 68, 255).endVertex();
			buffer.pos(sliderPosition - 14, 0.0, 0.0).tex(0.0f, 0.0f).color(68, 68, 68, 255).endVertex();
			tessellator.draw();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(0.0, this.height, 0.0).tex(0.0f, (float) (this.height + (int) this.sideScroller.scrollAmount) / 32.0f).color(32, 32, 32, 255).endVertex();
			buffer.pos(sliderPosition - 14, this.height, 0.0).tex((float) (sliderPosition - 14) / 32.0f, (float) (this.height + (int) this.sideScroller.scrollAmount) / 32.0f).color(32, 32, 32, 255).endVertex();
			buffer.pos(sliderPosition - 14, 0.0, 0.0).tex((float) (sliderPosition - 14) / 32.0f, (float) ((int) this.sideScroller.scrollAmount) / 32.0f).color(32, 32, 32, 255).endVertex();
			buffer.pos(0.0, 0.0, 0.0).tex(0.0f, (float) ((int) this.sideScroller.scrollAmount) / 32.0f).color(32, 32, 32, 255).endVertex();
			tessellator.draw();
		}
		Matrix4f matrix = mStack.getLast().getMatrix();
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.disableAlphaTest();
		RenderSystem.defaultBlendFunc();
		RenderSystem.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		int shadeColor = this.isTransparentBackground() ? 120 : 160;
		buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(matrix, (float) (sliderPosition + 4), 0.0f, 100.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) sliderPosition, 0.0f, 100.0f).color(0, 0, 0, shadeColor).endVertex();
		buffer.pos(matrix, (float) sliderPosition, (float) this.height, 100.0f).color(0, 0, 0, shadeColor).endVertex();
		buffer.pos(matrix, (float) (sliderPosition + 4), (float) this.height, 100.0f).color(0, 0, 0, 0).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(matrix, (float) (sliderPosition - 14), 0.0f, 100.0f).color(0, 0, 0, shadeColor /= 2).endVertex();
		buffer.pos(matrix, (float) (sliderPosition - 14 - 4), 0.0f, 100.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) (sliderPosition - 14 - 4), (float) this.height, 100.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) (sliderPosition - 14), (float) this.height, 100.0f).color(0, 0, 0, shadeColor).endVertex();
		tessellator.draw();
		RenderSystem.shadeModel(7424);
		RenderSystem.disableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
		Rectangle slideArrowBounds = new Rectangle(sliderPosition - 14, 0, 14, this.height);
		RenderSystem.enableAlphaTest();
		IRenderTypeBuffer.Impl immediate = IRenderTypeBuffer.getImpl(
		  Tessellator.getInstance().getBuffer());
		this.font.renderStringAtPos(
		  ">", (float) (sliderPosition - 7) - (float) this.font.getStringWidth(">") / 2.0f,
		  (float) (this.height / 2),
		  (slideArrowBounds.contains(mouseX, mouseY) ? 0xFFFFA0 : 0xFFFFFF) | MathHelper.clamp(
		    MathHelper.ceil((1.0 - this.sideSlider.scrollAmount) * 255.0), 0, 255) << 24,
		  false, mStack.getLast().getMatrix(), immediate, false, 0, 0xF000F0);
		this.font.renderStringAtPos(
		  "<", (float) (sliderPosition - 7) - (float) this.font.getStringWidth("<") / 2.0f,
		  (float) (this.height / 2),
		  (slideArrowBounds.contains(mouseX, mouseY) ? 0xFFFFA0 : 0xFFFFFF) | MathHelper.clamp(
		    MathHelper.ceil(this.sideSlider.scrollAmount * 255.0), 0, 255) << 24,
		  false, mStack.getLast().getMatrix(), immediate, false, 0, 0xF000F0);
		immediate.finish();
		Rectangle scrollerBounds = this.sideScroller.getBounds();
		if (!scrollerBounds.isEmpty()) {
			ScissorsHandler.INSTANCE.scissor(new Rectangle(0, 0, sliderPosition - 14, this.height));
			int scrollOffset = (int) ((double) scrollerBounds.y - this.sideScroller.scrollAmount);
			for (Reference reference : this.references) {
				mStack.push();
				mStack.scale(reference.getScale(), reference.getScale(), reference.getScale());
				IFormattableTextComponent text = new StringTextComponent(StringUtils.repeat(
				  "  ", reference.getIndent()) + "- ").append(reference.getText());
				if (this.lastHoveredReference == null) {
					int n = (int) ((float) scrollOffset - 4.0f * reference.getScale());
					int n2 =
					  (int) ((float) this.font.getStringPropertyWidth(text) * reference.getScale());
					Objects.requireNonNull(this.font);
					if (new Rectangle(
					  scrollerBounds.x, n, n2, (int) ((float) (9 + 4) * reference.getScale())).contains(
					  mouseX, mouseY)) {
						this.lastHoveredReference = reference;
					}
				}
				this.font.func_238422_b_(
				  mStack, text.func_241878_f(), (float) scrollerBounds.x, (float) scrollOffset,
				  this.lastHoveredReference == reference ? 16769544 : 0xFFFFFF);
				mStack.pop();
				float f = scrollOffset;
				Objects.requireNonNull(this.font);
				scrollOffset = (int) (f + (float) (9 + 3) * reference.getScale());
			}
			ScissorsHandler.INSTANCE.removeLastScissor();
			this.sideScroller.renderScrollBar();
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Rectangle slideBounds = new Rectangle(0, 0, this.getSideSliderPosition() - 14, this.height);
		if (button == 0 && slideBounds.contains(mouseX, mouseY) &&
		    this.lastHoveredReference != null) {
			this.minecraft.getSoundHandler().play(
			  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			this.lastHoveredReference.go();
			return true;
		}
		Rectangle slideArrowBounds =
		  new Rectangle(this.getSideSliderPosition() - 14, 0, 14, this.height);
		if (button == 0 && slideArrowBounds.contains(mouseX, mouseY)) {
			this.setExpanded(!this.isExpanded());
			this.minecraft.getSoundHandler().play(
			  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean isExpanded() {
		return this.sideSlider.scrollTarget == 1.0;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recursive) {
		this.sideSlider.scrollTo(expanded ? 1.0 : 0.0, true, 2000L);
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		Rectangle slideBounds = new Rectangle(0, 0, this.getSideSliderPosition() - 14, this.height);
		if (slideBounds.contains(mouseX, mouseY)) {
			this.sideScroller.offset(ClothConfigInitializer.getScrollStep() * -amount, true);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	private int getSideSliderPosition() {
		return (int) (this.sideSlider.scrollAmount * (double) this.sideExpandLimit.get() +
		              14.0);
	}
	
	private class ConfigEntryReference
	  implements Reference {
		private final AbstractConfigEntry<?> entry;
		private final int layer;
		
		public ConfigEntryReference(AbstractConfigEntry<?> entry, int layer) {
			this.entry = entry;
			this.layer = layer;
		}
		
		@Override
		public int getIndent() {
			return this.layer;
		}
		
		@Override
		public ITextComponent getText() {
			return this.entry.getFieldName();
		}
		
		@Override
		public float getScale() {
			return 1.0f;
		}
		
		@Override
		public void go() {
			int[] i = new int[]{0};
			for (AbstractConfigEntry child : GlobalizedClothConfigScreen.this.listWidget.getEventListeners()) {
				int i1 = i[0];
				if (this.goChild(i, null, child)) {
					return;
				}
				i[0] = i1 + child.getItemHeight();
			}
		}
		
		private boolean goChild(int[] i, Integer expandedParent, AbstractConfigEntry<?> root) {
			boolean expanded;
			if (root == this.entry) {
				GlobalizedClothConfigScreen.this.listWidget.scrollTo(
				  expandedParent == null ? (double) i[0] : (double) expandedParent, true);
				return true;
			}
			int j = i[0];
			i[0] = i[0] + root.getInitialReferenceOffset();
			boolean bl = expanded = root instanceof IExpandable && ((IExpandable) root).isExpanded();
			if (root instanceof IExpandable) {
				((IExpandable) root).setExpanded(true);
			}
			List<? extends IGuiEventListener> children = root.getEventListeners();
			if (root instanceof IExpandable) {
				((IExpandable) root).setExpanded(expanded);
			}
			for (IGuiEventListener child : children) {
				if (!(child instanceof ReferenceProvider)) continue;
				int i1 = i[0];
				if (this.goChild(
				  i, expandedParent != null ? expandedParent : (root instanceof IExpandable && !expanded
				                                                ? j : null),
				  ((ReferenceProvider) child).provideReferenceEntry())) {
					return true;
				}
				i[0] = i1 + ((ReferenceProvider) child).provideReferenceEntry().getItemHeight();
			}
			return false;
		}
	}
	
	private class CategoryReference
	  implements Reference {
		private final ITextComponent category;
		
		public CategoryReference(ITextComponent category) {
			this.category = category;
		}
		
		@Override
		public ITextComponent getText() {
			return this.category;
		}
		
		@Override
		public float getScale() {
			return 1.0f;
		}
		
		@Override
		public void go() {
			int i = 0;
			for (AbstractConfigEntry child : GlobalizedClothConfigScreen.this.listWidget.getEventListeners()) {
				if (child instanceof CategoryTextEntry &&
				    ((CategoryTextEntry) child).category == this.category) {
					GlobalizedClothConfigScreen.this.listWidget.scrollTo(i, true);
					return;
				}
				i += child.getItemHeight();
			}
		}
	}
	
	private interface Reference {
		default int getIndent() {
			return 0;
		}
		
		ITextComponent getText();
		
		float getScale();
		
		void go();
	}
	
	private static class CategoryTextEntry extends AbstractConfigListEntry<Void> {
		private final ITextComponent category;
		private final ITextComponent text;
		
		public CategoryTextEntry(ITextComponent category, ITextComponent text) {
			super(new StringTextComponent(UUID.randomUUID().toString()));
			this.category = category;
			this.text = text;
		}
		
		@Override
		public int getItemHeight() {
			List strings = Minecraft.getInstance().fontRenderer.trimStringToWidth(this.text,
			                                                                      this.getParent()
			                                                                        .getItemWidth());
			if (strings.isEmpty()) {
				return 0;
			}
			return 4 + strings.size() * 10;
		}
		
		@Override public Void getValue() {
			return null;
		}
		
		@Override public void setValue(Void value) {}
		
		@Override
		public boolean isMouseInside(
		  int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight
		) {
			return false;
		}
		
		@Override
		public void renderEntry(
		  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
		  int mouseY, boolean isHovered, float delta
		) {
			super.renderEntry(
			  mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
			int yy = y + 2;
			List<IReorderingProcessor> texts =
			  Minecraft.getInstance().fontRenderer.trimStringToWidth(this.text,
			                                                         this.getParent().getItemWidth());
			for (IReorderingProcessor text : texts) {
				Minecraft.getInstance().fontRenderer.func_238407_a_(
				  mStack, text, (float) (x - 4 + entryWidth / 2 -
				                           Minecraft.getInstance().fontRenderer.func_243245_a(text) /
				                           2), (float) yy, -1);
				yy += 10;
			}
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.emptyList();
		}
	}
	
	private static class EmptyEntry extends AbstractConfigListEntry<Void> {
		private final int height;
		
		public EmptyEntry(int height) {
			super(new StringTextComponent(UUID.randomUUID().toString()));
			this.height = height;
		}
		
		@Override
		public int getItemHeight() {
			return this.height;
		}
		
		@Override public Void getValue() {
			return null;
		}
		
		@Override public void setValue(Void value) {}
		
		@Override
		public boolean isMouseInside(
		  int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight
		) {
			return false;
		}
		
		@Override
		public void renderEntry(
		  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
		  int mouseY, boolean isHovered, float delta
		) {
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.emptyList();
		}
	}
	
	@Override public int getFocusedScroll() {
		return listWidget.getFocusedScroll();
	}
	
	@Override public int getFocusedHeight() {
		return listWidget.getFocusedHeight();
	}
	
	@Override public SearchBarWidget getSearchBar() {
		return null;
	}
}

