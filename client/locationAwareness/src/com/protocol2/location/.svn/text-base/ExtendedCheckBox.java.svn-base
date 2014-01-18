package com.protocol2.location;

public class ExtendedCheckBox implements Comparable<ExtendedCheckBox>
{          
    private String friendName = "";
    private boolean isChecked;
    public ExtendedCheckBox(String name, boolean checked)
    {
        friendName = name;
        isChecked = checked;
    }
    public void setChecked(boolean value)
    {
        this.isChecked = value; 
    }
    public boolean getChecked()
    {
        return isChecked;
    }  
 
    public String getText() 
    {
         return friendName;
    }
    public void setText(String text) 
    {
         friendName = text;
    }
 
    //Should change to identifying by unique ID because friends can have same name
    //@Override
 
    public int compareTo(ExtendedCheckBox other)
    {
         if(friendName != null)
              return friendName.compareTo(other.getText());
         else
              throw new IllegalArgumentException();
    }
 
}