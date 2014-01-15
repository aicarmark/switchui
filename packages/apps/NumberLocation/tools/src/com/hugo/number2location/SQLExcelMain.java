package com.hugo.number2location;

import java.util.Scanner;



//import org.eclipse.swt.SWT;
//import org.eclipse.swt.events.SelectionEvent;
//import org.eclipse.swt.events.SelectionListener;
//import org.eclipse.swt.widgets.Button;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.FileDialog;
//import org.eclipse.swt.widgets.Shell;
//
//import javax.swing.event.*;

public class SQLExcelMain  {

	/**
	 * 
	 */
	
	public static void main(String []args){
		Scanner scanner=new Scanner(System.in);
		while(true){
		System.out.println("input your name:");
		System.out.printf("%s!\n",scanner.next());
		}
	}
//	private static final long serialVersionUID = 1L;
//	private Shell shell;
//	private Button buttonIntialize;
//	private Button buttonImport;
//	private Button buttonExport;
//	private Button buttonImportCompare;
//	private Button buttonDiff;
//	private Button clean;
//	
//	public static void main(String []args){
//		 
//		SQLExcelMain window=new SQLExcelMain();
//		window.Open();
//	}
//	public void Open(){
//		final Display display=new Display();
//		shell=new Shell();
//		shell.setSize(360, 300);
//		shell.setText("Number&Location tools");
//		
//		buttonIntialize=new Button(shell,SWT.NONE);
//		buttonIntialize.setBounds(120, 20, 110, 25);
//		buttonIntialize.setText("Intialize");
//		buttonIntialize.addSelectionListener(new SelectionListener(){
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				FileDialog fd = new FileDialog(shell, SWT.OPEN);
//				fd.setText("Open");
//				fd.setFilterPath("D:/Log");
//				String[] filterExt = { "*db","*.*" };
//				fd.setFilterExtensions(filterExt);
//				String filePath=fd.open();
//				 SheeTOperation.setFilePath(filePath);
//				 SheeTOperation.getInstance().IntializeTables();
//			}
//			
//		});
//		
//		buttonImport=new Button(shell,SWT.NONE);
//		buttonImport.setBounds(120, 60, 110, 25);
//		buttonImport.setText("import raw data");
//		buttonImport.addSelectionListener(new SelectionListener() {
//
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				if(SheeTOperation.filePath.length()<1){
//					 FileDialog fd = new FileDialog(shell, SWT.OPEN);
//						fd.setText("Open");
//						fd.setFilterPath("D:/Log");
//						String[] filterExt = { "*db","*.*" };
//						fd.setFilterExtensions(filterExt);
//						String filePath=fd.open();
//						SheeTOperation.setFilePath(filePath); 
//				 }
//			   SheeTOperation.getInstance().Read2DB("");
//				
//			}
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//
//			}
//
//		});
//		buttonImportCompare=new Button(shell,SWT.NONE);
//		buttonImportCompare.setBounds(120,100,110,25);
//		buttonImportCompare.setText("Import target data");
//		buttonImportCompare.addSelectionListener(new SelectionListener(){
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				if(SheeTOperation.filePath.length()<1){
//					 FileDialog fd = new FileDialog(shell, SWT.OPEN);
//						fd.setText("Open");
//						fd.setFilterPath("D:/Log");
//						String[] filterExt = { "*db","*.*" };
//						fd.setFilterExtensions(filterExt);
//						String filePath=fd.open();
//						SheeTOperation.setFilePath(filePath); 
//				 }
//				 SheeTOperation.getInstance().RawdataCompare2NuberTables();
//			}
//			
//		});
//		buttonDiff=new Button(shell,SWT.NONE);
//		buttonDiff.setBounds(120, 140, 110, 25);
//		buttonDiff.setText("Diff");
//		buttonDiff.addSelectionListener(new SelectionListener(){
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				if(SheeTOperation.filePath.length()<1){
//					 FileDialog fd = new FileDialog(shell, SWT.OPEN);
//						fd.setText("Open");
//						fd.setFilterPath("D:/Log");
//						String[] filterExt = { "*db","*.*" };
//						fd.setFilterExtensions(filterExt);
//						String filePath=fd.open();
//						SheeTOperation.setFilePath(filePath); 
//				 }
//				 SheeTOperation.getInstance().Diff();
//			}
//			
//		});
//		buttonExport=new Button(shell,SWT.NONE);
//		buttonExport.setBounds(120, 180, 110, 25);
//		buttonExport.setText("Export to csv");
//		buttonExport.addSelectionListener(new SelectionListener(){
//
//			@Override
//			public void widgetDefaultSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void widgetSelected(SelectionEvent arg0) {
//				// TODO Auto-generated method stub
////				FileDialog fd = new FileDialog(shell, SWT.OPEN);
////				fd.setText("Open");
////				fd.setFilterPath("D:/");
////				fd.open();
//				 
//				 while(SheeTOperation.filePath.length()<1){
//					 FileDialog fd = new FileDialog(shell, SWT.OPEN);
//						fd.setText("Open");
//						fd.setFilterPath("D:/Log");
//						String[] filterExt = { "*db","*.*" };
//						fd.setFilterExtensions(filterExt);
//						String filePath=fd.open();
//						SheeTOperation.setFilePath(filePath); 
//				 }
//				 SheeTOperation.getInstance().Write2CSV();
//			
//			}
//			
//		});
//		
//		
//		
//		//shell.pack();
//		shell.open();
//		while (!shell.isDisposed()) {
//		if (!display.readAndDispatch())
//			display.sleep();
//		}
//	}
}
