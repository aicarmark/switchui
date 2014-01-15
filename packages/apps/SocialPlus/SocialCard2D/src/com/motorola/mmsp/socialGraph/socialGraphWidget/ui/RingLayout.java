package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import java.util.HashMap;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.motorola.mmsp.socialGraph.socialGraphWidget.Contact;
import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel.ModelListener;


@SuppressWarnings("deprecation")
public class RingLayout extends AbsoluteLayout {
	private static final String TAG = "SocialGraphWidget";
	public static final int SIZE_TYPE_1 = 1;
	public static final int SIZE_TYPE_2 = 2;
	public static final int SIZE_TYPE_3 = 3;
	
	private RingLayoutModel model = null;
	private OnItemClickListener l_click = null;
	
	private ModelListener mModelListener = new ModelListener() {				
		//@Override
		public void onImageStretchChange(boolean bStretch) {
			loadDataFromModel(RingLayout.this.model);
		}
		
		//@Override
		public void onContactChange(int pos, Contact contact, int size, boolean mode) {
			loadDataFromModel(RingLayout.this.model);
		}
		
		//@Override
		public void onAllContactChange(HashMap<Integer, Contact> contacts,
				HashMap<Integer, Integer> sizes, boolean mode) {
			loadDataFromModel(RingLayout.this.model);
		}
		
		public void onDataLoadFinish() {

		}
	};
	
	public RingLayout(Context context) {
		super(context);
	}

