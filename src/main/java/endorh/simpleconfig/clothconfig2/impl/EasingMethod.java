package endorh.simpleconfig.clothconfig2.impl;

import java.util.function.Function;

public interface EasingMethod {
	double apply(double var1);
	
	enum EasingMethodImpl implements EasingMethod {
		NONE(v -> 1.0),
		LINEAR(v -> v),
		EXPO(v -> v == 1.0 ? 1 : -Math.pow(2, -10 * v) + 1),
		QUAD(v -> -1 * v * (v - 2)),
		QUART(v -> v == 1.0 ? 1 : 1 * (-1 * ((v = v - 1) * v * v * v - 1))),
		SINE(v -> Math.sin(v * (Math.PI / 2))),
		CUBIC(v -> (v = v - 1) * v * v + 1),
		QUINTIC(v -> (v = v - 1) * v * v * v * v + 1),
		CIRC(v -> Math.sqrt(1 - (v = v - 1) * v));
		
		private final Function<Double, Double> function;
		
		EasingMethodImpl(Function<Double, Double> function) {
			this.function = function;
		}
		
		@Override
		public double apply(double v) {
			return this.function.apply(v);
		}
		
		public String toString() {
			return this.name();
		}
	}
}

