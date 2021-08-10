package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.Expandable;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListEntry<T, C extends BaseListCell, SELF extends BaseListEntry<T, C, SELF>>
  extends TooltipListEntry<List<T>>
  implements Expandable {
	protected static final ResourceLocation CONFIG_TEX =
	  new ResourceLocation("cloth-config2", "textures/gui/cloth_config.png");
	@NotNull
	protected final List<C> cells;
	@NotNull
	protected final List<IGuiEventListener> widgets;
	protected boolean expanded;
	protected boolean deleteButtonEnabled;
	protected boolean insertInFront;
	@Nullable
	protected Consumer<List<T>> saveConsumer;
	protected ListLabelWidget labelWidget;
	protected Widget resetWidget;
	@NotNull
	protected Function<SELF, C> createNewInstance;
	@NotNull
	protected Supplier<List<T>> defaultValue;
	@Nullable
	protected ITextComponent addTooltip = new TranslationTextComponent("text.cloth-config.list.add");
	@Nullable
	protected ITextComponent removeTooltip =
	  new TranslationTextComponent("text.cloth-config.list.remove");
	
	@ApiStatus.Internal
	public BaseListEntry(
	  @NotNull ITextComponent fieldName,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  @Nullable Supplier<List<T>> defaultValue, @NotNull Function<SELF, C> createNewInstance,
	  @Nullable Consumer<List<T>> saveConsumer, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, tooltipSupplier, defaultValue, createNewInstance, saveConsumer, resetButtonKey,
		  false);
	}
	
	@ApiStatus.Internal
	public BaseListEntry(
	  @NotNull ITextComponent fieldName,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  @Nullable Supplier<List<T>> defaultValue, @NotNull Function<SELF, C> createNewInstance,
	  @Nullable Consumer<List<T>> saveConsumer, ITextComponent resetButtonKey,
	  boolean requiresRestart
	) {
		this(
		  fieldName, tooltipSupplier, defaultValue, createNewInstance, saveConsumer, resetButtonKey,
		  requiresRestart, true, true);
	}
	
	@ApiStatus.Internal
	public BaseListEntry(
	  @NotNull ITextComponent fieldName,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  @Nullable Supplier<List<T>> defaultValue, @NotNull Function<SELF, C> createNewInstance,
	  @Nullable Consumer<List<T>> saveConsumer, ITextComponent resetButtonKey,
	  boolean requiresRestart, boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(fieldName, tooltipSupplier, requiresRestart);
		this.deleteButtonEnabled = deleteButtonEnabled;
		this.insertInFront = insertInFront;
		this.cells = Lists.newArrayList();
		this.labelWidget = new ListLabelWidget();
		this.widgets = Lists.newArrayList(this.labelWidget);
		this.resetWidget =
		  new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
			 resetButtonKey) + 6, 20, resetButtonKey, widget -> {
			  this.widgets.removeAll(this.cells);
			  for (BaseListCell cell : this.cells) {
				  cell.onDelete();
			  }
			  this.cells.clear();
			  defaultValue.get().stream().map(this::cellWithValue).forEach(this.cells::add);
			  for (BaseListCell cell : this.cells) {
				  cell.onAdd();
			  }
			  this.widgets.addAll(this.cells);
		  });
		this.widgets.add(this.resetWidget);
		this.saveConsumer = saveConsumer;
		this.createNewInstance = createNewInstance;
		this.defaultValue = defaultValue;
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
	public boolean isEdited() {
		if (super.isEdited()) {
			return true;
		}
		return this.cells.stream().anyMatch(BaseListCell::isEdited);
	}
	
	public boolean isMatchDefault() {
		Optional<List<T>> defaultValueOptional = this.getDefaultValue();
		if (defaultValueOptional.isPresent()) {
			List<T> value = this.getValue();
			List<T> defaultValue = defaultValueOptional.get();
			if (value.size() != defaultValue.size()) {
				return false;
			}
			for (int i = 0; i < value.size(); ++i) {
				if (Objects.equals(value.get(i), defaultValue.get(i))) continue;
				return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isRequiresRestart() {
		return this.cells.stream().anyMatch(BaseListCell::isRequiresRestart);
	}
	
	@Override
	public void setRequiresRestart(boolean requiresRestart) {
	}
	
	public SELF self() {
		//noinspection unchecked
		return (SELF) this;
	}
	
	public boolean isDeleteButtonEnabled() {
		return this.deleteButtonEnabled;
	}
	
	protected abstract C cellWithValue(T var1);
	
	@NotNull
	public Function<SELF, C> getCreateNewInstance() {
		return this.createNewInstance;
	}
	
	public void setCreateNewInstance(@NotNull Function<SELF, C> createNewInstance) {
		this.createNewInstance = createNewInstance;
	}
	
	@Nullable
	public ITextComponent getAddTooltip() {
		return this.addTooltip;
	}
	
	public void setAddTooltip(@Nullable ITextComponent addTooltip) {
		this.addTooltip = addTooltip;
	}
	
	@Nullable
	public ITextComponent getRemoveTooltip() {
		return this.removeTooltip;
	}
	
	public void setRemoveTooltip(@Nullable ITextComponent removeTooltip) {
		this.removeTooltip = removeTooltip;
	}
	
	@Override
	public Optional<List<T>> getDefaultValue() {
		if (this.defaultValue == null)
			return Optional.empty();
		return Optional.ofNullable(this.defaultValue.get());
	}
	
	@Override
	public int getItemHeight() {
		if (this.expanded) {
			int i = 24;
			for (BaseListCell entry : this.cells) {
				i += entry.getCellHeight();
			}
			return i;
		}
		return 24;
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		if (!this.expanded) {
			ArrayList<IGuiEventListener> elements = new ArrayList<>(this.widgets);
			elements.removeAll(this.cells);
			return elements;
		}
		return this.widgets;
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		List<ITextComponent> errors =
		  this.cells.stream().map(BaseListCell::getConfigError).filter(Optional::isPresent)
			 .map(Optional::get).collect(Collectors.toList());
		if (errors.size() > 1)
			return Optional.of(new TranslationTextComponent("text.cloth-config.multi_error"));
		return errors.stream().findFirst();
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	@Override
	public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
		this.labelWidget.rectangle.x = x - 15;
		this.labelWidget.rectangle.y = y;
		this.labelWidget.rectangle.width = entryWidth + 15;
		this.labelWidget.rectangle.height = 24;
		return new Rectangle(
		  this.getParent().left, y, this.getParent().right - this.getParent().left, 20);
	}
	
	protected boolean isInsideCreateNew(double mouseX, double mouseY) {
		return mouseX >= (double) (this.labelWidget.rectangle.x + 12) &&
		       mouseY >= (double) (this.labelWidget.rectangle.y + 3) &&
		       mouseX <= (double) (this.labelWidget.rectangle.x + 12 + 11) &&
		       mouseY <= (double) (this.labelWidget.rectangle.y + 3 + 11);
	}
	
	protected boolean isInsideDelete(double mouseX, double mouseY) {
		return this.isDeleteButtonEnabled() &&
		       mouseX >= (double) (this.labelWidget.rectangle.x + 25) &&
		       mouseY >= (double) (this.labelWidget.rectangle.y + 3) &&
		       mouseX <= (double) (this.labelWidget.rectangle.x + 25 + 11) &&
		       mouseY <= (double) (this.labelWidget.rectangle.y + 3 + 11);
	}
	
	@Override
	public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (this.addTooltip != null && this.isInsideCreateNew(mouseX, mouseY)) {
			return Optional.of(new ITextComponent[]{this.addTooltip});
		}
		if (this.removeTooltip != null && this.isInsideDelete(mouseX, mouseY)) {
			return Optional.of(new ITextComponent[]{this.removeTooltip});
		}
		if (this.getTooltipSupplier() != null) {
			return this.getTooltipSupplier().get();
		}
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
		BaseListCell focused = !this.expanded || this.getListener() == null ||
		                       !(this.getListener() instanceof BaseListCell) ? null
		                                                                     : (BaseListCell) this.getListener();
		boolean insideCreateNew = this.isInsideCreateNew(mouseX, mouseY);
		boolean insideDelete = this.isInsideDelete(mouseX, mouseY);
		this.blit(
		  matrices, x - 15, y + 5, 33,
		  (this.labelWidget.rectangle.contains(mouseX, mouseY) && !insideCreateNew && !insideDelete
		   ? 18 : 0) + (this.expanded ? 9 : 0), 9, 9);
		this.blit(matrices, x - 15 + 13, y + 5, 42, insideCreateNew ? 9 : 0, 9, 9);
		if (this.isDeleteButtonEnabled()) {
			this.blit(
			  matrices, x - 15 + 26, y + 5, 51, focused == null ? 0 : (insideDelete ? 18 : 9), 9, 9);
		}
		this.resetWidget.x = x + entryWidth - this.resetWidget.getWidth();
		this.resetWidget.y = y;
		this.resetWidget.active =
		  this.isEditable() && this.getDefaultValue().isPresent() && !this.isMatchDefault();
		this.resetWidget.render(matrices, mouseX, mouseY, delta);
		Minecraft.getInstance().fontRenderer.func_238407_a_(
		  matrices, this.getDisplayedFieldName().func_241878_f(),
		  this.isDeleteButtonEnabled() ? (float) (x + 24) : (float) (x + 24 - 9), (float) (y + 6),
		  this.labelWidget.rectangle.contains(mouseX, mouseY) && !this.resetWidget.isMouseOver(
			 mouseX, mouseY) && !insideDelete && !insideCreateNew ? -1638890
		                                                         : this.getPreferredTextColor());
		if (this.expanded) {
			int yy = y + 24;
			for (BaseListCell cell : this.cells) {
				cell.render(
				  matrices, -1, yy, x + 14, entryWidth - 14, cell.getCellHeight(), mouseX, mouseY,
				  this.getParent().getFocused() != null && this.getParent().getFocused()
					 .equals(this) && this.getListener() != null && this.getListener().equals(cell),
				  delta);
				yy += cell.getCellHeight();
			}
		}
	}
	
	@Override
	public void updateSelected(boolean isSelected) {
		for (BaseListCell cell : this.cells) {
			cell.updateSelected(isSelected && this.getListener() == cell && this.expanded);
		}
	}
	
	@Override
	public int getInitialReferenceOffset() {
		return 24;
	}
	
	public boolean insertInFront() {
		return this.insertInFront;
	}
	
	public class ListLabelWidget implements IGuiEventListener {
		protected Rectangle rectangle = new Rectangle();
		
		public boolean mouseClicked(double double_1, double double_2, int int_1) {
			if (BaseListEntry.this.resetWidget.isMouseOver(double_1, double_2)) {
				return false;
			}
			if (BaseListEntry.this.isInsideCreateNew(double_1, double_2)) {
				C cell;
				BaseListEntry.this.expanded = true;
				if (BaseListEntry.this.insertInFront()) {
					cell = BaseListEntry.this.createNewInstance.apply(BaseListEntry.this.self());
					BaseListEntry.this.cells.add(0, cell);
					BaseListEntry.this.widgets.add(0, cell);
				} else {
					cell = BaseListEntry.this.createNewInstance.apply(BaseListEntry.this.self());
					BaseListEntry.this.cells.add(cell);
					BaseListEntry.this.widgets.add(cell);
				}
				cell.onAdd();
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
			if (BaseListEntry.this.isDeleteButtonEnabled() &&
			    BaseListEntry.this.isInsideDelete(double_1, double_2)) {
				IGuiEventListener focused = BaseListEntry.this.getListener();
				if (BaseListEntry.this.expanded && focused instanceof BaseListCell) {
					((BaseListCell) focused).onDelete();
					//noinspection SuspiciousMethodCalls
					BaseListEntry.this.cells.remove(focused);
					BaseListEntry.this.widgets.remove(focused);
					Minecraft.getInstance().getSoundHandler().play(
					  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				}
				return true;
			}
			if (this.rectangle.contains(double_1, double_2)) {
				BaseListEntry.this.expanded = !BaseListEntry.this.expanded;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
			return false;
		}
	}
}

