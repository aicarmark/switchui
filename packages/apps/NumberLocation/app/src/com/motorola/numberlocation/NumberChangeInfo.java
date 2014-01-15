package com.motorola.numberlocation;

public class NumberChangeInfo {
	private AddCollection Add = null;
	private DeleteCollection Delete = null;
	private UpdateCollection Update = null;

	public NumberChangeInfo(AddCollection add, DeleteCollection delete,
			UpdateCollection update) {
		super();
		this.Add = add;
		this.Delete = delete;
		this.Update = update;
	}

	public NumberChangeInfo() {
		super();
	}

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
