package com.protocol2.location;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class Settings extends Activity {
	
	private SeekBar proximityControl;
	private TextView  mCurGridsize;
	private SharedPreferences gridProximity;
	
	
	private static final int MINIMUM = 1500; // minimum size of grid, in feet.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.settings_screen);
	    
	    proximityControl = (SeekBar) findViewById(R.id.proximitySlider);
	    mCurGridsize = (TextView) findViewById(R.id.curGridsize);
	    
	    gridProximity = getSharedPreferences("gridProximity", MODE_PRIVATE); // set up a "preference" that will store the first location with respect to grid 1
	    int tempGrid = gridProximity.getInt("gridProximity", -1);
	    if (tempGrid == -1)
	    {
	    	mCurGridsize.setText("Current grid size (in feet): 1500");
	    }
	    else
	    {
	    	mCurGridsize.setText("Current grid size (in feet): " + tempGrid);
	    }
	    
	    
	    proximityControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				progress += MINIMUM;
				// Save preference as grid size
			    SharedPreferences.Editor gridEditor = gridProximity.edit();
			    gridEditor.putInt("gridProximity", progress); // save the grid size (but first convert it to meters) as a preference. 
			    gridEditor.commit();
			    
			    mCurGridsize.setText("Current grid size (in feet): " + progress);
				
			}
		});
	   
	}
}
