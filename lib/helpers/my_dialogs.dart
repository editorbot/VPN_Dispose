import 'package:flutter/material.dart';
import 'package:get/get.dart';

class MyDialogs {

  static showProgress(){
    Get.dialog(Center(child: CircularProgressIndicator(),));
  }
}