import 'package:flutter/material.dart';
import 'package:get/get.dart';

class MyDialogs {

  static info({required String msg}){
    Get.snackbar("Info", msg);
  }

  static error({required String msg}){
    Get.snackbar("Error", msg);
  }

  static success(){}

  static showProgress(){
    Get.dialog(Center(child: CircularProgressIndicator(),));
  }
}