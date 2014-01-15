package com.motorola.numberlocation;

import java.util.ArrayList;

public class ItemsArray {
private ArrayList<ItemInfo> Item = null;

public ItemsArray() {
	super();
	Item = new ArrayList<ItemInfo>();
}

public void setItem(ArrayList<ItemInfo> item) {
	this.Item = item;
}

public ArrayList<ItemInfo> getItem() {
	return Item;
}


}
