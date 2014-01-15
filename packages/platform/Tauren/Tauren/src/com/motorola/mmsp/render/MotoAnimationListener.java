package com.motorola.mmsp.render;

/**
 * Must run at UI thread
 */
public interface MotoAnimationListener {
	void onAnimationStart(MotoAnimation animation);

	void onAnimationStop(MotoAnimation animation);

	void onAnimationPause(MotoAnimation animation);

	void onAnimationResume(MotoAnimation animation);

	@Deprecated
	void onAnimationEnd(MotoAnimation animation);

	void onAnimationCancel(MotoAnimation animation);

	void onAnimationRepeat(MotoAnimation animation);

	void onAnimationSeek(MotoAnimation animation, float progress);
}