package com.hugo.number2location;

 


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
 

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import javax.swing.JPanel;

 

public class Number2LocationMain extends JFrame {
 public Number2LocationMain(){
		final JPanel jp = new JPanel();
		JButton btnIntialize;
		JButton raw2NumberTables;
		JButton raw2CompareNumberTables;
		JButton export2CSV;
		JButton diff;
		JButton international;
		JButton cleanDB;
		jp.setSize(640, 480);
		btnIntialize = new JButton("Intialize");
		btnIntialize.setBounds(120, 30, 100, 30);
		btnIntialize.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				System.out.println("Ok Pressed");
				if(BusinessOperation.filePath.length()<1){
				   JFileChooser fc = new JFileChooser();
		            fc.showOpenDialog(jp);
		            File file=fc.getSelectedFile();
		            String filePath=file.getAbsolutePath();
		            BusinessOperation.setFilePath(filePath);
		            BusinessOperation.getInstance().IntializeTables();
		            System.out.println(filePath);
				}else{
					BusinessOperation.getInstance().IntializeTables();
				}
		     

			}

		});
		raw2NumberTables = new JButton("Raw2NumberTables");
		raw2NumberTables.setBounds(120, 80, 120, 50);
		raw2NumberTables.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().Read2DB("");
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().Read2DB("");
					}
			}

		});

		raw2CompareNumberTables = new JButton("RawDataCompare2NumberTables");
		raw2CompareNumberTables.setBounds(120, 110, 120, 60);
		raw2CompareNumberTables.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().RawdataCompare2NuberTables();
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().RawdataCompare2NuberTables();
					}
			}

		});
		
		diff = new JButton("Diff");
		diff.setBounds(120, 160, 120, 60);
		diff.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().Diff();
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().Diff();
					}
			}

		});
		
		export2CSV = new JButton("export2CSV");
		export2CSV.setBounds(120, 220, 120, 60);
		export2CSV.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().Write2CSV();
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().Write2CSV();
					}
			}

		});
		international=new JButton("Internationalcode");
		international.setBounds(120, 260, 120, 60);
		international.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().ProcessInternationalCode();
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().ProcessInternationalCode();
					}
			}

		});
		
		cleanDB=new JButton("CleanDB");
		cleanDB.setBounds(120, 300, 120, 60);
		cleanDB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if(BusinessOperation.filePath.length()<1){
					   JFileChooser fc = new JFileChooser();
			            fc.showOpenDialog(jp);
			            File file=fc.getSelectedFile();
			            String filePath=file.getAbsolutePath();
			            BusinessOperation.setFilePath(filePath);
			            BusinessOperation.getInstance().CleanDB();
			            System.out.println(filePath);
					}else{
						BusinessOperation.getInstance().CleanDB();
					}
			}

		});

		jp.add(btnIntialize);
		jp.add(raw2NumberTables);
		jp.add(raw2CompareNumberTables);
		jp.add(diff);
		jp.add(export2CSV);
		jp.add(international);
		jp.add(cleanDB);
		this.add(jp);
		pack();

 }

	public static void main(String[] args) {
		new Number2LocationMain().setVisible(true);
	}
}
