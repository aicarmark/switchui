package com.motorola.mmsp.socialGraph.socialGraphWidget.model;

import java.util.ArrayList;

import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;

public class ShortcutsRankingCalculator {
	ArrayList<Shortcut> oldShortcuts = null;
	ArrayList<Integer> hiddenContacts = null;
	ArrayList<Integer> newTops = null;
	
	ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
	ArrayList<ChangeHistory> chagneHistory = new ArrayList<ChangeHistory>();
	
	int[] mPosAuto = null;
	
	public ShortcutsRankingCalculator(ArrayList<Shortcut> oldShortcuts,
			ArrayList<Integer> hiddenContacts,
			ArrayList<Integer> newTops, int[] posAuto) {
		this.oldShortcuts = oldShortcuts;
		if (this.oldShortcuts == null) {
			this.oldShortcuts = new ArrayList<Shortcut>();
		}
		
		this.hiddenContacts = hiddenContacts;
		if (this.hiddenContacts == null) {
			this.hiddenContacts = new ArrayList<Integer>();
		}
		
		this.newTops = newTops;
		if (this.newTops == null) {
			this.newTops = new ArrayList<Integer>();
		}
		
		mPosAuto = posAuto;
		
		Log.d("ShortcutsRanking", "start to calculate");
		calculate();
	}
	
