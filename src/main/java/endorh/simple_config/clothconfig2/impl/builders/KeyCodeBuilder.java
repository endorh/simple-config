package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.clothconfig2.gui.entries.KeyCodeEntry;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeBuilder
  extends FieldBuilder<ModifierKeyCode, KeyCodeEntry> {
	@Nullable
	private Consumer<ModifierKeyCode> saveConsumer = null;
	@NotNull
	private Function<ModifierKeyCode, Optional<ITextComponent[]>> tooltipSupplier =
	  bool -> Optional.empty();
	private final ModifierKeyCode value;
	private boolean allowKey = true;
	private boolean allowMouse = true;
	private boolean allowModifiers = true;
	
	public KeyCodeBuilder(
	  ITextComponent resetButtonKey, ITextComponent fieldNameKey, ModifierKeyCode value
	) {
		super(resetButtonKey, fieldNameKey);
		this.value = ModifierKeyCode.copyOf(value);
	}
	
	public KeyCodeBuilder setAllowModifiers(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
		if (!allowModifiers) {
			this.value.setModifier(Modifier.none());
		}
		return this;
	}
	
	public KeyCodeBuilder setAllowKey(boolean allowKey) {
		if (!this.allowMouse && !allowKey) {
			throw new IllegalArgumentException();
		}
		this.allowKey = allowKey;
		return this;
	}
	
	public KeyCodeBuilder setAllowMouse(boolean allowMouse) {
		if (!this.allowKey && !allowMouse) {
			throw new IllegalArgumentException();
		}
		this.allowMouse = allowMouse;
		return this;
	}
	
	public KeyCodeBuilder setErrorSupplier(
	  @Nullable Function<InputMappings.Input, Optional<ITextComponent>> errorSupplier
	) {
		return this.setModifierErrorSupplier(
		  keyCode -> errorSupplier.apply(keyCode.getKeyCode()));
	}
	
	public KeyCodeBuilder setModifierErrorSupplier(
	  @Nullable Function<ModifierKeyCode, Optional<ITextComponent>> errorSupplier
	) {
		this.errorSupplier = errorSupplier;
		return this;
	}
	
	public KeyCodeBuilder requireRestart() {
		this.requireRestart(true);
		return this;
	}
	
	public KeyCodeBuilder setSaveConsumer(Consumer<InputMappings.Input> saveConsumer) {
		return this.setModifierSaveConsumer(keyCode -> saveConsumer.accept(keyCode.getKeyCode()));
	}
	
	public KeyCodeBuilder setDefaultValue(Supplier<InputMappings.Input> defaultValue) {
		return this.setModifierDefaultValue(
		  () -> ModifierKeyCode.of(defaultValue.get(), Modifier.none()));
	}
	
	public KeyCodeBuilder setModifierSaveConsumer(Consumer<ModifierKeyCode> saveConsumer) {
		this.saveConsumer = saveConsumer;
		return this;
	}
	
	public KeyCodeBuilder setModifierDefaultValue(Supplier<ModifierKeyCode> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public KeyCodeBuilder setDefaultValue(InputMappings.Input defaultValue) {
		return this.setDefaultValue(ModifierKeyCode.of(defaultValue, Modifier.none()));
	}
	
	public KeyCodeBuilder setDefaultValue(ModifierKeyCode defaultValue) {
		this.defaultValue = () -> defaultValue;
		return this;
	}
	
	public KeyCodeBuilder setTooltipSupplier(
	  @NotNull Function<InputMappings.Input, Optional<ITextComponent[]>> tooltipSupplier
	) {
		return this.setModifierTooltipSupplier(
		  keyCode -> tooltipSupplier.apply(keyCode.getKeyCode()));
	}
	
	public KeyCodeBuilder setModifierTooltipSupplier(
	  @NotNull Function<ModifierKeyCode, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public KeyCodeBuilder setTooltipSupplier(
	  @NotNull Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = bool -> tooltipSupplier.get();
		return this;
	}
	
	public KeyCodeBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = bool -> tooltip;
		return this;
	}
	
	public KeyCodeBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = bool -> Optional.ofNullable(tooltip);
		return this;
	}
	
	@Override
	@NotNull
	public KeyCodeEntry build() {
		KeyCodeEntry entry =
		  new KeyCodeEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(),
		                   this.defaultValue, this.saveConsumer, null, this.isRequireRestart());
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		if (this.errorSupplier != null) {
			entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
		}
		entry.setAllowKey(this.allowKey);
		entry.setAllowMouse(this.allowMouse);
		entry.setAllowModifiers(this.allowModifiers);
		return entry;
	}
}

