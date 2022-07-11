package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.Modifier;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.gui.entries.KeyCodeEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeBuilder
  extends FieldBuilder<ModifierKeyCode, KeyCodeEntry, KeyCodeBuilder> {
	private boolean allowKey = true;
	private boolean allowMouse = true;
	private boolean allowModifiers = true;
	
	public KeyCodeBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, ModifierKeyCode value
	) {
		super(builder, name, value.copy());
	}
	
	public KeyCodeBuilder setAllowModifiers(boolean allowModifiers) {
		this.allowModifiers = allowModifiers;
		if (!allowModifiers)
			this.value.setModifier(Modifier.none());
		return this;
	}
	
	public KeyCodeBuilder setAllowKey(boolean allowKey) {
		if (!this.allowMouse && !allowKey)
			throw new IllegalArgumentException("Must allow mouse or key (at least one)");
		this.allowKey = allowKey;
		return this;
	}
	
	public KeyCodeBuilder setAllowMouse(boolean allowMouse) {
		if (!this.allowKey && !allowMouse)
			throw new IllegalArgumentException("Must allow mouse or key (at least one)");
		this.allowMouse = allowMouse;
		return this;
	}
	
	@Override protected @NotNull KeyCodeEntry buildEntry() {
		KeyCodeEntry entry = new KeyCodeEntry(fieldNameKey, value);
		entry.setAllowKey(this.allowKey);
		entry.setAllowMouse(this.allowMouse);
		entry.setAllowModifiers(this.allowModifiers);
		return entry;
	}
}

