package com.protocol2.location;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


public class ExtendedCheckBoxListView extends LinearLayout {
    private TextView friendName;
    private CheckBox checkBox;
    private TableLayout table;
    private TableRow row;
    private ExtendedCheckBox checkBoxText;
 
    public ExtendedCheckBoxListView(Context context, ExtendedCheckBox aCheckBoxifiedText) {
    	super(context);
    	
    	// Set orientation to be horizontal 
    	this.setOrientation(HORIZONTAL);
    	
    	table = new TableLayout(context);
    	table.setColumnStretchable(0, true);
   
    	checkBoxText = aCheckBoxifiedText;
    	checkBox = new CheckBox(context);
    	checkBox.setPadding(0, 0, 0, 0);
//    	checkBox.setGravity(Gravity.RIGHT); // set to the right
    	// Set the initial state of the checkbox. 
    	checkBox.setChecked(aCheckBoxifiedText.getChecked());
    	// Set the right listener for the checkbox, used to update 
    	// our data holder to change it's state after a click too
    	
    	checkBox.setOnClickListener( new OnClickListener()
    	{
    		/**
    		 *  When clicked change the state of the 'mCheckBoxText' too
             */
    		@Override	
    		public void onClick(View v) {
    			checkBoxText.setChecked(getCheckBoxState());
    		} 
         });        
	
  		friendName = new TextView(context);
   		friendName.setText(aCheckBoxifiedText.getText());
   		friendName.setTextColor(Color.BLACK);
   		friendName.setTextSize(17);
   		friendName.setPadding(10, 0, 0, 0);
   		friendName.setTypeface(Typeface.SERIF);
   		friendName.setGravity(Gravity.CENTER_VERTICAL);
   		
   		// 250 is the width of the text field -- should maybe change this number later
//   		addView(friendName, new LinearLayout.LayoutParams(
  //                 LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        // Remove some controls in order to prevent a strange flickering when clicking on the TextView!
   		friendName.setClickable(false);
   		friendName.setFocusable(false);
   		friendName.setFocusableInTouchMode(false);
   		
   		row = new TableRow(context);
   		row.addView(friendName, new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
   		row.addView(checkBox, new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
   	
    	// Add the checkbox		
   		table.addView(row,  new TableLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
   		
   		addView(table, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
 
   		setOnClickListener( new OnClickListener() 
   		{
   			/**
   			 * Check or unchecked the current checkbox!
   			 */
   			@Override 
   			public void onClick(View v) {
   				toggleCheckBoxState(); 
   			}
         });	        
    }	
    
    public void setText(String words) 
    {
    	friendName.setText(words);
    }  	
    	
    public void toggleCheckBoxState() 
    {	
    	setCheckBoxState(!getCheckBoxState());
    }	
    
    public void setCheckBoxState(boolean bool) 
    { 	
    	checkBox.setChecked(bool);
       	checkBoxText.setChecked(bool); 
    }	

    public boolean getCheckBoxState()
    {
        return checkBox.isChecked();
    }
}