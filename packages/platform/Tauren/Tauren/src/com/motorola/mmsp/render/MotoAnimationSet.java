package com.motorola.mmsp.render;

import java.util.ArrayList;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

/**
 * 
 * the animationset defined in xml is like:
 *
<MotoAniamtationSet render:ordering="sequentially">
     <MotoAniamtationSet render:ordering="together">
         <MotoLinearAnimation
             render:type="rotate" 
             render:affect_type="set"
             render:from="5" 
             render:to="-1030" />
         <MotoLinearAnimation
             render:type="scale" 
             render:affect_type="delta"
             render:from="5" 
             render:to="-1030"/>
     </MotoAniamtationSet>
     <MotoLinearAnimation
         render:type="traslate_x" 
         render:affect_type="delta"
         render:from="5" 
         render:to="-1030"/>
 </MotoAniamtationSet>
 * 
 * 
 */
public class MotoAnimationSet extends MotoAnimation {
	public static final int ANIMATIONSET_ORDERING_TOGETHER = 0;
	public static final int ANIMATIONSET_ORDERING_SEQUENTIALLY = 1;
	
	
	protected ArrayList<MotoAnimation> mChildren = new ArrayList<MotoAnimation>();
	protected int mOrdering = ANIMATIONSET_ORDERING_TOGETHER;
	protected boolean mHasNext;
	protected int mRunningChildSequentially;
	//protected boolean mEnd;
//	protected int mRepeatIndex;
	
	public MotoAnimationSet(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String sOrder = (String)attr.getValue("ordering");
		if ("together".equals(sOrder)) {
			mOrdering = ANIMATIONSET_ORDERING_TOGETHER;
		} else if ("sequentially".equals(sOrder)) {
			mOrdering = ANIMATIONSET_ORDERING_SEQUENTIALLY;
		}
	}
	
	public MotoAnimationSet(int ordering) {
		mOrdering = ordering;
	}
	
	public void setOrdering(int ordering) {
		mOrdering = ordering;
		updateDuration();
	}

	public int getOrdering() {
		return mOrdering;
	}
	
	@Override
	public void updateDuration() {
		int size = (mChildren != null) ? mChildren.size() : 0;
		if (size > 0) {
			int duration = 0;
			if (mOrdering == ANIMATIONSET_ORDERING_SEQUENTIALLY) {
				for (int i=0; i<size; i++) {
					MotoAnimation child = mChildren.get(i);
					long childDuration = (child != null) ? child.getTotalDuration() : 0;
					if (childDuration == DURATION_ENDLESS) {
						duration = DURATION_ENDLESS;
						break;
					}
					duration += childDuration;
				}
			} else {
				for (int i=0; i<size; i++) {
					MotoAnimation child = mChildren.get(i);
					int childDuration = (child != null) ? child.getTotalDuration() : 0;
					if (childDuration == DURATION_ENDLESS) {
						duration = DURATION_ENDLESS;
						break;
					}
					if (childDuration > duration) {
						duration = childDuration;
					}
				}
			}
			mDuration = duration;
		} else {
			mDuration = 0;
		}
		
		updateParentDuration();
	}
	
	@Override
	public void updateParentDuration() {
		long totalDuration = getTotalDuration();
		mHasNext = ((mDuration > 0) && ((totalDuration > 0) || (totalDuration == DURATION_ENDLESS)));
		
		if (mParent != null) {
			mParent.updateDuration();
		}
	}
	
	public void setTogether(MotoAnimation... animations) {
		ArrayList<MotoAnimation> animationArrayList = new ArrayList<MotoAnimation>(animations.length);
		int size = animations != null ? animations.length : 0;
		for (int i=0; i<size; i++) {
			animationArrayList.add(animations[i]);
		}
		setTogether(animationArrayList);
	}
	
	public void setTogether(ArrayList<MotoAnimation> animations) {
		mOrdering = ANIMATIONSET_ORDERING_TOGETHER;
		for (MotoAnimation animaiton: mChildren) {
			animaiton.mParent = null;
		}
		mChildren.clear();
		if (animations != null) {
			for (MotoAnimation animation: animations) {
				mChildren.add(animation);
				animation.mParent = this;
			}
		}
		updateDuration();
	}
	
