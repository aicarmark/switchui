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
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.LocalColumnViewFrameLeftFragment;
import com.motorola.filemanager.local.LocalFileOperationsFragment;
import com.motorola.filemanager.local.LocalLeftFileManagerFragment;
import com.motorola.filemanager.samba.RemoteContentFragment;

public class CustomDialogFragment extends DialogFragment {

    static boolean mIsContextmenu = false;
    public static final int DIALOG_NEW_FOLDER = 6;
    public static final int DIALOG_DELETE = 7;
    public static final int DIALOG_RENAME = 8;
    public static final int DIALOG_ZIP = 9;
    public static final int DIALOG_EXTRACT = 10;
    public static final int DIALOG_UNZIP_OVERWRITE = 11;
    public static final int DIALOG_SHORTCUT_RENAME = 12;
    public static final int DIALOG_RENAME_EXIST = 13;
    public static final int DIALOG_RENAME_EMPTY = 14;
    public static final int DIALOG_RENAME_INVALID = 15;
    public static final int DIALOG_ZIP_ENCRYPTION = 16;
    public static final int DIALOG_SHORTCUT_DELETE = 17;
    public static final int DIALOG_ZIP_OPTION =18;
    static private Context mContext = null;
    static private String mFileName = null;
    static boolean mHandledByLeft = false;

    public static CustomDialogFragment newInstance(int id, boolean isContextMenu, Context context) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        mContext = context;
        mHandledByLeft = false;

