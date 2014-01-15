/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-10-03   IKTABLETMAIN-348    XQH748      initial
 */

package com.motorola.filemanager.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.RemoteContentFragment;

public class RemoteCustomDialogFragment extends CustomDialogFragment {

    static boolean mIsContextmenu = false;
    static private String mFileName = null;

    public static RemoteCustomDialogFragment newInstance(int id, boolean isContextMenu,
                                                         Context context) {
        RemoteCustomDialogFragment frag = new RemoteCustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        return frag;
    }

    public static RemoteCustomDialogFragment newInstance(int id, boolean isContextMenu,
                                                         Context context, String fileName) {
        RemoteCustomDialogFragment frag = new RemoteCustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        mFileName = fileName;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = null;
        int id = getArguments().getInt(("id"));
        switch (id) {
            case R.id.new_folder :
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View view = inflater.inflate(R.layout.dialog_new_folder, null);
                final EditText et = (EditText) view.findViewById(R.id.foldername);
                et.setText("");
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.create_new_folder)
                                .setView(view)
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.createNewFolder(et.getText().toString());
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                break;

            case DIALOG_DELETE :
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(getString(R.string.menu_delete))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage(getString(R.string.really_delete_selected))
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.handleDelete();
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                break;
            case DIALOG_RENAME :
                inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.dialog_new_folder, null);
                final EditText et2 = (EditText) view.findViewById(R.id.foldername);
                et2.setText(mFileName);
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.menu_rename)
                                .setView(view)
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.handleRename(et2.getText().toString(),
                                                    mIsContextmenu);

                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
            case DIALOG_RENAME_EXIST :
                inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.dialog_new_folder, null);
                final EditText et3 = (EditText) view.findViewById(R.id.foldername);
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.rename_failed_title)
                                .setMessage(getString(R.string.renaming_exist, mFileName))
                                .setView(view)
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.handleRename(et3.getText().toString(),
                                                    mIsContextmenu);

                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
            case DIALOG_RENAME_EMPTY :
                inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.dialog_new_folder, null);
                final EditText et4 = (EditText) view.findViewById(R.id.foldername);
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.rename_failed_title)
                                .setMessage(getString(R.string.renaming_empty))
                                .setView(view)
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.handleRename(et4.getText().toString(),
                                                    mIsContextmenu);

                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
            case DIALOG_RENAME_INVALID :
                inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.dialog_new_folder, null);
                final EditText et5 = (EditText) view.findViewById(R.id.foldername);
                dialog =
                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.rename_failed_title)
                                .setMessage(getString(R.string.renaming_invalid))
                                .setView(view)
                                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        RemoteContentFragment det =
                                                (RemoteContentFragment) getFragmentManager()
                                                        .findFragmentById(R.id.details);
                                        if (det != null) {
                                            det.handleRename(et5.getText().toString(),
                                                    mIsContextmenu);

                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                break;
        }

        return dialog;
    }

}
