package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.clothconfig2.gui.entries.KeyCodeEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(value = Dist.CLIENT)
public class KeyCodeBuilder extends FieldBuilder<ModifierKeyCode, KeyCodeEntry, KeyCodeBuilder> {
	private boolean allowKey = true;
	private boolean allowMouse = true;
	private boolean allowModifiers = true;
	
	public KeyCodeBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, ModifierKeyCode value
	) {
		super(builder, name, value);
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
	
	@Override
	@NotNull
	public KeyCodeEntry buildEntry() {
		KeyCodeEntry entry =
		  new KeyCodeEntry(
		    this.fieldNameKey, this.value
		  );
		entry.setAllowKey(this.allowKey);
		entry.setAllowMouse(this.allowMouse);
		entry.setAllowModifiers(this.allowModifiers);
		return entry;
	}
}

