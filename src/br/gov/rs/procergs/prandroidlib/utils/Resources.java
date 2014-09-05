package br.gov.rs.procergs.prandroidlib.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class Resources {

	public Resources() {
	}
	
	public static String getString(Context ctx, int resId){
		return ctx.getResources().getString(resId);
	}
	
	public static Drawable getDrawable(Context ctx, int resId){
		return ctx.getResources().getDrawable(resId);
	}
	
}
