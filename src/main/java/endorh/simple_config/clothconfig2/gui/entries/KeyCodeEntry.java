package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeEntry
  extends TooltipListEntry<ModifierKeyCode> {
	private ModifierKeyCode value;
	private final ModifierKeyCode original;
	private final Button buttonWidget;
	private final Button resetButton;
	private final Consumer<ModifierKeyCode> saveConsumer;
	private final Supplier<ModifierKeyCode> defaultValue;
	private final List<IGuiEventListener> widgets;
	private boolean allowMouse = true;
	private boolean allowKey = true;
	private boolean allowModifiers = true;
	
	@Deprecated
	@ApiStatus.Internal
	public KeyCodeEntry(
	  ITextComponent fieldName, ModifierKeyCode value, ITextComponent resetButtonKey,
	  Supplier<ModifierKeyCode> defaultValue, Consumer<ModifierKeyCode> saveConsumer,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart
	) {
		super(fieldName, tooltipSupplier, requiresRestart);
		this.defaultValue = defaultValue;
		this.value = value.copy();
		this.original = value.copy();
		this.buttonWidget = new Button(0, 0, 150, 20, NarratorChatListener.EMPTY,
		                               widget -> this.getConfigScreen().setFocusedBinding(this));
		this.resetButton =
		  new Button(0, 0, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
			 resetButtonKey) + 6, 20, resetButtonKey, widget -> {
			  this.value = this.getDefaultValue().orElse(null).copy();
			  this.getConfigScreen().setFocusedBinding(null);
		  });
		this.saveConsumer = saveConsumer;
		this.widgets = Lists.newArrayList(this.buttonWidget, this.resetButton);
	}
	
	@Override
	public boolean isEdited() {
		return super.isEdited() || !this.original.equals(this.getValue());
	}
	
	public boolean isAllowModifiers() {
		return this.allowModifiers;
	}
	
	public void setAllowModifiers(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
	}
	
	public boolean isAllowKey() {
		return this.allowKey;
	}
	
	public void setAllowKey(boolean allowKey) {
		this.allowKey = allowKey;
	}
	
	public boolean isAllowMouse() {
		return this.allowMouse;
	}
	
	public void setAllowMouse(boolean allowMouse) {
		this.allowMouse = allowMouse;
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	@Override
	public ModifierKeyCode getValue() {
		return this.value;
	}
	
	public void setValue(ModifierKeyCode value) {
		this.value = value;
	}
	
	@Override
	public Optional<ModifierKeyCode> getDefaultValue() {
		return Optional.ofNullable(this.defaultValue).map(Supplier::get).map(ModifierKeyCode::copy);
	}
	
	private ITextComponent getLocalizedName() {
		return this.value.getLocalizedName();
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
		                          !this.getDefaultValue().get().equals(this.getValue());
		this.resetButton.y = y;
		this.buttonWidget.active = this.isEditable();
		this.buttonWidget.y = y;
		this.buttonWidget.setMessage(this.getLocalizedName());
		if (this.getConfigScreen().getFocusedBinding() == this) {
			this.buttonWidget.setMessage(
			  new StringTextComponent("> ").mergeStyle(TextFormatting.WHITE).append(
				 this.buttonWidget.getMessage().copyRaw().mergeStyle(TextFormatting.YELLOW)).append(
				 new StringTextComponent(" <").mergeStyle(TextFormatting.WHITE)));
		}
		ITextComponent displayedFieldName = this.getDisplayedFieldName();
		if (Minecraft.getInstance().fontRenderer.getBidiFlag()) {
			Minecraft.getInstance().fontRenderer.func_238407_a_(
			  matrices, displayedFieldName.func_241878_f(), (float) (window.getScaledWidth() - x -
			                                                         Minecraft.getInstance().fontRenderer.getStringPropertyWidth(
				                                                        displayedFieldName)),
			  (float) (y + 6), 0xFFFFFF);
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
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return this.widgets;
	}
}

