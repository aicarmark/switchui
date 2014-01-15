package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.Skin;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.SkinLayout;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.SkinResources;

public class SetSkinActivity extends Activity{
	private final String TAG = "SocialGraphWidget";
	private AdapterView<Adapter> gallery;
	private SkinAdapter adapter;
	private SkinLayout socialPanel;
	int skin = 0;
	
	//@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.set_skin_activity);
		creatView(-1);
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.set_skin_activity);
		creatView(skin);
	}

	private void creatView(int skinSave) {
		socialPanel = (SkinLayout) findViewById(R.id.ringlayout);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			gallery = (AdapterView) findViewById(R.id.gallery_land);
		} else {
			gallery = (AdapterView) findViewById(R.id.gallery);
		}
		
		if (skinSave != -1) {
			skin = skinSave;
		} else {
			skin = Setting.getInstance(SetSkinActivity.this).getSkin();
		}

		adapter = new SkinAdapter(this);
		adapter.setHighlight(skin);
		gallery.setAdapter(adapter);
		gallery.setOnItemClickListener(new OnItemClickListener() {
			// @Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				socialPanel.setSkin(arg2);
				skin = arg2;
				if (adapter != null) {
					adapter.setHighlight(skin);
					adapter.notifyDataSetChanged();
				}
			}
		});
		
		gallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Log.d(TAG, "onItemSelected() is called.");
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
					if (adapter != null) {
						adapter.setHighlight(position);
						adapter.notifyDataSetChanged();
					}
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {

			}

		});

		gallery.setSelection(skin);
		
		socialPanel.setSkin(skin);
		final TextView set_skin = (TextView) findViewById(R.id.set_skin);
		set_skin.setOnClickListener(new OnClickListener() {
			// @Override
			public void onClick(View v) {
				Setting.getInstance(SetSkinActivity.this).setSkin(skin);
				finish();
			}
		});
	}
}

class SkinAdapter extends SimpleAdapter {
	private static final String BG = "bg";
	private int highlightInex = 0;
	
	public SkinAdapter(Context context) {
		this(context, getData(context), getResource(), getFrom(), getTo());
	}
	
	public SkinAdapter(Context context, List<? extends Map<String, ?>> data,
			int resource, String[] from, int[] to) {
		super(context, data, resource, from, to);
	}
	
	public void setHighlight(int index) {
		highlightInex = index;
	}
	
	private static List<? extends Map<String, ?>> getData(Context context) {
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String,Object>>();
		ArrayList<Skin> skins = SkinResources.getInstance(context).getSkins();
		int count = skins.size();		
		
		for (int i = 0; i < count; i++) {
			HashMap<String, Object> item = new HashMap<String, Object>();
			Skin skin = skins.get(i);
			item.put(BG, skin.properties.get(Skin.THUMBNAIL));
			data.add(item);
		}
		return data;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		if (view != null) {
			ImageView image = (ImageView) (view.findViewById(R.id.skinselected));
			if (position == highlightInex && image != null) {
				image.setImageResource(R.drawable.skin_thumbnail_selected);
			} else if (image != null) {
				image.setImageResource(0);
			}
		}
		
		return view;
	}
	
	private static int getResource() {
		return R.layout.set_skin_cell;
	}
	
	private static String[] getFrom() {
		return new String[] { BG };
	}
	
	private static int[] getTo() {
		return new int[] { R.id.bg };
	}
}
