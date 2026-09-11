package com.sisaimpresion.cotizador;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.*;

public class MainActivity extends Activity {
    private WebView webView; private ValueCallback<Uri[]> fileCallback; private Uri cameraUri; private static final int FILE_REQ=42;
    @Override public void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);webView=new WebView(this);setContentView(webView);WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);s.setBuiltInZoomControls(false);s.setDisplayZoomControls(false);webView.addJavascriptInterface(new NativeBridge(),"AndroidBridge");webView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if("file".equals(u.getScheme()))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}});webView.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=cb;Intent pick=new Intent(Intent.ACTION_GET_CONTENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");Intent cam=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);try{File dir=new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"captures");dir.mkdirs();File f=new File(dir,"sisa_"+System.currentTimeMillis()+".jpg");cameraUri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".provider",f);cam.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){cam=null;}Intent chooser=Intent.createChooser(pick,"Seleccionar imagen del producto");if(cam!=null)chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{cam});startActivityForResult(chooser,FILE_REQ);return true;}});webView.loadUrl("file:///android_asset/index.html");}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==FILE_REQ&&fileCallback!=null){Uri[] result=null;if(resultCode==RESULT_OK){if(data!=null&&data.getData()!=null)result=new Uri[]{data.getData()};else if(cameraUri!=null)result=new Uri[]{cameraUri};}fileCallback.onReceiveValue(result);fileCallback=null;cameraUri=null;}}
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
    public class NativeBridge{
      @JavascriptInterface public void savePdf(String b64,String filename){runOnUiThread(()->{try{byte[] data=Base64.decode(b64,Base64.DEFAULT);saveToDownloads(data,filename);Toast.makeText(MainActivity.this,"PDF guardado en Descargas",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(MainActivity.this,"No se pudo guardar el PDF",Toast.LENGTH_LONG).show();}});}
      @JavascriptInterface public void sharePdf(String b64,String filename,String text,String target,String email){runOnUiThread(()->{try{byte[] data=Base64.decode(b64,Base64.DEFAULT);File dir=new File(getCacheDir(),"shared");dir.mkdirs();File f=new File(dir,filename);try(FileOutputStream os=new FileOutputStream(f)){os.write(data);}Uri uri=FileProvider.getUriForFile(MainActivity.this,getPackageName()+".provider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.putExtra(Intent.EXTRA_TEXT,text);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);if(email!=null&&!email.isEmpty())i.putExtra(Intent.EXTRA_EMAIL,new String[]{email});if("whatsapp".equals(target)){if(isInstalled("com.whatsapp"))i.setPackage("com.whatsapp");else if(isInstalled("com.whatsapp.w4b"))i.setPackage("com.whatsapp.w4b");}startActivity(Intent.createChooser(i,"Compartir cotización"));}catch(Exception e){Toast.makeText(MainActivity.this,"No se pudo compartir el PDF",Toast.LENGTH_LONG).show();}});}
    }
    private boolean isInstalled(String p){try{getPackageManager().getPackageInfo(p,0);return true;}catch(Exception e){return false;}}
    private void saveToDownloads(byte[] data,String filename)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,filename);v.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/SISA Cotizador");Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IOException();try(OutputStream os=getContentResolver().openOutputStream(u)){os.write(data);}}
}
