////////////////////////////////////////////////////  REFERENCE *//////////////////////////////////////////////////////////////////
/*
	----------	This code is from the the vendor of the device	------------------
 */
////////////////////////////////////////////////////  REFERENCE *//////////////////////////////////////////////////////////////////

package com.android.barcode;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class DeviceControl {
	private BufferedWriter CtrlFile;
	
	public DeviceControl(String path) throws IOException	
	{
		File DeviceName = new File(path);
		CtrlFile = new BufferedWriter(new FileWriter(DeviceName, false));	//open file
	}
	
	public void PowerOnDevice() throws IOException		//poweron barcode device
	{
		CtrlFile.write("on");
		CtrlFile.flush();
	}
	
	public void PowerOffDevice() throws IOException	//poweroff barcode device
	{
  		CtrlFile.write("off");
  		CtrlFile.flush();
	}
	
	public void TriggerOnDevice() throws IOException	//make barcode begin to scan
	{
		CtrlFile.write("trig");
		CtrlFile.flush();
	}
	
	public void TriggerOffDevice() throws IOException	//make barcode stop scan
	{
		CtrlFile.write("trigoff");
		CtrlFile.flush();
	}
	
	public void DeviceClose() throws IOException		//close file
	{
		CtrlFile.close();
	}
}
