package endorh.simpleconfig.ui.impl.builders;

import com.google.common.collect.ImmutableList;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.entries.SelectionListEntry;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class SelectorBuilder<V>
  extends FieldBuilder<V, SelectionListEntry<V>, SelectorBuilder<V>> {
	protected final V value;
	protected final V[] valuesArray;
	protected Function<V, Component> nameProvider = null;
	
	public SelectorBuilder(
	  ConfigFieldBuilder builder, Component name, V[] valuesArray, V value
	) {
		super(SelectionListEntry.class, builder, name, value);
		Objects.requireNonNull(value);
		this.valuesArray = valuesArray;
		this.value = value;
	}
	
	public SelectorBuilder<V> setNameProvider(
	  Function<V, Component> enumNameProvider
	) {
		nameProvider = enumNameProvider;
		return this;
	}
	
	@Override
	@NotNull
	public SelectionListEntry<V> buildEntry() {
		return new SelectionListEntry<>(
		  fieldNameKey,
		  valuesArray != null? ImmutableList.copyOf(valuesArray) : ImmutableList.of(value),
		  value, nameProvider);
	}
}

