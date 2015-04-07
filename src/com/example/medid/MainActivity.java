package com.example.medid;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {
	
	private static final String TAG = "MyActivity";
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private IntentFilter ndef;
	private IntentFilter[] mFilters;
	private String[][] mTechLists;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mFilters = new IntentFilter[] {
                ndef,
        };
		
        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] { MifareClassic.class.getName() } };
       
        Intent intent = getIntent();
        
        resolveIntent(intent); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	
	void resolveIntent(Intent intent) {
        // 1) Parse the intent and get the action that triggered this intent
        String action = intent.getAction();
        // 2) Check if it was triggered by a tag discovered interruption.
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            //  3) Get an instance of the TAG from the NfcAdapter
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            // 4) Get an instance of the Mifare classic card from this TAG intent
            MifareClassic mfc = MifareClassic.get(tagFromIntent);
            byte[] data;
           
            try {       //  5.1) Connect to card
            mfc.connect();
            boolean auth = false;
            String cardData = null;
            // 5.2) and get the number of sectors this card has..and loop thru these sectors
            int secCount = mfc.getSectorCount();
            int bCount = 0;
            int bIndex = 0;
            for(int j = 0; j < secCount; j++){
                // 6.1) authenticate the sector
                auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT);
                if(auth){
                    // 6.2) In each sector - get the block count
                    bCount = mfc.getBlockCountInSector(j);
                    bIndex = 0;
                    for(int i = 0; i < bCount; i++){
                        bIndex = mfc.sectorToBlock(j);
                        // 6.3) Read the block
                        data = mfc.readBlock(bIndex);   
                        // 7) Convert the data into a string from Hex format.               
                        Log.i(TAG, getHexString(data, data.length));
                        bIndex++;
                    }
                }else{ // Authentication failed - Handle it
                   
                }
            }   
        }catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
    }// End of method
	
        
}	
	
	@Override
    public void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

	 @Override
     public void onNewIntent(Intent intent) {
         Log.i("Foreground dispatch", "Discovered tag with intent: " + intent);
         resolveIntent(intent);           
     }
	 
	 @Override
     public void onPause() {
         super.onPause();
         mAdapter.disableForegroundDispatch(this);
     }

	 public static String getHexString(byte[] b, int length) {
	        String result = "";

	        for (int i = 0; i < length; i++) {
	            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	            result += " ";
	        }

	        return result.toUpperCase();
	    }
	
}