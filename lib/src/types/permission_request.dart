import 'package:flutter_inappwebview_internal_annotations/flutter_inappwebview_internal_annotations.dart';

import '../in_app_webview/webview.dart';
import 'permission_resource_type.dart';
import 'permission_response.dart';
import 'frame_info.dart';

part 'permission_request.g.dart';

///Class that represents the response used by the [WebView.onPermissionRequest] event.
@ExchangeableObject()
class PermissionRequest_ {
  ///The origin of web content which attempt to access the restricted resources.
  Uri origin;

  ///List of resources the web content wants to access.
  ///
  ///**NOTE for iOS**: this list will have only 1 element and will be used by the [PermissionResponse.action]
  ///as the resource to consider when applying the corresponding action.
  List<PermissionResourceType_> resources;

  ///The frame that initiates the request in the web view.
  FrameInfo_? frame;

  PermissionRequest_(
      {required this.origin, this.resources = const [], this.frame});
}
