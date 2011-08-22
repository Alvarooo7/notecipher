/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")savedInstanceState;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.notepadbot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;


public class Notepadbot extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int REKEY_ID = Menu.FIRST + 2;
    private static final int SHARE_ID = Menu.FIRST + 3;
    private static final int VIEW_ID = Menu.FIRST + 4;
    
    private NotesDbAdapter mDbHelper;
    
    private Uri dataStream;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getIntent() != null)
		{
			
			if(getIntent().hasExtra(Intent.EXTRA_STREAM)) {
				dataStream = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
			}
			else
				dataStream = getIntent().getData();
			
		}
        
        SQLiteDatabase.loadLibs(this);
        
        setContentView(R.layout.notes_list);

        registerForContextMenu(getListView());

		if (savedInstanceState != null)
		{
			
		}
    }
    
    
    private void loadData ()
    {
    	
    	Intent passingIntent = new Intent(this,ImageStore.class);

		passingIntent.setData(dataStream);
		startActivityForResult(passingIntent, 1);
		
		dataStream = null;
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		

    	mDbHelper = NotesDbAdapter.getInstance(this);
    	
    	if (!mDbHelper.isOpen())
			showPassword();
    	else
    		fillData();
    	

    	if (dataStream != null)
			loadData();
	
	}


	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		 findViewById(R.id.listlayout).setOnTouchListener(new OnTouchListener ()
	        {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					createNote();
					return false;
				}
	        	
	        }
	        		
	        );
	}



	private void showPassword ()
    {
    	 // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setView(textEntryView)
            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	EditText eText = ((android.widget.EditText)textEntryView.findViewById(R.id.password_edit));
                	String password = eText.getText().toString();
                	
                	unlockDatabase(password);
                	
                	
                	eText.setText("");
                	System.gc();
                	
                }
            })
            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                }
            })
            .create().show();
    }
	
	private void showRekeyDialog ()
    {
    	 // This example shows how to add a custom layout to an AlertDialog
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setView(textEntryView)
            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                	EditText eText = ((android.widget.EditText)textEntryView.findViewById(R.id.password_edit));

                	String newPassword = eText.getText().toString();
                	
                	rekeyDatabase(newPassword);
                	
                	eText.setText("");
                	System.gc();
                	
                }
            })
            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked cancel so do some stuff */
                }
            })
            .create().show();
    }
    
    private void unlockDatabase (String password)
    {

    	try
    	{
    	
    		mDbHelper.open(password);
    		fillData();
    	}
    	catch (Exception e)
    	{
    		Toast.makeText(this, "Unable to unlock your notes. Are you sure you entered the right PIN?", Toast.LENGTH_LONG).show();
    		showPassword();
    	}
    }
    
    private void rekeyDatabase (String password)
    {

    	try
    	{
    	    	mDbHelper.rekey(password);    		

    	}
    	catch (Exception e)
    	{
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

    	}
    }
    
    private void fillData() {
        Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{NotesDbAdapter.KEY_TITLE};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
        	    new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        setListAdapter(notes);
        
        if (notes.isEmpty())
        {
        	Toast.makeText(this, "Tap anywhere to create a new note", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        menu.add(0, REKEY_ID, 0, R.string.menu_rekey);
        
        
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case INSERT_ID:
            createNote();
            return true;
        case REKEY_ID:
            showRekeyDialog();
            return true;            
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, VIEW_ID, 0, R.string.menu_view);
		menu.add(0, SHARE_ID, 0, R.string.menu_share);
		menu.add(0, DELETE_ID, 0, R.string.menu_delete);
        
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info;
    	
		switch(item.getItemId()) {
    	case DELETE_ID:
    		info = (AdapterContextMenuInfo) item.getMenuInfo();
	        mDbHelper.deleteNote(info.id);
	        fillData();
	        return true;
    	case SHARE_ID:
    		info = (AdapterContextMenuInfo) item.getMenuInfo();
    		shareEntry(info.id);
	     
	        return true;
    	case VIEW_ID:
    		info = (AdapterContextMenuInfo) item.getMenuInfo();
    		viewEntry(info.id);
	     
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    private void shareEntry(long id)
    {
    	Cursor note = mDbHelper.fetchNote(id);
    	 startManagingCursor(note);
    	 
    	 byte[] blob = note.getBlob(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_DATA));
         
         if (blob != null)
         {
        	 try
        	 {
        		 NoteUtils.shareImage(this, blob);
        	 }
        	 catch (IOException e)
        	 {
        		 Toast.makeText(this, "Error exporting image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        	 }
         }
         else
         {
        	 String body = note.getString(
                     note.getColumnIndexOrThrow(NotesDbAdapter.KEY_BODY));
        	 NoteUtils.shareText(this, body);
         }
         
         note.close();
    }
    
    private void viewEntry(long id)
    {
    	Cursor note = mDbHelper.fetchNote(id);
    	 startManagingCursor(note);
    	 
    	 byte[] blob = note.getBlob(note.getColumnIndexOrThrow(NotesDbAdapter.KEY_DATA));
         
         if (blob != null)
         {
        	 String title = note.getString(
                     note.getColumnIndexOrThrow(NotesDbAdapter.KEY_TITLE));
        	 
        	 NoteUtils.savePublicImage(this, title, blob);
        	 
         }
         
         note.close();
    }
    
    private void createNote() {
        Intent i = new Intent(this, NoteEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, NoteEdit.class);
        i.putExtra(NotesDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
    	mDbHelper = NotesDbAdapter.getInstance(this);

        fillData();
    }
    
    
   

	@Override
	protected void onStop() {
		super.onStop();
		
		  
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		NoteUtils.cleanupTmp(this);
		
	//	if (mDbHelper != null)
		//	mDbHelper.close();
	}


   
}
