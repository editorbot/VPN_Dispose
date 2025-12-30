import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:vpn_serve/controller/native_ad_controller.dart';
import 'package:vpn_serve/helpers/my_dialogs.dart';

import 'config.dart';
class AdHelper {
 static Future<void> initAds() async {
    // TODO: Initialize Google Mobile Ads SDK
    await MobileAds.instance.initialize();
  }

 static InterstitialAd? _interstitialAd;
 static bool _interstitialAdLoaded = false;
 static NativeAd? _nativeAd;
 static bool _nativeAdLoaded = false;
 static void  precacheInterstitialAd() {
   log("Pre cache interstitial ad - id: ${Config.interstitialId}");
   if(Config.hideads) return;

   InterstitialAd.load(
     adUnitId: "ca-app-pub-3940256099942544/1033173712",
     request: AdRequest(),
     adLoadCallback: InterstitialAdLoadCallback(
       onAdLoaded: (ad) {
         //ad listener
         ad.fullScreenContentCallback = FullScreenContentCallback(
           onAdDismissedFullScreenContent: (ad) {
             _resetInterstitialAd();
             precacheInterstitialAd();
           },
         );
         _interstitialAd=ad;
         _interstitialAdLoaded=true;
       },
       onAdFailedToLoad: (err) {
        _resetInterstitialAd();
         log('Failed to load an interstitial ad: ${err.message}');

       },
     ),
   );
 }
 static void _resetInterstitialAd(){
   _interstitialAd?.dispose();
     _interstitialAd=null;
     _interstitialAdLoaded = false;
 }
// TODO: Implement _loadInterstitialAd()
  static void  showInterstitialAd({required VoidCallback onComplete}) {
    if(Config.hideads){
      onComplete();
      return;
    }
    if(_interstitialAdLoaded && _interstitialAd !=null){
      _interstitialAd?.show();
      onComplete();
      return;
    }
    MyDialogs.showProgress();
    InterstitialAd.load(
      adUnitId: "ca-app-pub-3940256099942544/1033173712",
      request: AdRequest(),
      adLoadCallback: InterstitialAdLoadCallback(
        onAdLoaded: (ad) {
          //ad listener
          ad.fullScreenContentCallback = FullScreenContentCallback(
            onAdDismissedFullScreenContent: (ad) {
              onComplete();
              _resetInterstitialAd();
              precacheInterstitialAd();
            },
          );
          Get.back();
          ad.show();
        },
        onAdFailedToLoad: (err) {
          Get.back();
          log('Failed to load an interstitial ad: ${err.message}');
          onComplete();
        },
      ),
    );
  }
  static void precacheNativeAd() {
    log("Pre cache native ad - id: ${Config.nativeId}");
    if(Config.hideads) return ;

    _nativeAd= NativeAd(
      // factoryId: "yourFactoryId",
        adUnitId: "/21775744923/example/native",
        listener: NativeAdListener(
          onAdLoaded: (ad) {
            log('$NativeAd loaded.');
            _nativeAdLoaded=true;

          },
          onAdFailedToLoad: (ad, error) {
            // Dispose the ad here to free resources.
            _resetNativeAd();
            debugPrint('$NativeAd failed to load: $error');

          },
        ),
        request: const AdManagerAdRequest(),
        // Styling
        nativeTemplateStyle: NativeTemplateStyle(
          // Required: Choose a template.
          templateType: TemplateType.small,

        ))
      ..load();
  }
  static void _resetNativeAd(){
   _nativeAd?.dispose();
    _nativeAd=null;
    _nativeAdLoaded = false;
  }
 /// Loads a native ad.
 /// ca-app-pub-3940256099942544/2247696110
 static NativeAd? loadNativeAd({required NativeAdController adController}) {
   if(Config.hideads) return null;
   if(_nativeAdLoaded && _nativeAd != null){
   adController.adLoaded.value=true;
     return _nativeAd;
   }
     return NativeAd(
       // factoryId: "yourFactoryId",
         adUnitId: "/21775744923/example/native",
         listener: NativeAdListener(
           onAdLoaded: (ad) {
             log('$NativeAd loaded.');
             adController.adLoaded.value=true;
             _resetNativeAd();
             precacheNativeAd();
           },
           onAdFailedToLoad: (ad, error) {
             // Dispose the ad here to free resources.
             _resetNativeAd();
             debugPrint('$NativeAd failed to load: $error');

           },
         ),
         request: const AdManagerAdRequest(),
         // Styling
         nativeTemplateStyle: NativeTemplateStyle(
           // Required: Choose a template.
           templateType: TemplateType.small,

         ))
       ..load();
   }


 static void  showRewardedAd({required VoidCallback onComplete}) {
   MyDialogs.showProgress();
   RewardedAd.load(
     adUnitId: "ca-app-pub-3940256099942544/1033173712",
     request: AdRequest(),
     rewardedAdLoadCallback: RewardedAdLoadCallback(
       onAdLoaded: (ad) {

         Get.back();
         ad.show(onUserEarnedReward:
             (AdWithoutView ad, RewardItem rewardItem) {
           onComplete();
         },);
       },
       onAdFailedToLoad: (err) {
         Get.back();
         log('Failed to load an rewarded ad: ${err.message}');
         
       },
     ),
   );
 }
}