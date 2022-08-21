package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.util.math.MathHelper;

public class RectangleAnimator {
	protected ToggleAnimator animator;
	protected Rectangle target = new Rectangle();
	protected Rectangle origin = new Rectangle();
	protected Rectangle current = new Rectangle();
	protected float lastUpdate = -1F;
	
	public RectangleAnimator() {
		this(250L);
	}
	
	public RectangleAnimator(long length) {
		this.animator = new ToggleAnimator(length);
	}
	
	public void reset(Rectangle origin, Rectangle target) {
		this.target = target.copy();
		this.origin = origin.copy();
		animator.resetTarget();
	}
	
	public void setTarget(Rectangle target) {
		reset(getCurrent(), target);
	}
	
	public void setEaseInTarget(Rectangle target) {
		reset(getCurrentEaseIn(), target);
	}
	
	public void setEaseOutTarget(Rectangle target) {
		reset(getCurrentEaseOut(), target);
	}
	
	public void setEaseInOutTarget(Rectangle target) {
		reset(getCurrentEaseInOut(), target);
	}
	
	protected void updateCurrent(float progress) {
		if (lastUpdate != progress) {
			int l = (int) MathHelper.lerp(progress, origin.x, target.x);
			int t = (int) MathHelper.lerp(progress, origin.y, target.y);
			int w = ((int) MathHelper.lerp(progress, origin.getMaxX(), target.getMaxX())) - l;
			int h = ((int) MathHelper.lerp(progress, origin.getMaxY(), target.getMaxY())) - t;
			current.setBounds(l, t, w, h);
			lastUpdate = progress;
		}
	}
	
	public Rectangle getCurrent() {
		updateCurrent(animator.progress);
		return current;
	}
	
	public Rectangle getCurrentEaseIn() {
		updateCurrent(animator.getEaseIn());
		return current;
	}
	
	public Rectangle getCurrentEaseOut() {
		updateCurrent(animator.getEaseOut());
		return current;
	}
	
	public Rectangle getCurrentEaseInOut() {
		updateCurrent(animator.getEaseInOut());
		return current;
	}
	
	public boolean isInProgress() {
		return animator.isInProgress();
	}
	
	public float getProgress() {
		return animator.getProgress();
	}
	
	public Rectangle getTarget() {
		return target;
	}
	
	public Rectangle getOrigin() {
		return origin;
	}
	
	public Rectangle setOrigin(Rectangle origin) {
		this.origin = origin.copy();
		return origin;
	}
}
