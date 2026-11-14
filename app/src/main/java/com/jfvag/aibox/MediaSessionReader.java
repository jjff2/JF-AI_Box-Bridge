package com.jfvag.aibox;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** The user must explicitly enable notification access on the AI Box. */
public final class MediaSessionReader extends NotificationListenerService {
    static boolean hasAccess(Context context) {
        String list=Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return list!=null && list.contains(context.getPackageName()+"/");
    }
    public static JSONObject read(Context context) {return read(context,true);}
    static JSONObject readState(Context context){return read(context,false);}
    private static JSONObject read(Context context,boolean withArtwork) {
        JSONObject result=new JSONObject();
        try {
            boolean enabled=hasAccess(context);
            result.put("permission",enabled);
            result.put("available",false);
            if(!enabled){result.put("reason","notification_access_required");return result;}
            MediaSessionManager manager=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if(manager==null){result.put("reason","media_session_unavailable");return result;}
            ComponentName component=new ComponentName(context,MediaSessionReader.class);
            List<MediaController> sessions=manager.getActiveSessions(component);
            MediaController best=null;
            int bestScore=-1;
            for(MediaController session:sessions){
                PlaybackState state=session.getPlaybackState();
                MediaMetadata meta=session.getMetadata();
                if(state==null && meta==null)continue;
                int score=meta!=null?1:0;
                if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)score+=10;
                if(score>bestScore){bestScore=score;best=session;}
            }
            if(best==null){result.put("reason","no_active_media_session");return result;}
            MediaMetadata meta=best.getMetadata();
            PlaybackState state=best.getPlaybackState();
            result.put("available",true);
            result.put("packageName",best.getPackageName());
            result.put("title",meta!=null?string(meta,MediaMetadata.METADATA_KEY_TITLE):"");
            result.put("artist",meta!=null?string(meta,MediaMetadata.METADATA_KEY_ARTIST):"");
            result.put("album",meta!=null?string(meta,MediaMetadata.METADATA_KEY_ALBUM):"");
            result.put("playing",state!=null && state.getState()==PlaybackState.STATE_PLAYING);
            result.put("playbackState",state==null?-1:state.getState());
            // Compact thumbnail. Some apps (e.g. video DRM) do not expose artwork.
            Bitmap cover=null;
            if(meta!=null){
                cover=meta.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if(cover==null)cover=meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if(cover==null)cover=meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            }
            if(withArtwork && cover!=null){
                int w=cover.getWidth(),h=cover.getHeight();
                if(w>0&&h>0){
                    double scale=Math.min(1.0,160.0/Math.max(w,h));
                    Bitmap reduced=scale<1?Bitmap.createScaledBitmap(cover,Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),true):cover;
                    ByteArrayOutputStream buf=new ByteArrayOutputStream();
                    if(reduced.compress(Bitmap.CompressFormat.JPEG,68,buf)&&buf.size()<65000){
                        result.put("artwork","data:image/jpeg;base64,"+Base64.encodeToString(buf.toByteArray(),Base64.NO_WRAP));
                    }
                    if(reduced!=cover)reduced.recycle();
                }
            }
        }catch(SecurityException ex){
            try{result.put("permission",false);result.put("available",false);result.put("reason","permission_denied");}catch(Exception ignored){}
        }catch(Exception ex){
            try{result.put("available",false);result.put("reason","media_unavailable");}catch(Exception ignored){}
        }
        return result;
    }
    private static String string(MediaMetadata m,String key){String s=m.getString(key);return s==null?"":s;}
}
