// THIS IS A BETA! I DON'T RECOMMEND USING IT IN PRODUCTION CODE JUST YET

/*
 * Copyright 2012 Roman Nurik
 * Copyright 2013 Niek Haarman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.gov.rs.procergs.prandroidlib.adapters;

import static com.nineoldandroids.view.ViewHelper.setAlpha;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import br.gov.rs.procergs.prandroidlib.interfaces.OnDismissCallback;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * A {@link android.view.View.OnTouchListener} that makes the list items in a
 * {@link ListView} dismissable. {@link ListView} is given special treatment
 * because by default it handles touches for its list items... i.e. it's in
 * charge of drawing the pressed state (the list selector), handling list item
 * clicks, etc.
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(listView, new SwipeDismissListViewTouchListener.OnDismissCallback() {
 * 	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 * 		for (int position : reverseSortedPositions) {
 * 			adapter.remove(adapter.getItem(position));
 * 		}
 * 		adapter.notifyDataSetChanged();
 * 	}
 * });
 * listView.setOnTouchListener(touchListener);
 * </pre>
 */
@SuppressLint("Recycle")
public class SwipeDismissListViewTouchListener implements View.OnTouchListener {
	// Cached ViewConfiguration and system-wide constant values
	private int mSlop;
	private int mMinFlingVelocity;
	private int mMaxFlingVelocity;
	private long mAnimationTime;

	// Fixed properties
	private AbsListView mListView;
	private OnDismissCallback mCallback;
	private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

	// Transient properties
	private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
	private int mDismissAnimationRefCount = 0;
	private float mDownX;
	private float mDownY;
	private boolean mSwiping;
	private VelocityTracker mVelocityTracker;
	private boolean mPaused;
	private PendingDismissData mCurrentDismissData;

	private int mVirtualListCount = -1;

	/**
	 * Constructs a new swipe-to-dismiss touch listener for the given list view.
	 * 
	 * @param listView
	 *            The list view whose items should be dismissable.
	 * @param callback
	 *            The callback to trigger when the user has indicated that she
	 *            would like to dismiss one or more list items.
	 */
	public SwipeDismissListViewTouchListener(AbsListView listView, OnDismissCallback callback) {
		ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
		mSlop = vc.getScaledTouchSlop();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mAnimationTime = listView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
		mListView = listView;
		mCallback = callback;
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (mVirtualListCount == -1) {
			mVirtualListCount = mListView.getAdapter().getCount();
		}

		if (mViewWidth < 2) {
			mViewWidth = mListView.getWidth();
		}

		switch (motionEvent.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			view.onTouchEvent(motionEvent);
			return handleDownEvent(motionEvent);
		case MotionEvent.ACTION_MOVE:
			return handleMoveEvent(motionEvent);
		case MotionEvent.ACTION_UP:
			return handleUpEvent(motionEvent);
		}
		return false;
	}

	private boolean handleDownEvent(MotionEvent motionEvent) {
		if (mPaused) {
			return false;
		}

		// Find the child view that was touched (perform a hit test)
		Rect rect = new Rect();
		int childCount = mListView.getChildCount();
		int[] listViewCoords = new int[2];
		mListView.getLocationOnScreen(listViewCoords);
		int x = (int) motionEvent.getRawX() - listViewCoords[0];
		int y = (int) motionEvent.getRawY() - listViewCoords[1];
		View downView = null;
		for (int i = 0; i < childCount && downView == null; i++) {
			View child = mListView.getChildAt(i);
			child.getHitRect(rect);
			if (rect.contains(x, y)) {
				downView = child;
			}
		}

		if (downView != null) {
			mDownX = motionEvent.getRawX();
			mDownY = motionEvent.getRawY();
			int downPosition = mListView.getPositionForView(downView);

			mCurrentDismissData = new PendingDismissData(downPosition, downView);

			if (mPendingDismisses.contains(mCurrentDismissData) || downPosition >= mVirtualListCount) {
				// Cancel, we're already processing this position
				mCurrentDismissData = null;
				return false;
			} else {

				mVelocityTracker = VelocityTracker.obtain();
				mVelocityTracker.addMovement(motionEvent);
			}
		}
		return true;
	}

