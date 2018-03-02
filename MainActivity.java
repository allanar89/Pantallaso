package cu.slam.pantallaso;

import java.io.File;
import java.io.InputStream;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.Surface;
import android.view.View;

public class MainActivity extends Activity {

	private static boolean DEBUG = true;
	private static String TAG = "ScreenCapture";
	private int mBytePerPixel = getBytePerPixel();
	private int mFrameBufferSize = this.mWidth * this.mHeight * this.mBytePerPixel;
	private int mHeight = getHeight();
	private int mWidth = getLineLength() / this.mBytePerPixel;
	AssetManager am;
	Surface superficie;
	boolean lista = false;
	private String ruta = Environment.getExternalStorageDirectory() + "/www/";

	static {
		System.loadLibrary("screencapture");
	}

	public static Bitmap bitmapRotate(Bitmap paramBitmap, int paramInt) {
		switch (paramInt) {
		default:
			return rotate(paramBitmap, 0);
		case 0:
			return rotate(paramBitmap, 0);
		case 1:
			return rotate(paramBitmap, 90);
		case 2:
			return rotate(paramBitmap, 180);
		case 3:
		}
		return rotate(paramBitmap, 270);
	}

	private native int getBytePerPixel();

	private native boolean getFrameBuffer(byte[] paramArrayOfByte);

	private native int getHeight();

	private native int getLineLength();

	public static Bitmap rotate(Bitmap paramBitmap, int paramInt) {
		Matrix localMatrix=null;
		if ((paramInt != 0) && (paramBitmap != null)) {
			localMatrix = new Matrix();
			localMatrix.setRotate(paramInt, paramBitmap.getWidth() / 2.0F,
					paramBitmap.getHeight() / 2.0F);
		}
		try {
			int i = paramBitmap.getWidth();
			int j = paramBitmap.getHeight();
			Bitmap localBitmap = Bitmap.createBitmap(paramBitmap, 0, 0, i, j,
					localMatrix, true);
			if (paramBitmap != localBitmap) {
				paramBitmap.recycle();
				paramBitmap = localBitmap;
			}
			return paramBitmap;
		} catch (OutOfMemoryError localOutOfMemoryError) {
			while (!DEBUG)
				;
			Log.d(TAG, "Out of Memmory");
		}
		return paramBitmap;
	}

	public boolean capture(String paramString, int paramInt) {
		return capture(paramString, Bitmap.Config.ARGB_8888,
				Bitmap.CompressFormat.JPEG, paramInt);
	}

	public boolean capture(String paramString, Bitmap.Config paramConfig,
			Bitmap.CompressFormat paramCompressFormat, int paramInt) {
		if (DEBUG)
			Log.d(TAG, "FrameBufferSize = " + this.mFrameBufferSize
					+ " Width = " + this.mWidth + " Height = " + this.mHeight
					+ " BytePerPixel =" + this.mBytePerPixel);
		byte[] arrayOfByte = new byte[this.mFrameBufferSize];
		if (!getFrameBufferData(arrayOfByte)) {
			if (DEBUG)
				Log.e(TAG, "Error on getting FrameBuffer");
			return false;
		}
		Bitmap localBitmap = Bitmap.createBitmap(this.mWidth, this.mHeight,
				paramConfig);
		localBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(arrayOfByte));
		try {
			FileOutputStream localFileOutputStream = new FileOutputStream(
					paramString);
			if (localBitmap.hasAlpha())
				localBitmap.setHasAlpha(false);
			bitmapRotate(localBitmap, paramInt).compress(paramCompressFormat,
					100, localFileOutputStream);
			localFileOutputStream.close();
			return true;
		} catch (Exception localException) {
			if (DEBUG)
				Log.d(TAG, "Fail to save as image.");
		}
		return false;
	}

	public boolean getFrameBufferData(byte[] paramArrayOfByte) {
		return getFrameBuffer(paramArrayOfByte);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// am = getAssets();
		// SelTarea();

		cargarPag();// copia el index.html hacia la carpeta con el servidor web
		capturarPantalla();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	void SelTarea() {
		try {
			ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> process = am.getRunningAppProcesses();
			/*
			 * for(RunningAppProcessInfo rapi: process){ Log.e("Tareas: ",
			 * ""+rapi.processName); }
			 */

			this.finish();
		} catch (Exception er) {
			er.printStackTrace();
		}
	}

	private void openScreenshot(File imageFile) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		Uri uri = Uri.fromFile(imageFile);
		intent.setDataAndType(uri, "image/*");
		startActivity(intent);
	}

	/* Carga el index.html desde assets y lo copia en la ruta del servidor */
	void cargarPag() {
		if (!lista) {
			try {
				AssetManager am = getAssets();
				InputStream is = am.open("index.html");
				ByteArrayBuffer bab = new ByteArrayBuffer(1);
				int i = 0;
				while ((i = is.read()) != -1)
					bab.append(i);
				File pag = new File(ruta, "index.html");
				FileOutputStream fos = new FileOutputStream(pag);
				fos.write(bab.toByteArray());
				fos.close();
				lista = true;
				Log.e("MSG", "OK cargar pag");
			} catch (Exception e) {
				Log.e("Exception", e.getMessage());
			}
		}
	}

	void capturarPantalla() {
		try {
			View ventana = getWindow().getDecorView().getRootView();
			Display disp = getWindowManager().getDefaultDisplay();

			ventana.setDrawingCacheEnabled(true);
			Bitmap bmp = Bitmap.createBitmap(ventana.getDrawingCache(true));
			ventana.setDrawingCacheEnabled(false);
			File cap = new File(ruta, "cap.jpg");
			if (cap.exists())
				cap.delete();			
			FileOutputStream fos = new FileOutputStream(cap);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();
			//openScreenshot(cap);
			Log.e("MSG", "OK captura");
		} catch (Exception e) {
			Log.e("MSG", "BAD captura " + e.getMessage());
			e.printStackTrace();
		}
	}
}
