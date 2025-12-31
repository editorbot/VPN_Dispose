import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:vpn_serve/models/network_data.dart';

class NetworkCard extends StatelessWidget {
  final NetworkData data;
  const NetworkCard({super.key, required this.data});

  @override
  Widget build(BuildContext context) {

    return Card(
elevation: 2,
      child: InkWell(
        onTap: (){

        },
        borderRadius: BorderRadius.circular(15),
        child: ListTile(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
          title: Icon(data.icon.icon,
          color: data.icon.color,
            size: data.icon.size,
          ),
          //flag
          leading:Text(data.title),
          subtitle: Text(data.subtitle),

        ),
      ),
    );
  }

}