	private boolean handleMoveEvent(MotionEvent motionEvent) {
		if (mVelocityTracker == null || mPaused) {
			return false;
		}

		mVelocityTracker.addMovement(motionEvent);
		float deltaX = motionEvent.getRawX() - mDownX;
		float deltaY = motionEvent.getRawY() - mDownY;
		if (Math.abs(deltaX) > mSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
			mSwiping = true;
			mListView.requestDisallowInterceptTouchEvent(true);

			// Cancel ListView's touch (un-highlighting the item)
			MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
			cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
			mListView.onTouchEvent(cancelEvent);
		}

		if (mSwiping) {
			ViewHelper.setTranslationX(mCurrentDismissData.view, deltaX);
			setAlpha(mCurrentDismissData.view, Math.max(0f, Math.min(1f, 1f - 2f * Math.abs(deltaX) / mViewWidth)));
			return true;
		}
		return false;
	}

	private boolean handleUpEvent(MotionEvent motionEvent) {
		if (mVelocityTracker == null) {
			return false;
		}

		float deltaX = motionEvent.getRawX() - mDownX;
		mVelocityTracker.addMovement(motionEvent);
		mVelocityTracker.computeCurrentVelocity(1000);
		float velocityX = Math.abs(mVelocityTracker.getXVelocity());
		float velocityY = Math.abs(mVelocityTracker.getYVelocity());
		boolean dismiss = false;
		boolean dismissRight = false;
		if (Math.abs(deltaX) > mViewWidth / 2) {
			dismiss = true;
			dismissRight = deltaX > 0;
		} else if (mMinFlingVelocity <= velocityX && velocityX <= mMaxFlingVelocity && velocityY < velocityX) {
			dismiss = true;
			dismissRight = mVelocityTracker.getXVelocity() > 0;
		}

		if (dismiss) {
			// mDownView gets null'd before animation ends
			final PendingDismissData pendingDismissData = mCurrentDismissData;
			++mDismissAnimationRefCount;
			animate(mCurrentDismissData.view).translationX(dismissRight ? mViewWidth : -mViewWidth).alpha(0).setDuration(mAnimationTime).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					performDismiss(pendingDismissData);
				}
			});
			mVirtualListCount--;
			mPendingDismisses.add(mCurrentDismissData);
		} else {
			// cancel
			animate(mCurrentDismissData.view).translationX(0).alpha(1).setDuration(mAnimationTime).setListener(null);
		}
		mVelocityTracker = null;
		mDownX = 0;
		mCurrentDismissData = null;
		mSwiping = false;
		return false;
	}

	private static class PendingDismissData implements Comparable<PendingDismissData> {
		public int position;
		public View view;

		public PendingDismissData(int position, View view) {
			this.position = position;
			this.view = view;
		}

		@Override
		public int compareTo(PendingDismissData other) {
			// Sort by descending position
			return other.position - position;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + position;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PendingDismissData other = (PendingDismissData) obj;
			if (position != other.position)
				return false;
			return true;
		}

	}

	private void performDismiss(final PendingDismissData data) {
		// Animate the dismissed list item to zero-height and fire the
		// dismiss callback when all dismissed list item animations have
		// completed.

		final ViewGroup.LayoutParams lp = data.view.getLayoutParams();
		final int originalHeight = data.view.getHeight();

		ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				lp.height = (Integer) valueAnimator.getAnimatedValue();
				data.view.setLayoutParams(lp);
			}
		});

		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				--mDismissAnimationRefCount;
				if (mDismissAnimationRefCount == 0) {
					// No active animations, process all pending dismisses.
					// Sort by descending position
					Collections.sort(mPendingDismisses);

					int[] dismissPositions = new int[mPendingDismisses.size()];
					for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
						dismissPositions[i] = mPendingDismisses.get(i).position;
					}
					mCallback.onDismiss(mListView, dismissPositions);

					ViewGroup.LayoutParams lp;
					for (PendingDismissData pendingDismiss : mPendingDismisses) {
						// Reset view presentation
						ViewHelper.setAlpha(pendingDismiss.view, 1f);
						ViewHelper.setTranslationX(pendingDismiss.view, 0);
						lp = pendingDismiss.view.getLayoutParams();
						lp.height = 0;
						pendingDismiss.view.setLayoutParams(lp);
					}

					mPendingDismisses.clear();
				}
			}
		});
		animator.start();
	}
}
