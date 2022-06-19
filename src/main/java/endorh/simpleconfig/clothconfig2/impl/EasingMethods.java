package endorh.simpleconfig.clothconfig2.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EasingMethods {
	private static final List<EasingMethod> METHODS = new ArrayList<>();
	
	public static void register(EasingMethod easingMethod) {
		METHODS.add(easingMethod);
	}
	
	public static List<EasingMethod> getMethods() {
		return Collections.unmodifiableList(METHODS);
	}
	
	static {
		METHODS.addAll(Arrays.asList(EasingMethod.EasingMethodImpl.values()));
	}
}

