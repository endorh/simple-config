package endorh.simpleconfig.ui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@OnlyIn(value = Dist.CLIENT)
public class Modifier {
	private final short value;
	
	private Modifier(short value) {
		this.value = value;
	}
	
	public static Modifier none() {
		return Modifier.of((short) 0);
	}
	
	public static @Nullable Modifier ofKeyCode(int code) {
		if (Minecraft.IS_RUNNING_ON_MAC? code == 343 || code == 347 : code == 341 || code == 345) {
			return of(false, true, false);
		} else if (code == 344 || code == 340) {
			return of(false, false, true);
		} else if (code == 342 || code == 346) {
			return of(true, false, false);
		}
		return null;
	}
	
	public static Modifier of(boolean alt, boolean control, boolean shift) {
		short value = Modifier.setFlag((short) 0, (short) 1, alt);
		value = Modifier.setFlag(value, (short) 2, control);
		value = Modifier.setFlag(value, (short) 4, shift);
		return Modifier.of(value);
	}
	
	public static Modifier of(short value) {
		return new Modifier(value);
	}
	
	public static Modifier current() {
		return Modifier.of(Screen.hasAltDown(), Screen.hasControlDown(), Screen.hasShiftDown());
	}
	
	private static short setFlag(short base, short flag, boolean val) {
		return val ? Modifier.setFlag(base, flag) : Modifier.removeFlag(base, flag);
	}
	
	private static short setFlag(short base, short flag) {
		return (short) (base | flag);
	}
	
	private static short removeFlag(short base, short flag) {
		return (short) (base & ~flag);
	}
	
	private static boolean getFlag(short base, short flag) {
		return (base & flag) != 0;
	}
	
	public boolean matchesCurrent() {
		return this.equals(Modifier.current());
	}
	
	public short getValue() {
		return this.value;
	}
	
	public boolean hasAlt() {
		return Modifier.getFlag(this.value, (short) 1);
	}
	
	public boolean hasControl() {
		return Modifier.getFlag(this.value, (short) 2);
	}
	
	public boolean hasShift() {
		return Modifier.getFlag(this.value, (short) 4);
	}
	
	public boolean isEmpty() {
		return this.value == 0;
	}
	
	public Modifier merge(Modifier mod) {
		return new Modifier((short) (value | mod.value));
	}
	
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!(other instanceof Modifier)) {
			return false;
		}
		return this.value == ((Modifier) other).value;
	}
	
	public int hashCode() {
		return Objects.hash(this.value);
	}
}

