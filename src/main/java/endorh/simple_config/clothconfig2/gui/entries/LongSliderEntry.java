package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class LongSliderEntry extends TooltipListEntry<Long> {
	protected Slider sliderWidget;
	protected Button resetButton;
	protected AtomicLong value;
	protected final long orginial;
	private long minimum;
	private long maximum;
	private final Consumer<Long> saveConsumer;
	private final Supplier<Long> defaultValue;
	private Function<Long, ITextComponent> textGetter =
	  value -> new StringTextComponent(String.format("Value: %d", value));
	private final List<IGuiEventListener> widgets;
	
	@Deprecated
	@ApiStatus.Internal
	public LongSliderEntry(
	  ITextComponent fieldName, long minimum, long maximum, long value, Consumer<Long> saveConsumer,
	  ITextComponent resetButtonKey, Supplier<Long> defaultValue
	) {
		this(fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue, null);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public LongSliderEntry(
	  ITextComponent fieldName, long minimum, long maximum, long value, Consumer<Long> saveConsumer,
	  ITextComponent resetButtonKey, Supplier<Long> defaultValue,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this(
		  fieldName, minimum, maximum, value, saveConsumer, resetButtonKey, defaultValue,
		  tooltipSupplier, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public LongSliderEntry(
	  ITextComponent fieldName, long minimum, long maximum, long value2, Consumer<Long> saveConsumer,
	  ITextComponent resetButtonKey, Supplier<Long> defaultValue,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart
	) {
		super(fieldName, tooltipSupplier, requiresRestart);
		this.orginial = value2;
		this.defaultValue = defaultValue;
		this.value = new AtomicLong(value2);
		this.saveConsumer = saveConsumer;
		this.maximum = maximum;
		this.minimum = minimum;
		this.sliderWidget = new Slider(0, 0, 152, 20, ((double) this.value.get() - (double) minimum) /
		                                              (double) Math.abs(maximum - minimum));
		this.resetButton = new Button(0, 0,
		                              Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
		                                resetButtonKey) + 6, 20, resetButtonKey,
		                              widget -> this.setValue(defaultValue.get()));
		this.sliderWidget.setMessage(this.textGetter.apply(this.value.get()));
		this.widgets = Lists.newArrayList(this.sliderWidget, this.resetButton);
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	public Function<Long, ITextComponent> getTextGetter() {
		return this.textGetter;
	}
	
	public LongSliderEntry setTextGetter(Function<Long, ITextComponent> textGetter) {
		this.textGetter = textGetter;
		this.sliderWidget.setMessage(textGetter.apply(this.value.get()));
		return this;
	}
	
	@Override public Long getValue() {
		return this.value.get();
	}
	
	@Override public void setValue(Long value) {
		this.value.set(value);
	}
	
	@Deprecated
	public void setValue(long value) {
		this.sliderWidget.setValue(
		  (double) (MathHelper.clamp(value, this.minimum, this.maximum) - this.minimum) /
		  (double) Math.abs(this.maximum - this.minimum));
		this.value.set(Math.min(Math.max(value, this.minimum), this.maximum));
		this.sliderWidget.func_230979_b_();
	}
	
	@Override
	public Optional<Long> getDefaultValue() {
		return this.defaultValue == null ? Optional.empty()
		                                 : Optional.ofNullable(this.defaultValue.get());
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.widgets;
	}
	
	@Override
	public boolean isEdited() {
		return super.isEdited() || this.getValue() != this.orginial;
	}
	
	public LongSliderEntry setMaximum(long maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public LongSliderEntry setMinimum(long minimum) {
		this.minimum = minimum;
		return this;
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
		                          this.defaultValue.get() != this.value.get();
		this.resetButton.y = y;
		this.sliderWidget.active = this.isEditable();
		this.sliderWidget.y = y;
		ITextComponent displayedFieldName = this.getDisplayedFieldName();
		if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  matrices, displayedFieldName.func_241878_f(), (float) (window.getScaledWidth() - x -
			                                                         Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
				                                                        displayedFieldName)),
			  (float) (y + 6), this.getPreferredTextColor());
			this.resetButton.x = x;
			this.sliderWidget.x = x + this.resetButton.getWidth() + 1;
		} else {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  matrices, displayedFieldName.func_241878_f(), (float) x, (float) (y + 6),
			  this.getPreferredTextColor());
			this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
			this.sliderWidget.x = x + entryWidth - 150;
		}
		this.sliderWidget.setWidth(150 - this.resetButton.getWidth() - 2);
		this.resetButton.render(matrices, mouseX, mouseY, delta);
		this.sliderWidget.render(matrices, mouseX, mouseY, delta);
	}
	
	private class Slider
	  extends AbstractSlider {
		protected Slider(int int_1, int int_2, int int_3, int int_4, double double_1) {
			super(int_1, int_2, int_3, int_4, NarratorChatListener.EMPTY, double_1);
		}
		
		public void func_230979_b_() {
			this.setMessage(LongSliderEntry.this.textGetter.apply(LongSliderEntry.this.value.get()));
		}
		
		protected void func_230972_a_() {
			LongSliderEntry.this.value.set((long) ((double) LongSliderEntry.this.minimum +
			                                       (double) Math.abs(LongSliderEntry.this.maximum -
			                                                         LongSliderEntry.this.minimum) *
			                                       this.sliderValue));
		}
		
		public boolean keyPressed(int int_1, int int_2, int int_3) {
			if (!LongSliderEntry.this.isEditable()) {
				return false;
			}
			return super.keyPressed(int_1, int_2, int_3);
		}
		
		public boolean mouseDragged(
		  double double_1, double double_2, int int_1, double double_3, double double_4
		) {
			if (!LongSliderEntry.this.isEditable()) {
				return false;
			}
			return super.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		}
		
		public double getValue() {
			return this.sliderValue;
		}
		
		public void setValue(double integer) {
			this.sliderValue = integer;
		}
	}
}

