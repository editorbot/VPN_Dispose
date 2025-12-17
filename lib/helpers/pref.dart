import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:hive/hive.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../models/vpn.dart';

class Pref{
static late Box _box;
  static Future<void> initializeHive() async {
    await Hive.initFlutter();
    _box=await Hive.openBox("data");
  }
static Vpn get vpn=>Vpn.fromJson(jsonDecode(_box.get("vpn")??"{}"));
  static set vpn(Vpn v)=>_box.put("vpn", jsonEncode(v));

//for storing vpn servers details-not needed for the vpn details when u restart
static List<Vpn> get vpnList{
  List<Vpn> temp=[];
  final data=jsonDecode(_box.get("vpnList")??"[]");

  for (var i in data) temp.add(Vpn.fromJson(i));

  return temp;
}
static set vpnList(List<Vpn> v)=>_box.put("vpnList", jsonEncode(v));
}