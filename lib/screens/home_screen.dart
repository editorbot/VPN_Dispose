import 'dart:convert';
import 'dart:developer';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';

import 'package:get/get.dart';
import '../controller/home_controller.dart';
import '../main.dart';
import '../models/vpn_config.dart';
import '../models/vpn_status.dart';
import '../services/vpn_engine.dart';
import '../widgets/count_down_timer.dart';
import '../widgets/home_card.dart';
import 'location_screen.dart';

class HomeScreen extends StatelessWidget {
   HomeScreen({super.key});

final _controller=Get.put(HomeController());

  // List<VpnConfig> _listVpn = [];
  @override
  Widget build(BuildContext context) {
    VpnEngine.vpnStageSnapshot().listen((event) {
      _controller.vpnState.value = event;
    });
    return Scaffold(
      backgroundColor: Colors.blue.shade50,
      appBar: AppBar(title: Text('OpenVPN Demo'),backgroundColor: Colors.blue,),
      bottomNavigationBar: SafeArea(child: _changeLocation()),
      body: SingleChildScrollView(
        scrollDirection: Axis.vertical,
        child: Column(
mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(height: mq.height*.02,width: double.maxFinite,),
             Obx(()=> _vpnButton()),

              // Center(
              //   child: TextButton(
              //     style: TextButton.styleFrom(
              //       shape: StadiumBorder(),
              //       backgroundColor: Theme.of(context).primaryColor,
              //     ),
              //     child: Text(
              //       _controller.vpnState.value == VpnEngine.vpnDisconnected
              //           ? 'Connect VPN'
              //           : _controller.vpnState.value.replaceAll("_", " ").toUpperCase(),
              //       style: TextStyle(color: Colors.white),
              //     ),
              //     onPressed: _connectClick,
              //   ),
              // ),
              // StreamBuilder<VpnStatus?>(
              //   initialData: VpnStatus(),
              //   stream: VpnEngine.vpnStatusSnapshot(),
              //   builder: (context, snapshot) => Text(
              //       "${snapshot.data?.byteIn ?? ""}, ${snapshot.data?.byteOut ?? ""}",
              //       textAlign: TextAlign.center),
              // ),
              //
              // //sample vpn list
              // Column(
              //     children: _listVpn
              //         .map(
              //           (e) => ListTile(
              //         title: Text(e.country),
              //         leading: SizedBox(
              //           height: 20,
              //           width: 20,
              //           child: Center(
              //               child: _selectedVpn == e
              //                   ? CircleAvatar(
              //                   backgroundColor: Colors.green)
              //                   : CircleAvatar(
              //                   backgroundColor: Colors.grey)),
              //         ),
              //         onTap: () {
              //           log("${e.country} is selected");
              //           setState(() => _selectedVpn = e);
              //         },
              //       ),
              //     )
              //         .toList())

              Obx(
                ()=> Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    Expanded(
                      child: HomeCard(title: _controller.vpn.value!.countryLong.isEmpty ? "Country"
                          :_controller.vpn.value!.countryLong,
                          subTitle: "FREE",
                          icon: CircleAvatar(
                            radius: 30,
                        child: _controller.vpn.value!.countryLong.isEmpty ?Icon(Icons.vpn_key,)
                            :null,
                backgroundImage: _controller.vpn.value!.countryLong.isEmpty?null
                :AssetImage("assets/flags/${_controller.vpn.value!.countryShort.toLowerCase()}.png")
                            ,)),
                    ),
                    Expanded(
                      child: HomeCard(title:_controller.vpn.value==null? "100ms":'${_controller.vpn.value!.ping} ms',
                          subTitle: "PING",
                          icon: CircleAvatar(
                            backgroundColor: Colors.orange,
                            radius: 30,
                            child: Icon(Icons.vpn_key,color: Colors.white,),)),
                    ),
                  ],
                ),
              ),
             SizedBox(height: mq.height*.1,),
        StreamBuilder<VpnStatus?>(
            initialData: VpnStatus(),
            stream: VpnEngine.vpnStatusSnapshot(),
            builder: (context, snapshot) => Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.max,
              children: [
                Expanded(
                  child: HomeCard(title: "${snapshot.data?.byteIn ?? 'O Kbps'}", subTitle: "Download",
                      icon: CircleAvatar(
                        radius: 30,
                        child: Icon(Icons.vpn_key,),)),
                ),
                Expanded(
                  child: HomeCard(title: "${snapshot.data?.byteOut ?? "0 Kbps"}", subTitle: "Upload",
                      icon: CircleAvatar(
                        backgroundColor: Colors.orange,
                        radius: 30,
                        child: Icon(Icons.vpn_key,color: Colors.white,),)),
                ),
              ],
            )


                // Text(
                // "${snapshot.data?.byteIn ?? ""}, ${snapshot.data?.byteOut ?? ""}",
                // textAlign: TextAlign.center),
          ),

            ]),
      ),
    );
  }



//vpn button
Widget _vpnButton()=>Column(
  children: [
    //button
    Semantics(
      button: true,
      child: InkWell(
        borderRadius: BorderRadius.circular(100),
        onTap: (){
          _controller.connectToVpn();
// _controller.startTimer.value=!_controller.startTimer.value;
        },
        child: Container(
          padding: EdgeInsets.all(16),
          decoration: BoxDecoration(shape: BoxShape.circle,color: _controller.getButtonColor.withValues( alpha: .1 )),
          child: Container(
            padding: EdgeInsets.all(16),
            decoration: BoxDecoration(shape: BoxShape.circle,color: _controller.getButtonColor.withValues( alpha: .3 )),
            child: Container(
              width: mq.height*.2,
              height: mq.height*.2,
              decoration: BoxDecoration(shape: BoxShape.circle,color: _controller.getButtonColor),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.power_settings_new_rounded,size: 28,color: Colors.white,),
              SizedBox(height: 4,),
              Text(_controller.getButtonText,style: TextStyle(fontSize: 12,color: Colors.white),)
            ],
            ),),
          ),
        ),
      ),
    ),

//connection status label
  Container(
    padding: EdgeInsets.symmetric(vertical: 6,horizontal: 16,),
    margin: EdgeInsets.only(top: mq.height*.015,bottom: mq.height*.05),
    //You cannot use color or anything outside boxdecoration and inside it at the same time
    decoration: BoxDecoration(borderRadius: BorderRadius.circular(15),color: Colors.blue),
    child: Text(_controller.vpnState.value==VpnEngine.vpnDisconnected?"disconnect":_controller.vpnState.replaceAll("_", " ").toUpperCase()),

  ),
    Obx(()=> CountDownTimer(startTimer: _controller.vpnState.value==VpnEngine.vpnConnected,)),
  ],
);

Widget _changeLocation()=>Semantics(
  button: true,
  child: InkWell(
    onTap: ()=>Get.to(()=>LocationScreen()),
    child: Container(
    color: Colors.blue,
      padding: EdgeInsets.symmetric(horizontal: 20),
      height: 60,
      child: Row(
        children: [
          Icon(CupertinoIcons.globe),
          SizedBox(width: 20,),
          Text("Change Location"),
          Spacer(),
          CircleAvatar(
              child: Icon(Icons.keyboard_arrow_right_rounded)

          )
        ],
      ),
    ),
  ),
);
}

