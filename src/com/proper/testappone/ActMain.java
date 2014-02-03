package com.proper.testappone;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
//import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
//import com.android.barcode.BarcodeDemoActivity.MyTask;
import com.proper.testappone.data.Product;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
//import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
//import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ActMain extends Activity {
	public static final int KEY_SCAN = 111;
    public static final int KEY_F1 = 112;
    public static final int KEY_F2 = 113;
    public static final int KEY_F3 = 114;
    private int KEY_POSITION = 0;
    private List<Product> SavedProductList;
    private static final int ACTION_GETSINGLEPRODUCT = 3;
    
    private List<Product> productList;
    
    private static DeviceControl DevCtrl;
	private SerialPort mSerialPort;
	private static String buff = new String();
	public int fd;
	private ReadThread mReadThread;
	private Button close;
	private static Button scan;
	private TextView mReception;
	private static final String TAG = "SerialPort";
	private static boolean key_start = true;
	private static boolean Powered = false;
	private boolean Opened = false;
	private static Timer timer = new Timer();
	private static Timer retrig_timer = new Timer();
	private static SoundPool soundPool;
	private static	int soundId;
	private Handler handler = null;
	private Handler t_handler = null;
	private Handler n_handler = null;
	private boolean ops = false;
	private boolean scanSuccess = false;
	private String scannerInput;
	
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
    
	public List<Product> getSavedProductList() {
		return SavedProductList;
	}

	public void setSavedProductList(List<Product> savedProductList) {
		SavedProductList = savedProductList;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lyt_main);
		
        close = (Button) this.findViewById(R.id.btnClose);
        close.setOnClickListener(new ClickEvent()); 
        scan = (Button) this.findViewById(R.id.btnScan);
        scan.setOnClickListener(new ClickEvent());
        mReception = (TextView) this.findViewById(R.id.etxtScanInput);  
        
        try {
        	DevCtrl = new DeviceControl("/proc/driver/scan");
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Log.d("OnCreate", "IOException - Unable to initiate a new DeviceControl object");
        	new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					finish();
				}
			}).show();
        	return;
		}
        ops = true;

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
        
        //****************************************************************************************************************************
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
	                    mReception.append(buff);
	                    soundPool.play(soundId, 1, 1, 0, 0, 1);
            			key_start = true;
           				scan.setEnabled(true);
           				retrig_timer.cancel();
	                }
	            }
	    };
	    //**********************************************************************************************************************************************
	    
	}

	/*private static class THandler extends Handler {
		//Handler t_handler = new Handler() {
			private final WeakReference<ActMain> mActivity;

		    public THandler(ActMain activity) {
		      mActivity = new WeakReference<ActMain>(activity);
		    }
		    
	    	@Override
	    	public void handleMessage(Message msg) {
	    		super.handleMessage(msg);
	    		 ActMain activity = mActivity.get();
	    	      if (activity != null) {
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
	    	}
	    }
	    
	private static class NHandler extends Handler {
		//n_handler = new Handler() {
		//private final WeakReference<ActMain> mActivity;
		//MyTask nTask = mTask;
		private ActMain thisActivity;
		public NHandler(ActMain passedActivity) {
			super();
			thisActivity = passedActivity;
		}
		
		class MyTask extends TimerTask
	    {
			//public MyTask() {
			//}
			@Override
			public void run() {
				
				Message message = new Message();
				message.what = 1;
				thisActivity.t_handler.sendMessage(message);
				//ActMain.this.t_handler.sendMessage(message);
			}
	    }

    	@Override
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		if(msg.what == 1) {
        	try {
        			if(key_start == false)
        			{
        				DevCtrl.TriggerOffDevice();
        				timer = new Timer();				//start a timer, when machine is idle for some time, cut off power to save energy.
        				//timer.schedule(nTask, 60000);
        				timer.schedule(new MyTask(), 60000);
        				scan.setEnabled(true);
        				key_start = true;
        			}
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
	    
	private static class MyHandler extends Handler {
		//handler = new Handler() {
			 
	            @Override
	            public void handleMessage(Message msg) {
	                super.handleMessage(msg);
	                if(msg.what == 1 && key_start == false){
	                	
	                	String input = String.format("%s", buff != null? buff : "");
	                	staticScannerInput = input;
	                	//mReception.append(buff);
	                    soundPool.play(soundId, 1, 1, 0, 0, 1);
	        			key_start = true;
	       				scan.setEnabled(true);
	       				retrig_timer.cancel();
	                }
	            }
	    }*/
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mnu_main, menu);
		return true;
	}
	
	class MyTask extends TimerTask
    {
		//public MyTask() {
		//}
		@Override
		public void run() {
			
			Message message = new Message();
			message.what = 1;
			t_handler.sendMessage(message);
		}
    }
    
    class RetrigTask extends TimerTask
    {
		@Override
		public void run() {
			
			Message message = new Message();
			message.what = 1;
			n_handler.sendMessage(message);
		}
    }
    
	@Override
	protected void onPause() {
		
		if(ops == true)
		{
        mReadThread.interrupt();
        timer.cancel();
        retrig_timer.cancel();
       	try {
       		DevCtrl.PowerOffDevice();
			Thread.sleep(1000);
		} catch (IOException e) {
			Log.d("OnPaused", "IOException - triggered");
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.d("OnPaused", "InterruptedException - triggered");
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
    
    private void PrepareProductList(String[] inputParam) {
    	WebserviceTask newTask = new WebserviceTask();
    	newTask.execute(inputParam);
    }
    
    class ClickEvent implements View.OnClickListener {

		@Override  
        public void onClick(View v) {
			switch (v.getId()) {
				case R.id.btnClose: 	//close.getId():
					ActMain.this.finish();
					break;
				case R.id.btnScan: 		//scan.getId():
					int prodCount = 0;
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
							//if (scanSuccess == true) {
							if (buff != null && scanSuccess != false) {
								// Navigate to the next screen and initiate a query based on scan data
								String[] actionString = {String.format("%s", ACTION_GETSINGLEPRODUCT), buff};
								PrepareProductList(actionString);
								
								scanSuccess = false; //reset for the next scan
							} else {
								Toast.makeText(ActMain.this, "Scan was not performed successfully", Toast.LENGTH_LONG).show();
							}
							prodCount = prodCount + 1;
			            	prodCount = prodCount - 1;
			            	
			            	//if (!getProductList().isEmpty()) {
			            	if (productList != null && !productList.isEmpty()) {
			            		Intent i = new Intent(ActMain.this, ActDetails.class);
								i.putExtra("SCANDATA_EXTRA", buff);
								i.putExtra("ACTION_EXTRA", ACTION_GETSINGLEPRODUCT);
								i.putExtra("PRODUCT_EXTRA", productList.get(0));
								startActivity(i);
							}
							else {
								Toast.makeText(ActMain.this, "The product scanned does not exist in our database", Toast.LENGTH_LONG).show();
							}
			            	
			            	if (mReception.getText().toString().trim().length() > 0) {
			            		prodCount = prodCount + 1;
			            	}
	            		}
					} catch (IOException e) {
						Log.i("Scan ClickEvent", "IOException - getPorductByBarcode threw this madness");
						e.printStackTrace();
					} catch (NullPointerException e) {
	            		Log.i("Scan ClickEvent", "NullPointerException - getPorductByBarcode threw this madness [Line= 452 - list empty]");
	            		Log.d("Scan ClickEvent", "NullPointerException - getPorductByBarcode threw this madness [Line= 452 - list empty]");
	            		e.printStackTrace();
	            	}
					break;
				default:
					//Yell exception
					Log.d("Button Click event", "Something went terribly wrong here! Android couldn't determine which buttion was clicked, Uhnmm?!?");
					break;
			}
			if (mReception.getText().toString().trim().length() > 0) {
        		int prodcount = 0;
				prodcount = prodcount + 1;
        	}
        }  
    }
    
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
					setScannerInput(mSerialPort.ReadSerial(fd, 1024));
					
					Log.d(TAG,"end");
					if(buff != null){
						scanSuccess = true;
						//setScannerInput(mSerialPort.ReadSerial(fd, 1024));  //	*********  Set value		*******
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
					e.printStackTrace();
				}
			}
		}
    }

    
    private class WebserviceTask extends AsyncTask<String[], String, Integer> {
    	protected ProgressDialog dialog;
    	
    	@Override
    	protected void onPreExecute() {
    		dialog = new ProgressDialog(ActMain.this);
            CharSequence message = "Working hard...contacting webservice...";
            CharSequence title = "Please Wait";
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMessage(message);
            dialog.setTitle(title);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
    	}

    	@Override
    	protected Integer doInBackground(String[]... params) {
    		String act = params[0][0].toString().trim();
    		int thisAction = Integer.parseInt(act.trim());
    		String thisParam = String.format("%s", (params[0][1]==null?"":params[0][1]));
    		int recordsFound = 0; //something went wrong, if this value doesn't change
    		//int taskLoad = 0;
    		XmlPullParser receivedData = null;
    		
    		if (thisAction == 2 || thisAction == 3) {
    			
    			// *****************************************	BEGIN	tryDownloadingData	**********************************************************
    			//String serviceUrl = "http://pmdtestserver:9080/com.lebel.restsample/api/v1/product";
    			String serviceUrl = "http://192.168.10.248:9080/com.lebel.restsample/api/v1/product";
    			//String serviceUrl = "http://192.168.0.104:8080/com.lebel.restsample/api/v1/product";
    			final String thisLogTag = "tryDownloadingData";
    			
    			switch (thisAction) {
    			case 3:
    				serviceUrl += "/getProductByIdInXML/" + thisParam;
    				break;
    			case 2:
    				serviceUrl += "/gettop20InXML";
    				ActMain.this.productList = new ArrayList<Product>();
    				break;
    			}
    			
    			try {
    				URL xmlUrl = new URL(serviceUrl);
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
    	        					prod.setProductId(Integer.parseInt(onHand.trim()));
    	        					prod.setBinNo(binNo);
    	        					prod.setPrice1(Float.valueOf(price1.trim()));
    	        					prod.setOutOfStock(Integer.parseInt(outOfStock.trim()));
    	        					
    	        					ActMain.this.productList.add(prod);
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
    			
    			// *****************************************	END	 tryDownloadingData	**********************************************************
    			
    			return recordsFound;
    		}
    		else if (thisAction == 0) {
    			//Log Unable to parse string ("doWhat")
    			recordsFound = 0;
    		}
    		return recordsFound;
    	}

    	@Override
    	protected void onPostExecute(Integer result) {
    		dialog.dismiss();
    		
    		if (result == 0) {
    			//Log Unable to parse string ("doWhat")
    			CharSequence message = "Show Error - Unable to parse String doWhat";
    			Toast.makeText(ActMain.this, message, Toast.LENGTH_LONG).show();
    		}
    		else if (result < 0) {
    			//Log something unexplained went wrong
    			CharSequence message1 = "Show Error - Something unexpected happened";
    			Toast.makeText(ActMain.this, message1, Toast.LENGTH_LONG).show();
    		}
    	}

    	@Override
    	protected void onProgressUpdate(String... values) {
    		super.onProgressUpdate(values);
    	}
    }

}
