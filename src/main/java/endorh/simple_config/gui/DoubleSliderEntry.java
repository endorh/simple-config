package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of the missing double slider entry in Cloth API
 */
@OnlyIn(Dist.CLIENT)
public class DoubleSliderEntry extends TooltipListEntry<Double> {
	protected DoubleSliderEntry.Slider sliderWidget;
	protected Button resetButton;
	protected AtomicDouble value;
	protected final double original;
	private double minimum;
	private double maximum;
	private final Consumer<Double> saveConsumer;
	private final Supplier<Double> defaultValue;
	private Function<Double, ITextComponent> textGetter;
	private final List<IGuiEventListener> widgets;
	
	/** @deprecated */
	@Deprecated
	@Internal
	public DoubleSliderEntry(ITextComponent fieldName, double minimum, double maximum, double value, Consumer<Double> saveConsumer, ITextComponent resetButtonKey, Supplier<Double> defaultValue) {
		this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, null);
	}
	
	/** @deprecated */
	@Deprecated
	@Internal
	public DoubleSliderEntry(ITextComponent fieldName, double minimum, double maximum, double value, Consumer<Double> saveConsumer, ITextComponent resetButtonKey, Supplier<Double> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, tooltipSupplier, false);
	}
	
	/** @deprecated Use the builder */
	@Deprecated
	@Internal
	public DoubleSliderEntry(ITextComponent fieldName, double minimum, double maximum, double value, Consumer<Double> saveConsumer, ITextComponent resetButtonKey, Supplier<Double> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
		//noinspection UnstableApiUsage
		super(fieldName, tooltipSupplier, requiresRestart);
		this.textGetter = v -> new StringTextComponent(String.format("Value: %.2f", v));
		this.original = value;
		this.defaultValue = defaultValue;
		this.value = new AtomicDouble(value);
		this.saveConsumer = saveConsumer;
		this.maximum = maximum;
		this.minimum = minimum;
		this.sliderWidget = new DoubleSliderEntry.Slider(0, 0, 152, 20, (this.value.get() - minimum) / Math.abs(maximum - minimum));
		this.resetButton = new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(resetButtonKey) + 6, 20, resetButtonKey, widget -> setValue(defaultValue.get()));
		this.sliderWidget.setMessage(this.textGetter.apply(this.value.get()));
		this.widgets = Lists.newArrayList(new IGuiEventListener[]{this.sliderWidget, this.resetButton});
	}
	
	@Override public void save() {
		if (saveConsumer != null)
			saveConsumer.accept(this.getValue());
	}
	
	public Function<Double, ITextComponent> getTextGetter() {
		return this.textGetter;
	}
	
	public DoubleSliderEntry setTextGetter(Function<Double, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		this.sliderWidget.setMessage(textGetter.apply(this.value.get()));
		return this;
	}
	
	@Override public Double getValue() {
		return this.value.get();
	}
	
	/** @deprecated */
	@Deprecated
	public void setValue(double value) {
		this.sliderWidget.setValue(
		  (MathHelper.clamp(value, this.minimum, this.maximum) - this.minimum) / Math.abs(this.maximum - this.minimum));
		this.value.set(Math.min(Math.max(value, this.minimum), this.maximum));
		this.sliderWidget.func_230979_b_();
	}
	
	@Override public Optional<Double> getDefaultValue() {
		return defaultValue == null ? Optional.empty() : Optional.ofNullable(
		  defaultValue.get());
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.widgets;
	}
	
	@Override public boolean isEdited() {
		return super.isEdited() || this.getValue() != this.original;
	}
	
	public DoubleSliderEntry setMaximum(double maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public DoubleSliderEntry setMinimum(double minimum) {
		this.minimum = minimum;
		return this;
	}
	
	@Override public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth,
	  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent() && this.defaultValue.get() != this.value.get();
		this.resetButton.y = y;
		this.sliderWidget.active = this.isEditable();
		this.sliderWidget.y = y;
		ITextComponent displayedFieldName = this.getDisplayedFieldName();
		if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
			Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)(window.getScaledWidth() - x - Minecraft.getInstance().fontRenderer.getStringPropertyWidth(displayedFieldName)), (float)(y + 6), getPreferredTextColor());
			this.resetButton.x = x;
			this.sliderWidget.x = x + this.resetButton.getWidth() + 1;
		} else {
			Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, displayedFieldName.func_241878_f(), (float)x, (float)(y + 6), this.getPreferredTextColor());
			this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
			this.sliderWidget.x = x + entryWidth - 150;
		}
		
		this.sliderWidget.setWidth(150 - this.resetButton.getWidth() - 2);
		this.resetButton.render(matrices, mouseX, mouseY, delta);
		this.sliderWidget.render(matrices, mouseX, mouseY, delta);
	}
	
	private class Slider extends AbstractSlider {
		protected Slider(int int_1, int int_2, int int_3, int int_4, double double_1) {
			super(int_1, int_2, int_3, int_4, NarratorChatListener.EMPTY, double_1);
		}
		
		@Override public void func_230979_b_() {
			this.setMessage(
			  DoubleSliderEntry.this.textGetter.apply(DoubleSliderEntry.this.value.get()));
		}
		
		@Override protected void func_230972_a_() {
			DoubleSliderEntry.this.value.set(DoubleSliderEntry.this.minimum + Math.abs(DoubleSliderEntry.this.maximum - DoubleSliderEntry.this.minimum) * this.sliderValue);
		}
		
		@Override public boolean keyPressed(int int_1, int int_2, int int_3) {
			return isEditable() && super.keyPressed(int_1, int_2, int_3);
		}
		
		@Override public boolean mouseDragged(double double_1, double double_2, int int_1, double double_3,
		                             double double_4) {
			return isEditable() && super.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		}
		
		public double getValue() {
			return this.sliderValue;
		}
		
		public void setValue(double integer) {
			this.sliderValue = integer;
		}
	}
}
