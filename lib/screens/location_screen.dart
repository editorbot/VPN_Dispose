import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:lottie/lottie.dart';
import 'package:vpn_serve/controller/native_ad_controller.dart';
import 'package:vpn_serve/helpers/ad_helper.dart';

import '../controller/location_controller.dart';
import '../main.dart';
import '../widgets/vpn_card.dart';


class LocationScreen extends StatelessWidget {
   LocationScreen({super.key});
   final _controller=LocationController();
   final _adController=NativeAdController();
  // final _controller=LocationController();
  @override
  Widget build(BuildContext context) {


   if(_controller.vpnList.isEmpty) _controller.getVpnData();
   
  _adController.ad= AdHelper.loadNativeAd(adController: _adController);
   
    return Scaffold(
      appBar: AppBar(title: Text('VPN Locations (${_controller.vpnList.length})'),backgroundColor: Colors.blue,),
bottomNavigationBar:
_adController.ad!=null && _adController.adLoaded.isTrue ?
// SizedBox(height: 200,child: AdWidget(ad: _adController.ad!),):null,
// Small template
      ConstrainedBox(
    constraints: const BoxConstraints(
    minWidth: 320, // minimum recommended width
      minHeight: 90, // minimum recommended height
      maxWidth: 400,
      maxHeight: 200,
    ),
    child: AdWidget(ad: _adController.ad!),
    ):null,
floatingActionButton: FloatingActionButton(onPressed: (){ _controller.getVpnData();},child: Icon(Icons.refresh),),
body: Obx(()=>_controller.isloading.value?_loadingWidget():_controller.vpnList.isEmpty ?_noVPNfound():vpnData())
    );
  }

  vpnData()=>ListView.builder(physics: BouncingScrollPhysics(),itemBuilder: (cxt,i)=> VpnCard(vpn: _controller.vpnList[i]) ,itemCount: _controller.vpnList.length,);

  _loadingWidget()=>SizedBox(
width: double.infinity,
    height: double.infinity,
    child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Expanded(child: LottieBuilder.asset("assets/lottie/loading.json",width: mq.height*.3,)),
        Padding(
          padding: const EdgeInsets.only(bottom: 200),
          child: Text("Loading VPN's............."),
        ),

      ],
    ),
  );

  _noVPNfound()=>Center(
    child: Text("No VPN Found!"),
  );
}
