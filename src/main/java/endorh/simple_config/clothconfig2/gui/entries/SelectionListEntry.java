package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class SelectionListEntry<T> extends TooltipListEntry<T> {
	private final ImmutableList<T> values;
	private final AtomicInteger index;
	private final int original;
	private final Button buttonWidget;
	private final Button resetButton;
	private final Consumer<T> saveConsumer;
	private final Supplier<T> defaultValue;
	private final List<IGuiEventListener> widgets;
	private final Function<T, ITextComponent> nameProvider;
	
	@Deprecated
	@ApiStatus.Internal
	public SelectionListEntry(
	  ITextComponent fieldName, T[] valuesArray, T value, ITextComponent resetButtonKey,
	  Supplier<T> defaultValue, Consumer<T> saveConsumer
	) {
		this(fieldName, valuesArray, value, resetButtonKey, defaultValue, saveConsumer, null);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public SelectionListEntry(
	  ITextComponent fieldName, T[] valuesArray, T value, ITextComponent resetButtonKey,
	  Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<T, ITextComponent> nameProvider
	) {
		this(
		  fieldName, valuesArray, value, resetButtonKey, defaultValue, saveConsumer, nameProvider,
		  null);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public SelectionListEntry(
	  ITextComponent fieldName, T[] valuesArray, T value, ITextComponent resetButtonKey,
	  Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<T, ITextComponent> nameProvider,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this(
		  fieldName, valuesArray, value, resetButtonKey, defaultValue, saveConsumer, nameProvider,
		  tooltipSupplier, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public SelectionListEntry(
	  ITextComponent fieldName, T[] valuesArray, T value, ITextComponent resetButtonKey,
	  Supplier<T> defaultValue, Consumer<T> saveConsumer, Function<T, ITextComponent> nameProvider,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart
	) {
		super(fieldName, tooltipSupplier, requiresRestart);
		this.values =
		  valuesArray != null ? ImmutableList.copyOf(valuesArray) : ImmutableList.of(value);
		this.defaultValue = defaultValue;
		this.index = new AtomicInteger(this.values.indexOf(value));
		this.index.compareAndSet(-1, 0);
		this.original = this.values.indexOf(value);
		this.buttonWidget = new Button(0, 0, 150, 20, NarratorChatListener.EMPTY, widget -> {
			this.index.incrementAndGet();
			this.index.compareAndSet(this.values.size(), 0);
		});
		this.resetButton = new Button(
		  0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
		  resetButtonKey) + 6, 20, resetButtonKey, widget -> this.index.set(this.getDefaultIndex()));
		this.saveConsumer = saveConsumer;
		this.widgets = Lists.newArrayList(this.buttonWidget, this.resetButton);
		this.nameProvider = nameProvider == null ? t -> new TranslationTextComponent(
		  t instanceof Translatable ? ((Translatable) t).getKey() : t.toString()) : nameProvider;
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	@Override
	public boolean isEdited() {
		return super.isEdited() || !Objects.equals(this.index.get(), this.original);
	}
	
	@Override public T getValue() {
		return this.values.get(this.index.get());
	}
	
	@Override public void setValue(T value) {
		final int index = this.values.indexOf(value);
		if (index < 0)
			throw new IllegalArgumentException("Invalid value for selection: \"" + value + "\"");
		this.index.set(index);
	}
	
	@Override
	public Optional<T> getDefaultValue() {
		return this.defaultValue == null ? Optional.empty()
		                                 : Optional.ofNullable(this.defaultValue.get());
	}
	
	@Override
	public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.render(
		  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() &&
		                          this.getDefaultIndex() != this.index.get();
		this.resetButton.y = y;
		this.buttonWidget.active = this.isEditable();
		this.buttonWidget.y = y;
		this.buttonWidget.setMessage(this.nameProvider.apply(this.getValue()));
		ITextComponent displayedFieldName = this.getDisplayedFieldName();
		if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  matrices, displayedFieldName.func_241878_f(), (float) (window.getScaledWidth() - x -
			                                                         Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
				                                                        displayedFieldName)),
			  (float) (y + 6), this.getPreferredTextColor());
			this.resetButton.x = x;
			this.buttonWidget.x = x + this.resetButton.getWidth() + 2;
		} else {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  matrices, displayedFieldName.func_241878_f(), (float) x, (float) (y + 6),
			  this.getPreferredTextColor());
			this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
			this.buttonWidget.x = x + entryWidth - 150;
		}
		this.buttonWidget.setWidth(150 - this.resetButton.getWidth() - 2);
		this.resetButton.render(matrices, mouseX, mouseY, delta);
		this.buttonWidget.render(matrices, mouseX, mouseY, delta);
	}
	
	private int getDefaultIndex() {
		return Math.max(0, this.values.indexOf(this.defaultValue.get()));
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.widgets;
	}
	
	public interface Translatable {
		@NotNull String getKey();
	}
}

