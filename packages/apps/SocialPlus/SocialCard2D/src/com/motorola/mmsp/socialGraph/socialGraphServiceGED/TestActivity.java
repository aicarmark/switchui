package com.motorola.mmsp.socialGraph.socialGraphServiceGED;





import com.motorola.mmsp.socialGraph.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class TestActivity extends Activity {
	Button button;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		button=(Button)findViewById(R.id.test);
		button.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent test=new Intent();
				test.setAction("com.motorola.mmsp.intent.action.test");
				arg0.getContext().sendBroadcast(test);
				
			}
			
		});
	}
}