// Rewritten MainActivity.kt
// Original uploaded file: /mnt/data/1c1ab067-3051-4f15-a925-efdebc7fc596.png

package com.example.vpn_serve

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.VpnService
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDex
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.IOException
import java.io.StringReader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.flutter.plugins.googlemobileads.GoogleMobileAdsPlugin

/**
 * Clean, corrected MainActivity for single-engine Flutter apps.
 * Changes made:
 *  - `super.onCreate` is now called first in onCreate
 *  - Removed the fallback MethodChannel that conflicted with real handler
 *  - Keep a single MethodChannel registration (vpnControlMethod) after super.configureFlutterEngine
 *  - Added clear logs to trace lifecycle and incoming MethodChannel calls
 */
class MainActivity : FlutterActivity() {

    private lateinit var vpnControlMethod: MethodChannel
    private lateinit var vpnControlEvent: EventChannel
    private lateinit var vpnStatusEvent: EventChannel

    private var vpnStageSink: EventChannel.EventSink? = null
    private var vpnStatusSink: EventChannel.EventSink? = null

    private val EVENT_CHANNEL_VPN_STAGE = "vpnStage"
    private val EVENT_CHANNEL_VPN_STATUS = "vpnStatus"
    private val METHOD_CHANNEL_VPN_CONTROL = "vpnControl"
    private val VPN_REQUEST_ID = 1

    private var vpnProfile: VpnProfile? = null

    private var config: String = ""
    private var username: String = ""
    private var password: String = ""
    private var name: String = ""
    private var dns1: String = VpnProfile.DEFAULT_DNS1
    private var dns2: String = VpnProfile.DEFAULT_DNS2

    private var bypassPackages: ArrayList<String>? = null

