package br.gov.rs.procergs.prandroidlib.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ListView;
import android.widget.Toast;

public class SwipeGestureListener extends SimpleOnGestureListener implements OnTouchListener {
	
	Context context;
	GestureDetector gDetector;
	static final int SWIPE_MIN_DISTANCE = 120;
	static final int SWIPE_MAX_OFF_PATH = 250;
	static final int SWIPE_THRESHOLD_VELOCITY = 200;
	ListView listView;
	View view;
	public boolean isLeft = false;
	public boolean isRight = false;
	
	public SwipeGestureListener() {
		super();
	}

	public SwipeGestureListener(Context context, ListView listView, View view) {
		this(context, null, listView, view);
	}
	
	public SwipeGestureListener(Context context, GestureDetector gDetector, ListView listView, View view) {

		if (gDetector == null)
			gDetector = new GestureDetector(context, this);

		this.context = context;
		this.gDetector = gDetector;
		this.listView = listView;
		this.view = view;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,	float velocityY) {
		
		final int position = listView.pointToPosition(Math.round(e1.getX()), Math.round(e1.getY()));

		String countryName = (String) listView.getItemAtPosition(position);

		if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
			if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH || Math.abs(velocityY) < SWIPE_THRESHOLD_VELOCITY) {
				return false;
			}
			if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE) {
				Toast.makeText(context, "bottomToTop" + countryName, Toast.LENGTH_SHORT).show();
			} else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) {
				Toast.makeText(context, "topToBottom  " + countryName, Toast.LENGTH_SHORT).show();
			}
		} else {
			if (Math.abs(velocityX) < SWIPE_THRESHOLD_VELOCITY) {
				return false;
			}
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
				this.isLeft = true;
				//Toast.makeText(context, "swipe RightToLeft " + isLeft + " item selecionado: " + countryName, Toast.LENGTH_SHORT).show();
				
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
				this.isRight = true;
				//Toast.makeText(context, "swipe LeftToright " + isRight + " item selecionado: "+ countryName, Toast.LENGTH_SHORT).show();
			}
		}
		return super.onFling(e1, e2, velocityX, velocityY);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return gDetector.onTouchEvent(event);
	}

	public GestureDetector getDetector() {
		return gDetector;
	}

}
