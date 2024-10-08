package com.ftrono.DJames.application

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.ftrono.DJames.R
import com.ftrono.DJames.services.FloatingViewService
import kotlin.math.roundToInt


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val TAG: String = HomeFragment::class.java.getSimpleName()

    //Views:
    private var baloon: View? = null
    private var baloon_arrow: View? = null
    private var mega_face: ImageView? = null
    private var spotifyLogo: ImageView? = null

    //View resources:
    private var descr_login_status: TextView? = null    //eventReceiver (login)
    private var descr_main: TextView? = null    //eventReceiver (login), setOverlayActive(utilities)
    private var descr_use: TextView? = null    //eventReceiver (login, volumeSettings, utilities)
    private var startButton: Button? = null    //eventReceiver (login), setOverlayActive(utilities)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Load views:
        descr_login_status = requireActivity().findViewById<TextView>(R.id.descr_login_status)
        baloon = requireActivity().findViewById<View>(R.id.baloon)
        baloon_arrow = requireActivity().findViewById<View>(R.id.baloon_arrow)
        descr_main = requireActivity().findViewById<TextView>(R.id.descr_main)
        descr_use = requireActivity().findViewById<TextView>(R.id.descr_use)
        mega_face = requireActivity().findViewById<ImageView>(R.id.DJames_face)
        startButton = requireActivity().findViewById<Button>(R.id.start_button)
        spotifyLogo = requireActivity().findViewById<ImageView>(R.id.spotify_logo)

        //Check initial orientation:
        var config = getResources().getConfiguration()
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.DJames_face
                bottomToTop = R.id.start_button
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.descr_login_status
                rightToLeft = R.id.baloon
                setMargins(0, 0, (50*density).roundToInt(),0)   //marginRight
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = R.id.baloon
                bottomToBottom = R.id.baloon
                rightToLeft = R.id.baloon
                setMargins(0, 0,(-25*density).roundToInt(),0)   //marginRight
            }
        }

        //Check Login status:
        if (prefs.spotifyToken == "") {
            setViewLoggedOut()
        } else {
            descr_login_status!!.text = getString(R.string.str_status_logged)
            if (overlay_active) {
                setOverlayActive()
            } else {
                setOverlayInactive()
            }
        }

        //Start personal Receiver:
        val actFilter = IntentFilter()
        actFilter.addAction(ACTION_HOME_LOGGED_IN)
        actFilter.addAction(ACTION_HOME_LOGGED_OUT)
        actFilter.addAction(ACTION_SETTINGS_VOL_UP)
        actFilter.addAction(ACTION_OVERLAY_ACTIVATED)
        actFilter.addAction(ACTION_OVERLAY_DEACTIVATED)

        //register all the broadcast dynamically in onCreate() so they get activated when app is open and remain in background:
        requireActivity().registerReceiver(homeReceiver, actFilter, AppCompatActivity.RECEIVER_EXPORTED)
        Log.d(TAG, "HomeReceiver started.")

        //Spotify:
        spotifyLogo!!.setOnClickListener(View.OnClickListener {
            spotifyTap(requireActivity())
        })
        descr_login_status!!.setOnClickListener(View.OnClickListener {
            spotifyTap(requireActivity())
        })

        //Start:
        startButton!!.setOnClickListener(View.OnClickListener {
            if (!overlay_active) {
                //START:
                if (!isMyServiceRunning(FloatingViewService::class.java)) {
                    var intentOS = Intent(requireActivity(), FloatingViewService::class.java)
                    if (prefs.autoClock) {
                        intentOS.putExtra("faded", true)
                    }
                    requireActivity().startService(intentOS)
                    if (prefs.volumeUpEnabled) {
                        Toast.makeText(requireActivity(), "Use the OVERLAY or VOLUME UP / SHUTTER button to speak!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireActivity(), "Use the OVERLAY button to speak!", Toast.LENGTH_LONG).show()
                    }

                }
                //Start fake lock screen:
                if (prefs.autoClock) {
                    val intent1 = Intent(requireActivity(), FakeLockScreen::class.java)
                    startActivity(intent1)
                    requireActivity().finish()
                }
//                Snackbar.make(findViewById(R.id.content_main), getString(R.string.str_use_active), Snackbar.LENGTH_LONG)
//                    .setAction("CLOSE") { }
//                    .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
//                    .show()
            } else {
                //STOP:
                if (isMyServiceRunning(FloatingViewService::class.java)) {
                    requireActivity().stopService(Intent(requireActivity(), FloatingViewService::class.java))
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()
        //ON RESUME() ONLY:
        //Check Login status:
        if (!spotifyLoggedIn) {
            setViewLoggedOut()
            if (overlay_active) {
                setOverlayActive()
            } else {
                setOverlayInactive()
            }
        } else if (!Settings.canDrawOverlays(requireActivity())) {
            overlay_active = setOverlayInactive()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //unregister receivers:
        requireActivity().unregisterReceiver(homeReceiver)
        //empty views:
        descr_login_status = null
        descr_main = null
        descr_use = null
        startButton = null
        spotifyLogo = null
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //VERTICAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = R.id.DJames_face
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.baloon
                rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
                setMargins(0, (20*density).roundToInt(),0,0)   //marginTop
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = ConstraintLayout.LayoutParams.UNSET   //clear
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToLeft = R.id.baloon
                rightToRight = R.id.baloon
                topToBottom = R.id.baloon
                setMargins(0, (-40*density).roundToInt(),0,0)   //marginTop
            }
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //HORIZONTAL:
            baloon!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                leftToRight = R.id.DJames_face
                bottomToTop = R.id.start_button
            }
            mega_face!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = R.id.descr_login_status
                rightToLeft = R.id.baloon
                setMargins(0,0, (50*density).roundToInt(),0)   //marginRight
            }
            baloon_arrow!!.updateLayoutParams<ConstraintLayout.LayoutParams> {
                leftToLeft = ConstraintLayout.LayoutParams.UNSET   //clear
                rightToRight = ConstraintLayout.LayoutParams.UNSET   //clear
                topToBottom = ConstraintLayout.LayoutParams.UNSET   //clear
                topToTop = R.id.baloon
                bottomToBottom = R.id.baloon
                rightToLeft = R.id.baloon
                setMargins(0, 0,(-25*density).roundToInt(),0)   //marginRight
            }
        }
    }


    fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = requireActivity().getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    fun spotifyTap(context: Context) {
        if (!spotifyLoggedIn) {
            Toast.makeText(context, "Log in from Settings to unlock music functions!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Logged in to Spotify as: ${prefs.spotUserName}!", Toast.LENGTH_LONG).show()
        }
    }


    fun setViewLoggedOut(): Boolean {
        //Set NOT Logged-In UI:
        descr_login_status!!.text = getString(R.string.str_status_not_logged)
        //Set logo to B&W:
        val matrix = ColorMatrix()
        matrix.setSaturation(0F)
        val filter = ColorMatrixColorFilter(matrix)
        spotifyLogo!!.colorFilter = filter
        return false
    }


    //Set Overlay Active view in Main:
    fun setOverlayActive(): Boolean {
        try {
            if (Settings.canDrawOverlays(requireActivity())) {
                startButton!!.text = "S T O P"
                startButton!!.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(), R.color.colorStop)
                descr_main!!.setTextColor(AppCompatResources.getColorStateList(requireActivity(), R.color.colorHeader))
                descr_main!!.setTypeface(null, Typeface.BOLD_ITALIC)
                descr_main!!.text = getString(R.string.str_main_stop)
                if (prefs.volumeUpEnabled) {
                    descr_use!!.text = getString(R.string.str_use_active)
                } else {
                    descr_use!!.text = getString(R.string.str_use_active_no_vol)
                }
                descr_use!!.setTextColor(AppCompatResources.getColorStateList(requireActivity(), R.color.light_grey))
            }
            Log.d(TAG, "SetOverlayActive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayActive(): resources not available.")
        }
        return true
    }


    //Set Overlay Inactive view in Main:
    fun setOverlayInactive(): Boolean {
        try {
            startButton!!.text = "S T A R T"
            startButton!!.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(), R.color.colorAccent)
            descr_main!!.setTextColor(AppCompatResources.getColorStateList(requireActivity(), R.color.light_grey))
            descr_main!!.setTypeface(null, Typeface.ITALIC)
            descr_main!!.text = getString(R.string.str_main_start)
            descr_use!!.text = getString(R.string.str_use_main)
            descr_use!!.setTextColor(AppCompatResources.getColorStateList(requireActivity(), R.color.mid_grey))
            Log.d(TAG, "SetOverlayInactive()")
        } catch (e: Exception) {
            Log.d(TAG, "SetOverlayInactive(): resources not available.")
        }
        return false
    }


    //PERSONAL RECEIVER:
    private var homeReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            //When logged in:
            if (intent!!.action == ACTION_HOME_LOGGED_IN) {
                Log.d(TAG, "HOME: ACTION_HOME_LOGGED_IN.")
                try {
                    //Set Logged-In UI:
                    descr_login_status!!.text = context!!.getString(R.string.str_status_logged)
                    //Set logo to B&W:
                    val matrix = ColorMatrix()
                    matrix.setSaturation(1F)
                    val filter = ColorMatrixColorFilter(matrix)
                    spotifyLogo!!.colorFilter = filter
                } catch (e: Exception) {
                    Log.d(TAG, "HOME: ACTION_MAIN_LOGGED_IN: resources not available.")
                }
            }

            //When Settings VOLUME-UP is changed:
            if (intent.action == ACTION_SETTINGS_VOL_UP) {
                Log.d(TAG, "MAIN: ACTION_SETTINGS_VOL_UP.")
                try {
                    if (prefs.volumeUpEnabled) {
                        descr_use!!.text = context!!.resources.getString(R.string.str_use_active)
                    } else {
                        descr_use!!.text =
                            context!!.resources.getString(R.string.str_use_active_no_vol)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "HOME: ACTION_SETTINGS_VOL_UP: resources not available.")
                }
            }

            //When logged in:
            if (intent.action == ACTION_HOME_LOGGED_OUT) {
                Log.d(TAG, "HOME: ACTION_HOME_LOGGED_OUT.")
                setViewLoggedOut()
            }


            //When Overlay is activated:
            if (intent.action == ACTION_OVERLAY_ACTIVATED) {
                Log.d(TAG, "HOME: ACTION_OVERLAY_ACTIVATED.")
                setOverlayActive()
            }


            //When Overlay is deactivated:
            if (intent.action == ACTION_OVERLAY_DEACTIVATED) {
                Log.d(TAG, "HOME: ACTION_OVERLAY_DEACTIVATED.")
                setOverlayInactive()
            }

        }

    }
}