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
 // TODO: Implement _loadInterstitialAd()
static void  showInterstitialAd({required VoidCallback onComplete}) {
   if(Config.hideads){
     onComplete;
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
 /// Loads a native ad.
 /// ca-app-pub-3940256099942544/2247696110
 static NativeAd? loadNativeAd({required NativeAdController adController}) {
   if(Config.hideads){

     return null;
   }
   return NativeAd(
     // factoryId: "yourFactoryId",
       adUnitId: "/21775744923/example/native",
       listener: NativeAdListener(
         onAdLoaded: (ad) {
           log('$NativeAd loaded.');
          adController.adLoaded.value=true;
         },
         onAdFailedToLoad: (ad, error) {
           // Dispose the ad here to free resources.
           debugPrint('$NativeAd failed to load: $error');
           ad.dispose();
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