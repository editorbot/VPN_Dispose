import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get_rx/src/rx_types/rx_types.dart';
import 'package:get/get_state_manager/src/rx_flutter/rx_obx_widget.dart';
import 'package:vpn_serve/api/apis.dart';
import 'package:vpn_serve/models/ip_details.dart';
import 'package:vpn_serve/models/network_data.dart';
import 'package:vpn_serve/widgets/network_card.dart';

class NetworkTestScreen extends StatelessWidget {
  const NetworkTestScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final ipData =IpDetails.fromJson({}).obs;
    APIs.getIPDetails(ipData: ipData);
    return Scaffold(
      appBar: AppBar(title: Text("Network Test Screen"),),
      floatingActionButton: FloatingActionButton(
        onPressed: (){
        ipData.value=IpDetails.fromJson({});
        APIs.getIPDetails(ipData: ipData);
      },child: Icon(Icons.refresh),),

      body: Obx(()=> ListView(
        physics: BouncingScrollPhysics(),
        children: [
         NetworkCard(data:
         NetworkData(icon: Icon(CupertinoIcons.location_solid),
             title: "IP Address", subtitle: ipData.value.query),
         ),
          NetworkCard(data:
          NetworkData(icon: Icon(Icons.location_city),
              title: "Internet Provider", subtitle: ipData.value.isp),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.location),
              title: "Location", subtitle: ipData.value.country.isEmpty ? "Fetching...." : "${ipData.value.city},${ipData.value.regionName},${ipData.value.country}"),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.location_solid),
              title: "Pin-code", subtitle: ipData.value.zip),
          ),
          NetworkCard(data:
          NetworkData(icon: Icon(CupertinoIcons.time),
              title: "Timezone", subtitle: ipData.value.timezone),
          ),
        ],
      ),
    )
    );
  }
}
