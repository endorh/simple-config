package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.SimpleConfigMod;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.Expandable;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class SubCategoryListEntry
  extends TooltipListEntry<List<AbstractConfigListEntry<?>>>
  implements Expandable {
	private static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation(SimpleConfigMod.MOD_ID, "textures/gui/cloth_config.png");
	private final List<AbstractConfigListEntry<?>> entries;
	private final CategoryLabelWidget widget;
	private final List<IGuiEventListener> children;
	private boolean expanded;
	
	@Deprecated
	public SubCategoryListEntry(
	  ITextComponent categoryName, List<AbstractConfigListEntry<?>> entries, boolean defaultExpanded
	) {
		super(categoryName, null);
		this.entries = entries;
		this.expanded = defaultExpanded;
		this.widget = new CategoryLabelWidget();
		this.children = Lists.newArrayList(this.widget);
		this.children.addAll(entries);
		//noinspection unchecked
		this.setReferenceProviderEntries((List<ReferenceProvider<?>>) (List<?>) entries);
	}
	
	@Override
	public boolean isExpanded() {
		return this.expanded;
	}
	
	@Override
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	@Override
	public boolean isRequiresRestart() {
		for (AbstractConfigListEntry<?> entry : this.entries) {
			if (!entry.isRequiresRestart()) continue;
			return true;
		}
		return false;
	}
	
	@Override
	public void setRequiresRestart(boolean requiresRestart) {
	}
	
	public ITextComponent getCategoryName() {
		return this.getFieldName();
	}
	
	@Override
	public List<AbstractConfigListEntry<?>> getValue() {
		return this.entries;
	}
	
	@Override public void setValue(List<AbstractConfigListEntry<?>> value) {
		throw new UnsupportedOperationException("Cannot change entries of category");
	}
	
	@Override
	public Optional<List<AbstractConfigListEntry<?>>> getDefaultValue() {
		return Optional.empty();
	}
	
	@Override
	public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.render(
		  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		Minecraft.getInstance().getTextureManager().bindTexture(CONFIG_TEX);
		RenderHelper.disableStandardItemLighting();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		this.blit(
		  matrices, x - 15, y + 5, 24,
		  (this.widget.rectangle.contains(mouseX, mouseY) ? 18 : 0) + (this.expanded ? 9 : 0), 9, 9);
		Minecraft.getInstance().fontRenderer.func_238407_a_(
		  matrices, this.getDisplayedFieldName().func_241878_f(), (float) x, (float) (y + 6),
		  this.widget.rectangle.contains(mouseX, mouseY) ? -1638890 : -1);
		for (AbstractConfigListEntry<?> entry : this.entries) {
			entry.setParent((DynamicEntryListWidget) this.getParent());
			entry.setScreen(this.getConfigScreen());
		}
		if (this.expanded) {
			int yy = y + 24;
			for (AbstractConfigListEntry<?> entry : this.entries) {
				entry.render(
				  matrices, -1, yy, x + 14, entryWidth - 14, entry.getItemHeight(), mouseX, mouseY,
				  isHovered && this.getListener() == entry, delta);
				yy += entry.getItemHeight();
			}
		}
	}
	
	@Override
	public void updateSelected(boolean isSelected) {
		for (AbstractConfigListEntry<?> entry : this.entries) {
			entry.updateSelected(this.expanded && isSelected && this.getListener() == entry);
		}
	}
	
	@Override
	public boolean isEdited() {
		for (AbstractConfigListEntry<?> entry : this.entries) {
			if (!entry.isEdited()) continue;
			return true;
		}
		return false;
	}
	
	@Override
	public void lateRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (this.expanded) {
			for (AbstractConfigListEntry<?> entry : this.entries) {
				entry.lateRender(matrices, mouseX, mouseY, delta);
			}
		}
	}
	
	@Override
	public int getMorePossibleHeight() {
		if (!this.expanded) {
			return -1;
		}
		ArrayList<Integer> list = new ArrayList<>();
		int i = 24;
		for (AbstractConfigListEntry<?> entry : this.entries) {
			i += entry.getItemHeight();
			if (entry.getMorePossibleHeight() < 0) continue;
			list.add(i + entry.getMorePossibleHeight());
		}
		list.add(i);
		return list.stream().max(Integer::compare).orElse(0) - this.getItemHeight();
	}
	
	@Override
	public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
		this.widget.rectangle.x = x - 15;
		this.widget.rectangle.y = y;
		this.widget.rectangle.width = entryWidth + 15;
		this.widget.rectangle.height = 24;
		return new Rectangle(
		  this.getParent().left, y, this.getParent().right - this.getParent().left, 20);
	}
	
	@Override
	public int getItemHeight() {
		if (this.expanded) {
			int i = 24;
			for (AbstractConfigListEntry entry : this.entries) {
				i += entry.getItemHeight();
			}
			return i;
		}
		return 24;
	}
	
	@Override
	public int getInitialReferenceOffset() {
		return 24;
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.expanded ? this.children : Collections.singletonList(this.widget);
	}
	
	@Override
	public void save() {
		this.entries.forEach(AbstractConfigEntry::save);
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		ITextComponent error = null;
		for (AbstractConfigListEntry<?> entry : this.entries) {
			Optional<ITextComponent> configError = entry.getConfigError();
			if (!configError.isPresent()) continue;
			if (error != null) {
				return Optional.ofNullable(
				  new TranslationTextComponent("text.cloth-config.multi_error"));
			}
			return configError;
		}
		return Optional.ofNullable(error);
	}
	
	public class CategoryLabelWidget
	  implements IGuiEventListener {
		private final Rectangle rectangle = new Rectangle();
		
		public boolean mouseClicked(double double_1, double double_2, int int_1) {
			if (this.rectangle.contains(double_1, double_2)) {
				SubCategoryListEntry.this.expanded = !SubCategoryListEntry.this.expanded;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
			return false;
		}
	}
}