	public RingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RingLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
	}
	

	public void setModel(RingLayoutModel model) {
		clearModel();
		this.model = model;
		if (this.model != null) {
			this.model.addListener(mModelListener);
		}
		
		loadDataFromModel(this.model);
	}
	
	public void clearModel() {
		if (this.model != null) {
			this.model.removeListener(mModelListener);
		}
	}
	
	private void loadDataFromModel(RingLayoutModel model) {
		if (model != null) {
			ScaleType scaleType = model.bStrenchPhoto ? ScaleType.FIT_XY : ScaleType.CENTER_INSIDE;
			int[] photoIds = getPhotoIds();
			for (int i = 0; photoIds != null && i < photoIds.length; i++) {
				ImageView photo = (ImageView)findViewById(photoIds[i]);
				if (photo != null && model.contacts != null 
						&& model.contacts.get(i) != null) {
					photo.setScaleType(scaleType);
					photo.setImageBitmap(model.contacts.get(i).getPhoto());
				}
			}
			
			int[] nameIds = getNameIds();
			for (int i = 0; i < nameIds.length; i++) {
				TextView name = (TextView)findViewById(nameIds[i]);
				if (name != null && model.contacts != null &&model.contacts.get(i) != null) {
					String nameStr = model.contacts.get(i).getName();
					String realName = null;
					if (nameStr != null) {
						realName = nameStr.trim();
					}
					name.setText(realName);
					if (model.contacts.get(i) != null && model.contacts.get(i).getPerson() <= 0) {
						name.setBackgroundResource(0);
					} else {
						name.setBackgroundResource(bars_sizes[model.sizes.get(i)]);						
					}
				}
			}
			
			int[] btnTextIds = getTextIds();
			for (int i = 0; i < btnTextIds.length; i++) {
				TextView text = (TextView)findViewById(btnTextIds[i]);
				if (text != null && model.contacts != null) {
					if (i >= model.contacts.size() || (model.contacts.get(i) != null && model.contacts.get(i).getPerson() <= 0)) {
						text.setVisibility(VISIBLE);
					} else {
						text.setVisibility(GONE);
					}
				}
			}
			int[] btnImageIds = getButtonIds();
			for (int i = 0; i < btnImageIds.length; i++) {
				ImageButton button = (ImageButton)findViewById(btnImageIds[i]);
				if (button != null && model.contacts != null) {
					if (i >= model.contacts.size() || (model.contacts.get(i) != null && model.contacts.get(i).getPerson() <= 0)) {
						button.setVisibility(VISIBLE);
					} else {
						button.setVisibility(GONE);
					}
				}
			}
			
			int[] changeBtnImageIds = getChangeButtonIds();
			for (int i = 0; i < changeBtnImageIds.length; i++) {
				ImageButton button = (ImageButton)findViewById(changeBtnImageIds[i]);
				if (button != null && model.contacts != null) {
					if (i >= model.contacts.size() || (model.contacts.get(i) != null && model.contacts.get(i).getPerson() <= 0)) {
						button.setVisibility(GONE);						
					} else {
						button.setVisibility(VISIBLE);
					}
				}
			}
		}
	}
	
	public void setOnItemClickListener(OnItemClickListener l) {
		this.l_click = l;
		int[] buttonIds = getButtonIds();
		for (int i = 0 ; buttonIds != null && i < buttonIds.length; i++) {
			View v = findViewById(buttonIds[i]);
			if (v != null) {
				final int pos = i;
				v.setOnClickListener(new OnClickListener() {					
					//@Override
					public void onClick(View v) {
						if (l_click != null) {
							l_click.onItemClick(RingLayout.this, v, pos);
						}
					}
				});
			}
		}
		
		int[] changeButtonIds = getChangeButtonIds();
		for (int i = 0 ; changeButtonIds != null && i < changeButtonIds.length; i++) {
			View v = findViewById(changeButtonIds[i]);
			if (v != null) {
				final int pos = i;
				v.setOnClickListener(new OnClickListener() {					
					//@Override
					public void onClick(View v) {
						if (l_click != null) {
							l_click.onItemClick(RingLayout.this, v, pos);
						}
					}
				});
			}
		}
		
		int[] imagelayoutIds = getImagelayoutIds();
		for (int i = 0 ; imagelayoutIds != null && i < imagelayoutIds.length; i++) {
			View v = findViewById(imagelayoutIds[i]);
			if (v != null) {
				final int pos = i;
				v.setOnClickListener(new OnClickListener() {					
					//@Override
					public void onClick(View v) {
						if (l_click != null) {
							l_click.onItemClick(RingLayout.this, v, pos);
						}
					}
				});
			}
		}
		
		
		
		//add for bg
		int[] bgIds = getBgIds();
		for (int i = 0 ; bgIds != null && i < bgIds.length; i++) {
			View v = findViewById(bgIds[i]);
			if (v != null) {
				final int pos = i;
				v.setOnClickListener(new OnClickListener() {					
					//@Override
					public void onClick(View v) {
						if (l_click != null) {
							l_click.onItemClick(RingLayout.this, v, pos);
						}
					}
				});
			}
		}
	}
	
	
	
	private int[] getPhotoIds() {
		return photos_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private int[] getNameIds() {
		return names_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private int[] getButtonIds() {
		return button_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private int[] getChangeButtonIds() {
		return change_button_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private int[] getImagelayoutIds() {
		return imagelayout_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private int[] getTextIds() {
		return text_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	
	private int[] getBgIds() {
		return bgs_all[getContext().getResources().getInteger(R.integer.screen_size)];
	}
	
	private static int[] photos_hdpi = new int[] {
			R.id.image1,
			R.id.image2,
			R.id.image3,
			R.id.image4,
			R.id.image5,
			R.id.image6,
			R.id.image7,
			R.id.image8,
			R.id.image9,
	};
	private static int[] photos_mdpi = new int[] {
		R.id.image1,
		R.id.image2,
		R.id.image3,
		R.id.image4,
		R.id.image5,
		R.id.image6,
		R.id.image7,
	};
	
	private static int[] names_hdpi = new int[] {
			R.id.text1,
			R.id.text2,
			R.id.text3,
			R.id.text4,
			R.id.text5,
			R.id.text6,
			R.id.text7,
			R.id.text8,
			R.id.text9,
	};
	private static int[] names_mdpi = new int[] {
		R.id.text1,
		R.id.text2,
		R.id.text3,
		R.id.text4,
		R.id.text5,
		R.id.text6,
		R.id.text7,
	};
	
	private static int[] button_hdpi = new int[] {
			R.id.button1,
			R.id.button2,
			R.id.button3,
			R.id.button4,
			R.id.button5,
			R.id.button6,
			R.id.button7,
			R.id.button8,
			R.id.button9,
	};
	private static int[] button_mdpi = new int[] {
		R.id.button1,
		R.id.button2,
		R.id.button3,
		R.id.button4,
		R.id.button5,
		R.id.button6,
		R.id.button7,
	};

	
	private static int[] text_hdpi = new int[] {
		R.id.btntext1,
		R.id.btntext2,
		R.id.btntext3,
		R.id.btntext4,
		R.id.btntext5,
		R.id.btntext6,
		R.id.btntext7,
		R.id.btntext8,
		R.id.btntext9,
	};
	private static int[] text_mdpi = new int[] {
		R.id.btntext1,
		R.id.btntext2,
		R.id.btntext3,
		R.id.btntext4,
		R.id.btntext5,
		R.id.btntext6,
		R.id.btntext7,
	};
	
	private static int[] change_button_hdpi = new int[] {
		R.id.change_button1,
		R.id.change_button2,
		R.id.change_button3,
		R.id.change_button4,
		R.id.change_button5,
		R.id.change_button6,
		R.id.change_button7,
		R.id.change_button8,
		R.id.change_button9,
	};
	private static int[] change_button_mdpi = new int[] {
		R.id.change_button1,
		R.id.change_button2,
		R.id.change_button3,
		R.id.change_button4,
		R.id.change_button5,
		R.id.change_button6,
		R.id.change_button7,
	};
	
	private static int[] imagelayout_hdpi = new int[] {
		R.id.imagelayout1,
		R.id.imagelayout2,
		R.id.imagelayout3,
		R.id.imagelayout4,
		R.id.imagelayout5,
		R.id.imagelayout6,
		R.id.imagelayout7,
		R.id.imagelayout8,
		R.id.imagelayout9,
	};
	private static int[] imagelayout_mdpi = new int[] {
		R.id.imagelayout1,
		R.id.imagelayout2,
		R.id.imagelayout3,
		R.id.imagelayout4,
		R.id.imagelayout5,
		R.id.imagelayout6,
		R.id.imagelayout7,
	};
	
	private static int[] bgs_hdpi = new int[] {
		R.id.bg_IM_1,
		R.id.bg_IM_2,
		R.id.bg_IM_3,
		R.id.bg_IM_4,
		R.id.bg_IM_5,
		R.id.bg_IM_6,
		R.id.bg_IM_7,
		R.id.bg_IM_8,
		R.id.bg_IM_9
	};
	private static int[] bgs_mdpi = new int[] {
		R.id.bg_IM_1,
		R.id.bg_IM_2,
		R.id.bg_IM_3,
		R.id.bg_IM_4,
		R.id.bg_IM_5,
		R.id.bg_IM_6,
		R.id.bg_IM_7
	};
	
	private int photos_all[][] = new int[][] {
			null,
			photos_hdpi,
			photos_mdpi,
	};
	
	private int names_all[][] = new int[][] {
			null,
			names_hdpi,
			names_mdpi,
	};
	private int button_all[][] = new int[][] {
			null,
			button_hdpi,
			button_mdpi,
	};
	private int text_all[][] = new int[][] {
			null,
			text_hdpi,
			text_mdpi,
	};	
	
	private int bars_sizes[] = new int[] {
			0,
			R.drawable.bar_bg_big,
			R.drawable.bar_bg_medium,
			R.drawable.bar_bg_small,
	};
	
	private int change_button_all[][] = new int[][] {
			null,
			change_button_hdpi,
			change_button_mdpi,
	};
	
	private int imagelayout_all[][] = new int[][] {
			null,
			imagelayout_hdpi,
			imagelayout_mdpi,
	};

	
	private int bgs_all[][] = new int[][] {
			null,
			bgs_hdpi,
			bgs_mdpi,
	};
	
	public interface OnItemClickListener{
		public void onItemClick(View v, View item, int pos);
	}
}
