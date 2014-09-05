package br.gov.rs.procergs.prandroidlib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Calendar;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class JSONCache {

	private Context context;
	private String url;
	private long timeout;
	private File cache;
	private File fileDestino;
	private SharedPreferences sharedPreferences;
	private boolean readFromServer;
	private String mensagem = "";

	public JSONCache() {
	}

	public JSONCache(Context context, String url, int timeout) {
		this.context = context;
		this.url = url;
		this.timeout = timeout;
		this.cache = new File(context.getExternalCacheDir().getAbsolutePath() + "/json/");
		this.fileDestino = new File(cache.getAbsolutePath() + "/" + fileNameFromUrl());
	}

	public boolean isConnectionAvailable(){
		return NetworkUtils.isNetworkAvailable(context);
	}
	
	public void execute() {
		if (NetworkUtils.isNetworkAvailable(context)) {
			if (!this.fileExists()) {
				this.download();
				this.readFromServer = true;
				//Log.d("ITEM", "this.readFromServer: " + this.readFromServer);
			} else {
				if (this.isExpired()) {
					//Log.d("ITEM", "expirou... " + this.isExpired());
					this.download();
					//Log.d("ITEM", "fez o download do servidor... " + this.readFromServer);
					this.readFromServer = true;
				} else {
					if (fileDestino.length() == 0){
						//Log.d("ITEM", "tamanho do arquivo ?? igual a zero... " + this.readFromServer);
						this.download();
						this.readFromServer = true;
					}else{
						this.readFromServer = false;
					}
				}
			}
		}else{
			//Log.d("ITEM", "Falha de conex??o!");
			this.mensagem = "Sem conex??o com a rede. Verifique sua conex??o e tente novamente";
		}
	}

	public void refresh() {
		if (!this.readFromServer) {
			this.download();
			this.readFromServer = true;
		} else {
			this.readFromServer = false;
		}
	}

	public JSONObject getJSON() {
		JSONObject jObject = null;
		FileChannel fc = null;
		MappedByteBuffer bb = null;
		String jString = null;
		FileInputStream stream = null;

		try {
			stream = new FileInputStream(this.fileDestino.getAbsoluteFile());
			try {
				fc = stream.getChannel();
				bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
				jString = Charset.defaultCharset().decode(bb).toString();
			} finally {
				stream.close();
			}

			jObject = new JSONObject(jString);
			
			//Log.d("ITEM", "arquivo json lido localmente no m??todo getJSON()");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jObject;
	}

	public String fileNameFromUrl() {
		String name = this.url.replace("/", "");
		name = name.replace("?", "");
		name = name.replace("&", "");
		name = name.replace(":", "");
		name = name.replace(".", "");
		name = name.replace(".json", "");

		return name;
	}

	private boolean fileExists() {
		return fileDestino.exists();
	}

	private void download() {
		StringBuilder response = new StringBuilder();
		BufferedReader input = null;
		String line = null;

		try {
			//Log.d("ITEM", "this.url no m??todo download: " + this.url);
			if (!cache.exists()) {
				cache.mkdirs();
			}

			if (!fileDestino.exists()) {
				fileDestino.createNewFile();
			}

			URL u = new URL(this.url);
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setConnectTimeout(1000);

			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				input = new BufferedReader(new InputStreamReader(conn.getInputStream()), 8192);

				while ((line = input.readLine()) != null) {
					response.append(line);
				}

				if (fileExists()) {
					FileOutputStream fos = new FileOutputStream(fileDestino);
					if (isJSONValid(response.toString())){
						Log.d("ITEM", "JSON V??lido:" + response.toString());
						fos.write(response.toString().getBytes());
					}else{
						fileDestino.delete();
						//Log.d("ITEM", "fileDestino.deleted?: "+ fileDestino.delete());
						//Log.d("ITEM", "JSON inv??lido: " + response.toString());
					}
					fos.close();
					//Log.d("ITEM", "M??todo Download: Feito o download do arquivo; file size: " + fileDestino.length());
				} else {
					//Log.d("ITEM", "falha na cria????o do arquivo no m??todo fileExists(): " + fileExists() + "; file size: " + fileDestino.length());
				}

				input.close();
			}else{
				Alert.show(context.getApplicationContext(), "Sem conex??o com a rede. Verifique sua conex??o e tente novamente");
				//Log.d("ITEM", "falha na conex??o");
			}

			long now = ((Calendar.getInstance().getTimeInMillis()) / (1000 * 60)); // minutos
			long expires = now + (this.timeout * 60);
			GravaPreferencias(expires);

		} catch (ConnectTimeoutException c) {
			Log.d("ITEM", "timeout na conex??o com o servidor: " + c.getMessage());
		} catch (MalformedURLException me) {
			Log.d("ITEM", me.getMessage());
		} catch (IOException ioe) {
			Log.d("ITEM", ioe.getMessage());
		}
	}

	/**
	 * Faz a valida????o do json
	 * O arquivo de cache s?? ?? gerado
	 * se passar nessa valida????o
	 * @param test
	 * @return
	 */
	public boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException e) {
			try {
				new JSONArray(test);
			} catch (JSONException ex) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isExpired() {
		long now = ((Calendar.getInstance().getTimeInMillis()) / (1000 * 60)); // minutos
		//Log.d("ITEM", "now: " + now);
		long expires = RecuperaPreferencias();
		//Log.d("ITEM", "expires: " + expires);
		return now > expires;
	}

	public void GravaPreferencias(long expires) {

		sharedPreferences = this.context.getSharedPreferences("PREFS_PRIVATE", Context.MODE_PRIVATE);

		Editor prefsPrivateEditor = sharedPreferences.edit();
		prefsPrivateEditor.putLong(fileNameFromUrl()+"_expires", expires);

		prefsPrivateEditor.commit();
	}

	public long RecuperaPreferencias() {

		sharedPreferences = this.context.getSharedPreferences("PREFS_PRIVATE", Context.MODE_PRIVATE);

		this.timeout = sharedPreferences.getLong(fileNameFromUrl()+"_expires", this.timeout);
		sharedPreferences = null;

		return timeout;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
	
	

}
