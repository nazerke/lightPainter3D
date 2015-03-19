package com.example.lightPainter3D;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

public class GraphHelper {
	File directory;
	File myExternalFile;
	PrintWriter writer;
	FileOutputStream outputStream;
	ArrayList<float[]> dataList;
	 float []toWrite;
	 String strToWrite;
	public GraphHelper(Context context)
	{directory = context.getExternalFilesDir(filepath);}
	
	public GraphHelper(Application application)
	{directory = application.getExternalFilesDir(filepath);}
	private String filepath = "MyFileStorage";
public void writeToFile(String filename,ArrayList <float[]>data)
{
	if(isExternalStorageAvailable()&&!isExternalStorageReadOnly())
	{
		myExternalFile = new File(directory, filename);
	 try {
		outputStream = new FileOutputStream(myExternalFile);
		writer = new PrintWriter(outputStream);
	    dataList = data;

	    for(int i=0;i< data.size();i++)
	    {
			toWrite = data.get(i);
			writer.print(toWrite);
		}
	    writer.close();
	    outputStream.close();
	    
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}}
	    
}
private static boolean isExternalStorageReadOnly() {  
	  String extStorageState = Environment.getExternalStorageState();  
	  if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {  
		 
	   return true;  
	  }  
	  return false;  
	 }  
	 
	 private static boolean isExternalStorageAvailable() {  
	  String extStorageState = Environment.getExternalStorageState(); 
	 
	  if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {  
		   return true;  
	  }  
	 return false;

	 }
	 }
