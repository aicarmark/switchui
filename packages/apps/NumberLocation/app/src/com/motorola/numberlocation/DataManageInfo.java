package com.motorola.numberlocation;

public class DataManageInfo {
	private AddCollection Add = null;
	private DeleteCollection Delete = null;
	private UpdateCollection Update = null;

	public void setAdd(AddCollection add) {
		this.Add = add;
	}

	public AddCollection getAdd() {
		return Add;
	}

	public void setDelete(DeleteCollection delete) {
		this.Delete = delete;
	}

	public DeleteCollection getDelete() {
		return Delete;
	}

	public void setUpdate(UpdateCollection update) {
		this.Update = update;
	}

	public UpdateCollection getUpdate() {
		return Update;
	}
}
