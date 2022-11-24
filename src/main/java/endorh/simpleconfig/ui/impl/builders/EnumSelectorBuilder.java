package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.EnumListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class EnumSelectorBuilder<V extends Enum<?>> extends FieldBuilder<V, EnumListEntry<V>, EnumSelectorBuilder<V>> {
	protected final Class<V> clazz;
	protected Function<V, ITextComponent> enumNameProvider = EnumListEntry.DEFAULT_NAME_PROVIDER::apply;
	protected @Nullable Function<V, List<ITextComponent>> enumTooltipProvider = null;
	
	public EnumSelectorBuilder(
	  ConfigFieldBuilder builder, ITextComponent name, V value
	) {
		super(EnumListEntry.class, builder, name, value);
		Objects.requireNonNull(value);
		this.value = value;
		//noinspection unchecked
		clazz = (Class<V>) value.getDeclaringClass();
	}
	
	public EnumSelectorBuilder<V> setEnumNameProvider(
	  Function<V, ITextComponent> enumNameProvider
	) {
		Objects.requireNonNull(enumNameProvider);
		this.enumNameProvider = enumNameProvider;
		return this;
	}
	
	public EnumSelectorBuilder<V> setEnumTooltipProvider(
	  Function<V, List<ITextComponent>> enumTooltipProvider
	) {
		Objects.requireNonNull(enumTooltipProvider);
		this.enumTooltipProvider = enumTooltipProvider;
		return this;
	}
	
	@Override @NotNull public EnumListEntry<V> buildEntry() {
		return new EnumListEntry<>(fieldNameKey, clazz, value, enumNameProvider, enumTooltipProvider);
	}
}

