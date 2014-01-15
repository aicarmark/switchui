package com.motorola.numberlocation;

public class UpdateCollection {
	private int Count = 0;
	private ItemsArray Items = null;

	public void setCount(int count) {
		this.Count = count;
	}

	public int getCount() {
		return Count;
	}

	public void setItems(ItemsArray items) {
		this.Items = items;
	}

	public ItemsArray getItems() {
		return Items;
	}
}
