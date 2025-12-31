import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:vpn_serve/models/network_data.dart';
import 'package:vpn_serve/widgets/network_card.dart';

class NetworkTestScreen extends StatelessWidget {
  const NetworkTestScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Network Test Screen"),),
      floatingActionButton: FloatingActionButton(onPressed: (){ },child: Icon(Icons.refresh),),

      body: ListView(
        physics: BouncingScrollPhysics(),
        children: [
         NetworkCard(data:
         NetworkData(icon: Icon(CupertinoIcons.location_solid),
             title: "IP Address", subtitle: "subtitle"),
         ),
          NetworkCard(data:
          NetworkData(icon: Icon(Icons.location_city),
              title: "Internet Provider", subtitle: "subtitle"),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.location),
              title: "Location", subtitle: "subtitle"),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.location_solid),
              title: "Pin-code", subtitle: "subtitle"),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.time),
              title: "Timezone", subtitle: "subtitle"),
          ),
        ],
      ),
    );
  }
}
