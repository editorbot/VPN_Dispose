import 'dart:math';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:vpn_serve/controller/home_controller.dart';
import 'package:vpn_serve/helpers/pref.dart';
import 'package:vpn_serve/services/vpn_engine.dart';

import '../models/vpn.dart';

class VpnCard extends StatelessWidget {
  final Vpn vpn;
  const VpnCard({super.key, required this.vpn});

  @override
  Widget build(BuildContext context) {
    final controller=Get.find<HomeController>();
    return Card(
elevation: 2,
      child: InkWell(
        onTap: (){
          controller.vpn.value=vpn;
          Pref.vpn=vpn;
          Get.back();

          if(controller.vpnState.value==VpnEngine.vpnConnected){
            VpnEngine.stopVpn();
            Future.delayed(Duration(seconds: 2),()=>controller.connectToVpn());
          }else{
            controller.connectToVpn();
          }
        },
        borderRadius: BorderRadius.circular(15),
        child: ListTile(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
          title: Text(vpn.countryLong),
          //flag
          leading: Image.asset("assets/flags/${vpn.countryShort.toLowerCase()}.png"),
          subtitle: Row(
            children: [
              Icon(Icons.speed_rounded),
              SizedBox(width: 5,),
              Text(_formatbytes(vpn.speed, 2)),
            ],
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(CupertinoIcons.person,size: 15,),
              SizedBox(width: 5,),
              Text(vpn.numVpnSessions.toString()),
            ],
          ),
        ),
      ),
    );
  }
  String _formatbytes(int bytes,int decimals){
    if(bytes<=0)return "0 B";
    const suffixes=["Bps","Kbps","Mbps","Tbps"];
    var i=(log(bytes)/log(1024)).floor();
    return '${(bytes/pow(1024, i)).toStringAsFixed(decimals)} ${suffixes[i]}';
  }
}
