package com.motorola.numberlocation;

public class ItemInfo {
	
	private String number;
	private String province;
	private String city;
	private String country;
	private int numberType;;
	

	public ItemInfo(String number, String province, String city, String country) {
		super();
		this.number = number;
		this.province = province;
		this.city = city;
		this.country = country;
	}

	public ItemInfo(String number) {
		super();
		this.number = number;
		this.province = null;
		this.city = null;
		this.country = null;
	}

	public ItemInfo(String province, String city) {
		super();
		this.province = province;
		this.city = city;
		this.number = null;
		this.country = null;
	}

	

	public ItemInfo() {
		super();
		this.number = null;
		this.province = null;
		this.city = null;
		this.country = null;
	}

	public void setNumber(String number) {
		this.number = number;
		this.numberType = getNumberType(number);
	}

	public String getNumber() {
		return number;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getProvince() {
		return province;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return city;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public int getNumberType() {
		return numberType;
	}
	
	public void setNumberType(int numberType) {
		this.numberType = numberType;
	}
	
	private int getNumberType(String number) {
		int numberType = NumberLocationConst.NUMBER_TYPE_UNKNOW;
		if (number.startsWith("00", 0)) {
			if (number.length() < 6)
				numberType = NumberLocationConst.NUMBER_TYPE_UNKNOW;
			else
				numberType = NumberLocationConst.NUMBER_TYPE_FIX_INTERNATIONAL;
		} else if (number.startsWith("0", 0)) {
			if (number.length() < 5)
				numberType = NumberLocationConst.NUMBER_TYPE_UNKNOW;
			else
				numberType = NumberLocationConst.NUMBER_TYPE_FIX_DOMESTIC;
		} else if (number.startsWith("1", 0)) {
			if (number.length() < 7)
				numberType = NumberLocationConst.NUMBER_TYPE_UNKNOW;
			else
				numberType = NumberLocationConst.NUMBER_TYPE_MOBILE;
		} else {
			numberType = NumberLocationConst.NUMBER_TYPE_UNKNOW;
		}
		return numberType;
	}
}