	public void setSequentially(MotoAnimation... animations) {
		ArrayList<MotoAnimation> animationArrayList = new ArrayList<MotoAnimation>(animations.length);
		int size = animations != null ? animations.length : 0;
		for (int i=0; i<size; i++) {
			animationArrayList.add(animations[i]);
		}
		setSequentially(animationArrayList);
	}
	
	public void setSequentially(ArrayList<MotoAnimation> animations) {
		mOrdering = ANIMATIONSET_ORDERING_SEQUENTIALLY;
		for (MotoAnimation animaiton: mChildren) {
			animaiton.mParent = null;
		}
		mChildren.clear();
		if (animations != null) {
			for (MotoAnimation animation: animations) {
				mChildren.add(animation);
				animation.mParent = this;
			}
		}
		updateDuration();
	}
	
	public void addMotoAnimation(MotoAnimation animation, boolean updateFrameCount) {
		mChildren.add(animation);
		animation.mParent = this;
		if (updateFrameCount) {
			updateDuration();
		}
	}
	
	public void addMotoAnimation(int index, MotoAnimation animation, boolean updateFrameCount) {
		mChildren.add(index, animation);
		animation.mParent = this;
		if (updateFrameCount) {
			updateDuration();
		}
	}
	
	private void updateDelete(int index) {
		if (index == mRunningChildSequentially) {
			if (mOrdering == ANIMATIONSET_ORDERING_SEQUENTIALLY) {
				if (mChildren.size() <= 0) {
					mHasNext = false;
					mRunningChildSequentially = 0;
					if (mListener != null) {
						mListener.onAnimationStop(this);
					}
				} else {
					mRunningChildSequentially ++;
					if (mRunningChildSequentially >= mChildren.size()) {
						resetAllChildren();
					}
				}
			} else {
				
			}
		}
	}
	
	public void removeMotoAnimation(int index) {
		MotoAnimation del = mChildren.remove(index);
		//if (del != null) {
			//del.stop();
		//}
		if (del != null) {
			del.mParent = null;
		}
		updateDelete(index);
		updateDuration();
	}
	
	public void removeMotoAnimation(MotoAnimation animation) {
		int index = mChildren.indexOf(animation);
		if (index >= 0) {
			mChildren.remove(animation);
			//if (animation != null) {
				//animation.stop();
			//}
			if (animation != null) {
				animation.mParent = null;
			}
			updateDelete(index);
			updateDuration();
		}
	}
	
	public MotoAnimation getAnimationAt(int index) {
		return mChildren.get(index);
	}
	
	public int getAnimationCount() {
		return mChildren.size();
	}
	
