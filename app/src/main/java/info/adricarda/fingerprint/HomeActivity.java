package info.adricarda.fingerprint;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HomeActivity extends AppCompatActivity {

    static final String MKEY = "masterKey:";
    static final String AESKEY = "encryptedAESKey:";
    SharedPreferences pref;
    SharedPreferences.Editor prefEditor;
    private byte iv[];
    List<String> itemAliases;
    ListView listView;
    ItemsAdapter listAdapter;
    KeyStore keyStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = this.getPreferences(Context.MODE_PRIVATE);
        prefEditor = pref.edit();

        super.onCreate(savedInstanceState);
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
        }
        catch(Exception e) {}
        refreshKeys();

        createKeys();

        setContentView(R.layout.list_entry);
        View listHeader = View.inflate(this, R.layout.list_entry_header, null);

        listView = (ListView) findViewById(R.id.listView);
        listView.addHeaderView(listHeader);
        listAdapter = new ItemsAdapter(this, R.id.keyAlias);
        listView.setAdapter(listAdapter);

        Button insertItem = (Button) listHeader.findViewById(R.id.generateItem);
        insertItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogInsert();
            }
        });
    }
    //restart authentication process when app is restarted
    @Override
    protected void onRestart(){
        super.onRestart();
        finish();
        Intent intent = new Intent(getApplicationContext(), FingerprintActivity.class);
        startActivity(intent);
    }

    //create AES key and store it safely using RSA(storeSecretKey function)
    public void createKeys() {
        try {
            if (!keyStore.containsAlias(MKEY)) {
                final int outputKeyLength = 256;
                SecureRandom secureRandom = new SecureRandom();
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                keyGenerator.init(outputKeyLength, secureRandom);
                SecretKey key = keyGenerator.generateKey();
                storeSecreteKey(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //store AES key encrypted with RSA public key. RSA keys are stored in a keystore
    public void storeSecreteKey(SecretKey key){
        String alias = MKEY;
        try {
            //generate RSA keys
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA","AndroidKeyStore");

            generator.initialize(new KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .build());
            KeyPair keyPair = generator.generateKeyPair();

            //encrypt AES key
            PublicKey publicKey = keyStore.getCertificate(MKEY).getPublicKey();
            Cipher input = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
            input.init(Cipher.ENCRYPT_MODE, publicKey);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, input);
            cipherOutputStream.write(key.getEncoded());
            cipherOutputStream.close();
            //save encrypted AES key in SharedPreferences
            byte [] vals = outputStream.toByteArray();
            prefEditor.putString( AESKEY, Base64.encodeToString(vals, Base64.DEFAULT));
            prefEditor.commit();

        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
    }
    //This method shows the alias of the key stored
    private void refreshKeys() {
        itemAliases = new ArrayList<>();
        try {
            Map<String, ?> allEntries = pref.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                //does not show AES key entry
                if (!entry.getKey().contains(":"))
                    itemAliases.add(entry.getKey());
            }
        }
        catch(Exception e) {}

        if(listAdapter != null)
            listAdapter.notifyDataSetChanged();
    }

    public void showDialogItem(String alias){
        String textUser = pref.getString(alias, "" );
        String cipherText = pref.getString(alias+":"+textUser, "");

        String clearText = decryptItem(cipherText, alias);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        View promptView = inflater.inflate(R.layout.dialog_item, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(HomeActivity.this);
        alertDialogBuilder.setView(promptView);
        final TextView dominio = (TextView) promptView.findViewById(R.id.dominio);
        final TextView user = (TextView) promptView.findViewById(R.id.user);
        final TextView password = (TextView) promptView.findViewById(R.id.password);

        dominio.setText(alias);
        user.setText(textUser);
        password.setText(clearText);

        alertDialogBuilder
                .setCancelable(true)
                .show();
    }

    public void showDialogInsert(){
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        View promptView = inflater.inflate(R.layout.dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(HomeActivity.this);
        alertDialogBuilder.setView(promptView);
        final TextView dominio = (TextView) promptView.findViewById(R.id.dominio);
        final TextView user = (TextView) promptView.findViewById(R.id.user);
        final TextView password = (TextView) promptView.findViewById(R.id.password);

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("Insert", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String textDominio = dominio.getText().toString();
                        String textUser = user.getText().toString();
                        String textPassword = password.getText().toString();
                        if (pref.contains(textDominio)) {
                            Toast.makeText(getApplicationContext(), "Domain already present", Toast.LENGTH_LONG).show();
                            return;
                        }
                        encryptAndInsertData(textDominio, textUser, textPassword);
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .create()
                .show();
    }

    private void encryptAndInsertData(String textDomain, String textUser, String textPassword) {

        try {
            //get encrypted AES key from Preferences
            String encryptedAesPassword = pref.getString(AESKEY, "");
            byte AESKey[] = Base64.decode(encryptedAesPassword, Base64.DEFAULT);
            //get RSA private key in order to decrypt AES key
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(MKEY, null);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(AESKey), cipher);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }
            //get secret AES key from the plaintext obtained with RSA decryption
            SecretKey originalAESKey = new SecretKeySpec(bytes, 0, bytes.length, "AES");

            //now use AES key to encrypt user's data
            Cipher c = null;
            Cipher c2 = null;

            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c2 = Cipher.getInstance("AES/ECB/PKCS5Padding");

            SecureRandom random = new SecureRandom();
            iv = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivectorSpecv = new IvParameterSpec(iv);
            c.init(Cipher.ENCRYPT_MODE, originalAESKey, ivectorSpecv);
            c2.init(Cipher.ENCRYPT_MODE, originalAESKey);

            byte input[] = textPassword.getBytes();
            //store username, IV(needed for decryption later) and encrypted password
            prefEditor.putString(textDomain, textUser);
            prefEditor.putString(textDomain + ":IV", Base64.encodeToString(c2.doFinal(iv), Base64.DEFAULT));
            prefEditor.putString(textDomain + ":" + textUser, Base64.encodeToString(c.doFinal(input), Base64.DEFAULT));
            prefEditor.commit();
            refreshKeys();


        } catch (Exception e) {
            Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
        }
    }

    public void deleteKey(final String alias) {
        String user = pref.getString(alias, "");
        prefEditor.remove(alias);
        prefEditor.remove(alias+":"+user);
        prefEditor.remove(alias+"IV");
        prefEditor.commit();
        refreshKeys();
    }

    private String decryptItem(String cipherText, String domain){

        try {
            String encryptedAesPassword = pref.getString(AESKEY, "");
            String IV = pref.getString(domain+":IV", "");

            byte AESKey[] = Base64.decode(encryptedAesPassword, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(MKEY, null);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(new ByteArrayInputStream(AESKey), cipher);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte) nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }
            //rebuild AES key from bytes
            SecretKey originalAESKey = new SecretKeySpec(bytes, 0, bytes.length, "AES");
            //decrypt user data
            Cipher c = null;
            Cipher c2 = null;

            c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c2 = Cipher.getInstance("AES/ECB/PKCS5Padding");

            c2.init(Cipher.DECRYPT_MODE, originalAESKey);
            byte encryptedIv[] = Base64.decode(IV, Base64.DEFAULT);
            byte clearIV[] = c2.doFinal(encryptedIv);

            IvParameterSpec ivectorSpecv = new IvParameterSpec(clearIV);

            c.init(Cipher.DECRYPT_MODE, originalAESKey, ivectorSpecv);
            byte input[] = Base64.decode(cipherText, Base64.DEFAULT);
            byte clearText[] = c.doFinal(input);
            return new String(clearText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public class ItemsAdapter extends ArrayAdapter<String> {

        public ItemsAdapter(Context context, int textView) {
            super(context, textView);
        }

        @Override
        public int getCount() {
            return itemAliases.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.list_item, parent, false);

            final TextView keyAlias = (TextView) itemView.findViewById(R.id.keyAlias);
            keyAlias.setText(itemAliases.get(position));

            final Button deleteButton = (Button) itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteKey(keyAlias.getText().toString());
                }
            });

            final Button showButton = (Button) itemView.findViewById(R.id.showButton);
            showButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDialogItem(keyAlias.getText().toString());
                }
            });
            return itemView;
        }

        @Override
        public String getItem(int position) {
            return itemAliases.get(position);
        }

    }

}
