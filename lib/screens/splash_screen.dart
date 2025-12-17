import 'package:flutter/material.dart';


import '../main.dart';
import 'home_screen.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    Future.delayed(Duration(milliseconds: 1500),(){
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_)=>HomeScreen()));
    });
  }
  @override
  Widget build(BuildContext context) {
    mq=MediaQuery.of(context).size;
    return Scaffold(body: Stack(children: [
      
Positioned(child:
Image.asset("assets/images/img.png",),

width: mq.width*.3,

  left: mq.width*.4,

  top: mq.height*.1,

),
      Positioned(
          bottom: mq.height*.15,
          width: mq.width,
          child: Text("data",textAlign: TextAlign.center,))
    ],),);
  }
}
