package br.gov.rs.procergs.prandroidlib.utils;

import android.app.AlertDialog;
import android.content.Context;
import br.gov.rs.procergs.prandroidlib.R;

public class Alert {

	public static void show(Context context, String title, String mensagem){
		if (title == null || title.length() == 0)
			title = context.getResources().getString(R.string.app_name);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(title);
		builder.setMessage(mensagem);
		builder.setNegativeButton(R.string.fechar, null);
		builder.show();
	}
	
	public static void show(Context context, int resTitleId, String mensagem){
		String title = context.getResources().getString(resTitleId);
		show(context, title, mensagem);
	}
	
	public static void show(Context context, String mensagem){
		show(context, null, mensagem);
	}
}
