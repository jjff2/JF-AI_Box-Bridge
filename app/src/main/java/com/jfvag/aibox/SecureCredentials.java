package com.jfvag.aibox;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-local credentials, encrypted by Android Keystore; never sent to the dashboard. */
final class SecureCredentials {
 private static final String ALIAS="JF_EXLAP_LOCAL_V1";
 private static SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  SecretKey k=(SecretKey)ks.getKey(ALIAS,null);if(k!=null)return k;
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
  return g.generateKey();
 }
 static void save(Context ctx,String name,String value) throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
  byte[] encrypted=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
  ctx.getSharedPreferences("JF_EXLAP_PRIVATE",Context.MODE_PRIVATE).edit()
   .putString(name+"_iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP))
   .putString(name+"_data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
 }
 static String read(Context ctx,String name) {
  try {
   android.content.SharedPreferences p=ctx.getSharedPreferences("JF_EXLAP_PRIVATE",Context.MODE_PRIVATE);
   String iv=p.getString(name+"_iv",null),data=p.getString(name+"_data",null);
   if(iv==null||data==null)return "";
   Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.DEFAULT)));
   return new String(c.doFinal(Base64.decode(data,Base64.DEFAULT)),StandardCharsets.UTF_8);
  }catch(Exception e){return "";}
 }
}
