import 'dart:convert';
import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:vpn_serve/helpers/ad_helper.dart';

import '../helpers/pref.dart';
import '../models/vpn.dart';
import '../models/vpn_config.dart';
import '../services/vpn_engine.dart';


class HomeController extends GetxController{
  // final  RxBool startTimer=false.obs;
  final vpnState = VpnEngine.vpnDisconnected.obs;
  final Rx<Vpn?> vpn=Pref.vpn.obs;
  // final Rx<Vpn?> vpn=Vpn.fromJson({}).obs;

// Future<void> initializeData() async{}
  void connectToVpn() {
    ///Stop right here if user not select a vpn
    if (vpn.value!.openVPNConfigDataBase64.isEmpty) return;

    if (vpnState.value == VpnEngine.vpnDisconnected) {
// log("\nBefore: ${vpn.value!.openVPNConfigDataBase64}");
      final data =Base64Decoder().convert(vpn.value!.openVPNConfigDataBase64);
      final config=Utf8Decoder().convert(data);
      final vpnConfig=VpnConfig(country: vpn.value!.countryLong, username: "vpn", password: "vpn", config: config);
// log("\nAfter: ${config}");
      ///Start if stage is disconnected
      AdHelper.showInterstitialAd(onComplete: () async {
       await VpnEngine.startVpn(vpnConfig);
      });

    } else {
      ///Stop if stage is "not" disconnected

      VpnEngine.stopVpn();
    }
  }

  Color get getButtonColor{
  switch(vpnState.value){
    case VpnEngine.vpnDisconnected:return Colors.blue;
    case VpnEngine.vpnConnected:return Colors.green;


    default:return Colors.orangeAccent;
  }
  }

  String get getButtonText{
    switch(vpnState.value){
      case VpnEngine.vpnDisconnected:return 'Tap to Connect';
      case VpnEngine.vpnConnected:return 'Disconnect';


      default:return 'Connecting...';
    }
  }
}