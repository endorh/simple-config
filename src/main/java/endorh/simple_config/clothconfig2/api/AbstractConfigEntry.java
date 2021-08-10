package endorh.simple_config.clothconfig2.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen;
import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractConfigEntry<T>
  extends DynamicElementListWidget.ElementEntry<AbstractConfigEntry<T>>
  implements ReferenceProvider<T> {
	private AbstractConfigScreen screen;
	private Supplier<Optional<ITextComponent>> errorSupplier;
	@Nullable
	private List<ReferenceProvider<?>> referencableEntries = null;
	
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public final void setReferencableEntries(
	  @Nullable List<AbstractConfigEntry<?>> referencableEntries
	) {
		this.setReferenceProviderEntries(
		  referencableEntries.stream().map(AbstractConfigEntry::provideReferenceEntry)
			 .collect(Collectors.toList()));
	}
	
	public final void setReferenceProviderEntries(
	  @Nullable List<ReferenceProvider<?>> referencableEntries
	) {
		this.referencableEntries = referencableEntries;
	}
	
	public void requestReferenceRebuilding() {
		AbstractConfigScreen configScreen = this.getConfigScreen();
		if (configScreen instanceof ReferenceBuildingConfigScreen) {
			((ReferenceBuildingConfigScreen) configScreen).requestReferenceRebuilding();
		}
	}
	
	@Override
	@NotNull
	public AbstractConfigEntry<T> provideReferenceEntry() {
		return this;
	}
	
	@Deprecated
	@Nullable
	@ApiStatus.Internal
	@ApiStatus.ScheduledForRemoval
	public final List<AbstractConfigEntry<?>> getReferencableEntries() {
		return this.referencableEntries.stream().map(ReferenceProvider::provideReferenceEntry)
		  .collect(Collectors.toList());
	}
	
	@Nullable
	@ApiStatus.Internal
	public final List<ReferenceProvider<?>> getReferenceProviderEntries() {
		return this.referencableEntries;
	}
	
	public abstract boolean isRequiresRestart();
	
	public abstract void setRequiresRestart(boolean var1);
	
	public abstract ITextComponent getFieldName();
	
	public ITextComponent getDisplayedFieldName() {
		IFormattableTextComponent text = this.getFieldName().deepCopy();
		boolean hasError = this.getConfigError().isPresent();
		boolean isEdited = this.isEdited();
		if (hasError) {
			text = text.mergeStyle(TextFormatting.RED);
		}
		if (isEdited) {
			text = text.mergeStyle(TextFormatting.ITALIC);
		}
		if (!hasError && !isEdited) {
			text = text.mergeStyle(TextFormatting.GRAY);
		}
		return text;
	}
	
	public abstract T getValue();
	public abstract void setValue(T value);
	
	public final Optional<ITextComponent> getConfigError() {
		if (this.errorSupplier != null && this.errorSupplier.get().isPresent()) {
			return this.errorSupplier.get();
		}
		return this.getError();
	}
	
	public void lateRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
	}
	
	public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	public Optional<ITextComponent> getError() {
		return Optional.empty();
	}
	
	public abstract Optional<T> getDefaultValue();
	
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	@Nullable
	public final ClothConfigScreen getScreen() {
		if (this.screen instanceof ClothConfigScreen) {
			return (ClothConfigScreen) this.screen;
		}
		return null;
	}
	
	@Nullable
	public final AbstractConfigScreen getConfigScreen() {
		return this.screen;
	}
	
	public final void addTooltip(@NotNull Tooltip tooltip) {
		this.screen.addTooltip(tooltip);
	}
	
	public void updateSelected(boolean isSelected) {
	}
	
	@ApiStatus.Internal
	public final void setScreen(AbstractConfigScreen screen) {
		this.screen = screen;
	}
	
	public abstract void save();
	
	public boolean isEdited() {
		return this.getConfigError().isPresent();
	}
	
	@Override
	public int getItemHeight() {
		return 24;
	}
	
	public int getInitialReferenceOffset() {
		return 0;
	}
}

