package br.gov.rs.procergs.prandroidlib.list.item;

import android.graphics.drawable.Drawable;

public class EntryItem implements Item {

	public final int indice;
	public final String title;
	public final String subtitle;
	public int resIcon;
	public Drawable drawIcon;
		
	public EntryItem(int indice, String title, String subtitle, int resIcon) {
		this.indice = indice;
		this.title = title;
		this.subtitle = subtitle;
		this.resIcon = resIcon;
	}
	
	public EntryItem(int indice, String title, String subtitle) {
		this.indice = indice;
		this.title = title;
		this.subtitle = subtitle;
	}

	public EntryItem(int indice, String title, int resIcon) {
		this.indice = indice;
		this.title = title;
		this.subtitle = "";
		this.resIcon = resIcon;
	}

	public EntryItem(int indice, String title, Drawable drawableIcon) {
		this.indice = indice;
		this.title = title;
		this.subtitle = "";
		this.drawIcon = drawableIcon;
	}

	public EntryItem(int indice, String title) {
		this.indice = indice;
		this.title = title;
		this.subtitle = "";
		this.resIcon = 0;
	}

	@Override
	public boolean isSection() {
		return false;
	}

}
