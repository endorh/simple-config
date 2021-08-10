package endorh.simple_config.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonListEntry extends TooltipListEntry<Runnable> implements ISettableConfigListEntry<Runnable> {
	protected final Runnable original;
	protected final Supplier<ITextComponent> buttonLabelSupplier;
	protected final Consumer<Runnable> saveConsumer;
	protected final AtomicReference<Runnable> value = new AtomicReference<>();
	protected final Button button;
	protected List<Button> listeners;
	protected static final int BUTTON_WIDTH = 150;
	protected static final int BUTTON_HEIGHT = 20;
	
	public ButtonListEntry(
	  Runnable value, ITextComponent fieldName, Supplier<ITextComponent> buttonLabelSupplier,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) { this(value, fieldName, buttonLabelSupplier, tooltipSupplier, r -> {}); }
	
	@SuppressWarnings({"deprecation", "UnstableApiUsage"}) public ButtonListEntry(
	  Runnable value, ITextComponent fieldName, Supplier<ITextComponent> buttonLabelSupplier,
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<Runnable> saveConsumer
	) {
		super(fieldName, tooltipSupplier);
		this.original = value;
		this.buttonLabelSupplier = buttonLabelSupplier;
		this.saveConsumer = saveConsumer;
		this.value.set(value);
		this.button = new Button(
		  0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, buttonLabelSupplier.get(), p -> getValue().run());
		this.listeners = ImmutableList.of(button);
	}
	
	@Override public Runnable getValue() {
		return value.get();
	}
	
	@Override public void setValue(Runnable value) {
		this.value.set(value);
	}
	
	@Override public Optional<Runnable> getDefaultValue() {
		return Optional.ofNullable(original);
	}
	
	@Override public void save() {
		saveConsumer.accept(getValue());
	}
	
	@Override public void render(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		MainWindow window = Minecraft.getInstance().getMainWindow();
		final FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
		button.setMessage(buttonLabelSupplier.get());
		button.active = this.isEditable();
		this.button.y = y;
		if (fontRenderer.getBidiFlag()) {
			fontRenderer.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(),
			  (float)(window.getScaledWidth() - x - fontRenderer.getStringPropertyWidth(getDisplayedFieldName())),
			  (float)(y + 6), this.getPreferredTextColor());
			this.button.x = x;
		} else {
			fontRenderer.func_238407_a_(
			  mStack, getDisplayedFieldName().func_241878_f(), (float) x,
			  (float) (y + 6), this.getPreferredTextColor());
			this.button.x = x + entryWidth - BUTTON_WIDTH;
		}
		button.render(mStack, mouseX, mouseY, 0F);
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
}