    private var attached = true
    private var localJson: JSONObject? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        MultiDex.install(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Always call super first
        super.onCreate(savedInstanceState)
        Log.i("VPN_PLUGIN", "MainActivity onCreate called")

        // Register broadcast receiver after super to ensure system is initialized
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val stage = intent.getStringExtra("state")
                if (stage != null) setStage(stage)

                vpnStatusSink?.let { sink ->
                    try {
                        val duration = intent.getStringExtra("duration") ?: "00:00:00"
                        val lastPacketReceive = intent.getStringExtra("lastPacketReceive") ?: "0"
                        val byteIn = intent.getStringExtra("byteIn") ?: " "
                        val byteOut = intent.getStringExtra("byteOut") ?: " "

                        val json = JSONObject()
                        json.put("duration", duration)
                        json.put("last_packet_receive", lastPacketReceive)
                        json.put("byte_in", byteIn)
                        json.put("byte_out", byteOut)

                        localJson = json
                        if (attached) sink.success(json.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }, IntentFilter("connectionState"))

    }
    override fun provideFlutterEngine(context: Context): FlutterEngine? {
        return null
    }
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        // Ensure Flutter plugins and embedding are initialized first
        super.configureFlutterEngine(flutterEngine)
        Log.i("VPN_PLUGIN", "configureFlutterEngine called, registering channels")
//        GoogleMobileAdsPlugin.registerNativeAdFactory(flutterEngine, "yourFactoryId", YourNativeAdFactory(applicationContext))
        // Event channel for VPN stage
        vpnControlEvent = EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            EVENT_CHANNEL_VPN_STAGE
        )
        vpnControlEvent.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.i("VPN_PLUGIN", "vpnStage EventChannel onListen")
                vpnStageSink = events
            }

            override fun onCancel(arguments: Any?) {
                vpnStageSink?.endOfStream()
            }
        })

        // Event channel for VPN status
        vpnStatusEvent = EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            EVENT_CHANNEL_VPN_STATUS
        )
        vpnStatusEvent.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                Log.i("VPN_PLUGIN", "vpnStatus EventChannel onListen")
                vpnStatusSink = events
            }

            override fun onCancel(arguments: Any?) {}
        })

        // Method channel for VPN control — single, authoritative registration
        vpnControlMethod = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            METHOD_CHANNEL_VPN_CONTROL
        )

        vpnControlMethod.setMethodCallHandler { call, result ->
            Log.i("VPN_PLUGIN", "MethodCallHandler registered. Incoming method=" + call.method)

            when (call.method) {

                "stop" -> {
                    OpenVPNThread.stop()
                    setStage("disconnected")
                }

                "start" -> {
                    Log.i("VPN_PLUGIN", "Received start method from Dart")
                    config = call.argument("config") ?: ""
                    Log.i("VPN_PLUGIN", "Config length = " + (config.length))
                    name = call.argument("country") ?: ""
                    username = call.argument("username") ?: ""
                    password = call.argument("password") ?: ""
                    dns1 = call.argument("dns1") ?: VpnProfile.DEFAULT_DNS1
                    dns2 = call.argument("dns2") ?: VpnProfile.DEFAULT_DNS2
                    bypassPackages = call.argument("bypass_packages")

                    if (config.isEmpty() || name.isEmpty()) {
                        result.error("INVALID", "Config not valid", null)
                        return@setMethodCallHandler
                    }

                    prepareVPN()
                }

                "refresh" -> updateVPNStages()

                "refresh_status" -> updateVPNStatus()

                "stage" -> result.success(OpenVPNService.getStatus())

                "kill_switch" -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                        startActivity(intent)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }
    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        super.cleanUpFlutterEngine(flutterEngine)
        GoogleMobileAdsPlugin.unregisterNativeAdFactory(flutterEngine, "yourFactoryId")
    }
    private fun prepareVPN() {
        if (isConnected()) {
            setStage("prepare")

            try {
                val parser = ConfigParser()
                parser.parseConfig(StringReader(config))
                vpnProfile = parser.convertProfile()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ConfigParser.ConfigParseError) {
                e.printStackTrace()
            }

            val intent = VpnService.prepare(this)
            if (intent != null) startActivityForResult(intent, VPN_REQUEST_ID)
            else startVPN()

        } else {
            setStage("nonetwork")
        }
    }

    private fun startVPN() {
        try {
            setStage("connecting")

            val profile = vpnProfile ?: return

            if (profile.checkProfile(this) != de.blinkt.openvpn.R.string.no_error_found) {
                throw RemoteException(getString(profile.checkProfile(this)))
            }

            profile.mName = name
            profile.mProfileCreator = packageName
            profile.mUsername = username
            profile.mPassword = password
            profile.mDNS1 = dns1
            profile.mDNS2 = dns2

            if (dns1.isNotEmpty() && dns2.isNotEmpty())
                profile.mOverrideDNS = true

            bypassPackages?.let {
                profile.mAllowedAppsVpn.addAll(it)
                profile.mAllowAppVpnBypass = true
            }

            ProfileManager.setTemporaryProfile(this, profile)
            VPNLaunchHelper.startOpenVpn(profile, this)

        } catch (e: RemoteException) {
            setStage("disconnected")
            e.printStackTrace()
        }
    }

    private fun updateVPNStages() {
        setStage(OpenVPNService.getStatus())
    }

    private fun updateVPNStatus() {
        localJson?.let { json ->
            if (attached) vpnStatusSink?.success(json.toString())
        }
    }

    @SuppressLint("ServiceCast")
    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nInfo: NetworkInfo? = cm.activeNetworkInfo
        return nInfo != null && nInfo.isConnectedOrConnecting
    }

    private fun setStage(stage: String) {
        val sink = vpnStageSink ?: return
        if (!attached) return

        when (stage.uppercase()) {
            "CONNECTED" -> sink.success("connected")
            "DISCONNECTED" -> sink.success("disconnected")
            "WAIT" -> sink.success("wait_connection")
            "AUTH" -> sink.success("authenticating")
            "RECONNECTING" -> sink.success("reconnect")
            "NONETWORK" -> sink.success("no_connection")
            "CONNECTING" -> sink.success("connecting")
            "PREPARE" -> sink.success("prepare")
            "DENIED" -> sink.success("denied")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_ID) {
            if (resultCode == RESULT_OK) startVPN()
            else {
                setStage("denied")
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}
//class YourNativeAdFactory(val context: Context) : GoogleMobileAdsPlugin.NativeAdFactory {
//    override fun createNativeAd(nativeAd: NativeAd, options: Map<String, Any>?): NativeAdView {
//        val nativeAdView = LayoutInflater.from(context).inflate(R.layout.native_ad_layout, null) as NativeAdView
//
//        // Populate views
//        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
//        (nativeAdView.headlineView as TextView).text = nativeAd.headline
//
//        nativeAdView.mediaView = nativeAdView.findViewById(R.id.ad_media)
//        nativeAdView.mediaView?.mediaContent = nativeAd.mediaContent
//
//        // Handle optional fields with visibility checks
//        if (nativeAd.body == null) {
//            nativeAdView.bodyView?.visibility = View.INVISIBLE
//        } else {
//            nativeAdView.bodyView?.visibility = View.VISIBLE
//            (nativeAdView.bodyView as TextView).text = nativeAd.body
//        }
////        nativeAdView.setMediaContentAspectRatio(16.0f / 9.0f);  // Stabilize video aspect
//        // Set other views similarly...
//        nativeAdView.setNativeAd(nativeAd)
//        return nativeAdView
//    }
//
//}