        return frag;
    }

    public static CustomDialogFragment newInstance(int id, boolean isContextMenu, Context context,
                                                   boolean handledByLeft) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        mContext = context;
        mHandledByLeft = handledByLeft;
        return frag;
    }

    public static CustomDialogFragment newInstance(int id, boolean isContextMenu, Context context,
                                                   String fileName) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        mContext = context;
        mFileName = fileName;
        mHandledByLeft = false;
        return frag;
    }

    public static CustomDialogFragment newInstance(int id, boolean isContextMenu, Context context,
                                                   String fileName, boolean handledByLeft) {
        CustomDialogFragment frag = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putInt("id", id);
        frag.setArguments(args);
        mIsContextmenu = isContextMenu;
        mContext = context;
        mFileName = fileName;
        mHandledByLeft = handledByLeft;
        return frag;
    }

    public LocalFileOperationsFragment getRightFragment() {
        Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
        LocalFileOperationsFragment rightFragmentCast = null;
        if ((rightFragment != null) &&
                !(rightFragment.getClass().getName().equals(RemoteContentFragment.class.getName()))) {
            if ((rightFragment != null) &&
                    rightFragment.getClass().getName()
                            .equals(LocalColumnViewFrameLeftFragment.class.getName())) {
                if (mHandledByLeft) {
                    rightFragmentCast =
                            ((LocalColumnViewFrameLeftFragment) rightFragment)
                                    .getLeftMostFragment();
                } else {
                    rightFragmentCast =
                            ((LocalColumnViewFrameLeftFragment) rightFragment).getColumnFragment();
                }
            } else {
                rightFragmentCast = (LocalFileOperationsFragment) rightFragment;
            }
        }
        return rightFragmentCast;
    }

    public LocalFileOperationsFragment getLeftFragment() {
        Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
        LocalFileOperationsFragment leftFragmentCast = null;
        if ((leftFragment != null) &&
                leftFragment.getClass().getName().equals(LocalLeftFileManagerFragment.class
                        .getName())) {
            leftFragmentCast = (LocalFileOperationsFragment) leftFragment;
        }
        return leftFragmentCast;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog dialog = null;
        int id = getArguments().getInt(("id"));
        final LocalFileOperationsFragment handlingFragmentCast;

        if (mHandledByLeft == false) {
            handlingFragmentCast = getRightFragment();
        } else {
            if (((FileManagerApp) getActivity().getApplication()).getViewMode() == FileManagerApp.COLUMNVIEW) {
                handlingFragmentCast = getRightFragment();
            } else {
                handlingFragmentCast = getLeftFragment();
            }
        }
        try {
            switch (id) {
                case DIALOG_NEW_FOLDER :
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    View view = inflater.inflate(R.layout.dialog_new_folder, null);
                    final EditText et = (EditText) view.findViewById(R.id.foldername);
                    et.setText("");
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.create_new_folder).setView(view)
                                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (handlingFragmentCast != null) {
                                                handlingFragmentCast.createNewFolder(et.getText()
                                                        .toString());
                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
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
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (((FileManagerApp) getActivity().getApplication())
                                                    .getViewMode() == FileManagerApp.COLUMNVIEW &&
                                                    mHandledByLeft) {
                                                BaseFileManagerFragment fragment =
                                                        (BaseFileManagerFragment) getFragmentManager()
                                                                .findFragmentById(R.id.details);
                                                fragment.handleDelete(false);
                                            } else {
                                                if (handlingFragmentCast != null) {
                                                    handlingFragmentCast
                                                            .handleDelete(mIsContextmenu);
                                                }
                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    break;
                case DIALOG_RENAME :
                    inflater = LayoutInflater.from(getActivity());
                    view = inflater.inflate(R.layout.dialog_new_folder, null);
                    final EditText et2 = (EditText) view.findViewById(R.id.foldername);
                    et2.setText(mFileName);
                    dialog =
                            new AlertDialog.Builder(getActivity()).setTitle(R.string.menu_rename)
                                    .setView(view).setPositiveButton(android.R.string.ok,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    if (handlingFragmentCast != null) {
                                                        handlingFragmentCast.handleRename(et2
                                                                .getText().toString(),
                                                                mIsContextmenu);

                                                    }
                                                }
                                            }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
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
                                    .setView(view).setPositiveButton(android.R.string.ok,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    if (handlingFragmentCast != null) {
                                                        handlingFragmentCast.handleRename(et3
                                                                .getText().toString(),
                                                                mIsContextmenu);

                                                    }
                                                }
                                            }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
                case DIALOG_RENAME_EMPTY :
                    inflater = LayoutInflater.from(getActivity());
                    view = inflater.inflate(R.layout.dialog_new_folder, null);
                    final EditText et4 = (EditText) view.findViewById(R.id.foldername);
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.rename_failed_title)
                                    .setMessage(getString(R.string.renaming_empty)).setView(view)
                                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (handlingFragmentCast != null) {
                                                handlingFragmentCast.handleRename(et4.getText()
                                                        .toString(), mIsContextmenu);

                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
                case DIALOG_RENAME_INVALID :
                    inflater = LayoutInflater.from(getActivity());
                    view = inflater.inflate(R.layout.dialog_new_folder, null);
                    final EditText et5 = (EditText) view.findViewById(R.id.foldername);
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.rename_failed_title)
                                    .setMessage(getString(R.string.renaming_invalid)).setView(view)
                                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (handlingFragmentCast != null) {
                                                handlingFragmentCast.handleRename(et5.getText()
                                                        .toString(), mIsContextmenu);

                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
 /*               case DIALOG_ZIP_OPTION :
                    final String[] items = {getResources().getString(R.string.zip_encryption_type_normal),
                            getResources().getString(R.string.zip_encryption_type_legacy),
                            getResources().getString(R.string.zip_encryption_type_128),
                            getResources().getString(R.string.zip_encryption_type_256)};

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                                     .setTitle(getResources().getString(R.string.compress_using))
                                     .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int index) {
                        if (handlingFragmentCast != null) {
                         switch(index){
                            case Zip.NO_ENCRYPTION:
                                   handlingFragmentCast.setEncryptionType(Zip.NO_ENCRYPTION);
                                   FragmentTransaction ft2 = getFragmentManager().beginTransaction();
                                   DialogFragment newFragment2 =
                                       CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_ZIP,
                                                 mIsContextmenu, mContext, handlingFragmentCast.getZipFileName(false), mHandledByLeft);
                                    // Show the dialog.
                                    newFragment2.show(ft2, "dialog");
                                    break;
                           case Zip.LEGACY:
                                   handlingFragmentCast.setEncryptionType(Zip.LEGACY);
                                   FragmentTransaction ft3 = getFragmentManager().beginTransaction();
                                   DialogFragment newFragment3 =
                                            CustomDialogFragment.newInstance(
                                              CustomDialogFragment.DIALOG_ZIP_ENCRYPTION, mIsContextmenu, mContext,
                                              handlingFragmentCast.getZipFileName(false), mHandledByLeft);
                                    // Show the dialog.
                                    newFragment3.show(ft3, "dialog");
                                    break;
                             case Zip.ONETWENTYEIGHT_BIT_AES:
                                 handlingFragmentCast.setEncryptionType(Zip.ONETWENTYEIGHT_BIT_AES);
                                 FragmentTransaction ft4 = getFragmentManager().beginTransaction();
                                 DialogFragment newFragment4 =
                                               CustomDialogFragment.newInstance(
                                               CustomDialogFragment.DIALOG_ZIP_ENCRYPTION, mIsContextmenu, mContext,
                                 handlingFragmentCast.getZipFileName(false), mHandledByLeft);
                                 // Show the dialog.
                                 newFragment4.show(ft4, "dialog");
                                 break;
                             case Zip.TWOFIFTYSIX_BIT_AES:
                                 handlingFragmentCast.setEncryptionType(Zip.TWOFIFTYSIX_BIT_AES);
                                 FragmentTransaction ft5 = getFragmentManager().beginTransaction();
                                 DialogFragment newFragment5 =
                                         CustomDialogFragment.newInstance(
                                         CustomDialogFragment.DIALOG_ZIP_ENCRYPTION, mIsContextmenu, mContext,
                                         handlingFragmentCast.getZipFileName(false), mHandledByLeft);
                                 // Show the dialog.
                                 newFragment5.show(ft5, "dialog");
                                 break;
                            }
                         }
                       }
                    });
                    dialog = builder.create();
                    break;


                case DIALOG_ZIP :
                    dialog = new ZipDialog(mContext, mFileName);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                            getString(R.string.create),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the keyboard since it sometimes
                                    // stays on screen until after Zip is done
                                    InputMethodManager inputMethodManager =
                                            (InputMethodManager) mContext
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                                    View v = ((ZipDialog) dialog).getWindow().peekDecorView();
                                    if (inputMethodManager != null && v != null) {
                                        inputMethodManager.hideSoftInputFromWindow(v
                                                .getWindowToken(), 0);
                                    }
                                    if (handlingFragmentCast != null) {
                                        handlingFragmentCast.handleZip(mIsContextmenu,
                                                ((ZipDialog) dialog).getZipFileName(),
                                                null,
                                                mHandledByLeft);
                                    }
                                }
                            });
                    dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                            getString(android.R.string.cancel),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
                case DIALOG_ZIP_ENCRYPTION :
                    int encryptType = Zip.NO_ENCRYPTION;
                    if (handlingFragmentCast != null) {
                        encryptType = handlingFragmentCast.getEncryptionType();
                    }
                    dialog = new ZipEncryptionDialog(mContext, mFileName, encryptType);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                            getString(R.string.create),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the keyboard since it sometimes
                                    // stays on screen until after Zip is done
                                    InputMethodManager inputMethodManager =
                                            (InputMethodManager) mContext
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                                    View v =
                                            ((ZipEncryptionDialog) dialog).getWindow()
                                                    .peekDecorView();
                                    if (inputMethodManager != null && v != null) {
                                        inputMethodManager.hideSoftInputFromWindow(v
                                                .getWindowToken(), 0);
                                    }
                                    if (handlingFragmentCast != null) {

                                        handlingFragmentCast.handleZip(mIsContextmenu,
                                                ((ZipEncryptionDialog) dialog).getZipFileName(),
                                                ((ZipEncryptionDialog) dialog).getPassword(),
                                                mHandledByLeft);
                                    }
                                }
                            });
                    dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                            getString(android.R.string.cancel),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
                case DIALOG_EXTRACT :
                    dialog = new ExtractDialog(mContext);
                    dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                            getString(R.string.extract),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the keyboard since it sometimes
                                    // stays on screen until after extract is
                                    // done

                                    InputMethodManager inputMethodManager =
                                            (InputMethodManager) mContext
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                                    View v = ((ExtractDialog) dialog).getWindow().peekDecorView();
                                    if (inputMethodManager != null && v != null) {
                                        inputMethodManager.hideSoftInputFromWindow(v
                                                .getWindowToken(), 0);
                                    }
                                    if (handlingFragmentCast != null) {
                                        handlingFragmentCast
                                                .extractZipFile(((ExtractDialog) dialog)
                                                        .getPassword());
                                    }

                                }
                            });
                    dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                            getString(android.R.string.cancel),
                            new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;

                case DIALOG_UNZIP_OVERWRITE :
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setMessage(getString(R.string.Overwrite_Contents))
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(R.string.overwrite, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (handlingFragmentCast != null) {
                                                handlingFragmentCast.handleStartUnzip();

                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();

                    break;
                    */
                case DIALOG_SHORTCUT_RENAME :
                    inflater = LayoutInflater.from(getActivity());
                    view = inflater.inflate(R.layout.dialog_new_folder, null);
                    final EditText shortcutet = (EditText) view.findViewById(R.id.foldername);
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.menu_rename_shorcut)
                                    .setMessage(R.string.message_rename_shorcut).setView(view)
                                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Fragment leftFragment =
                                                    getFragmentManager()
                                                            .findFragmentById(R.id.home_page);
                                            if ((leftFragment != null) &&
                                                    (leftFragment.getClass().getName()
                                                            .equals(HomePageLeftFileManagerFragment.class
                                                                    .getName()))) {
                                                ((HomePageLeftFileManagerFragment) leftFragment)
                                                        .renameCurrentShortcut(shortcutet.getText()
                                                                .toString());
                                            }

                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();
                    dialog.getWindow()
                            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    break;
                case DIALOG_SHORTCUT_DELETE :
                    dialog =
                            new AlertDialog.Builder(getActivity())
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setTitle(R.string.menu_delete_shorcut)
                                    .setMessage(getString(R.string.message_delete_shorcut))
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Fragment leftFragment =
                                                    getFragmentManager()
                                                            .findFragmentById(R.id.home_page);
                                            if ((leftFragment != null) &&
                                                    (leftFragment.getClass().getName()
                                                            .equals(HomePageLeftFileManagerFragment.class
                                                                    .getName()))) {
                                                ((HomePageLeftFileManagerFragment) leftFragment)
                                                        .deleteCurrentShortcut();
                                            }
                                        }
                                    }).setNegativeButton(android.R.string.cancel,
                                            new OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                }
                                            }).create();

                    break;

            }
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return dialog;
        }
    }
}
