import 'dart:convert';
import 'dart:developer';

import 'package:csv/csv.dart';
import 'package:get/get.dart';
import 'package:http/http.dart';
import 'package:vpn_serve/helpers/my_dialogs.dart';
import 'package:vpn_serve/helpers/pref.dart';

import '../models/vpn.dart';

class APIs{
  static Future<List<Vpn>> getVPNservers() async{
    final List<Vpn> vpnList=[];
    try {
      final res=await get(Uri.parse('http://www.vpngate.net/api/iphone/'));
      final csvString=res.body.split("#")[1].replaceAll('*', '');
      // log(res.body);
      List<List<dynamic>> list=const CsvToListConverter().convert(csvString);

      final header=list[0];

      for(int i=1;i<list.length-1;++i){
        Map<String,dynamic> tempJson={};
        for(int j=0;j<header.length;++j){
          tempJson.addAll({header[j].toString():list[i][j]});
        }
        vpnList.add(Vpn.fromJson(tempJson));
      }
        log(vpnList.first.hostname);
    }  catch (e) {
      MyDialogs.error(msg: e.toString());
      // TODO
      log("GetVPNServersE:$e");
    }
    vpnList.shuffle();
    if(vpnList.isNotEmpty) Pref.vpnList=vpnList;

    return vpnList;
  }
  static Future<void> getIPDetails({required Rx<IPDetails>} ipData) async{
    final List<Vpn> vpnList=[];
    try {
      final res=await get(Uri.parse('http://www.vpngate.net/api/iphone/'));
     final data=jsonDecode(res.body);
      log(vpnList.first.hostname);
    }  catch (e) {
      // TODO
      log("GetVPNServersE:$e");
    }

  }
}