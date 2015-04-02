/**
 * Copyright 2013-2014 Wolfgang Koller
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

/**
 * Cordova (Android) plugin for accessing the power-management functions of the device
 * @author Wolfgang Koller <viras@users.sourceforge.net>
 * modifiée par Carole BRUNO pour réveil avec téléphone vérouillé le 14/05/2014
 * modifiée par Jean Marc BRUNO pour mise à jour le 01/04/2015
 **/

 
package org.apache.cordova.powermanagement;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;


import android.app.KeyguardManager;		// mon import
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;

/**
 * Plugin class which does the actual handling
 */
public class PowerManagement extends CordovaPlugin {
	// As we only allow one wake-lock, we keep a reference to it here
	private PowerManager.WakeLock wakeLock = null;
	private PowerManager powerManager = null;
	// mes déclarations
	private KeyguardManager myKeyGuard  = null;
	private KeyguardManager.KeyguardLock myLock  = null;

	/**
	 * Fetch a reference to the power-service when the plugin is initialized
	 */
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		
		this.powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
		// mes déclarations
		this.myKeyGuard = (KeyguardManager)cordova.getActivity().getSystemService(Context.KEYGUARD_SERVICE);
		this.myLock = myKeyGuard.newKeyguardLock("tag");
	}
	
	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {

		PluginResult result = null;
		Log.d("PowerManagementPlugin", "Plugin execute called - " + this.toString() );
		Log.d("PowerManagementPlugin", "Action is " + action + (PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP) + "key");
		Log.d("PowerManagementPlugin", "Action is " + action + (PowerManager.FLAG_KEEP_SCREEN_ON | PowerManager.ACQUIRE_CAUSES_WAKEUP) + "key");
		
		try {
			if( action.equals("acquire") ) {
					if( args.length() > 0 && args.getBoolean(0) ) {
						Log.d("PowerManagementPlugin", "Only dim lock" );
						// ACQUIRE_CAUSES_WAKEUP pour réveiller le téléphone et enleve le verrouillage
						result = this.acquire( PowerManager.FLAG_KEEP_SCREEN_ON | PowerManager.ACQUIRE_CAUSES_WAKEUP  );
						this.myLock.disableKeyguard();

					}
					else {
						// ACQUIRE_CAUSES_WAKEUP pour réveiller le téléphone et enleve le verrouillage
						result = this.acquire( PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP );
						this.myLock.disableKeyguard();
					}
			}
			else if( action.equals("release") ) {
				result = this.release();
			}
		}
		catch( JSONException e ) {
			result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
		}
		
		callbackContext.sendPluginResult(result);
		return true;
	}
	
	/**
	 * Acquire a wake-lock
	 * @param p_flags Type of wake-lock to acquire
	 * @return PluginResult containing the status of the acquire process
	 */
	private PluginResult acquire( int p_flags ) {
		PluginResult result = null;
		
		if (this.wakeLock == null) {
			this.wakeLock = this.powerManager.newWakeLock(p_flags, "PowerManagementPlugin");
			try {
				this.wakeLock.acquire();
				result = new PluginResult(PluginResult.Status.OK);
			}
			catch( Exception e ) {
				this.wakeLock = null;
				result = new PluginResult(PluginResult.Status.ERROR,"Can't acquire wake-lock - check your permissions!");
			}
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION,"WakeLock already active - release first");
		}
		
		return result;
	}
	
	/**
	 * Release an active wake-lock
	 * @return PluginResult containing the status of the release process
	 */
	private PluginResult release() {
		PluginResult result = null;
		
		if( this.wakeLock != null ) {
			this.wakeLock.release();
			this.wakeLock = null;
			
			result = new PluginResult(PluginResult.Status.OK, "OK");
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "No WakeLock active - acquire first");
		}
		
		return result;
	}
	
	/**
	 * Make sure any wakelock is released if the app goes into pause
	 */
	@Override
	public void onPause(boolean multitasking) {
		if( this.wakeLock != null ) {
			this.wakeLock.release();
			// ma fonction
			this.myLock.reenableKeyguard();
		}

		super.onPause(multitasking);
	}
	
	/**
	 * Make sure any wakelock is acquired again once we resume
	 */
	@Override
	public void onResume(boolean multitasking) {
		if( this.wakeLock != null ) {
			this.wakeLock.acquire();
			// ma fonction
			this.myLock.disableKeyguard();
		}

		super.onResume(multitasking);
	}
}
