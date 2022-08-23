package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.BooleanListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class BooleanToggleBuilder
  extends FieldBuilder<Boolean, BooleanListEntry, BooleanToggleBuilder> {
	@Nullable private Function<Boolean, Component> yesNoTextSupplier = null;
	
	public BooleanToggleBuilder(
	  ConfigFieldBuilder builder, Component name, boolean value
	) {
		super(BooleanListEntry.class, builder, name, value);
	}
	
	public BooleanToggleBuilder setYesNoTextSupplier(
	  @Nullable Function<Boolean, Component> yesNoTextSupplier
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