	public void calculate() {
		ArrayList<Integer> oldShortcutContacts = new ArrayList<Integer>();
		ArrayList<Integer> oldShortcutContactsBk = new ArrayList<Integer>();
		ArrayList<Integer> oldShortcutSizes = new ArrayList<Integer>();
		ArrayList<Integer> newShortcutContacts = new ArrayList<Integer>();
		ArrayList<Integer> newShortcutSizes = new ArrayList<Integer>();
		
		//rankSizes save the size to fill newShortcutSizes
		ArrayList<Integer> rankSizes = new ArrayList<Integer>();
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			int shortcutSize = calculateSize(i);
			rankSizes.add(i, shortcutSize);
		}
		//debug log info
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			Log.d("ShortcutsRanking", "rank size[ " + i + " ] = "
					+ rankSizes.get(i));
		}// log
		
		//1. old shortcuts
		for (int i=0; i < Constant.SHORTCUT_COUNT; i++) {
			if (i >= oldShortcuts.size()) {
				oldShortcutContacts.add(i, 0);
				oldShortcutSizes.add(i,0);
			} else {
				Shortcut shortcut = oldShortcuts.get(i);
				oldShortcutContacts.add(i, shortcut.id);
				oldShortcutSizes.add(i, shortcut.size);
			}
		}
		oldShortcutContactsBk = (ArrayList<Integer>)oldShortcutContacts.clone();
		
		//2. calculate new contact and size to shown on widget.
		//2.1 calculate the contacts need to populate.
		ArrayList<Integer> newTopsNoneHidden = (ArrayList<Integer>)this.newTops.clone();
		
		//debug log info
		for (int i = 0; i < newTopsNoneHidden.size(); i++) {
			Log.d("ShortcutsRanking", "new tops befroe hidden person id = "
					+ newTopsNoneHidden.get(i));
		}//log
		
		for (int hiddenContact : this.hiddenContacts) {
			newTopsNoneHidden.remove((Object)Integer.valueOf(hiddenContact));
		}
		
		//debug log info
		for (int i = 0; i < newTopsNoneHidden.size(); i++) {
			Log.d("ShortcutsRanking", "new tops after hidden person id = "
					+ newTopsNoneHidden.get(i));
		}//log
		
		//2.1 if no people in frequency table, must fill shortcuts with zero
		if (newTopsNoneHidden == null || newTopsNoneHidden.size() <= 0) {
			// make the shortcut with zero content.
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				Shortcut shortcut = new Shortcut();
				shortcut.id = 0;
				shortcut.size = rankSizes.get(i);
				shortcut.pos = mPosAuto[i];
				this.shortcuts.add(i, shortcut);
			}
			
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				if (i >= newShortcutContacts.size()) {
					newShortcutContacts.add(i, 0);
				} else {
					newShortcutContacts.set(i, 0);
				}
			}
			// compare new and old contacts, and identify the changes.
			this.chagneHistory = new ArrayList<ChangeHistory>();
			// person id change
			for (int i = 0; i < newShortcutContacts.size(); i++) {
				int oldId = (i < oldShortcutContactsBk.size()) ? oldShortcutContactsBk
						.get(i)
						: 0;
				if (oldId != 0) {
					ChangeHistory changeHistoryItem = new ChangeHistory();
					changeHistoryItem.id = 0;
					changeHistoryItem.type = ChangeHistory.CHANGE_POPULATED;
					changeHistoryItem.data1 = 0;
					changeHistoryItem.data2 = mPosAuto[i];
					this.chagneHistory.add(changeHistoryItem);
				}

			}
			return;
		}
		
		//the biggest position
		//debug log info
		for (int i = 0; i < oldShortcutContacts.size(); i++) {
			Log.d("ShortcutsRanking", "before biggest old Shortcut Contacts person id = "
					+ oldShortcutContacts.get(i));
		}//log
		
		if (newTopsNoneHidden.get(0) != oldShortcutContacts.get(0)) {
			Integer person = newTopsNoneHidden.get(0);
			int oldIndex = oldShortcutContacts.indexOf(person);
			if (oldIndex >= 0) {
				oldShortcutContacts.set(oldIndex, oldShortcutContacts.get(0));
				oldShortcutContacts.set(0, person);
			} else {
				oldShortcutContacts.set(0, person);
			}
		}
				
		//debug log info
		for (int i = 0; i < oldShortcutContacts.size(); i++) {
			Log.d("ShortcutsRanking", "after biggest old Shortcut Contacts person id = "
					+ oldShortcutContacts.get(i));
		}//log
		
		//2.2 statics the position need to replace.
		ArrayList<Integer> replacePositions = new ArrayList<Integer>();
		//2.2.1 if the contact in this position is out of the range(9).
		ArrayList<Integer> readyToFillContacts = (ArrayList<Integer>)newTopsNoneHidden.clone();
		int replaceCount = 0;
		for (int i = 0; i < oldShortcutContacts.size(); i++) {
			int person = oldShortcutContacts.get(i);
			if (person != 0) {
				int find = newTopsNoneHidden.indexOf(person);
				Log.d("ShortcutsRanking", "oldcatcts person =" + person + " find position = " + find);
				if (find >= 0 && find < Constant.SHORTCUT_COUNT) {
					readyToFillContacts.remove(Integer.valueOf(person));
				} else {
					replacePositions.add(i);
					replaceCount++;
				}
			} else {
				if ((replaceCount < readyToFillContacts.size())
						&& (readyToFillContacts.get(replaceCount) != 0)) {
					replacePositions.add(i);
					replaceCount++;
				}
			}
		}		
		
		//2.2.2 if no contact in this position.
		//that will do more replace,so remove it and add it to up code
				
		//debug log info
		for (int i = 0; i < readyToFillContacts.size(); i++) {
			Log.d("ShortcutsRanking", "ready To Fill Contacts person id = "
					+ readyToFillContacts.get(i));
		}//log
		//debug log info
		for (int i = 0; i < replacePositions.size(); i++) {
			Log.d("ShortcutsRanking", "replace Positions = "
					+ replacePositions.get(i));
		}//log
		
		//2.3 fill the shortcuts contacts
		newShortcutContacts = (ArrayList<Integer>)oldShortcutContacts.clone();
		for (int i = 0; i < replacePositions.size(); i++) {
			int pos = replacePositions.get(i);
			if (readyToFillContacts.size() <= 0) {
				if (pos >= newShortcutContacts.size()) {
					newShortcutContacts.add(pos, 0);
				} else {
					newShortcutContacts.set(pos, 0);
				}
			} else {
				int person = readyToFillContacts.get(0);
				if (pos >= newShortcutContacts.size()) {
					newShortcutContacts.add(pos, person);
				} else {
					newShortcutContacts.set(pos, person);
				}
				readyToFillContacts.remove((int) 0);
			}
		}
		
		//2.4 calculate shortcuts' sizes
		int occupySizes = 0;
		for (int j = 0; j < newShortcutContacts.size(); j++) {
			int person = newShortcutContacts.get(j);
			if (person > 0) {
				int index = newTopsNoneHidden.indexOf(person);
				Log.d("ShortcutsRanking", "person index = " + index);
				if ((index >= 0) && (index < rankSizes.size())) {
					//debug log info 
					Log.d("ShortcutsRanking", "rankSize arrary size = " + rankSizes.size());
					Log.d("ShortcutsRanking", "newShortcutSizes arrary size = " + newShortcutSizes.size());
					//log
					
					int shortcutSize = rankSizes.get(index);
					
					//debug log info
					Log.d("ShortcutsRanking", "get shortcut sieze from rankSize = " + shortcutSize + " position = " + j);
					//log
					newShortcutSizes.add(j, shortcutSize);
					occupySizes++;
				} 
			} else {
				newShortcutSizes.add(j, 0);
			}
		}
		for (int j = 0; j < newShortcutSizes.size(); j++) {
			int size = newShortcutSizes.get(j);
			if (size == 0) {
				if (occupySizes < rankSizes.size()) {
					int shortcutSize = rankSizes.get(occupySizes);
					newShortcutSizes.set(j, shortcutSize);
					occupySizes++;
				}
			}
		}
		//debug log info
		for (int i = 0; i < newShortcutSizes.size(); i++) {
			Log.d("ShortcutsRanking", "new shortcut size" + i + ": "
					+ newShortcutSizes.get(i));
		}//log

		//2.5 if person is less than shortcut number, fill the left shortcuts with 0.
		for (int i = newShortcutContacts.size(); i < Constant.SHORTCUT_COUNT; i++) {
			newShortcutContacts.add(i, 0);
		}
		
		for (int i = newShortcutSizes.size(); i < Constant.SHORTCUT_COUNT; i++) {
			newShortcutSizes.add(i, 0);
		}
		//debug log info
		for (int i = 0; i < newShortcutContacts.size(); i++) {
			Log.d("ShortcutsRanking", "new shortcut contacts person id = "
					+ newShortcutContacts.get(i));
		}//log
		
		//make the shortcut from contact array and size array.
		for (int i =0; i < Constant.SHORTCUT_COUNT; i++) {
			Shortcut shortcut = new Shortcut();
			if (i < newShortcutContacts.size()) {
				shortcut.id = newShortcutContacts.get(i);
			} else {
				shortcut.id = 0;
			}
			if (i < newShortcutSizes.size()) {
				shortcut.size = newShortcutSizes.get(i);
			} else {
				shortcut.size = 0;
			}
			
			shortcut.pos = mPosAuto[i];
			this.shortcuts.add(i, shortcut);
		}
		//debug log info
		for (int i = 0; i < this.shortcuts.size(); i++) {
			Log.d("ShortcutsRanking", "new shortcut id = "
					+ this.shortcuts.get(i).id + "size="
					+ this.shortcuts.get(i).size + "pos="
					+ this.shortcuts.get(i).pos);
		}//log
		
		//3.compare new and old contacts,sizes, and identify the changes.
		this.chagneHistory = new ArrayList<ChangeHistory>();
		//person id change
		for (int i = 0; i < newShortcutContacts.size(); i++) {
			int id = newShortcutContacts.get(i);
			
			if (id == 0) {
				int oldId = (i < oldShortcutContactsBk.size()) ? oldShortcutContactsBk
						.get(i)
						: 0;
				if(oldId != 0){
					ChangeHistory changeHistoryItem = new ChangeHistory();
					changeHistoryItem.id = id;
					changeHistoryItem.type = ChangeHistory.CHANGE_POPULATED;
					changeHistoryItem.data1 = 0;
					changeHistoryItem.data2 = mPosAuto[i];
					this.chagneHistory.add(changeHistoryItem);
				}

			} else {
				int oldIndex = oldShortcutContactsBk.indexOf(id);
				if (oldIndex == i) {
					// none change
				} else if (oldIndex < 0) {
					ChangeHistory changeHistoryItem = new ChangeHistory();
					changeHistoryItem.id = id;
					changeHistoryItem.type = ChangeHistory.CHANGE_POPULATED;
					changeHistoryItem.data1 = 0;
					changeHistoryItem.data2 = mPosAuto[i];
					this.chagneHistory.add(changeHistoryItem);

				} else {
					ChangeHistory changeHistoryItem = new ChangeHistory();
					changeHistoryItem.id = id;
					changeHistoryItem.type = ChangeHistory.CHANGE_POSITION;
					changeHistoryItem.data1 = mPosAuto[oldIndex];
					changeHistoryItem.data2 = mPosAuto[i];
					this.chagneHistory.add(changeHistoryItem);
				}
			}
		}
		//shortcut size change
		/*boolean fullFilled = newTopsNoneHidden.size() >= Constant.SHORTCUT_COUNT ? true
				: false;*/
		if (/*fullFilled*/true) {
			ArrayList<Integer> diffSizes = new ArrayList<Integer>();
			ArrayList<Integer> diffIndex = new ArrayList<Integer>();
			int diffNum = 0;
			for (int i = 0; i < newShortcutSizes.size(); i++) {
				int newSize = newShortcutSizes.get(i);
				int oldSize = oldShortcutSizes.get(i);

				if ((newSize != oldSize) ) {
					diffSizes.add(diffNum, newSize);
					diffIndex.add(diffNum, i);
					diffNum++;
				}

			}
			
			Log.d("ShortcutsRanking", "diffSizes array size" + diffSizes.size());
			
			while (diffSizes.size() > 1) {
				for (int i = 1; i < diffSizes.size(); i++) {
					if ((diffSizes.get(0) != diffSizes.get(i))) {
						ChangeHistory changeHistoryItem = new ChangeHistory();
						changeHistoryItem.id = 0;
						changeHistoryItem.type = ChangeHistory.CHANGE_SIZE;
						changeHistoryItem.data1 = mPosAuto[diffIndex.get(0)];
						changeHistoryItem.data2 = mPosAuto[diffIndex.get(i)];
						this.chagneHistory.add(changeHistoryItem);
						diffSizes.remove(0);
						diffSizes.remove(i - 1);
						diffIndex.remove(0);
						diffIndex.remove(i - 1);
						break;
					}
				}
				break;
			}
		}
		//debug log info
		Log.d("ShortcutsRanking", "change history count=" + this.chagneHistory.size());
		for (int i = 0; i < this.chagneHistory.size(); i++) {
			ChangeHistory changeHistoryItem = this.chagneHistory.get(i);
			Log.d("ShortcutsRanking", "change history " + i + ": id = "
					+ changeHistoryItem.id + " type= " + changeHistoryItem.type
					+ " data1=" + changeHistoryItem.data1 + " data2="
					+ changeHistoryItem.data2);
		}//log
	}
	
	
	public static int calculateSize(int order) {
		int shortcutSize = 0;
		if (order < Constant.SHORTCUT_BIG_COUNT) {
			shortcutSize = Constant.SHORTCUT_SIZE_BIG;
		} else if (order < Constant.SHORTCUT_BIG_COUNT + Constant.SHORTCUT_MEDIUM_COUNT) {
			shortcutSize = Constant.SHORTCUT_SIZE_MEDIA;
		} else {
			shortcutSize = Constant.SHORTCUT_SIZE_SMALL;
		}
		return shortcutSize;
	}
	
	public ArrayList<Shortcut> getShortcuts() {
		return this.shortcuts;
	}
	
	public ArrayList<ChangeHistory> getChangeHistroy() {
		return this.chagneHistory;
	}
}


class Shortcut {
	public int id;
	public int pos;
	public int size;
}

class ChangeHistory {
	public static final int CHANGE_NONE = 0;
	public static final int CHANGE_SIZE = 1;
	public static final int CHANGE_POSITION = 2;
	public static final int CHANGE_POPULATED = 3;
	public int id;
	public int type;
	public int data1 = -1;
	public int data2 = -1;
}