	@Deprecated
	@SuppressWarnings("unchecked")
	public ArrayList<MotoAnimation> getAllAnimations() {
		return (ArrayList<MotoAnimation>)mChildren.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<MotoAnimation> getChildren() {
		return (ArrayList<MotoAnimation>)mChildren.clone();
	}
	
	@Override
	public MotoAnimation findAnimationById(int id) {
		if (getId() == id) {
			return this;
		}
		
		int size = mChildren.size();
		for (int i=0; i<size; i++) {
			MotoAnimation child = mChildren.get(i);
			if (child != null) {
				MotoAnimation find = child.findAnimationById(id);
				if (find != null) {
					return find;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void start() {
		super.start();
		int size = mChildren.size();
		for (int i=0; i<size; i++) {
			MotoAnimation child = mChildren.get(i);
			if (child != null) {
				child.start();
			}
		}
	}
	
	@Override
	public void seekToProgress(float progress) {
		seekToProgress(progress, null);
	}
	
	@Override
	public void seekToTime(int time) {
		if (mDuration == DURATION_ENDLESS){
			//Don't know what it means. just return
			return;
		}
		if (time > getTotalDuration()) {
			mRepeatIndex = mRepeatCount - 1;
			seekToProgress(1f);
		} else {
			mRepeatIndex = time % mDuration;
			seekToProgress((float)time / mDuration);
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		//mEnd = false;
		long totalDuration = getTotalDuration();
		mHasNext = ((mDuration > 0) && ((totalDuration > 0) || (totalDuration == DURATION_ENDLESS)));
		resetAllChildren();
	}
	
	@Override
	public int reset(int time) {
		reset();
		return next(time, null);
	}
	
	private void resetAllChildren() {
		int size = mChildren.size();
		for (int i=0; i<size; i++) {
			MotoAnimation animation = mChildren.get(i);
			if (animation != null) {
				animation.reset();
			}
		}
		mRunningChildSequentially = 0;
	}

	@Override
	protected boolean hasNext() {
		return mHasNext;
	}

	@Override
	protected int next(int time, MotoAnimationProperties animationProperties) {
		/*if (mEnd) {
			mHasNext = false;
			mStatus = STATUS_STOPED;
			//seekToTime(mDuration, animationProperties);
			if (mListener != null) {
			
				mListener.onAnimationStop(this);
			}
			return 0;
		}*/
		int leftTime = 0;
		int size = mChildren.size();
		if (mOrdering == ANIMATIONSET_ORDERING_SEQUENTIALLY) {
			MotoAnimation child = mChildren.get(mRunningChildSequentially);
			if (child.hasNext()) {
				if (child.isGoing()) {
					leftTime = child.next(time, animationProperties);
				} else {
					//Let the picture stay at the current status, or let other animations block me.
					if (mListener != null) {
						mListener.onAnimationPause(this);
					}
				}
			} 
			if (!child.hasNext()) {
				if (mRepeatCount == REPEAT_COUNT_ENDLESS) {
					if (mDuration != DURATION_ENDLESS && leftTime > mDuration) {
						leftTime %= mDuration;
					}
					while (leftTime >= 0) {
						mRunningChildSequentially++;
						if (mRunningChildSequentially >= size) {
							mRunningChildSequentially = 0;
							if (mListener != null) {
								mListener.onAnimationRepeat(this);
							}
						}
						MotoAnimation item = mChildren.get(mRunningChildSequentially);
						if (item != null) {
							leftTime = item.reset(leftTime);
						}
					}
				} else {
					while (leftTime >= 0) {
						mRunningChildSequentially++;
						if (mRunningChildSequentially >= size) {
							mRepeatIndex++;
                            if (mRepeatIndex >= mRepeatCount) {
                                // if (mParent == null) {
                                // mHasNext = true;
                                // mStatus = STATUS_RUNNING;
                                // mEnd = true;
                                // } else {
                                mStatus = STATUS_STOPED;
                                mHasNext = false;
                                if (mListener != null) {
                                    mListener.onAnimationStop(this);
                                }
                                // }
                                break;
                            } else {
								mRunningChildSequentially = 0;
								if (mListener != null) {
									mListener.onAnimationRepeat(this);
								}
							}
						}
						MotoAnimation item = mChildren.get(mRunningChildSequentially);
						if (item != null) {
							leftTime = item.reset(leftTime);
						}
					}
				}
			}
			
		} else {
			boolean bAllStopped = true;
			leftTime = -1;
			int childLeftTime = mDuration;
			int childLeftTimeTemp = 0;
			for (int i=0; i<size; i++) {
				MotoAnimation child = mChildren.get(i);
				if (child != null) {
					if (child.hasNext()) {
						if (child.isGoing()) {
							childLeftTimeTemp = child.next(time, animationProperties);
							if (childLeftTimeTemp < childLeftTime) {
								childLeftTime = childLeftTimeTemp;
							}
						} else {
							//Let the picture stay at the last status, or let other animations block me.
							if (mListener != null) {
								mListener.onAnimationPause(this);
							}
						}
					}
					if (child.hasNext()) {
						bAllStopped = false;
					}
				}
			}
			
			if (bAllStopped) {
				if (mRepeatCount == REPEAT_COUNT_ENDLESS) {
					if (mDuration == DURATION_ENDLESS) {
						for (int i=0; i<size; i++) {
							MotoAnimation child = mChildren.get(i);
							if (child != null) {
								child.reset(childLeftTime);
							}
						}
					} else {
						int leftTimeOnce = childLeftTime % mDuration;
						for (int i=0; i<size; i++) {
							MotoAnimation child = mChildren.get(i);
							if (child != null) {
								child.reset(leftTimeOnce);
							}
						}
					}
					if (mListener != null) {
						mListener.onAnimationRepeat(this);
					}
					
				} else {
					int oldRepeatIndex = mRepeatIndex;
					mRepeatIndex += (childLeftTime / mDuration) + 1;
                    if (mRepeatIndex >= mRepeatCount) {
                        // if (mParent == null) {
                        // mHasNext = true;
                        // mStatus = STATUS_RUNNING;
                        // mEnd = true;
                        // } else {
                        mStatus = STATUS_STOPED;
                        mHasNext = false;
                        // seekToTime(mDuration, animationProperties);
                        leftTime = childLeftTime
                                - ((mRepeatCount - oldRepeatIndex - 1) * mDuration);
                        if (mListener != null) {
                            mListener.onAnimationStop(this);
                        }
                        // }
                    } else {
						int leftTimeOnce = childLeftTime % mDuration;
						for (int i=0; i<size; i++) {
							MotoAnimation child = mChildren.get(i);
							if (child != null) {
								child.reset(leftTimeOnce);
							}
						}
						if (mListener != null) {
							mListener.onAnimationRepeat(this);
						}
					}
				}
			}
		}
		
		return leftTime;
	}
	
	@Override
	protected void seekToTime(int time, MotoAnimationProperties animationProperties) {
		if (mDuration == DURATION_ENDLESS){
			//Don't know what it means. just return
			return;
		}
		if (time > getTotalDuration()) {
			mRepeatIndex = mRepeatCount -1;
			seekToProgress(1f, animationProperties);
		} else {
			mRepeatIndex = time % mDuration;
			seekToProgress((float)time/mDuration, animationProperties);
		}
	}
	
	@Override
	protected void seekToProgress(float progress, MotoAnimationProperties animationProperties) {
		if (mDuration == DURATION_ENDLESS){
			//Don't know what it means. just return
			return;
		}
		
		int time = (int)(progress * mDuration);
		if (mOrdering == ANIMATIONSET_ORDERING_TOGETHER) {
			if (mListener != null) {
				mListener.onAnimationSeek(this, progress);
			}
			int size = mChildren.size();
			for (int i=0; i<size; i++) {
				MotoAnimation child = mChildren.get(i);
				if (child != null) {
					if (animationProperties != null) {
						child.seekToTime(time, animationProperties);
					} else {
						child.seekToTime(time);
					}
				}
			}
			if (time >= mDuration) {
				mHasNext = false;
			}
		} else {
			if (mListener != null) {
				mListener.onAnimationSeek(this, progress);
			}
			long duration = 0;
			int size = mChildren.size();
			boolean hasNext = false;
			for (int i=0; i<size; i++) {
				mRunningChildSequentially = i;
				MotoAnimation child = mChildren.get(i);
				if (child == null) {
					continue;
				}
				duration += child.getTotalDuration();
				if (time < duration) {
					if (animationProperties != null) {
						child.seekToTime((int)(time - (duration - child.getTotalDuration())), animationProperties);
					} else {
						child.seekToTime((int)(time - (duration - child.getTotalDuration())));
					}
					//if (time < duration) {
					hasNext = true;
					//}
					break;
				} else {
					if (animationProperties != null) {
						child.seekToProgress(1f, animationProperties);
					} else {
						child.seekToProgress(1f);
					}
					child.stop();
				}
			}
			mHasNext = hasNext;
		}
		
		if (!mHasNext && null != mListener) {
			mListener.onAnimationStop(this);
		}

	}

	@Override
	protected int getTotalDuration() {
		if ((mRepeatCount == REPEAT_COUNT_ENDLESS) || (mDuration == DURATION_ENDLESS)) {
			return DURATION_ENDLESS;
		} else {
			return getDuration() * getRepeatCount();
		}
	}
}
