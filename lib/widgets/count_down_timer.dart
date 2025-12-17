import 'dart:async';

import 'package:flutter/material.dart';


class CountDownTimer extends StatefulWidget {
  const CountDownTimer({super.key, required this.startTimer});
  final bool startTimer;
  @override
  State<CountDownTimer> createState() => _CountDownTimerState();
}

class _CountDownTimerState extends State<CountDownTimer> {
  Timer? _timer;
  Duration _duration= Duration();
  _startTimer(){
    _timer=Timer.periodic(Duration(seconds: 1), (timer){
      setState(() {
        _duration=Duration(seconds: _duration.inSeconds+1);
      });
    });
  }
  _stopTimer(){
    setState(() {
      _timer?.cancel();
      _timer=null;
      _duration=Duration();
    });
  }

  @override
  Widget build(BuildContext context) {
    if(_timer==null || !widget.startTimer) {
      widget.startTimer ? _startTimer() : _stopTimer();
    }
String twoDigit(int n)=>n.toString().padLeft(2,'0');
final minutes=twoDigit(_duration.inMinutes.remainder(60));
final seconds=twoDigit(_duration.inSeconds.remainder(60));
final hours=twoDigit(_duration.inHours);
    return Text("$hours:$minutes:$seconds",style: TextStyle(fontSize: 17),);
  }
}
