package endorh.simpleconfig.clothconfig2.impl.builders;

import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class EnumSelectorBuilder<V extends Enum<?>> extends FieldBuilder<V, EnumListEntry<V>, EnumSelectorBuilder<V>> {
	protected final Class<V> clazz;
	protected Function<Enum<?>, ITextComponent> enumNameProvider = EnumListEntry.DEFAULT_NAME_PROVIDER;
	
	public EnumSelectorBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, V value
	) {
		super(builder, name, value);
		Objects.requireNonNull(value);
		this.value = value;
		//noinspection unchecked
		clazz = (Class<V>) value.getDeclaringClass();
	}
	
	public EnumSelectorBuilder<V> setEnumNameProvider(
	  Function<Enum<?>, ITextComponent> enumNameProvider
	) {
		Objects.requireNonNull(enumNameProvider);
		this.enumNameProvider = enumNameProvider;
		return this;
	}
	
	@Override @NotNull public EnumListEntry<V> buildEntry() {
		return new EnumListEntry<>(fieldNameKey, clazz, value, enumNameProvider);
	}
}

