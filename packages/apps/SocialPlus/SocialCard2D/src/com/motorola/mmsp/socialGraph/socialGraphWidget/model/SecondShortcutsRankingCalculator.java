package com.motorola.mmsp.socialGraph.socialGraphWidget.model;

import java.util.ArrayList;

import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;

public class SecondShortcutsRankingCalculator extends ShortcutsRankingCalculator{ 
	
	public SecondShortcutsRankingCalculator(ArrayList<Shortcut> oldShortcuts,
			ArrayList<Integer> hiddenContacts, ArrayList<Integer> newTops,
			int[] posAuto) {
		super(oldShortcuts, hiddenContacts, newTops, posAuto);

	}
	
	@Override
	public void calculate() { 
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
			
		ArrayList<Integer> newTopsNoneHidden = (ArrayList<Integer>)this.newTops.clone();		
		for (int hiddenContact : this.hiddenContacts) {
			newTopsNoneHidden.remove((Object)Integer.valueOf(hiddenContact));
		}
		//if no people in frequency table, must fill shortcuts with zero
		if (newTopsNoneHidden == null || newTopsNoneHidden.size() <= 0) {
			// make the shortcut with zero content.
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				Shortcut shortcut = new Shortcut();
				shortcut.id = 0;
				shortcut.size = rankSizes.get(i);
				shortcut.pos = mPosAuto[i];
				this.shortcuts.add(i, shortcut);
			}
		} else {
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				Shortcut shortcut = new Shortcut();
				if( i < newTopsNoneHidden.size() ){					
					shortcut.id = newTopsNoneHidden.get(i);					
				}
				else {					
					shortcut.id = 0;				
				}
				shortcut.size = rankSizes.get(i);
				shortcut.pos = mPosAuto[i];
				shortcuts.add(i, shortcut);
				
			}
		}

		for(int newIndex = 0 ; ( oldShortcuts!=null ) && newIndex < shortcuts.size() ; newIndex++ ){
			if( shortcuts.get(newIndex).id != oldShortcuts.get(newIndex).id) {
				ChangeHistory his = new ChangeHistory();
				his.id = newIndex;
				his.type = ChangeHistory.CHANGE_POPULATED;
				chagneHistory.add(his);
			}
		}
		
//		ArrayList<Integer> oldShortcutContacts = new ArrayList<Integer>();
//		ArrayList<Integer> newShortcutContacts = new ArrayList<Integer>();
//		
//		for (int i=0; i < Constant.SHORTCUT_COUNT; i++) {
//			if (i >= oldShortcuts.size()) {
//				oldShortcutContacts.add(i, 0);
//			} else {
//				oldShortcutContacts.add(i,  oldShortcuts.get(i).id);
//			}
//		}
//		for (int i=0; i < Constant.SHORTCUT_COUNT; i++) {
//			if (i >= oldShortcuts.size()) {
//				newShortcutContacts.add(i, 0);
//			} else {
//				newShortcutContacts.add(i,  shortcuts.get(i).id);
//			}
//		}
		
		
	}
	
	
	
	
	
}
