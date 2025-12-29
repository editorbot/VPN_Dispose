import 'dart:developer';

import 'package:firebase_remote_config/firebase_remote_config.dart';
import 'package:flutter/material.dart';
class Config {
  static final _config = FirebaseRemoteConfig.instance;
  static const _defaultvalue={"interstitial_ad": "ca-app-pub-3940256099942544/1033173712",
    "native_ad": "/21775744923/example/native",
    "rewarded_ad": "ca-app-pub-3940256099942544/1033173712",
    "shows_ads": true};
  static Future<void> initConfig() async {

    await _config.setConfigSettings(RemoteConfigSettings(
      fetchTimeout: const Duration(minutes: 1),
      minimumFetchInterval: const Duration(minutes: 30),
    ));
    await _config.setDefaults(_defaultvalue);
    await _config.fetchAndActivate();
    log("Remote Config data: ${_config.getAll()}");

    _config.onConfigUpdated.listen((event) async {
      await _config.activate();
log("updated: ${_config.getAll()}");
      // Use the new config values here.
    });
  }
  static bool get _showAd=>_config.getBool("shows_ads");
  static String get nativeId=>_config.getString("native_ad");
  static String get rewardId=>_config.getString("rewarded_ad");
  static String get interstitialId=>_config.getString("interstitial_ad");

  static bool get hideads=>!_showAd;
}