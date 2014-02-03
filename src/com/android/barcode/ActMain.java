package com.android.barcode;


import com.proper.testappone.R;
import com.proper.testappone.data.Product;
import com.proper.testappone.ActDetails;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.android.barcode.DeviceControl;
import com.android.barcode.SerialPort;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class ActMain extends Activity {
	public static final int KEY_SCAN = 111;
    public static final int KEY_F1 = 112;
    public static final int KEY_F2 = 113;
    public static final int KEY_F3 = 114;
    public static final int KEY_YELLOW = 115;
    private int KEY_POSITION = 0;
    
    //private static final int MSG_DO_QUERY = 0;
    private static final int MSG_QUERY_DONE = 1;
    private static final int  MSG_QUERY_STARTING = 2;
    //private static final int MSG_QUERY_SHUTDOWN = 3;
	
	private DeviceControl DevCtrl;
	private SerialPort mSerialPort;
	private String buff = new String();
	public int fd;
	private ReadThread mReadThread;
	protected ProgressDialog wsDialog;
	private Button close;
	private Button scan;
	private EditText mReception;
	private Handler handler = null;
	private static final String TAG = "SerialPort";
	private boolean key_start = true;
	private boolean Powered = false;
	private boolean Opened = false;
	private Timer timer = new Timer();
	private Timer retrig_timer = new Timer();
	private SoundPool soundPool;
	private	int soundId;
	private Handler t_handler = null;
	private Handler n_handler = null;
	private Handler wsHandler = null;
	private boolean ops = false;
	private static final int ACTION_GETSINGLEPRODUCT = 3;
    private List<Product> productList;
	private String scannerInput;
	int wsLineNumber = 0;
	String actionString[];
	private boolean hasWsRan = false;
	private int wsRunCount = 0;
	
	public String getScannerInput() {
		return scannerInput;
	}

	public void setScannerInput(String scannerInput) {
		this.scannerInput = scannerInput;
	}

	public List<Product> getProductList() {
		return productList;
	}

	public void setProductList(List<Product> productList) {
		this.productList = productList;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lyt_main);
		
		mReception = (EditText) findViewById(R.id.etxtScanInput);
        //mReception.setText(""); //Clear the control before we add a listener
        mReception.addTextChangedListener(new TextChanged());
        close = (Button) this.findViewById(R.id.btnClose);
        close.setOnClickListener(new ClickEvent()); 
        scan = (Button) this.findViewById(R.id.btnScan);
        scan.setOnClickListener(new ClickEvent());
        
        
		try {
			ClassPool pool = javassist.ClassPool.getDefault();
			CtClass cc = pool.get("com.android.barcode.ActMain");
			javassist.CtMethod wsMethod = cc.getDeclaredMethod("QueryWebService");
	        wsLineNumber = wsMethod.getMethodInfo().getLineNumber(0);
		} catch (NotFoundException e1) {
			e1.printStackTrace();
			Log.d("onCreate", "NotFoundException - Could not find class specifies: com.android.barcode.ActMain");
		}
        
        try {
        	DevCtrl = new DeviceControl("/proc/driver/scan");
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Log.d(TAG, "AAA");
        	new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					finish();
				}
			}).show();
        	return;
		}
        ops = true;
        
        KEY_POSITION = 0; //Set for Yellow button to scan
        
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
	    
        t_handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		if(msg.what == 1) {
        			try {
        				DevCtrl.PowerOffDevice();
        			} catch (IOException e) {
        				Log.d(TAG, "BBB");
						// TODO Auto-generated catch block
        				e.printStackTrace();
        			}//powersave
        			Powered = false;
        		}
        	}
        };
        
        n_handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		if(msg.what == 1) {
            	try {
            			if(key_start == false)
            			{
            				DevCtrl.TriggerOffDevice();
            				timer = new Timer();				//start a timer, when machine is idle for some time, cut off power to save energy.
            				timer.schedule(new MyTask(), 60000);
            				scan.setEnabled(true);
            				key_start = true;
            			}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        };
        
		handler = new Handler() {
	            @Override
	            public void handleMessage(Message msg) {
	                super.handleMessage(msg);
	                if(msg.what == 1 && key_start == false){
	                    //mReception.append(buff);
	                    mReception.setText(buff);
	                    soundPool.play(soundId, 1, 1, 0, 0, 1);
            			key_start = true;
           				scan.setEnabled(true);
           				retrig_timer.cancel();
	                }
	            }
	    };
	    
	    wsHandler =  new Handler() {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case MSG_QUERY_STARTING:
						wsDialog = new ProgressDialog(ActMain.this);
			            CharSequence message = "Working hard...contacting webservice...";
			            CharSequence title = "Please Wait";
			            wsDialog.setCancelable(true);
			            wsDialog.setCanceledOnTouchOutside(false);
			            wsDialog.setMessage(message);
			            wsDialog.setTitle(title);
			            wsDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			            wsDialog.show();
						break;
					case MSG_QUERY_DONE:
						wsDialog.dismiss();
			    		int result = msg.obj  != null? (Integer) msg.obj : 0;
			    		if (result == 0) {
			    			//Log Unable to parse string ("doWhat")
			    			CharSequence thisMessage = String.format("Show Error - Unable to parse QueryWebService input params String to integer [Line = %s]", wsLineNumber);
			    			//CharSequence thisMessage = "Show Error - Unable to parse QueryWebService input params String to integer [Line = 415]";
			    			Toast.makeText(ActMain.this, thisMessage, Toast.LENGTH_LONG).show();
			    		}
			    		else if (result < 0) {
			    			//Log something unexplained went wrong
			    			CharSequence thisMessage1 = "Show Error - Something unexpected happened";
			    			Toast.makeText(ActMain.this, thisMessage1, Toast.LENGTH_LONG).show();
			    		}
			    		break;
				}
				super.handleMessage(msg);
			}
	    	
	    };
	}
	
	class MyTask extends TimerTask
    {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message message = new Message();
			message.what = 1;
			t_handler.sendMessage(message);
		}
    }
    
    class RetrigTask extends TimerTask
    {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message message = new Message();
			message.what = 1;
			n_handler.sendMessage(message);
		}
    }
    
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(ops == true)
		{
        mReadThread.interrupt();
        timer.cancel();
        retrig_timer.cancel();
       	try {
       		DevCtrl.PowerOffDevice();
			Thread.sleep(1000);
		} catch (IOException e) {
     		Log.d(TAG, "CCC");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       	Powered = false;
       	if(Opened == true)
       	{
       		mSerialPort.close(fd);
       		Opened = false;
       	}
		}
    	Log.d(TAG, "onPause");
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(ops == true)
		{
		if(Opened == false) {
			try {
				//mSerialPort = new SerialPort("/dev/eser1",9600);//3a
				mSerialPort = new SerialPort("/dev/eser0",9600);//35
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				Log.d(TAG, "DDD");
				// TODO Auto-generated catch block
				e.printStackTrace();
				new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					finish();
					}
				}).show();
				ops = false;
				soundPool.release();
				try {
					DevCtrl.DeviceClose();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				super.onResume();
				return;
			}
			fd = mSerialPort.getFd();
			if(fd > 0){
				Log.d(TAG,"opened");
				Opened = true;
			}
		}
		mReadThread = new ReadThread();
		mReadThread.start();
		}
    	Log.d(TAG, "onResume");
		super.onResume();
	}
    
    public void onDestroy() {
		// TODO Auto-generated method stub
    	if(ops == true)
    	{
    		try {
    			soundPool.release();
    			DevCtrl.DeviceClose();
    		} catch (IOException e) {
    			Log.d(TAG, "EEE");
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
		super.onDestroy();
	}
    
    private void QueryWebService(String[] inputParam) {
    	if (hasWsRan == false) {
    		wsHandler.obtainMessage(MSG_QUERY_STARTING).sendToTarget();
        	if (wsRunCount == 0) {wsRunCount ++;}
        	//************************************		Do Work		**********************************************
        	productList = new ArrayList<Product>();
        	String act = inputParam[0].toString().trim();
    		int thisAction = Integer.parseInt(act.trim());
    		String thisParam = String.format("%s", (inputParam[1]==null?"":inputParam[1]));
    		int recordsFound = 0; //something went wrong, if this value doesn't change
    		//int taskLoad = 0;
    		XmlPullParser receivedData = null;
    		
    		if (thisAction == 2 || thisAction == 3) {
    			
    			// **************************	BEGIN	tryDownloadingData	************************************
    			//String serviceUrl = "http://pmdtestserver:9080/com.lebel.restsample/api/v1/product";
    			String serviceUrl = "http://192.168.10.248:9080/com.lebel.restsample/api/v1/product";
    			//String serviceUrl = "http://192.168.0.103:8080/com.lebel.restsample/api/v1/product";
    			final String thisLogTag = "tryDownloadingData";
    			
    			switch (thisAction) {
    			case 3:
    				//serviceUrl += "/getProductByIdInXML/" + thisParam;
    				serviceUrl = serviceUrl + "/getProductByIdInXML/" + thisParam;
    				break;
    			case 2:
    				serviceUrl += "/gettop20InXML";
    				break;
    			}
    			
    			try {
    				URL xmlUrl = new URL(serviceUrl.trim());
    				XmlPullParserFactory ppfactory = XmlPullParserFactory.newInstance();
    		        ppfactory.setNamespaceAware(true);
    		        receivedData = ppfactory.newPullParser();
    				URLConnection conn = xmlUrl.openConnection();

    	            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    	            DocumentBuilder builder = dfactory.newDocumentBuilder();
    	            Document doc = builder.parse(conn.getInputStream());
    	            
    	            TransformerFactory tf = TransformerFactory.newInstance();
    	            Transformer transformer = tf.newTransformer();
    	            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    	            StringWriter writer = new StringWriter();
    	            transformer.transform(new DOMSource(doc), new StreamResult(writer));
    	            String xmlString = writer.getBuffer().toString().replaceAll("\n|\r", "");
    	            
    	            receivedData.setInput(new StringReader(xmlString));
    	            
    	            
    	            Element docEl = doc.getDocumentElement();
    	            NodeList nList = docEl.getChildNodes();
    	            
    	            if (nList != null && nList.getLength() > 0) {
    	                
    	            	//################	Loop to get total item	######################
    	            	for (int i = 0; i < nList.getLength(); i++) {
    	                	if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) {
    	                        Element el = (Element) nList.item(i);
    	                        if (el.getNodeName().equalsIgnoreCase("Product")) {
    	                        	recordsFound ++;
    	                        }
    	                	}
    	                }
    	            	
    	            	////################	Loop to process data	######################
    	            	for (int i = 0; i < nList.getLength(); i++) {
    	                	if (nList.item(i).getNodeType() == Node.ELEMENT_NODE) {
    	                        Element el = (Element) nList.item(i);
    	                        if (el.getNodeName().equalsIgnoreCase("Product")) {
    	                        	Product prod = new Product();
    	                        	
    	                        	//Expected fields in the XML records
    	                    		String productId = el.getElementsByTagName("ProductId").item(0).getTextContent();
    	                    		String suppCode = el.getElementsByTagName("SuppCode").item(0).getTextContent();
    	                    		String supplierCat = el.getElementsByTagName("SupplierCat").item(0).getTextContent();
    	                    		String format = el.getElementsByTagName("Format").item(0).getTextContent();
    	                    		String artist = el.getElementsByTagName("Artist").item(0).getTextContent();
    	                    		String title = el.getElementsByTagName("Title").item(0).getTextContent();
    	                    		String shortDesc = el.getElementsByTagName("ShortDescription").item(0).getTextContent();
    	                    		String barCode = el.getElementsByTagName("Barcode").item(0).getTextContent();
    	                    		String onHand = el.getElementsByTagName("OnHand").item(0).getTextContent();
    	                    		String dealerPrice = el.getElementsByTagName("DealerPrice").item(0).getTextContent();
    	                    		String binNo = el.getElementsByTagName("BinNo").item(0).getTextContent();
    	                    		String price1 = el.getElementsByTagName("Price1").item(0).getTextContent();
    	                    		String outOfStock = el.getElementsByTagName("OutOfStock").item(0).getTextContent();
    	                            
    	                            prod.setProductId(Integer.parseInt(productId.trim()));
    	        					prod.setSuppCode(suppCode);
    	        					prod.setSupplierCat(supplierCat);
    	        					prod.setFormat(format);
    	        					prod.setArtist(artist);
    	        					prod.setTitle(title);
    	        					prod.setShortDescription(shortDesc);
    	        					prod.setBarcode(barCode);
    	        					prod.setOnHand(Integer.parseInt(onHand.trim()));
    	        					prod.setDealerPrice(Float.valueOf(dealerPrice.trim()));
    	        					prod.setBinNo(binNo);
    	        					prod.setPrice1(Float.valueOf(price1.trim()));
    	        					prod.setOutOfStock(Integer.parseInt(outOfStock.trim()));
    	        					
    	        					productList.add(prod);
    	        					//ActMain.this.productList.add(prod);
    	        					//taskLoad ++;
    	                        }
    	                    }
    	                }
    	            }
    	            
    	            
    			} catch(XmlPullParserException e) {
    				Log.e(thisLogTag, "XmlPullParserException while attemmpting to download XML", e);
    			} catch(IOException ex) {
    				Log.e(thisLogTag, "IO Exception - Failed  downloading XML", ex);
    			} catch (SAXException e) {
    				Log.e(thisLogTag, "SAXException while attemmpting to download XML", e);
    				e.printStackTrace();
    			} catch (ParserConfigurationException e) {
    				Log.e(thisLogTag, "ParserConfigurationException while attemmpting to download XML", e);
    				e.printStackTrace();
    			} catch (TransformerConfigurationException e) {
    				Log.e(thisLogTag, "TransformerConfigurationException while attemmpting to download XML", e);
    				e.printStackTrace();
    			} catch (TransformerException e) {
    				Log.e(thisLogTag, "TransformerException while attemmpting to download XML", e);
    				e.printStackTrace();
    			} catch (Exception ex) {
    				Log.e(thisLogTag, "TransformerException while attemmpting to download XML", ex);
    				ex.printStackTrace();
    			}
    			
    			// *********************************	END	 tryDownloadingData	**********************************
    			hasWsRan = true;
    		}
    		else if (thisAction == 0) {
    			//Log Unable to parse string ("doWhat")
    			recordsFound = 0;
    		}
        	//************************************		Do Work		**********************************************
        	wsHandler.obtainMessage(MSG_QUERY_DONE, recordsFound).sendToTarget();
    	}
    	//hasWsRan = false;
    }
    
    class TextChanged implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void afterTextChanged(Editable s) {
			String[] lines = s.toString().split("\n");
			//String barcode = lines[lines.length - 1];
			String barcode = lines[0];
			if (barcode.trim().length() > 0 && barcode.trim().length() > 8) {
				
				// Navigate to the next screen and initiate a query based on scan data
				actionString = new String[] {String.format("%s", ACTION_GETSINGLEPRODUCT), barcode};
				
				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						if (actionString != null) {
							QueryWebService(actionString);
						}
						else {
							Log.d("afterTextChanged - Runnable", "actionString appears to be null");
						}
					}	
				};
				Thread wsThread = new Thread(runnable);
				//if (wsThread.isAlive() == false) {
					if (hasWsRan == false && wsRunCount == 0) {
						wsThread.start();
						wsRunCount ++;
					}
					
				//}
				
				//scanSuccess = false; //reset for the next scan
				if(wsDialog != null && wsDialog.isShowing()){ wsDialog.dismiss(); }
				//if (wsThread.isAlive() == true) { 
					if (hasWsRan == true) {
						wsThread.interrupt();
					}
				//}
				if (productList != null && !productList.isEmpty()) {
            		if (productList.get(0).getProductId() == 0) {
            			//wsDialog.dismiss();
            			if(wsDialog != null && wsDialog.isShowing()){ wsDialog.dismiss(); }
            			//if (wsThread.isAlive() == true) { 
        					if (hasWsRan == true) {
        						wsThread.interrupt();
        					}
        				//}
            			Toast.makeText(ActMain.this, "The device only scanned a partial barcode, please try again", Toast.LENGTH_LONG).show();
            		}
            		else {
            			//if (wsThread.isAlive()) {
            			//	wsThread.interrupt();
            			//}
            			//wsDialog.dismiss();
            			if(wsDialog != null && wsDialog.isShowing()){ wsDialog.dismiss(); }
            			//if (wsThread.isAlive() == true) { 
        					if (hasWsRan == true) {
        						wsThread.interrupt();
        					}
        				//}
            			Intent i = new Intent(ActMain.this, ActDetails.class);
    					i.putExtra("SCANDATA_EXTRA", barcode);
    					i.putExtra("ACTION_EXTRA", ACTION_GETSINGLEPRODUCT);
    					i.putExtra("PRODUCT_EXTRA", productList.get(0));
    					startActivity(i);
            		}
				}
				else {
					//wsDialog.dismiss();
					//if (wsThread.isAlive() == true) { 
						if (hasWsRan == true) {
							wsThread.interrupt();
						}
					//}
					Toast.makeText(ActMain.this, "The product scanned does not exist in our database", Toast.LENGTH_LONG).show();
				}
			} else {
				wsDialog.dismiss();
				Toast.makeText(ActMain.this, "Scan was not performed successfully", Toast.LENGTH_LONG).show();
			}
			hasWsRan = false;
			wsRunCount = 0;
		}
    }
    
    class ClickEvent implements View.OnClickListener {  
        
        @Override  
        public void onClick(View v) {  
            if (v == close) {
            	ActMain.this.finish();
            	}
            else if(v == scan)
            {
            	try {
            		if(key_start == true)
            		{
            			if(Powered == false)
            			{
            				Powered = true;
            				DevCtrl.PowerOnDevice();
            			}
           				timer.cancel();
           				DevCtrl.TriggerOnDevice();
						scan.setEnabled(false);
						key_start = false;
						retrig_timer = new Timer();
						retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
            		}
				} catch (IOException e) {
					Log.d(TAG, "FFF");
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            /*else if(v == clearscreen)
            {
            	mReception.setText("");
            }*/
        }  
    }
    
    @Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KEY_SCAN:
				if(KEY_POSITION == 0) {
					scan.performClick();
				}
				break;
		}
		return super.onKeyLongPress(keyCode, event);
	}

    @Override    	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        	case KEY_SCAN:
        		if(KEY_POSITION == 0){
	            	try {
	            		if(key_start == true)
	            		{
	            			if(Powered == false)
	            			{
	            				Powered = true;
	            				DevCtrl.PowerOnDevice();
	            			}
	           				timer.cancel();
	           				DevCtrl.TriggerOnDevice();
							scan.setEnabled(false);
							key_start = false;
							retrig_timer = new Timer();
							retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
							
	            		}
					} catch (IOException e) {
						Log.d(TAG, "FFF");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        			//scan.performClick();
        		}
            	break;
        	case KEY_YELLOW:
        		if (KEY_POSITION == 0) {
        			try {
	            		if(key_start == true)
	            		{
	            			if(Powered == false)
	            			{
	            				Powered = true;
	            				DevCtrl.PowerOnDevice();
	            			}
	           				timer.cancel();
	           				DevCtrl.TriggerOnDevice();
							scan.setEnabled(false);
							key_start = false;
							retrig_timer = new Timer();
							retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
							
	            		}
					} catch (IOException e) {
						Log.d(TAG, "FFF");
						e.printStackTrace();
					}
        			//scan.performClick();
        		}
        		break;
        	case KEY_F1:
        		if(KEY_POSITION == 1){
	        		try {
	            		if(key_start == true)
	            		{
	            			if(Powered == false)
	            			{
	            				Powered = true;
	            				DevCtrl.PowerOnDevice();
	            			}
	           				timer.cancel();
	           				DevCtrl.TriggerOnDevice();
							scan.setEnabled(false);
							key_start = false;
							retrig_timer = new Timer();
							retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
	            		}
					} catch (IOException e) {
						Log.d(TAG, "FFF");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
            	break;
        	case KEY_F2:
        		if(KEY_POSITION == 2){
	        		try {
	            		if(key_start == true)
	            		{
	            			if(Powered == false)
	            			{
	            				Powered = true;
	            				DevCtrl.PowerOnDevice();
	            			}
	           				timer.cancel();
	           				DevCtrl.TriggerOnDevice();
							scan.setEnabled(false);
							key_start = false;
							retrig_timer = new Timer();
							retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
	            		}
					} catch (IOException e) {
						Log.d(TAG, "FFF");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
            	break;
        	case KEY_F3:
        		if(KEY_POSITION == 3){
	        		try {
	            		if(key_start == true)
	            		{
	            			if(Powered == false)
	            			{
	            				Powered = true;
	            				DevCtrl.PowerOnDevice();
	            			}
	           				timer.cancel();
	           				DevCtrl.TriggerOnDevice();
							scan.setEnabled(false);
							key_start = false;
							retrig_timer = new Timer();
							retrig_timer.schedule(new RetrigTask(), 3500);	//start a timer, if the data is not received within a period of time, stop the scan.
	            		}
					} catch (IOException e) {
						Log.d(TAG, "FFF");
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
            	break;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				try {
					Log.d(TAG,"read");
					buff = mSerialPort.ReadSerial(fd, 1024);
					Log.d(TAG,"end");
					if(buff != null){
						//ImHit = 0;
						Message msg = new Message();
                        msg.what = 1;
                        handler.sendMessage(msg);
            			timer = new Timer();
            			timer.schedule(new MyTask(), 60000);
					}else{
						Message msg = new Message();
                        msg.what = 0;
                        handler.sendMessage(msg);
					}
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
    }

}
