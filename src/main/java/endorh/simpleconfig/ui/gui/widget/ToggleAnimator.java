package endorh.simpleconfig.ui.gui.widget;

import static java.lang.Math.abs;

public class ToggleAnimator {
	protected float progress;
	protected float target;
	protected float lastProgress;
	protected long lastChange = 0L;
	protected long length;
	protected float min = 0F;
	protected float max = 1F;
	
	public ToggleAnimator() {this(250L);}
	
	public ToggleAnimator(long length) {this(0F, length);}
	
	public ToggleAnimator(float progress, long length) {
		target = lastProgress = this.progress = progress;
		this.length = length;
	}
	
	public void toggle() {
		setTarget(target <= 0.5);
	}
	
	public void resetTarget() {
		resetTarget(true);
	}
	
	public void resetTarget(boolean onOff) {
		this.target = onOff? 1F : 0F;
		lastProgress = 1F - target;
		lastChange = System.currentTimeMillis();
	}
	
	public void resetTarget(float target) {
		this.target = target;
		lastProgress = 0F;
		lastChange = System.currentTimeMillis();
	}
	
	public void setEaseOutTarget(boolean onOff) {
		setEaseOutTarget(onOff? 1F : 0F);
	}
	
	public void setEaseOutTarget(float target) {
		lastProgress = getEaseOut();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	// Preserve animation progress
	
	public void setEaseInTarget(boolean onOff) {
		setEaseInTarget(onOff? 1F : 0F);
	}
	
	public void setEaseInTarget(float target) {
		lastProgress = getEaseIn();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	
	public void setEaseInOutTarget(boolean onOff) {
		setEaseInOutTarget(onOff? 1F : 0F);
	}
	
	public void setEaseInOutTarget(float target) {
		lastProgress = getEaseInOut();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	
	public boolean isInProgress() {
		return System.currentTimeMillis() - lastChange < length * abs(target - lastProgress);
	}
	
	public float getProgress() {
		long time = System.currentTimeMillis();
		float len = length * abs(target - lastProgress);
		if (time - lastChange < len) {
			final float t = (time - lastChange) / len;
			return progress = lastProgress * (1 - t) + target * t;
		} else return progress = target;
	}
	
	public float getEaseOut() {
		final float t = getProgress();
		return mapRange(target < t? t * t : 2 * t - t * t);
	}
	
	public float getEaseIn() {
		final float t = getProgress();
		return mapRange(target < t? 2 * t - t * t : t * t);
	}
	
	public float getEaseInOut() {
		final float t = getProgress();
		return mapRange(t <= 0.5? 2 * t * t : -1F + (4 - 2 * t) * t);
	}
	
	public float mapRange(float in) {
		return min + in * (max - min);
	}
	
	public long getLastChange() {
		return lastChange;
	}
	
	public float getTarget() {
		return target;
	}
	
	public void setTarget(boolean onOff) {
		setTarget(onOff? 1F : 0F);
	}
	
	public void setTarget(float target) {
		lastProgress = getProgress();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	
	public float getRangeMin() {
		return min;
	}
	
	public float getRangeMax() {
		return max;
	}
	
	public void setOutputRange(float min, float max) {
		this.min = min;
		this.max = max;
	}
	
	public long getLength() {
		return length;
	}
	
	public void setLength(long length) {
		this.length = length;
	}
}
