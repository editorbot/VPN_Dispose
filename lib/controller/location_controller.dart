import 'package:get/get.dart';

import '../api/apis.dart';
import '../helpers/pref.dart';
import '../models/vpn.dart';

class LocationController  extends GetxController{
  List<Vpn> vpnList=Pref.vpnList;

  final RxBool isloading=false.obs;


  Future<void> getVpnData()async{
    isloading.value=true;

    vpnList=await APIs.getVPNservers();
    isloading.value=false;
  }
}