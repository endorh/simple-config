package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.entries.TooltipListEntry;
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
 * Implementation of the missing float slider entry in Cloth API
 */
@OnlyIn(Dist.CLIENT)
public class FloatSliderEntry extends TooltipListEntry<Float> implements ISettableConfigListEntry<Float> {
	protected FloatSliderEntry.Slider sliderWidget;
	protected Button resetButton;
	protected AtomicDouble value;
	protected final float original;
	private float minimum;
	private float maximum;
	private final Consumer<Float> saveConsumer;
	private final Supplier<Float> defaultValue;
	private Function<Float, ITextComponent> textGetter;
	private final List<IGuiEventListener> widgets;
	
	/** @deprecated */
	@Deprecated @Internal
	public FloatSliderEntry(ITextComponent fieldName, float minimum, float maximum, float value, Consumer<Float> saveConsumer, ITextComponent resetButtonKey, Supplier<Float> defaultValue) {
		this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, null);
	}
	
	/** @deprecated */
	@Deprecated @Internal
	public FloatSliderEntry(ITextComponent fieldName, float minimum, float maximum, float value, Consumer<Float> saveConsumer, ITextComponent resetButtonKey, Supplier<Float> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
		this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, tooltipSupplier, false);
	}
	
	/** @deprecated Use the builder */
	@SuppressWarnings("DeprecatedIsStillUsed") @Deprecated @Internal
	public FloatSliderEntry(ITextComponent fieldName, float minimum, float maximum, float value, Consumer<Float> saveConsumer, ITextComponent resetButtonKey, Supplier<Float> defaultValue, Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart) {
		//noinspection UnstableApiUsage
		super(fieldName, tooltipSupplier, requiresRestart);
		this.textGetter = v -> new StringTextComponent(String.format("Value: %.2f", v));
		this.original = value;
		this.defaultValue = defaultValue;
		this.value = new AtomicDouble(value);
		this.saveConsumer = saveConsumer;
		this.maximum = maximum;
		this.minimum = minimum;
		this.sliderWidget = new FloatSliderEntry.Slider(0, 0, 152, 20, (getValue() - minimum) / Math.abs(maximum - minimum));
		this.resetButton = new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(resetButtonKey) + 6, 20, resetButtonKey, widget -> setValue(defaultValue.get()));
		this.sliderWidget.setMessage(this.textGetter.apply(getValue()));
		this.widgets = Lists.newArrayList(new IGuiEventListener[]{this.sliderWidget, this.resetButton});
	}
	
	@Override public void save() {
		if (saveConsumer != null)
			saveConsumer.accept(this.getValue());
	}
	
	public Function<Float, ITextComponent> getTextGetter() {
		return this.textGetter;
	}
	
	public FloatSliderEntry setTextGetter(Function<Float, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		this.sliderWidget.setMessage(textGetter.apply(getValue()));
		return this;
	}
	
	@Override public Float getValue() {
		return (float) value.get();
	}
	
	@Override public void setValue(Float value) {
		setValue(value.floatValue());
	}
	
	public void setValue(float value) {
		this.sliderWidget.setValue(
		  (MathHelper.clamp(value, this.minimum, this.maximum) - this.minimum) / Math.abs(this.maximum - this.minimum));
		this.value.set(Math.min(Math.max(value, this.minimum), this.maximum));
		this.sliderWidget.func_230979_b_();
	}
	
	@Override public Optional<Float> getDefaultValue() {
		return defaultValue == null ? Optional.empty() : Optional.ofNullable(
		  defaultValue.get());
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.widgets;
	}
	
	@Override public boolean isEdited() {
		return super.isEdited() || this.getValue() != this.original;
	}
	
	public FloatSliderEntry setMaximum(float maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public FloatSliderEntry setMinimum(float minimum) {
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
		protected Slider(int int_1, int int_2, int int_3, int int_4, float float_1) {
			super(int_1, int_2, int_3, int_4, NarratorChatListener.EMPTY, float_1);
		}
		
		@Override public void func_230979_b_() {
			this.setMessage(
			  FloatSliderEntry.this.textGetter.apply(FloatSliderEntry.this.getValue()));
		}
		
		@Override protected void func_230972_a_() {
			FloatSliderEntry.this.value.set(FloatSliderEntry.this.minimum + Math.abs(FloatSliderEntry.this.maximum - FloatSliderEntry.this.minimum) * this.sliderValue);
		}
		
		@Override public boolean keyPressed(int int_1, int int_2, int int_3) {
			return isEditable() && super.keyPressed(int_1, int_2, int_3);
		}
		
		@Override public boolean mouseDragged(
		  double double_1, double double_2, int int_1, double double_3, double double_4
		) {
			return isEditable() && super.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		}
		
		public float getValue() {
			return (float) sliderValue;
		}
		
		public void setValue(float integer) {
			this.sliderValue = integer;
		}
	}
}
