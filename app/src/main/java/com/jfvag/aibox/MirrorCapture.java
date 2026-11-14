package com.jfvag.aibox;
import android.app.*;import android.content.*;import android.graphics.*;import android.hardware.display.*;import android.media.*;import android.media.projection.*;import android.os.*;import android.util.*;import java.io.*;
/** User-consented, local-network-only low-frame-rate preview. No microphone/audio capture. */
public final class MirrorCapture {
 private static MediaProjection projection;private static VirtualDisplay display;private static ImageReader reader;private static int width,height;private static volatile byte[] latest;private static HandlerThread captureThread;private static long lastEncodeMs;
 public static synchronized void start(Activity activity,int resultCode,Intent data){stop();try{
  MediaProjectionManager manager=(MediaProjectionManager)activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  projection=manager.getMediaProjection(resultCode,data);if(projection==null)return;
  DisplayMetrics dm=new DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
  width=Math.max(1,dm.widthPixels);height=Math.max(1,dm.heightPixels);
  // Reduce network and CPU load; retain original aspect ratio for touch coordinates.
  int outW=Math.min(960,width);int outH=Math.max(1,Math.round((float)height*outW/width));
  reader=ImageReader.newInstance(outW,outH,PixelFormat.RGBA_8888,2);
  captureThread=new HandlerThread("JF Mirror JPEG",android.os.Process.THREAD_PRIORITY_BACKGROUND);captureThread.start();lastEncodeMs=0;
  reader.setOnImageAvailableListener(r->{android.media.Image im=null;try{im=r.acquireLatestImage();if(im==null)return;long now=SystemClock.uptimeMillis();if(now-lastEncodeMs<95)return;lastEncodeMs=now;
    android.media.Image.Plane plane=im.getPlanes()[0];java.nio.ByteBuffer buffer=plane.getBuffer();int stride=plane.getRowStride(),pixel=plane.getPixelStride();
    Bitmap padded=Bitmap.createBitmap(stride/pixel,im.getHeight(),Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(buffer);
    Bitmap cropped=Bitmap.createBitmap(padded,0,0,im.getWidth(),im.getHeight());ByteArrayOutputStream bytes=new ByteArrayOutputStream();cropped.compress(Bitmap.CompressFormat.JPEG,65,bytes);latest=bytes.toByteArray();cropped.recycle();padded.recycle();
  }catch(Exception e){Log.w("JF-Mirror","frame",e);}finally{if(im!=null)im.close();}},new Handler(captureThread.getLooper()));
  projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){MirrorCapture.stop();}},new Handler(Looper.getMainLooper()));
  display=projection.createVirtualDisplay("JF Mirror Preview",outW,outH,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,null);
 }catch(Exception e){Log.e("JF-Mirror","start failed",e);stop();}}
 public static synchronized void stop(){latest=null;if(display!=null){display.release();display=null;}if(reader!=null){reader.close();reader=null;}if(projection!=null){MediaProjection p=projection;projection=null;try{p.stop();}catch(Exception ignored){}}if(captureThread!=null){captureThread.quitSafely();captureThread=null;}}
 public static byte[] frame(){return latest;}public static boolean active(){return projection!=null&&display!=null;}public static int width(){return width;}public static int height(){return height;}
}
