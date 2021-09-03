package endorh.simple_config.clothconfig2.impl.builders;

import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class BooleanToggleBuilder
  extends FieldBuilder<Boolean, BooleanListEntry, BooleanToggleBuilder> {
	@Nullable private Function<Boolean, ITextComponent> yesNoTextSupplier = null;
	
	public BooleanToggleBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, boolean value
	) {
		super(builder, name, value);
	}
	
	public BooleanToggleBuilder setYesNoTextSupplier(
	  @Nullable Function<Boolean, ITextComponent> yesNoTextSupplier
	) {
		this.yesNoTextSupplier = yesNoTextSupplier;
		return this;
	}
	
	@Override
	@NotNull
	public BooleanListEntry buildEntry() {
		final BooleanListEntry entry = new BooleanListEntry(fieldNameKey, value);
		if (yesNoTextSupplier != null)
			entry.setYesNoSupplier(yesNoTextSupplier);
		return entry;
	}
}

