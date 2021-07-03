package endorh.simple_config.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CompoundNBTEntry
  extends AbstractSerializableEntry<CompoundNBT, CompoundNBTEntry> {
	protected CompoundNBTEntry(CompoundNBT value) {
		super(value, CompoundNBT.class);
	}
	
	@Override protected String serialize(CompoundNBT value) {
		return value.toFormattedComponent().getString();
	}
	
	@Override protected @Nullable CompoundNBT deserialize(String value) {
		try {
			return new JsonToNBT(new StringReader(value)).readStruct();
		} catch (CommandSyntaxException ignored) {
			return null;
		}
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		return Optional.of(new TranslationTextComponent(
		  "simple-config.config.error.invalid_nbt"));
	}
}

