package com.hugo.number2location;

 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
 
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

 


public class BusinessOperation {
	public static  String filePath="";
	 
	public static HashMap<String, HashMap<String, Integer>> provIdMap=null;
	public static String DESDIR="D:/number2location";
	private Connection conn=null;
	private static HashMap<String,String> NUMBER_TABLES=new HashMap<String,String>(); 
	private static HashSet tableSet=new HashSet<String>();
	
    private static BusinessOperation instance;
	private BusinessOperation(String file) {
		//filePath = file;
		BusinessOperation.class.getResource("");
		try {
			Class.forName("org.sqlite.JDBC");
			 conn = DriverManager.getConnection("jdbc:sqlite:" + filePath);
			 String sqlStr="select name from sqlite_master where name like 'number_%';";
			 Statement st=null;
			 st=conn.createStatement();
			 ResultSet tableResult=st.executeQuery(sqlStr);
			 if(null!=tableResult){
				 while(tableResult.next()){
					 NUMBER_TABLES.put(tableResult.getString(1).toLowerCase(), tableResult.getString(1));
					 
				 }
			 }
		if(null!=st){

			 st.close();
			 st=null;
		}
			  
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	public static  BusinessOperation getInstance(){
		if(null==instance)
			instance=new BusinessOperation(filePath);
		return instance;
	}
   
	public void Read2DB(String file)  {
    Statement st=null;
    int i=0;
		 
			//st=conn.createStatement();
//			String resetNumberTable=null;
//			for(i=0;i<Tables.NumberTables.length;i++){
//				resetNumberTable="update "+Tables.NumberTables[i]+" set city_id=0;";
//				st.executeUpdate(resetNumberTable);
//			}
//			 
//			if(null!=st){
//				st.close();
//				st=null;
//			}
			CreateProvince("raw_data","province");
			CreateCity("raw_data","province","city");
			 
				try {
					Rawdata2NumberTables();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  
			//ProcessInternationalCode();
		   
	}

	public void Write2CSV() {
	   System.out.println("in write to csv");
		File file=new File(".");
		file.mkdir();
		 
		try {
			file=new File(file.getCanonicalPath()+"num2_location_exp.csv");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(file.exists()){
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(file.exists()){
			 System.out.println("absolute path:"+file.getAbsolutePath());
			try {
				FileWriter writer=new FileWriter(file);
				BufferedWriter bufferWrite=new BufferedWriter(writer);
				int total=0;
				Statement statement=conn.createStatement();
				for(int i=0;i<Tables.NumberTables.length;i++){
					String table=Tables.NumberTables[i];
					String tablePox=table.substring(8,table.length()-1);
					System.out.println("current table is:"+table+",table pox is:"+tablePox);
					String querySQL="select "+table+".rowid,province.province,city.city from" 
					+table+",province,city where "+
					table+".[city_id]=city.[id] and city.[province_id]=province.[id];";
					ResultSet resultSet=statement.executeQuery(querySQL);
					if(null!=resultSet){
						while(resultSet.next()){
							String rowid=resultSet.getString("rowid");
							String province=resultSet.getString("province");
							String city=resultSet.getString("city");
							System.out.println("query result table:"+table+",rowid"+rowid+",province:"+province+",city:"+city);
							String phoneNumber=null;
							if(tablePox.length()<3){
								phoneNumber=rowid;
							}else{
								 rowid=RowId2PhoneNumber(rowid);
								
							}
							phoneNumber=tablePox+rowid;
							bufferWrite.write(phoneNumber+","+province+","+city+"\n");
							total++;
						}
						bufferWrite.flush();
					}

				}
				 System.out.println("absolute path:"+file.getAbsolutePath());
				bufferWrite.close();
				writer.close();
			 System.out.println("totally,output:"+total);	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private String CopyDBFile(File oldFile, String newpath) {
		if (oldFile.exists()) {
			InputStream inStream;
			try {
				inStream = new FileInputStream(oldFile);
				FileOutputStream fs = new FileOutputStream(newpath);
				byte[] buffer = new byte[2000];
				int length = 0;
				int bytesum = 0;
				int byteread = 0;
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					fs.write(buffer, 0, byteread);
				}
				fs.flush();
				fs.close();
				inStream.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return "";
	}

	private void Rawdata2NumberTables( ) throws IOException {
		try {
		    Statement statement=conn.createStatement();
		    Statement updateStatement=conn.createStatement();
		    conn.setAutoCommit(false);
			long total=0;
			Long before=System.currentTimeMillis();
			String querySQL="select phoneNumber,cityName,provinceName from raw_data  ;";
			
			String cleanCityTable="delete from city where _id in(select _id from city where city like '%/%');";
			statement.executeUpdate(cleanCityTable);
			String resetNumberTable=null;
//				for(i=0;i<Tables.NumberTables.length;i++){
//					resetNumberTable="update "+Tables.NumberTables[i]+" set city_id=0;";
//					statement.executeUpdate(resetNumberTable);
//				}
			ResultSet result=statement.executeQuery(querySQL);
			while (result.next()) {
				String phoneNumber = result.getString("phoneNumber");
				String cityName = result.getString("cityName");
			    String provinceName=result.getString("provinceName");
				System.out.println("Phone# :" + phoneNumber + ",cityname:"
						+ cityName + ", province"+provinceName );
				int cityId=0;
				if(phoneNumber.length()<7){
					//cityId=getComposeCityIdByName(cityName,provinceName);
					 Statement st=conn.createStatement();
					
					String queryCityid="select city_id from number_0 where rowid="+(Integer.valueOf(phoneNumber));
					ResultSet resultCityId=st.executeQuery(queryCityid);
					if(null!=resultCityId){
						int tmpcityId=0;
						int finalCityId=0;
						while(resultCityId.next()){
						     tmpcityId=resultCityId.getInt("city_id");
						    
						}
						if(tmpcityId==0){
							cityId= getCityIdByName(cityName,provinceName);
							conn.createStatement().executeUpdate(CreateUpdateSQL(phoneNumber, cityName,
			 						cityId,""));
						} else{
							String queryCityNameSQL="select city,province_id from city where _id="+tmpcityId;
							int provinceId=0;
							String queryCityName=null;
							ResultSet queryCityResult=st.executeQuery(queryCityNameSQL);
							if(null!=queryCityResult){
								while(queryCityResult.next()){
									queryCityName=queryCityResult.getString("city");
									provinceId=queryCityResult.getInt("province_id");
								}
							}
							if(queryCityName.contains("/")){
								queryCityName=queryCityName+"/"+cityName;
								String updateCityinfo="update city set city= '"+queryCityName+"' where _id="+tmpcityId;
								st.executeUpdate(updateCityinfo);
								finalCityId=tmpcityId;
							} else {
								queryCityName = queryCityName + "/" + cityName;

								String addCitySql = "insert into city ( city,province_id,flag)" +
										" values ( '"
										+ queryCityName
										+ "',"
										+ provinceId+","+
										"2"+
										")";
								st.executeUpdate(addCitySql);
 
								// use insert statement to get id
								String getMaxRowid = "select max(_id) from city";
								ResultSet getMaxCityId = st
										.executeQuery(getMaxRowid);
								if (null != getMaxCityId) {
									while (getMaxCityId.next()) {
										finalCityId = getMaxCityId.getInt(1);
									}
								}
								String strsql="update city  set flag=3 where _id="+tmpcityId;
								st.executeUpdate(strsql);
								 
							}
						// begin to insert into this city info into ....	
						String update_cityIdSQL="update number_0 set city_id="+finalCityId+" where rowid="+Integer.valueOf(phoneNumber);	
							st.executeUpdate(update_cityIdSQL);
						}
					}
					st.close();
				} 
				else {
				cityId= getCityIdByName(cityName,provinceName);
				updateStatement.executeUpdate(CreateUpdateSQL(phoneNumber, cityName,
 						cityId,""));
				}
			
				total++;

			}
			conn.commit();
			Long after=System.currentTimeMillis();
			System.out.println("exit while loop,totally,executed:"+total+", and time cost is :"+(after-before));
			updateStatement.close();
			statement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void IntializeTables(){
		Statement st=null;
		Statement statement=null;
		Long beforeTime=System.currentTimeMillis();
		String sqlStr=null;
		
		try {
 
			st=conn.createStatement();
			statement=conn.createStatement();
			conn.setAutoCommit(false);
			sqlStr="select name from sqlite_master where name like 'number_%';";
			ResultSet tableResult=st.executeQuery(sqlStr);
			String tabname=null;
			String resetNumberTable=null;
			if(null!=tableResult){
				while(tableResult.next()){
					tabname=tableResult.getString(1);
					System.out.println("reset table is "+tabname);
					if(tabname.endsWith("00")){
						
					}else {
						resetNumberTable="update "+ tabname+" set city_id=0;";
						System.out.println("update sql:"+resetNumberTable);
						statement.executeUpdate(resetNumberTable);
					}
				}
			}
			 
//			for(i=0;i<Tables.NumberTables.length;i++){
//				resetNumberTable="update "+Tables.NumberTables[i]+" set city_id=0;";
//				st.addBatch(resetNumberTable);
//			}
//			st.executeBatch();
//			st.clearBatch();
// 
 
			
			sqlStr="delete from province";
			st.execute(sqlStr);
			
			sqlStr="delete from city";
			st.execute(sqlStr);
		// process city_compare,province_compare,raw_data_compare
			
			sqlStr="drop table if exists raw_data_compare";
			st.execute(sqlStr);
			
			sqlStr = "CREATE TABLE [raw_data_compare] ("
					+ "[phoneNumber] VARCHAR," + "[provinceName] VARCHAR,"
					+ "[cityName] VARCHAR," + "[carrierName] VARCHAR,"
					+ "[networkType] VARCHAR);";
			st.execute(sqlStr);
			
			sqlStr="drop table if exists province_compare";
			st.execute(sqlStr);
			
			sqlStr = "CREATE TABLE [province_compare] (" + "[id] INTEGER PRIMARY KEY,"
					+ "[province] VARCHAR(12));";
			st.execute(sqlStr);
			
			sqlStr="drop table if exists city_compare";
			st.execute(sqlStr);
			
			sqlStr = "CREATE TABLE [city_compare] (" + "[_id] INTEGER PRIMARY KEY,"
					+ "[city] VARCHAR(24)," + "[province_id] TINYINT);";
			st.execute(sqlStr);
		 
			conn.commit();
			Long after=System.currentTimeMillis();
			System.out.println("time cost :"+(after-beforeTime));
			st.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Table intialized!");
	}
	class CSVBean{
		public String PhoneNumber ;
		public String Province ;
		public String City ;
		public String Carrier ;
		public String networkType ;
		public CSVBean(){
			PhoneNumber="";
			Province="";
			City="";
			Carrier="";
			networkType="";
		}
		public void PrintBean(){
			System.out.println("The bean is:number:"+PhoneNumber+",Province: "+Province+", city: "+City+",Carrier: "+Carrier+",networkType:"+networkType);
		}
		
	}

	private CSVBean CSV2Bean(CSVBean csvbean, StringTokenizer stoken) {
		csvbean = new CSVBean();
		if (stoken != null) {
			if (stoken.countTokens() == 5) {
				if (stoken.hasMoreTokens())
					csvbean.PhoneNumber = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.Province = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.City = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.Carrier = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.networkType = stoken.nextToken().trim();
			} else if (stoken.countTokens() == 4) {
				if (stoken.hasMoreTokens())
					csvbean.PhoneNumber = stoken.nextToken().trim();
				if(csvbean.PhoneNumber.length()<7){
				if (stoken.hasMoreTokens())
					csvbean.Province = stoken.nextToken().trim();	
				if (stoken.hasMoreTokens())
					csvbean.City = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.networkType = stoken.nextToken().trim();
				} else {
					if (stoken.hasMoreTokens())
						csvbean.Province = csvbean.City = stoken.nextToken().trim();
					if (stoken.hasMoreTokens())
						csvbean.Carrier = stoken.nextToken().trim();
					if (stoken.hasMoreTokens())
						csvbean.networkType = stoken.nextToken().trim();
				}
			} else if(stoken.countTokens()==3){
				if (stoken.hasMoreTokens())
					csvbean.PhoneNumber = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.City = stoken.nextToken().trim();
				if (stoken.hasMoreTokens())
					csvbean.networkType = stoken.nextToken().trim();
			}
		}
		return csvbean;
	}
	
	public void RawdataCompare2NuberTables(){
		// first need to construct the city and province, since we need to compare by name not id
		CreateProvince("raw_data_compare","province_compare");
		CreateCity("raw_data_compare","province_compare","city_compare");
		
		String querySQL="select phoneNumber,cityName,provinceName from raw_data_compare ;";
		//int cityId = getCityIdByName(statement, "北京");
		//System.out.println("updated total:" + cityId);
    	Statement statement=null;
    	Statement updateStatement=null;
    	Statement st=null;
    	int i=0;
		try {
			statement = conn.createStatement();
			updateStatement=conn.createStatement();
			st=conn.createStatement();
			conn.setAutoCommit(false);
			Long before=System.currentTimeMillis();
			ResultSet result=statement.executeQuery(querySQL);
			while (result.next()) {
				String phoneNumber = result.getString("phoneNumber");
				String cityName = result.getString("cityName");
			    String provinceName=result.getString("provinceName");
				System.out.println("Phone# :" + phoneNumber + ",cityname:"
						+ cityName + ", province"+provinceName );
				int cityId =0;// getCityIdByName(cityName,provinceName);
				if(phoneNumber.length()<7){
					int finalCityId=0;
					//cityId=getComposeCityIdByName(cityName,provinceName);
					String queryCityid="select city_id from number_0_compare where rowid="+Integer.valueOf(phoneNumber);
					ResultSet resultCityId=st.executeQuery(queryCityid);
					if(null!=resultCityId){
						int tmpcityId=0;
						while(resultCityId.next()){
						     tmpcityId=resultCityId.getInt("city_id");
						    
						}
						if(tmpcityId==0){
							//cityId= getCityIdByName(cityName,provinceName);
							cityId=getComposeCityIdByName(cityName,provinceName,"_compare");
							finalCityId=cityId;
						} else{
							String queryCityNameSQL="select city,province_id from city_compare where _id="+tmpcityId;
							int provinceId=0;
							String queryCityName=null;
							ResultSet queryCityResult=st.executeQuery(queryCityNameSQL);
							if (null != queryCityResult) {
								while (queryCityResult.next()) {
									queryCityName = queryCityResult
											.getString("city");
									provinceId = queryCityResult
											.getInt("province_id");
								}
							}
							if (queryCityName.contains("/")) {
								queryCityName=queryCityName+"/"+cityName;
								String updateCityinfo="update city_compare set city= '"+queryCityName+"' where _id="+tmpcityId;
								st.executeUpdate(updateCityinfo);
								finalCityId=tmpcityId;
							} else {
								queryCityName = queryCityName + "/" + cityName;
								String addCitySql = "insert into city_compare ( city,province_id ) values ( '"
									+ queryCityName
									+ "',"
									+ provinceId
									+ ")";
						    	st.executeUpdate(addCitySql);
						    	// use insert statement to get id
								String getMaxRowid = "select max(_id) from city_compare";
								ResultSet getMaxCityId = st
										.executeQuery(getMaxRowid);
								if (null != getMaxCityId) {
									while (getMaxCityId.next()) {
										finalCityId = getMaxCityId.getInt(1);
									}
								}
							 
							}
						}
						
					}
					String update_cityIdSQL = "update number_0_compare set city_id="
							+ finalCityId
							+ " where rowid="
							+ Integer.valueOf(phoneNumber);
					System.out.println("update sql is :" + update_cityIdSQL);
					st.executeUpdate(update_cityIdSQL);
				} else {
					cityId = getComposeCityIdByName(cityName,provinceName,"_compare");
					updateStatement.executeUpdate(CreateUpdateSQL(phoneNumber,
							cityName, cityId, "_compare"));
				}
				
				i++;
				
				
			}
			 conn.commit();
			 Long after=System.currentTimeMillis();
			System.out.println("totally, executed:"+i+", cost time :"+(after-before));
			st.close();
			statement.close();
			updateStatement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private int getComposeCityIdByName(String cityName,String provinceName,String postfix){
		int cityId=0;
		Statement st=null;
		try {
			st=conn.createStatement();
			String getProvinceIdSQL="select id from "+"province"+postfix+" where province= '"+provinceName+"'";
			ResultSet provinceIdSet=st.executeQuery(getProvinceIdSQL);
			int provinceId=0;
			if(null!=provinceIdSet){
				while(provinceIdSet.next()){
					provinceId=provinceIdSet.getInt("id");
				}
			}
			String getCityIdSQL="select _id from "+"city"+postfix+" where province_id="+provinceId+
			" AND city= '"+cityName+"';";
			System.out.println("re compose sql is :"+getCityIdSQL);
			ResultSet cityIdResult=st.executeQuery(getCityIdSQL);
			if(null!=cityIdResult){
				while(cityIdResult.next()){
					cityId=cityIdResult.getInt(1);
				}
			}
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return cityId;
	}
	private int getCityIdByName(String cityName,String provinceName){
	 
		Statement statement = null;
		try {
			statement = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(null==provIdMap){
			provIdMap=new HashMap<String,HashMap<String,Integer>>();
			String queryProvince="select * from province;";
			try {
				String proname=null;
				int proid=0;
				HashMap<String, Integer> cityMap=null;
				ResultSet resultSet=statement.executeQuery(queryProvince);
				while(resultSet.next()){
					proname=resultSet.getString("province");
					proid=resultSet.getInt("id");
					cityMap=new HashMap<String,Integer>();
					{
						String cityQuery="select city,_id from city where province_id = "+proid;
						ResultSet cityResult=conn.createStatement().executeQuery(cityQuery);
						while(cityResult.next()){
							cityMap.put(cityResult.getString("city"), cityResult.getInt("_id"));
							
							System.out.println("city:"+cityResult.getString("city")+",id:"+cityResult.getInt("_id"));
						}
						provIdMap.put(proname, cityMap);
					}
				}
				statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
 
	   
//		if (null == cityIdMap) {
//			cityIdMap = new HashMap<String, Integer>();			 
//			String queryCity="select city,id from city ;";
//			try {
//				ResultSet result=statement .executeQuery(queryCity);
//				 if(null!=result){
//					 System.out.println("result is not null;"+cityName);
//					 while(result.next()){
//					 cityIdMap.put(result.getString("city"), result.getInt("id"));
//				 
//					 }
//					  
//				 }
//				statement.close();
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		HashMap curCitymap = (HashMap) provIdMap.get(provinceName);
		System.out.println("size of this map is :"+curCitymap.size()+"city"+cityName);
		int cityId = (Integer) curCitymap.get(cityName);

		return cityId;
	}
    private String CreateUpdateSQL(String phoneNumber,String CityName,int cityID, String postfix){
	  String sql=null;
	  int  number2City=0;
	  String tableName=null;
	  if(phoneNumber.length()<7){
		  number2City=Integer.valueOf(phoneNumber);
		  tableName="number_0";
	  }else if(phoneNumber.length()==7){
		  number2City=Integer.valueOf(phoneNumber.substring(3, 7))+1;
		  tableName="number_"+phoneNumber.substring(0, 3)+postfix;
		  if(!NUMBER_TABLES.containsKey(tableName.toLowerCase())){
			  CreateNumberTable(tableName);
			  NUMBER_TABLES.put(tableName, tableName);
		  }
	  }
	  sql="update "+tableName+" set city_id = "+cityID+" where rowid = "+number2City;
	  System.out.println("updated sql is:"+sql);
	  return sql;
  }
    public void Diff(){
    	int  total=0;
    	Statement statement=null;
    	Statement st=null;
    	PreparedStatement pstmt;
		try {
			statement=conn.createStatement();
			 st=conn.createStatement();
			 
	 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File file=new File(".");
		FileWriter writer=null;
		BufferedWriter bufferWrite=null;
		file.mkdir();
		try {
			file=new File(file.getCanonicalPath()+"diff.txt");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if(file.exists()){
			file.delete();
		}
		try {
			file.createNewFile();
			System.out.println("file path is"+file.getAbsolutePath());
		    writer=new FileWriter(file);
		    bufferWrite=new BufferedWriter(writer);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// get Tables from DB and parepare for diff
	 String sqlStr="select name from sqlite_master where name like 'number_%' order by name ;";	
	 ArrayList<String> Number_tables=new ArrayList<String>();
	 String tableName=null;
	 try {
		ResultSet tablesResult=statement.executeQuery(sqlStr);
		if(null!=tablesResult){
			while(tablesResult.next()){
				tableName=tablesResult.getString("name");
				if(tableName.endsWith("compare")||tableName.endsWith("00")){
					
				} else {
				Number_tables.add(tableName);
				}
				 
			}
			System.out.println("table size is :"+Number_tables.size());
		}
		
		Iterator <String>tableIterator=(Iterator) Number_tables.iterator(); 
		
		 
		String tarTable=null;
		String [] tablesArray= new String[50];
		Number_tables.toArray(tablesArray);
		int length=tablesArray.length;
		while(tableIterator.hasNext())
		 	{ //
		 		tableName=tableIterator.next();
		 		System.out.println("current table is "+tableName);
		 		tarTable=tableName+"_compare";
		 		sqlStr=" select city.[city], "+tableName+".rowid, city_compare.[city] as tarcity from  " + tableName+
		 				" left join city,"+tarTable+" left join city_compare,province,province_compare"+" where "+
		 				tableName+".city_id!=0 and "+
		 				tarTable+".city_id!=0  and "+
		 				"city.[province_id]=province.[id] and "+
		 				" city_compare.[province_id]=province_compare.[id] and  "+
		 				tableName+".[rowid]="+tarTable+".[rowid] and "+
		 				tableName+".[city_id]=city.[id] and "+
		 				tarTable+".[city_id]=city_compare.[id] and "+
		 				" (city.[city]!=city_compare.[city]  or "+   
		 				"province.[province]!=province_compare.[province] );";
		 	 System.out.println("SQL is :"+sqlStr);
		 	 long beforeQuery=System.currentTimeMillis();
		 	ResultSet  compareResult=null;
		 	String tmpsql="select * from raw_data";
		 pstmt=conn.prepareStatement(sqlStr);
		 	 compareResult=pstmt.executeQuery();
		 	long afterQuery=System.currentTimeMillis();
		 	System.out.println("query cost..."+(afterQuery-beforeQuery));
				if (null != compareResult) {
					int i=0;
					System.out.println("compareResult is not null, and size is :");
					 
				 	while (compareResult.next())
					{
						i++;
						beforeQuery=System.currentTimeMillis();
						System.out.println("Diff:"+i+" " + tableName + " -->"
								+ compareResult.getString(1) + ","
								+ compareResult.getString(2)+","
								+compareResult.getString(3));
						bufferWrite.write("different:" + tableName + " -->"
								+ compareResult.getString(1) + ","
								+ compareResult.getString(2) + ","
								+ compareResult.getString(3)+"\n");
						afterQuery=System.currentTimeMillis();
						System.out.println("get one value: time cost "+(afterQuery-beforeQuery)+" prepare for next value");
						
					}
				 	compareResult.close();
					 System.out.println(" exist this loop, prepare for next");	 
					// bufferWrite.flush(); 
				}
				 System.out.println("  prepare for next");	 
		 		
		 	}
		
		
	} catch (SQLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
		
		try {
			statement.close();
			bufferWrite.flush();
			bufferWrite.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
   public static void setFilePath(String filepath){
	   filePath=filepath;
   }
   public String RowId2PhoneNumber(String rowid){
	   String formattedPhoneNumber="";
	   int phoneNumberint=Integer.valueOf(rowid).intValue()-1;
	   String phoneNumberStr=String.valueOf(phoneNumberint);
	   int length=phoneNumberStr.length();
	   if(length==1){
		   formattedPhoneNumber="000"+phoneNumberStr;
	   } else if (length==2){
		   formattedPhoneNumber="00"+phoneNumberStr;
	   } else if (length==3){
		   formattedPhoneNumber="0"+phoneNumberStr;
	   }else if (length==4){
		   formattedPhoneNumber=phoneNumberStr;
	   }
	   return formattedPhoneNumber;
   }
   public void  ProcessInternationalCode(){
	   Statement statement=null;
	   Statement updateStatement=null;
	   try {
		statement=conn.createStatement();
		updateStatement=conn.createStatement();
		 
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	String queryRawdata="select * from raw_international;";
	try {
		ResultSet resultSet=statement.executeQuery(queryRawdata);
		int countryCode=0;
		String countryName=null;
		 Statement st=conn.createStatement();
		if(null!=resultSet){
		 
			while(resultSet.next()){
				
				countryCode=resultSet.getInt("number");
				countryName=resultSet.getString("country");
				System.out.println("Country code "+countryCode+", country name "+countryName);
//				String queryCountry="select * from country where country = '"+countryName+"'";
//				System.out.println(queryCountry);
//
//				ResultSet countryResult= st.executeQuery(queryCountry);
				int countryId=0;
				 { // insert new row
						 String addoneCountry="insert into country (country) values( '"+countryName+"')";
						 System.out.println(" add one country sql "+addoneCountry);
						 st.executeUpdate(addoneCountry);
						 String getIdSQL="select max(id) from country;";
						 ResultSet result= updateStatement.executeQuery(getIdSQL);
						 if(null!=result&&result.next()){
								countryId=result.getInt(1);
							}
						 
					}
				   
			
			String updateNumber00="update number_00 set country_id = "+	countryId+" where rowid="+countryCode;
			System.out.println("update number 00 sql is :"+updateNumber00);
			updateStatement.executeUpdate(updateNumber00);
			}
		}
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	try {
		updateStatement.close();
		statement.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   public void CleanDB(){
	   Statement st=null;
	   String sqlStr=null;
	   
	   try {
		   
		   int i=0;
		st=conn.createStatement();
		for(i=0;i<Tables.Tables_to_Compare.length;i++){
			sqlStr="drop table if exists "+Tables.Tables_to_Compare[i]+" ;";
			//st.execute(sqlStr);	
			System.out.println(sqlStr);
		}
		 
		sqlStr="drop table if exists raw_data;";
		st.execute(sqlStr);
		sqlStr="drop table if exists raw_data_toCompare;";
		st.execute(sqlStr);
		sqlStr="drop table if exists raw_international;";
		st.execute(sqlStr);
		 
		 
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   
   }
   
   public void CreateProvince(String sourceTable,String provinceTable){
	   String sql=null;
	   Statement st=null;
	   Statement updateSt=null;
	   System.out.println(" province table is:"+provinceTable);
	  // String insertSQL="insert into "+provinceTable+"(province) values(?)";
	   try {
		  st=conn.createStatement();
		  updateSt=conn.createStatement();
		  conn.setAutoCommit(false);
		  
		   
		 
		sql="select distinct provinceName from "+sourceTable;
		ResultSet provinceResult=st.executeQuery(sql);
		String provinceName=null;
		if(null!=provinceResult){
			while(provinceResult.next()){
				provinceName=provinceResult.getString("provinceName");
				sql="insert into "+provinceTable+" (province) values('"+provinceName+"');";
				System.out.println("insert sql is :"+sql);
				updateSt.execute(sql);
				 
			}
		 
 
		}
		conn.commit();
 
		if(null!=st){
			st.close();
			st=null;
			
		}
		if(null!=updateSt){
			updateSt.close();
			updateSt=null;
		}
		 
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
   }
   public void CreateCity(String source,String ProvinceTable,String cityTable){
	   Statement querySt=null;
	   Statement updateSt=null;
	   Statement st=null;
	   int provinceId=0;
	   String provinceName=null;
	   String cityName=null;
	   String sql=null;
	   try {
		querySt=conn.createStatement();
		updateSt=conn.createStatement();
		st=conn.createStatement();
		conn.setAutoCommit(false);
		sql="select * from "+ProvinceTable;
		ResultSet provinceResult=querySt.executeQuery(sql);
		if(null!=provinceResult){
			while(provinceResult.next()){
				provinceId=provinceResult.getInt("id");
				provinceName=provinceResult.getString("province");
				sql="select distinct cityname from "+source+" where provincename='"+provinceName+"' ;";
				ResultSet cityResult=st.executeQuery(sql);
				if(null!=cityResult){
					while(cityResult.next()){
						cityName=cityResult.getString(1);
						sql="insert into "+cityTable+"(city,province_id) values ( '"+cityName+"' ,"+provinceId+");";
						System.out.println("add city sql is :"+sql);
						updateSt.executeUpdate(sql);
					}
				}
			}
			conn.commit();
			if(null!=querySt){
				querySt.close();
				querySt=null;
			}
			if(null!=st){
				st.close();
				st=null;
			}
			if(null!=updateSt){
				updateSt.close();
				updateSt=null;
			}
			
		}
		
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   
   }
   
   public void CreateNumberTable(String TableName){
	   Statement st=null;
	   String sqlStr=null;
	   try {
		st=conn.createStatement();
		sqlStr="drop table if exists "+TableName+";";
		st.executeUpdate(sqlStr);
		//
		sqlStr="CREATE TABLE "+TableName+"( city_id INT);";
		st.executeUpdate(sqlStr);
		
		sqlStr="insert into "+TableName+" select * from number_0";
		st.execute(sqlStr);
		
		sqlStr="update "+TableName+" set city_id=0;";
		st.execute(sqlStr);
		System.out.println("Create "+TableName+" done !");
		st.close();
		st=null;
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
}

