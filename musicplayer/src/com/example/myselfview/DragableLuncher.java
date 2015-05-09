package com.example.myselfview;

import java.util.Random;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Scroller;

import com.example.musicplayer.PlayingActivity;
import com.example.musicplayer.R;

/**
 * 类说明：自定义View，模仿android 桌面Luncher
 * @author LiangAn
 * @version 创建时间：2015年3月18日 上午10:25:12
 */
public class DragableLuncher extends ViewGroup {
	private PlayingActivity musicPlayerActivity;
	// 按钮背景色
	int choseColor, defaultColor;
	// 底部按钮数组
	ImageButton[] bottomBar;
	// 负责得到滚动属性的对象
	private Scroller mScroller;
	// 负责触摸的功能类
	private VelocityTracker mVelocityTracker;
	// 滚动的起始X坐标
	private int mScrollX = 0;
	// 默认显示第几屏
	public int mCurrentScreen = 0;
	// 滚动结束X坐标
	private float mLastMotionX;

	// private static final String LOG_TAG = "DragableSpace";

	private static final int SNAP_VELOCITY = 1000;

	private final static int TOUCH_STATE_REST = 0;
	private final static int TOUCH_STATE_SCROLLING = 1;

	private int mTouchState = TOUCH_STATE_REST;

	private int mTouchSlop = 0;

	public DragableLuncher(Context context) {
		super(context);
		mScroller = new Scroller(context);
		// 得到状态位
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

		this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.FILL_PARENT));
	}

	public DragableLuncher(Context context, AttributeSet attrs) {
		super(context, attrs);
		mScroller = new Scroller(context);

		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

		this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.FILL_PARENT));

		/* 获得xml中设置的属性值，这里是指代默认显示第几屏幕 */		
		TypedArray a = getContext().obtainStyledAttributes(attrs,R.styleable.DragableLuncher);
		mCurrentScreen = a.getInteger(R.styleable.DragableLuncher_default_screen, 1);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}

		final float x = ev.getX();

		switch (action) {
		case MotionEvent.ACTION_MOVE:

			/* java中取得随机数字方法 */
			int m = (int) Math.random() * 10;
			Random a = new Random();
			int k = a.nextInt(100);

			System.out.println(m);
			System.out.println(k);

			// 取绝对值
			final int xDiff = (int) Math.abs(x - mLastMotionX);

			boolean xMoved = xDiff > mTouchSlop;

			if (xMoved) {
				// Scroll if the user moved far enough along the X axis
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			break;

		case MotionEvent.ACTION_DOWN:
			// Remember location of down touch
			mLastMotionX = x;

			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Release the drag
			mTouchState = TOUCH_STATE_REST;
			break;
		}

		return mTouchState != TOUCH_STATE_REST;
	}

	/**
	 * 设置是否打开触摸滑动
	 * 
	 * @param b
	 * @return
	 */
	public boolean isOpenTouchAnima(boolean b) {
		isOpen = b;
		return isOpen;
	}
	
	public void setMusicPlayerAct(PlayingActivity musicActivity){
		this.musicPlayerActivity = musicActivity;
	}
	

	public boolean isOpen = true;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isOpen) {
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(event);

			final int action = event.getAction();
			final float x = event.getX();

			switch (action) {
			case MotionEvent.ACTION_DOWN:
				// Log.i(LOG_TAG, "event : down");

				if (!mScroller.isFinished()) {
					mScroller.abortAnimation();
				}

				// Remember where the motion event started
				mLastMotionX = x;
				break;
			case MotionEvent.ACTION_MOVE:
				// Log.i(LOG_TAG,"event : move");
				// if (mTouchState == TOUCH_STATE_SCROLLING) {
				// Scroll to follow the motion event
				final int deltaX = (int) (mLastMotionX - x);
				mLastMotionX = x;

				// Log.i(LOG_TAG, "event : move, deltaX " + deltaX +
				// ", mScrollX " +
				// mScrollX);

				if (deltaX < 0) {
					if (mScrollX > 0) {
						scrollBy(Math.max(-mScrollX, deltaX), 0);
					}
				} else if (deltaX > 0) {
					final int availableToScroll = getChildAt(
							getChildCount() - 1).getRight()
							- mScrollX - getWidth();
					if (availableToScroll > 0) {
						scrollBy(Math.min(availableToScroll, deltaX), 0);
					}
				}
				// }
				break;
			case MotionEvent.ACTION_UP:
				// Log.i(LOG_TAG, "event : up");
				// if (mTouchState == TOUCH_STATE_SCROLLING) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000);
				int velocityX = (int) velocityTracker.getXVelocity();

				if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
					// Fling hard enough to move left
					snapToScreen(mCurrentScreen - 1);
				} else if (velocityX < -SNAP_VELOCITY
						&& mCurrentScreen < getChildCount() - 1) {
					// Fling hard enough to move right
					snapToScreen(mCurrentScreen + 1);
				} else {
					snapToDestination();
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				// }
				mTouchState = TOUCH_STATE_REST;
				break;
			case MotionEvent.ACTION_CANCEL:
				// Log.i(LOG_TAG, "event : cancel");
				mTouchState = TOUCH_STATE_REST;
			}
			mScrollX = this.getScrollX();
		} else {
			return false;
		}
		if (bottomBar != null) {
			for (int k = 0; k < bottomBar.length; k++) {
				if (k == mCurrentScreen) {
					bottomBar[k].setBackgroundColor(choseColor);
				} else {
					bottomBar[k].setBackgroundColor(defaultColor);
				}
			}
		}

		return true;
	}

	public void setBottomBarBg(ImageButton[] ib, int choseColor,
			int defaultColor) {
		this.bottomBar = ib;
		this.choseColor = choseColor;
		this.defaultColor = defaultColor;
	}

	private void snapToDestination() {
		final int screenWidth = getWidth();
		final int whichScreen = (mScrollX + (screenWidth / 2)) / screenWidth;
		// Log.i(LOG_TAG, "from des");
		snapToScreen(whichScreen);
	}

	/**
	 * 带动画效果显示界面
	 */
	public void snapToScreen(int whichScreen) {
		// Log.i(LOG_TAG, "snap To Screen " + whichScreen);
		mCurrentScreen = whichScreen;
		final int newX = whichScreen * getWidth();
		final int delta = newX - mScrollX;
		mScroller.startScroll(mScrollX, 0, delta, 0, Math.abs(delta) * 2);
		invalidate();
		//刷新底部分页按钮
		musicPlayerActivity.refreshPage(whichScreen);
	}

	/**
	 * 不带动画效果显示界面
	 */
	public void setToScreen(int whichScreen) {
		// Log.i(LOG_TAG, "set To Screen " + whichScreen);
		mCurrentScreen = whichScreen;
		final int newX = whichScreen * getWidth();
		mScroller.startScroll(newX, 0, 0, 0, 10);
		invalidate();
	}

	public int getCurrentScreen() {
		return mCurrentScreen;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childLeft = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				final int childWidth = child.getMeasuredWidth();
				child.layout(childLeft, 0, childLeft + childWidth,
						child.getMeasuredHeight());
				childLeft += childWidth;
			}
		}

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException("error mode.");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException("error mode.");
		}

		// The children are given the same width and height as the workspace
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		// Log.i(LOG_TAG, "moving to screen " + mCurrentScreen);
		scrollTo(mCurrentScreen * width, 0);
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			mScrollX = mScroller.getCurrX();
			scrollTo(mScrollX, 0);
			postInvalidate();
		}
	}
}
