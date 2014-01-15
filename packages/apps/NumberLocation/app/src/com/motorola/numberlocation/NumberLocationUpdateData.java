package com.motorola.numberlocation;

public class NumberLocationUpdateData {
	private int Status = 0;
	private String XMLParseVersion = null;
	private String DatabaseVersion = null;
	private NumberChangeInfo NumberChange = null;
	private DataManageInfo DataManage = null;

	public void setStatus(int status) {
		this.Status = status;
	}

	public int getStatus() {
		return Status;
	}

	public void setXMLParseVersion(String xMLParseVersion) {
		this.XMLParseVersion = xMLParseVersion;
	}

	public String getXMLParseVersion() {
		return XMLParseVersion;
	}

	public void setDatabaseVersion(String databaseVersion) {
		this.DatabaseVersion = databaseVersion;
	}

	public String getDatabaseVersion() {
		return DatabaseVersion;
	}

	public void setNumberChange(NumberChangeInfo numberChange) {
		this.NumberChange = numberChange;
	}

	public NumberChangeInfo getNumberChange() {
		return NumberChange;
	}

	public void setDataManage(DataManageInfo dataManage) {
		this.DataManage = dataManage;
	}

	public DataManageInfo getDataManage() {
		return DataManage;
	}
}
