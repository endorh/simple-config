package dnj.simple_config.core.entry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class INBTEntry extends AbstractSerializableEntry<INBT, INBTEntry> {
	protected INBTEntry(INBT value) {
		super(value, INBT.class);
	}
	
	@Override
	protected String serialize(INBT value) {
		return value.toFormattedComponent().getString();
	}
	
	@Override
	protected @Nullable INBT deserialize(String value) {
		try {
			return new JsonToNBT(new StringReader(value)).readValue();
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
