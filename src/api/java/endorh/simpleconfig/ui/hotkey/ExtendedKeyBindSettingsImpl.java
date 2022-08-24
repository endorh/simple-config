package endorh.simpleconfig.ui.hotkey;

import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindActivation;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping.KeyBindContext;

import java.util.Objects;

class ExtendedKeyBindSettingsImpl implements ExtendedKeyBindSettings {
	private final KeyBindActivation activation;
	private final KeyBindContext context;
	private final boolean allowExtraKeys;
	private final boolean orderSensitive;
	private final boolean exclusive;
	private final boolean matchByChar;
	private final boolean preventFurther;
	
	public ExtendedKeyBindSettingsImpl(
	  KeyBindActivation activation, KeyBindContext context, boolean allowExtraKeys,
	  boolean orderSensitive, boolean exclusive, boolean matchByChar, boolean preventFurther
	) {
		this.activation = activation;
		this.context = context;
		this.allowExtraKeys = allowExtraKeys;
		this.orderSensitive = orderSensitive;
		this.exclusive = exclusive;
		this.matchByChar = matchByChar;
		this.preventFurther = preventFurther;
	}
	
	@Override public KeyBindActivation getActivation() {
		return activation;
	}
	@Override public KeyBindContext getContext() {
		return context;
	}
	@Override public boolean isAllowExtraKeys() {
		return allowExtraKeys;
	}
	@Override public boolean isOrderSensitive() {
		return orderSensitive;
	}
	@Override public boolean isExclusive() {
		return exclusive;
	}
	@Override public boolean isMatchByChar() {
		return matchByChar;
	}
	@Override public boolean isPreventFurther() {
		return preventFurther;
	}
	
	@Override public ExtendedKeyBindSettings copy() {
		return new ExtendedKeyBindSettingsBuilder(this).build();
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExtendedKeyBindSettingsImpl that = (ExtendedKeyBindSettingsImpl) o;
		return allowExtraKeys == that.allowExtraKeys
		       && orderSensitive == that.orderSensitive
		       && exclusive == that.exclusive
		       && matchByChar == that.matchByChar
		       && preventFurther == that.preventFurther
		       && activation == that.activation
		       && context == that.context;
	}
	
	@Override public int hashCode() {
		return Objects.hash(
		  activation, context, allowExtraKeys, orderSensitive,
		  exclusive, matchByChar, preventFurther);
	}
}
