package org.bughardy.nfcult;

//Java Package
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
//Android Package
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v13.app.FragmentPagerAdapter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class MainActivity extends Activity implements ActionBar.TabListener {

	NfcAdapter adapter;
	PendingIntent pendingIntent;
	IntentFilter writeTagFilters[];
	boolean writeMode;
	static Tag mytag;
	Context ctx;
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		ctx = this;
		adapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
		writeTagFilters = new IntentFilter[] { tagDetected };
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        // Create the adapter that will return a fragment for each of the five
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }
    
    //Method to convert an Hex String to a Byte Array.
    public static byte[] hexStringToByteArray(String s) {
    	
        int len = s.length();
        byte[] data = new byte[len/2];
        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    //Method to convert a Byte Array to an Hex String.
    public static String byteArrayToHexString(byte[] bytes) {
    	
    	char[] hexChars = new char[bytes.length*2];
    	int k;
    	for(int i=0; i < bytes.length; i++) {
    		k = bytes[i] & 0xFF;
    		hexChars[i*2] = hexArray[k>>>4];
    		hexChars[i*2 + 1] = hexArray[k & 0x0F];
    	}
    return new String(hexChars);
    }
    
    //Returns a list of saved dumps.
    private ArrayList<String> getDumpList() {
    	
    	String[] savedDumps = {""};
    	ArrayList<String> dumps = new ArrayList<String>();
    	savedDumps = getApplicationContext().fileList();
    	for(int i=0; i<savedDumps.length; i++) {
    		try {
    			if(savedDumps[i].substring(savedDumps[i].lastIndexOf(".")).equals(".mfd")) {
    				dumps.add(savedDumps[i]);
    		}
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	return dumps;
    }

    //Increase OTP of +1 and write the modified dump into a new temp file. Both files will be removed at the end.
    private void increaseOTP(String filename) {
 	   	
    	try {
 	   		FileInputStream fis;
 	   		fis = openFileInput(filename);
 	 	    InputStreamReader isr = new InputStreamReader(fis);
 	 	    BufferedReader bufferedReader = new BufferedReader(isr);
 	   		FileOutputStream fos;
 	   		fos = openFileOutput("finOTP", Context.MODE_PRIVATE);
 	   		String line;
 	   		int i = 0;
			while ((line = bufferedReader.readLine()) != null) {
				if(i == 3){
					int temp = Integer.parseInt(line, 16);
					temp--;
					String newOTP = Integer.toString(temp, 16)+"\n";
					fos.write(newOTP.getBytes());
				}
				else {
					line = line+"\n";
					fos.write(line.getBytes());			
				}
				i++;
			}
			fis.close();
			fos.close();
			File file1 = new File(this.getFilesDir(), "tempId.mfd");			
			file1.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //Read all pages of Mifare UL and writes them in a file. One line per page. ( No compatibility yet wih nfc-tools dumps )
    private void readUL(String fileName) throws IOException {
		
			MifareUltralight ultralight = MifareUltralight.get(mytag);
			ultralight.connect();
			String[] page = new String[30];
			try {
				for(int i = 0; i < 16; i++) {
					page[i] = byteArrayToHexString(ultralight.readPages(i)).substring(0, 8) + "\n";
					FileOutputStream fos = openFileOutput(fileName+".mfd", Context.MODE_APPEND);
					fos.write(page[i].getBytes());
					fos.close();
				}
				ultralight.close();
		} catch(Exception e) { e.printStackTrace(); }
    }

    //Read a previous saved dump and write it on the Mifare UL. If write is okay returns green line in textview, otherwise it returns red line. 
    private void getDump(String file, int toShow) throws IOException {
    	
    	   ArrayList<Spannable> pagesWritten = new ArrayList<Spannable>();
    	   AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	   FileInputStream fis = openFileInput(file);
    	   InputStreamReader isr = new InputStreamReader(fis);
    	   BufferedReader bufferedReader = new BufferedReader(isr);
		   MifareUltralight ultralight = MifareUltralight.get(mytag);
		   ultralight.connect();
    	   String line;
    	   int i = 0;
    	   boolean error = false;
   		   TextView text = (TextView)findViewById(R.id.textView3);
		   text.setText("");
   		   while ((line = bufferedReader.readLine()) != null) {
   	    	   StringBuilder sb = new StringBuilder();
   	    	   error = false;
   			   try {   			   
   			   ultralight.writePage(i, hexStringToByteArray(line));
   			   } catch(Exception e) { 
   				   error = true;
   	   			   if ( ultralight.isConnected() ) {
   	   				   ultralight.close();
   	   			   }
   	   			   ultralight.connect();
   			   }
   			   i++;
   			   if(toShow==1) {
   				   sb.append("Page ").append(i).append(" -> " + getString(R.string.tab)).append(line).append("\n");
   				   Spannable WordToSpan = new SpannableString(sb);
   				   if(error) {
   					   WordToSpan.setSpan(new ForegroundColorSpan(Color.RED), 0, WordToSpan.length(), 0);
   				   } else {
   					   WordToSpan.setSpan(new ForegroundColorSpan(Color.rgb(04, 82, 27)), 0, WordToSpan.length(), 0);
   				   }
   				   pagesWritten.add(WordToSpan);
   			   }
   		   }
   		   if(toShow==1) {
   			   CharSequence[] cs = pagesWritten.toArray(new CharSequence[pagesWritten.size()]);
   			   String message = "";
   			   for(int j=0; j<16; j++){
   				   message = message + cs[j].toString();
   			   }
   			   builder.setMessage(message)
   			   .setTitle("Results:")
   			   .show();
   		   }
   		   if( ultralight.isConnected() ) {
   			   ultralight.close();
   		   }
   	}
    
    //Fix broken ultralight UID, it will write a custom UID ( 04666666 ) with the UL Id 0x04
    private boolean fixUID() throws IOException {
    	byte uidPacket[] = {(byte) 0xa2, 0x00, 0x04, 0x66, 0x66, 0x66}; // RANDOM UID -> 04 66 66 66 
    	boolean success;
    	try {
    		NfcA brokenTag = NfcA.get(mytag);
    		brokenTag.connect();
    		brokenTag.transceive(uidPacket);
    		brokenTag.close();
    		success = true;
    	}
    	catch(Exception e){
    		success = false;
    	}
    	return success;
    	
    	
    }
	
    
    //Set OTP sector in read-only mode
    private void crack() {
		
		MifareUltralight ultralight = MifareUltralight.get(mytag);
		boolean success=false;
		try {
            ultralight.connect();
            ultralight.writePage(2 ,new byte[] {0x04, 0x11,  (byte) 0xfa, 0x00});
            success=true;
        } catch (IOException e) {
        	e.printStackTrace();
        } finally {
            try {
                ultralight.close();
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
		if(success){
			Toast.makeText(ctx, "OTP sector locked", Toast.LENGTH_LONG ).show();
		} else {
			Toast.makeText(ctx, "Error", Toast.LENGTH_LONG ).show();
		}
        
	}
    
    //Read the actual timestamp saved on the ticket and returns it both in TextView
    private void readThisMoment(){
	    try {
	    	MifareUltralight ultralight = MifareUltralight.get(mytag);
	    	ultralight.connect();
	    	FileInputStream fis = openFileInput("dateDefault");
	    	InputStreamReader isr = new InputStreamReader(fis);
	    	BufferedReader bufferedReader = new BufferedReader(isr);
	    	String line;
	    	int[] page = {0,0};
	    	int i = 0,j = 0;
	    	boolean date = false;
	    	int[] defDate = {0,0,0};
	    	while ((line = bufferedReader.readLine()) != null) {
	    		System.out.println(line);
	    		if(line.equals("DATE")) {
	    			date = true;
	    		}
	    		else {
	    			if(date){
	        			int temp =  Integer.parseInt(line);
	        			defDate[j] = temp;
	        			j++;
	    			}
	    			if(date == false){
	    				page[i] = Integer.parseInt(line);
	    				i++;
	    			}
	    		}
	    		
	    	}
	    	byte[] page10 = ultralight.readPages(page[0]);
	    	ByteBuffer pageByte = ByteBuffer.wrap(page10);
	    	long result = pageByte.getInt() / 256;
	    	int ridesResult = pageByte.getInt();
	    	String rides = Integer.toBinaryString(ridesResult).substring(16);
	    	int ridesLeft = rides.indexOf("0");
	    	if (ridesLeft != -1) {
	    		ridesLeft = rides.substring(ridesLeft).length();
	    	}
	    	else { ridesLeft = 0; }
		    Calendar cal = Calendar.getInstance();
		    cal.set(defDate[0], defDate[1], defDate[2], 0, 0);
		    long old = cal.getTimeInMillis()/60000;
		    Calendar cal2 = Calendar.getInstance();
		    long finalTime = result*60000 + old*60000;
		    cal2.setTimeInMillis(finalTime);
		    StringBuilder output = new StringBuilder().append("Last stamp: ").append(pad(cal2.get(cal2.DAY_OF_MONTH))).append("-").append(pad(cal2.get(cal2.MONTH)+1)).append("-").append(pad(cal2.get(cal2.YEAR))).append(" - At: ").append(pad(cal2.get(cal2.HOUR_OF_DAY))).append(":").append(pad(cal2.get(cal2.MINUTE)));
		    TextView text = (TextView)findViewById(R.id.textView2);
		    text.setText(output);
		    ultralight.close();
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
        	Toast.makeText(ctx, "Error reading the tag, try again..", Toast.LENGTH_SHORT).show();
	    }	    

  }
  
    //Calculate the timestamp and writes it on the user specified page(s).
    private void writeThisMoment(int hour, int min) throws IOException{
    	
    	FileInputStream fis = openFileInput("dateDefault");
    	InputStreamReader isr = new InputStreamReader(fis);
    	BufferedReader bufferedReader = new BufferedReader(isr);
    	int[] page = {0,0};
    	String line;
    	int i = 0,j = 0;
    	boolean date = false;
    	int[] defDate = {0,0,0};
    	while ((line = bufferedReader.readLine()) != null) {
    		if(line.equals("DATE")) {
    			date = true;
    		}
    		else {
    			if(date){
        			int temp =  Integer.parseInt(line);
        			defDate[j] = temp;
        			j++;
    			}
    			if(date == false){
    				page[i] = Integer.parseInt(line);
    				i++;
    			}
    		}
    	}
    	Calendar cal = Calendar.getInstance(); 
    	int day = cal.get(Calendar.DAY_OF_MONTH);
	    int month = cal.get(Calendar.MONTH);
	    int year = cal.get(Calendar.YEAR);
    	cal.set(year, month, day, hour, min);
    	long now = cal.getTimeInMillis()/60000;
	    cal.set(defDate[0], defDate[1], defDate[2], 0, 0);
		int minutes = (int)(now - (cal.getTimeInMillis()/60000));
	    byte[] data = new byte[]{(byte)((minutes/65536) & 0x00ff) , (byte)((minutes/256) & 0x00ff) , (byte)(minutes & 0x0000ff) , 0x00};
	    MifareUltralight ultralight = MifareUltralight.get(mytag);
	    try {
          ultralight.connect();
          ultralight.writePage(10,data);
          ultralight.writePage(12,data);
          Toast.makeText(ctx, "Ticket stamped, timestamp set to:" + pad(day) + "/" + pad(month+1) + "/" + year + "  " + hour + ":" + pad(min), Toast.LENGTH_LONG ).show();
      } catch (IOException e) {
      	  Toast.makeText(ctx, "Error writing the tag, try again..", Toast.LENGTH_LONG ).show();
      } finally {
    	  try {
              ultralight.close();
          } catch (IOException e) {
          	  e.printStackTrace();
          	  Toast.makeText(ctx, "Error writing the tag, try again..", Toast.LENGTH_LONG ).show();
          }
      }		
  }
    
  private String pad(long result){
	
	  String toreturn = "";
		if(result<10)
			toreturn= "0"+result;
		else
			toreturn+= result;
		return toreturn;
	}
   
@Override
protected void onNewIntent(Intent intent){
	if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
		mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);    
		String[] techlist = mytag.getTechList();
		if (techlist.length != 0){
			boolean validtech=false;
			int i=0;
			while(i<techlist.length){
				if(techlist[i].equals("android.nfc.tech.MifareUltralight")){
					validtech=true;
					break;
				}
				i++;
			}
			if (validtech){
				Toast.makeText(ctx, "Tag found", Toast.LENGTH_LONG ).show();
				
			}
			else {
				Toast.makeText(ctx, "Tag found, but doesn't seems to be a Mifare UL\nBroken UID?", Toast.LENGTH_LONG ).show();
			}
		}
		else {
			Toast.makeText(ctx, "ERROR", Toast.LENGTH_LONG ).show();
		}
		
	}
}

@Override
public void onPause(){
	super.onPause();
	WriteModeOff();
}

@Override
public void onResume(){
	
	super.onResume();
	WriteModeOn();	
	Intent intent = getIntent();
	if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())){
		mytag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
		String[] techlist = mytag.getTechList();
		if (techlist.length != 0){
			boolean validtech=false;
			int i=0;
			while(i<techlist.length){
				if(techlist[i].equals("android.nfc.tech.MifareUltralight")){
					validtech=true;
					break;
				}
				i++;
			}
			if (validtech){
				Toast.makeText(ctx, "Tag found", Toast.LENGTH_LONG ).show();
			}
			else {
				Toast.makeText(ctx, "Tag found, but doesn't seems to be a Mifare UL", Toast.LENGTH_LONG ).show();
			}
		}
		else {
			Toast.makeText(ctx, "ERROR", Toast.LENGTH_LONG ).show();
		}
		intent = intent.setAction(null);
		}
	}

	private void WriteModeOn(){
		
		writeMode = true;
		adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
	}

	private void WriteModeOff(){
		
		writeMode = false;
		adapter.disableForegroundDispatch(this);
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    	
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    	
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
        	
            return 5; //Actually there are 5 pages
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                 	return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 4:
                	return getString(R.string.title_section4).toUpperCase(l);
                case 3:
                	return "Custom Edit".toUpperCase(l);
            }
            return null;
        }
    }
    
    public static class PlaceholderFragment extends Fragment {
    	
		MainActivity activity = (MainActivity) getActivity();
		/*Calendar cal0 = Calendar.getInstance();
		int Year = cal0.get(Calendar.YEAR);
		int Month = cal0.get(Calendar.MONTH);
		int Day = cal0.get(Calendar.DAY_OF_MONTH);
		int hour = cal0.get(Calendar.HOUR_OF_DAY);
		int min = cal0.get(Calendar.MINUTE);
		boolean Read = false;*/ //to check if possible to delete 
        private static final String ARG_SECTION_NUMBER = "section_number";
        
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        	
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
    
        	int page =  getArguments().getInt(ARG_SECTION_NUMBER);
     	
        	// Page for the LOCK Attack
        	if( page == 1 ) {
                View rootView = inflater.inflate(R.layout.fragment_one, container, false);
                Button btnOTP = ( Button ) rootView.findViewById(R.id.button1);
        		btnOTP.setOnClickListener(new View.OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				MainActivity activity = (MainActivity) getActivity();
            			try {
        					activity.crack();
        				} catch(Exception e) {
							Toast.makeText(activity, "There was an during the operation..", Toast.LENGTH_SHORT).show();
        				}
        			}
        		});
                return rootView;
        	}
        	
        	// Page for the TIME Attack 
        	if( page == 2 ) {
        		
                final View rootView = inflater.inflate(R.layout.fragment_two, container, false);
                //Defining actions for Read Button
                Button buttonRead = ( Button ) rootView.findViewById(R.id.button6);
        		buttonRead.setOnClickListener(new View.OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				MainActivity activity = (MainActivity) getActivity();
        		 		activity.readThisMoment();
        			}
        		});
        		//Defining actions for Write Button
                Button buttonWrite = ( Button ) rootView.findViewById(R.id.button7);
                final TimePicker timePicker = (TimePicker) rootView.findViewById(R.id.timePicker1);
        		buttonWrite.setOnClickListener(new View.OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				MainActivity activity = (MainActivity) getActivity();
        				try {
        					activity.writeThisMoment(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
        				} catch(Exception e) {
        					e.printStackTrace();
        					Toast.makeText(activity, "Error writing the tag", Toast.LENGTH_SHORT).show();
        				}
        			}
        		});
        		//Defining actions for Setting Button
        		Button buttonSetting = ( Button ) rootView.findViewById(R.id.button5);
        		buttonSetting.setOnClickListener(new View.OnClickListener() {
        			public void onClick(View v) {
        				final Dialog dialog = new Dialog(getActivity());
        				dialog.setContentView(R.layout.custom_dialog);
        				dialog.setTitle("Settings");
        				Button btnSave = (Button) dialog.findViewById(R.id.button12);
        				Calendar c = Calendar.getInstance();
        				final DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.datePicker2);
        				datePicker.setMaxDate(c.getTimeInMillis());
        				btnSave.setOnClickListener(new View.OnClickListener() {
                			public void onClick(View v) {                				
                				CheckBox checkbox0 = (CheckBox) dialog.findViewById(R.id.CheckBox0);
                				CheckBox checkbox1 = (CheckBox) dialog.findViewById(R.id.CheckBox1);
                				CheckBox checkbox2 = (CheckBox) dialog.findViewById(R.id.CheckBox2);
                				CheckBox checkbox3 = (CheckBox) dialog.findViewById(R.id.CheckBox3);
                				CheckBox checkbox4 = (CheckBox) dialog.findViewById(R.id.CheckBox4);
                				CheckBox checkbox5 = (CheckBox) dialog.findViewById(R.id.CheckBox5);
                				CheckBox checkbox6 = (CheckBox) dialog.findViewById(R.id.CheckBox6);
                				CheckBox checkbox7 = (CheckBox) dialog.findViewById(R.id.CheckBox7);
                				CheckBox checkbox8 = (CheckBox) dialog.findViewById(R.id.CheckBox8);
                				CheckBox checkbox9 = (CheckBox) dialog.findViewById(R.id.CheckBox9);
                				CheckBox checkbox10 = (CheckBox) dialog.findViewById(R.id.CheckBox10);
                				CheckBox checkbox11 = (CheckBox) dialog.findViewById(R.id.CheckBox11);
                				CheckBox checkbox12 = (CheckBox) dialog.findViewById(R.id.CheckBox12);
                				CheckBox checkbox13 = (CheckBox) dialog.findViewById(R.id.CheckBox13);
                				CheckBox checkbox14 = (CheckBox) dialog.findViewById(R.id.CheckBox14);
                				CheckBox checkbox15 = (CheckBox) dialog.findViewById(R.id.CheckBox15);
                				int day = datePicker.getDayOfMonth();
    							int month = datePicker.getMonth();
    							int year = datePicker.getYear();
        						try {
        							FileOutputStream fos = getActivity().openFileOutput("dateDefault", Context.MODE_PRIVATE);
        							//Can be changed using switch for cleaner code
        							if(checkbox0.isChecked()) {
										fos.write("0\n".getBytes());
        							}
        							if(checkbox1.isChecked()) {
										fos.write("1\n".getBytes());
        							}
        							if(checkbox2.isChecked()) {
										fos.write("2\n".getBytes());
        							}
        							if(checkbox3.isChecked()) {
										fos.write("3\n".getBytes());
        							}
        							if(checkbox4.isChecked()) {
										fos.write("4\n".getBytes());
        							}
        							if(checkbox5.isChecked()) {
										fos.write("5\n".getBytes());
        							}
        							if(checkbox6.isChecked()) {
										fos.write("6\n".getBytes());
        							}
        							if(checkbox7.isChecked()) {
										fos.write("7\n".getBytes());
        							}
        							if(checkbox8.isChecked()) {
										fos.write("8\n".getBytes());
        							}
        							if(checkbox9.isChecked()) {
										fos.write("9\n".getBytes());
        							}
        							if(checkbox10.isChecked()) {
										fos.write("10\n".getBytes());
        							}
        							if(checkbox11.isChecked()) {
										fos.write("11\n".getBytes());
        							}
        							if(checkbox12.isChecked()) {
										fos.write("12\n".getBytes());
        							}
        							if(checkbox13.isChecked()) {
										fos.write("13\n".getBytes());
        							}
        							if(checkbox14.isChecked()) {
										fos.write("14\n".getBytes());
        							}
        							if(checkbox15.isChecked()) {
										fos.write("15\n".getBytes());
        							}
        							fos.write("DATE\n".getBytes());
        							String date = String.valueOf(year)+"\n"+String.valueOf(month)+"\n"+String.valueOf(day);
        							fos.write(date.getBytes());
        							fos.close();
        							dialog.dismiss();
        							Toast.makeText(getActivity(), "Settings saved", Toast.LENGTH_SHORT).show();
        						} catch(Exception e){
        							e.printStackTrace();
        							Toast.makeText(getActivity(), "Error saving settings..", Toast.LENGTH_SHORT).show();
        						}
                			}
        				});
        				dialog.show();
        			}
        		});
        		return rootView;
        	}
        	
        	// Page for the REPLAY Attack 
        	if ( page == 3 ) {
       
        		View rootView = inflater.inflate(R.layout.fragment_three, container, false);
        		Button saveDump = ( Button ) rootView.findViewById(R.id.button2);
        		Button readDump = ( Button ) rootView.findViewById(R.id.button3);
        		Button manageDump = ( Button ) rootView.findViewById(R.id.button4);
        		Button autoReplay = ( Button ) rootView.findViewById(R.id.button10);
				saveDump.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {				
						//Create an AlertDialog to get filename for the dump
						AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
						alert.setTitle("Dump Filename");
						alert.setMessage("Insert filename for dump...");
						final EditText input = new EditText(getActivity());
						alert.setView(input);
						alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							MainActivity activity = (MainActivity) getActivity();
							try {
								String value = input.getText().toString();
								activity.readUL(value);
								Toast.makeText(getActivity(),"Dump saved to: " + value, Toast.LENGTH_SHORT).show();
							} catch (Exception e) {
								Toast.makeText(activity, "There was an during the operation..", Toast.LENGTH_SHORT).show();
							}
						}
						});
						alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						    Toast.makeText(getActivity(), "Insert dump name..", Toast.LENGTH_SHORT).show();
						  }
						});
						alert.show();
					}
				});
				//Read dump from the saved files
				readDump.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						final ArrayList<String> options= activity.getDumpList();
						final CharSequence[] cs = options.toArray(new CharSequence[options.size()]);
						AlertDialog.Builder builder = new AlertDialog.Builder(activity);
						builder.setTitle("Select dump: ");
						builder.setItems(cs, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							MainActivity activity = (MainActivity) getActivity();
							try {
									activity.getDump((String) cs[whichButton], 1);
							} catch (Exception e) {
								Toast.makeText(activity, "There was an during the operation..", Toast.LENGTH_SHORT).show();
								e.printStackTrace();
							}
							};
						});
						builder.show();
					
					}
				});
				
				manageDump.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						final ArrayList<String> options= activity.getDumpList();
						final CharSequence[] cs = options.toArray(new CharSequence[options.size()]);
						AlertDialog.Builder builder = new AlertDialog.Builder(activity);
						builder.setTitle("Select dump: ");
						//Defining what actions take with selected dump
						builder.setItems(cs, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							try {
								MainActivity activity = (MainActivity) getActivity();
								final String choosenFile = (String) cs[whichButton];
								final String[] options= {"Delete", "Rename"};
								AlertDialog.Builder builder = new AlertDialog.Builder(activity);
								builder.setTitle(choosenFile);
								builder.setItems(options, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									try {
										MainActivity activity = (MainActivity) getActivity();
										//Action: DELETE
										if(options[whichButton] == "Delete") {
											File file = new File(activity.getFilesDir(), choosenFile);
											boolean result =  file.delete();
											if (result) {
												Toast.makeText(activity, "Dump deleted", Toast.LENGTH_SHORT).show();
											}
										} else {
											//Action: Rename
											final File file = new File(activity.getFilesDir(), choosenFile);
											AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
											alert.setTitle("New filename: ");
											alert.setMessage("Insert new filename for dump...");
											final EditText input = new EditText(getActivity());
											alert.setView(input);
											alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
												MainActivity activity = (MainActivity) getActivity();
												String path = input.getText().toString();
												File renameFile = new File(activity.getFilesDir(), path);
												boolean result =  file.renameTo(renameFile);
												if(result) {
													Toast.makeText(activity, "Dump renamed", Toast.LENGTH_SHORT).show();
												}
											}
											});
											alert.show();
										}
									} catch(Exception e) { e.printStackTrace(); }
									};
								});
								builder.show();
							} catch(Exception e) { e.printStackTrace(); }
							};
						});
						builder.show();
					}
				});
				//Autoreplay the ticket: features still in beta!
				autoReplay.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MainActivity activity = (MainActivity) getActivity();
						try {
							activity.readUL("tempId");
							Toast.makeText(activity, "Reading ticket..", Toast.LENGTH_SHORT).show();
						} catch (IOException e) {
							e.printStackTrace();
							Toast.makeText(activity, "Error reading the ticket..", Toast.LENGTH_SHORT).show();
						}
						activity.increaseOTP("tempId.mfd");
						new AlertDialog.Builder(getActivity())
						.setTitle("Warning")
						.setMessage("Now put the UL clone tag")
						//Wait for the user to press the OK button, then write the new temp dump back to the ticket
						.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int which) { 
						    	MainActivity activity = (MainActivity) getActivity();
						    	try {
									activity.getDump("finOTP", 0);
									File file2 = new File(getActivity().getFilesDir(), "finOTP");
									file2.delete();
								} catch (IOException e) {
									Toast.makeText(getActivity(), "Error writing the dump..", Toast.LENGTH_SHORT).show();
									e.printStackTrace();
								}
						    }
						 })
						 .show();
					}
				});	
                return rootView;
        	}
        	
        	//Creditz page
        	if ( page == 5 ) {
        		
        		View rootView = inflater.inflate(R.layout.fragment_four, container, false);
        		TextView text0 = (TextView) rootView.findViewById(R.id.textView0);
        		TextView text = (TextView) rootView.findViewById(R.id.textView1);
        		text0.setText(R.string.title);
        		text.setText(Html.fromHtml(getString(R.string.creditz)));
        		if (text != null) {
        			   text.setMovementMethod(LinkMovementMethod.getInstance());
        			 }
        		return rootView;
        	}
        	
        	//Custom edit page
        	if( page == 4 ) {
        		
                View rootView = inflater.inflate(R.layout.fragment_five, container, false);
                Button btnUID = ( Button ) rootView.findViewById(R.id.button9);
                Button btnEdt = ( Button ) rootView.findViewById(R.id.button8);
                Button btnWrt = ( Button ) rootView.findViewById(R.id.button11);
                //Declaring TextView
                final EditText page0 = (EditText) rootView.findViewById(R.id.editText1);
                final EditText page1 = (EditText) rootView.findViewById(R.id.editText2);
                final EditText page2 = (EditText) rootView.findViewById(R.id.editText3);
                final EditText page3 = (EditText) rootView.findViewById(R.id.editText4);
                final EditText page4 = (EditText) rootView.findViewById(R.id.editText5);
                final EditText page5 = (EditText) rootView.findViewById(R.id.editText6);
                final EditText page6 = (EditText) rootView.findViewById(R.id.editText7);
                final EditText page7 = (EditText) rootView.findViewById(R.id.editText8);
                final EditText page8 = (EditText) rootView.findViewById(R.id.editText9);
                final EditText page9 = (EditText) rootView.findViewById(R.id.editText10);
                final EditText page10 = (EditText) rootView.findViewById(R.id.editText11);
                final EditText page11 = (EditText) rootView.findViewById(R.id.editText12);
                final EditText page12 = (EditText) rootView.findViewById(R.id.editText13);
                final EditText page13 = (EditText) rootView.findViewById(R.id.editText14);
                final EditText page14 = (EditText) rootView.findViewById(R.id.editText15);
                final EditText page15 = (EditText) rootView.findViewById(R.id.editText16);
        		//Defining actions for UID fix Button
                btnUID.setOnClickListener(new View.OnClickListener() {
        			@Override
        			public void onClick(View v) {
            			try {
            				final MainActivity activity = (MainActivity) getActivity();

                            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            				alert.setTitle("UID Fix Alert");
            				alert.setMessage("This will set your UID as a Mifare UL. Usefull when you brick your UID Changable ticket.");
            				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            					public void onClick(DialogInterface dialog, int button ) {
            						try {
										if (activity.fixUID()) {
											Toast.makeText(activity, "UID Changed!", Toast.LENGTH_SHORT).show();
										}
										else {
											Toast.makeText(activity, "Error during the operation..", Toast.LENGTH_SHORT).show();
										}
									} catch (IOException e) {
										e.printStackTrace();
									}
            					}
            				});
            				alert.show();
        				} catch(Exception e) {
							Toast.makeText(getActivity(), "There was an during the operation..", Toast.LENGTH_SHORT).show();
        				}
        			}
        		});
        		//Defining actions for Edit Button
        		btnEdt.setOnClickListener(new View.OnClickListener() {
        			public void onClick(View v) {
        				try {
        					MifareUltralight ultralight = MifareUltralight.get(mytag);
        			        ultralight.connect();
        			        page0.setText(byteArrayToHexString(ultralight.readPages(0)).substring(0, 8));
        			        page1.setText(byteArrayToHexString(ultralight.readPages(1)).substring(0, 8));
        			        page2.setText(byteArrayToHexString(ultralight.readPages(2)).substring(0, 8));
        			        page3.setText(byteArrayToHexString(ultralight.readPages(3)).substring(0, 8));
        			        page4.setText(byteArrayToHexString(ultralight.readPages(4)).substring(0, 8));
        			        page5.setText(byteArrayToHexString(ultralight.readPages(5)).substring(0, 8));
        			        page6.setText(byteArrayToHexString(ultralight.readPages(6)).substring(0, 8));
        			        page7.setText(byteArrayToHexString(ultralight.readPages(7)).substring(0, 8));
        			        page8.setText(byteArrayToHexString(ultralight.readPages(8)).substring(0, 8));
        			        page9.setText(byteArrayToHexString(ultralight.readPages(9)).substring(0, 8));
        			        page10.setText(byteArrayToHexString(ultralight.readPages(10)).substring(0, 8));
        			        page11.setText(byteArrayToHexString(ultralight.readPages(11)).substring(0, 8));
        			        page12.setText(byteArrayToHexString(ultralight.readPages(12)).substring(0, 8));
        			        page13.setText(byteArrayToHexString(ultralight.readPages(13)).substring(0, 8));
        			        page14.setText(byteArrayToHexString(ultralight.readPages(14)).substring(0, 8));
        			        page15.setText(byteArrayToHexString(ultralight.readPages(15)).substring(0, 8));
        			        ultralight.close();
        				} catch(Exception e) {
        					Toast.makeText(getActivity(), "Error reading Tag..", Toast.LENGTH_SHORT).show();
        					e.printStackTrace();
        				}
        			
        			}
        		});
        		//Defining actions for Write Button
        		btnWrt.setOnClickListener(new View.OnClickListener() {
        			public void onClick(View v) {
        				try {
        					int uid0 = Integer.parseInt(page0.getText().toString().substring(0, 2), 16);
        					int uid1 = Integer.parseInt(page0.getText().toString().substring(2, 4), 16);
        					int uid2 = Integer.parseInt(page0.getText().toString().substring(4, 6), 16);
        					int bcc1 = Integer.parseInt(page0.getText().toString().substring(6, 8), 16);
        					int uid3 = Integer.parseInt(page1.getText().toString().substring(0, 2), 16);
        					int uid4 = Integer.parseInt(page1.getText().toString().substring(2, 4), 16);
        					int uid5 = Integer.parseInt(page1.getText().toString().substring(4, 6), 16);
        					int uid6 = Integer.parseInt(page1.getText().toString().substring(6, 8), 16);
        					int bcc2 = Integer.parseInt(page2.getText().toString().substring(0, 2), 16);
        					//Performing XOR operation to check if UID's checksums are correct
        					int bcc1Check = 0x88 ^ uid0 ^ uid1 ^ uid2;
        					int bcc2Check = uid3 ^ uid4 ^ uid5 ^ uid6;
    						final boolean continueBcc[] = {true};
    						//If not prompt alert to user
        					if(bcc1Check != bcc1) {
        						new AlertDialog.Builder(getActivity())
        					    .setTitle("Warning")
        					    .setMessage("UID checksum BCC0 seems wrong. Correct value is: "+Integer.toHexString(bcc1Check).toUpperCase()+"\nDo you want to continue with your value?")
        					    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        					        public void onClick(DialogInterface dialog, int which) {
        					        	continueBcc[0] = true;
        					            dialog.cancel();
        					        }
        					     })
        					     .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
        					    	 public void onClick(DialogInterface dialog, int which) { 
        					    		 continueBcc[0] = false;
                					 }
        					     })
        					     .show();
        					}
        					if(bcc2Check != bcc2) {
        						new AlertDialog.Builder(getActivity())
        					    .setTitle("Warning")
        					    .setMessage("UID checksum BCC1 seems wrong. Correct value is: "+Integer.toHexString(bcc2Check).toUpperCase()+"\nDo you want to continue with your value?")
        					    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        					        public void onClick(DialogInterface dialog, int which) {
        					        	continueBcc[0] = true;
        					            dialog.cancel();
        					        }
        					     })
        					     .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
        					    	 public void onClick(DialogInterface dialog, int which) { 
        					    		 continueBcc[0] = false;
                					 }
        					     })
        					     .show();
        					}        						
        					if(continueBcc[0] == true) {
        						//Write pages to the ticket
        						MifareUltralight ultralight = MifareUltralight.get(mytag);
        						ultralight.connect();
        						ultralight.writePage(0, hexStringToByteArray(page0.getText().toString()));
        						ultralight.writePage(1, hexStringToByteArray(page1.getText().toString()));
        						ultralight.writePage(2, hexStringToByteArray(page2.getText().toString()));
        						ultralight.writePage(3, hexStringToByteArray(page3.getText().toString()));
        						ultralight.writePage(4, hexStringToByteArray(page4.getText().toString()));
        						ultralight.writePage(5, hexStringToByteArray(page5.getText().toString()));
        						ultralight.writePage(6, hexStringToByteArray(page6.getText().toString()));
        						ultralight.writePage(7, hexStringToByteArray(page7.getText().toString()));
        						ultralight.writePage(8, hexStringToByteArray(page8.getText().toString()));
        						ultralight.writePage(9, hexStringToByteArray(page9.getText().toString()));
        						ultralight.writePage(10, hexStringToByteArray(page10.getText().toString()));
        						ultralight.writePage(11, hexStringToByteArray(page11.getText().toString()));
        						ultralight.writePage(12, hexStringToByteArray(page12.getText().toString()));
        						ultralight.writePage(13, hexStringToByteArray(page13.getText().toString()));
        						ultralight.writePage(14, hexStringToByteArray(page14.getText().toString()));
        						ultralight.writePage(15, hexStringToByteArray(page15.getText().toString()));
        						ultralight.close();
        					}
        				} catch(Exception e){
        					Toast.makeText(getActivity(), "Error writing to Tag..", Toast.LENGTH_SHORT).show();
        					e.printStackTrace();
        				}
        			}
        		});
                return rootView;
        	} 
        	else { 
                View rootView = inflater.inflate(R.layout.fragment_main, container, false);
                return rootView;
        	}       
        }
    }   
}