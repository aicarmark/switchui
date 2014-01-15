package com.motorola.quicknote;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.motorola.quicknote.QuickNotesDB.QNColumn;

public class QNNotesList extends ListActivity {
    private static final String TAG = QNNotesList.class.getSimpleName();
    private static final String[] PROJECTION = new String[] {
        QNColumn._ID.column(), // 0
        QNColumn.TITLE.column(), // 1
        QNColumn.WIDGETID.column()
    };
    
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_WIDGETID = 2;
    
    public static final int MENU_ITEM_DELETE = Menu.FIRST;
    
    private Cursor _cursor = null;
    private int _delete_item = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
         //Cursor cursor = managedQuery(QuickNotesDB.QNotes.CONTENT_URI, PROJECTION, null,
        //        "modified DESC");

        ListView listView = getListView();
         // Inform the list we provide context menus for items
        listView.setOnCreateContextMenuListener(this);
        
        _cursor = getContentResolver().query(QuickNotesDB.CONTENT_URI, PROJECTION, null, null, null);
        if (_cursor == null) { return; }

        _cursor.moveToFirst();
        
        
        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, _cursor,
                new String[] { QNColumn.TITLE.column() }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            QNDev.qnAssert(false);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                 // Delete the note that the context menu is for
                _delete_item = (int)info.id - 1;
                AlertDialog dialog = (new AlertDialog.Builder(this))
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.delete_note))
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                _cursor.moveToPosition(_delete_item);
                                int    wId    = _cursor.getInt(COLUMN_INDEX_WIDGETID);
                                Intent i = new Intent(QNConstants.INTENT_ACTION_REMOVE_WIDGET);
                                i.putExtra("appWidgetIdToBeDeleted", wId);
                                sendBroadcast(i);
                            }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
                dialog.show();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        int position = 0;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            QNDev.qnAssert(false);
            return;
        }

        position = info.position;

        if(position > 0 ){
            position = position -1;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);
        cursor.close();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        _cursor.close();
    }

}
