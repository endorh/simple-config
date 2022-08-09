package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.entries.SelectionListEntry;
import net.minecraft.util.text.ITextComponent;
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
	protected Function<V, ITextComponent> nameProvider = null;
	
	public SelectorBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, V[] valuesArray, V value
	) {
		super(SelectionListEntry.class, builder, name, value);
		Objects.requireNonNull(value);
		this.valuesArray = valuesArray;
		this.value = value;
	}
	
	public SelectorBuilder<V> setNameProvider(
	  Function<V, ITextComponent> enumNameProvider
	) {
		this.nameProvider = enumNameProvider;
		return this;
	}
	
	@Override
	@NotNull
	public SelectionListEntry<V> buildEntry() {
		return new SelectionListEntry<>(
		  this.fieldNameKey, this.valuesArray, this.value,
		  this.nameProvider);
	}
}

