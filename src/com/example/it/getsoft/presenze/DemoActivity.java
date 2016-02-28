package com.example.it.getsoft.presenze;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.R;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.zktechnology.android.app.ZKActivity;
import com.zktechnology.android.app.ZKUserPrivileges.UserPrivileges;
import com.zktechnology.android.app.data.type.ZKAttendanceEvent;
import com.zktechnology.android.app.data.type.ZKAttendanceLog;
import com.zktechnology.android.app.data.type.ZKDepartment;
import com.zktechnology.android.app.data.type.ZKEmployee;
import com.zktechnology.android.app.login.ZKLoginReceiver;
import com.zktechnology.android.app.reports.GenericReportForEntityAndDate;
import com.zktechnology.android.app.reports.reports.entity_attendance_report.EntityAttendanceReport;
import com.zktechnology.android.core.login.LoginCombination;
import com.zktechnology.android.helpers.gui.dialog.MessageManager;
import com.zktechnology.android.helpers.util.Log;
import com.zktechnology.android.helpers.util.ZKDate;
import com.zktechnology.util.StringHelper;

public class DemoActivity extends ZKActivity implements ZKLoginReceiver {
	
	private static final String TAG = DemoActivity.class.getCanonicalName();
	private EntityAttendanceReport fragmentLog;  // show report


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, TAG);
		setContentView(R.layout.activity_demo);
		fixViews();
	} // aggiunto commento VER 4

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.demo, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void fixViews() {
		 
		// Create a standard report fragment
		this.fragmentLog = new EntityAttendanceReport();
	 
		// Setup this report with a preloaded entity, no need of a dialog for
		// choosing which entity we want to see
		this.fragmentLog
				.setInterfaceMode(GenericReportForEntityAndDate.MODE_PRE_SELECTED_ENTITY);
	 
		// Check that the activity is using the layout version with the
		// fragment_container FrameLayout
		FrameLayout container = (FrameLayout) findViewById(R.id.container);
		if (container != null) {
			// add fragment to the fragment container layout
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, fragmentLog).commit();
		}
	}
	
	// inizio
	@Override
	protected List<UserPrivileges> allowedPrivilegeAccess() {
	    return null;
	}
	 
	@Override
	// Overriding this method we skip the check of current user permissions
	public boolean checkAccessPermission() {
		return true;
	}
	 
	@Override
	// Overriding this method we skip to check if there is a current user logged
	// (by default there should be an user logged to enter in a ZK Application)
	public boolean checkCurrentUser() {
		return true;
	}
	 
	@Override
	// This method allows developer to show/hide views based on the user
	// permissions
	public void checkViewsVisibility() {
	}
	
	/**
	 *  UI references
	 */
	public void onCreateEmployeeClicked(View v) {
		 
//		CreateEmployeeDialog dialog = new CreateEmployeeDialog();
//		dialog.show(this.getSupportFragmentManager(),
//				CreateEmployeeDialog.class.getName());
	}
	
	
// Our first functionality will be create Check-in / Check out buttons that let user punch and identificate itself.	
	public void onCheckIN(final View v) {
		createADummyLog(true);
		enableFPAndCard(true);
	}
	 
	public void onCheckOUT(final View v) {
		createADummyLog(false);
		enableFPAndCard(true);
	}
	
	// Log
	private ZKAttendanceLog currentLog;
	 
	private void createADummyLog(final boolean in) {
		currentLog = new ZKAttendanceLog();
		currentLog.idAttendanceEvent = 1l; // Work
		currentLog.idDevice = "Demo Device";
		currentLog.status = in ? ZKAttendanceEvent.STATUS_IN: ZKAttendanceEvent.STATUS_OUT;
	}
	
	// ask CoreService to start listening for user logins: 
	private void enableFPAndCard(final boolean b) {		 
		// Register all hardware login devices to start/stop listening
		getLoginHandler().registerAllLoginDevicesListener(b);	 
		if (b) {
			getLoginHandler().showLoginWindow();
		}
	}
	
	// I seguenti 3 metodi sono richiesti dalla extend
	@Override
	protected int getDBXml() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected InputStream getQueriesXML() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getValidationXML() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	protected ZKLoginReceiver setLoginHandlerCallback() {
		return this;
	}
	
	// aggiunti per implements	ZKLoginReceiver
	@Override
	public void onLoginReceived(final LoginCombination lc) {
		// si ricava login information a la si salva in  attendance_log table
		enableFPAndCard(false);
	 
		// Retrieve the user information from the LC
		currentLog.idEmployee = lc.getAssociatedUserInfo().id;
	 
		// Using ZKDate simplifies the java - date dealing
		currentLog.date = new ZKDate(lc.getTimestamp());
	 
		// Attempts to insert the log in the database
		if (currentLog.insert(this)) {
	 
			// Sample of using of ZKEmployee data type
			List<Long> ids = new ArrayList<Long>();
			ids.add(lc.getAssociatedUserInfo().id);
	 
			// Retrieves employee info from database (This could be done also
			// with lc.getAssociatedUserInfo())
			ZKEmployee employee = ZKEmployee.select(this, ids).get(0);
	 
			// Sample of a message using MessageManager
			MessageManager.putMessage(
					this,
					"New log for "
							+ StringHelper.getFullName(employee.first_name,
									employee.last_name),
					MessageManager.OK_MESSAGE);
		}
	}	
	 
	@Override
	public void onLoginFailed(Bundle bundle) {
	}
	 
	@Override
	public void onLoginAttemptProblem(Bundle bundle) {
	}
	
	
	@Override
	public void postRegisterApp() {
		// Load the user info
		fillInfo();
	 
		// Register this activity for changes in the tables
		registerTableListener();
	 
	}
	
	private void registerTableListener() {
		// Register this application for changes in attendance_log table
		this.setTableChangedListener(ZKAttendanceLog.TABLE_MAIN, true);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		// Unregister table listeners
		this.setTableChangedListener(ZKAttendanceLog.TABLE_MAIN, false);
	}
	
	@Override
	public void onTableChanged(final String table) {
		Log.d(TAG, "onTableChanged " + table);
	 
		// Run the changes on the UI thread
		this.runOnUiThread(new Runnable() {
	 
			@Override
			public void run() {
				fillInfo();
			}
		});
	}
	
	/**
	 * Fills the UI info for this user
	 * 
	 * @param user
	 */
	private void fillInfo() {
		// Load info from all the company (note that we can specify the
		// start/end date).
		fragmentLog.loadInfo(ZKDepartment.ROOT_DEPARTMENT_ID);
	}

	

}
