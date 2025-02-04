import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_test/flutter_test.dart';

import '../constants.dart';

void onReceivedError() {
  final shouldSkip = kIsWeb
      ? true
      : ![
          TargetPlatform.android,
          TargetPlatform.iOS,
          TargetPlatform.macOS,
        ].contains(defaultTargetPlatform);

  group('onReceivedError', () {
    testWidgets('invalid url', (WidgetTester tester) async {
      final Completer<String> errorUrlCompleter = Completer<String>();
      final Completer<WebResourceErrorType> errorCodeCompleter =
          Completer<WebResourceErrorType>();

      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: InAppWebView(
            key: GlobalKey(),
            initialUrlRequest: URLRequest(url: TEST_NOT_A_WEBSITE_URL),
            onReceivedError: (controller, request, error) {
              errorUrlCompleter.complete(request.url.toString());
              errorCodeCompleter.complete(error.type);
            },
          ),
        ),
      );

      final String url = await errorUrlCompleter.future;
      final WebResourceErrorType errorType = await errorCodeCompleter.future;

      expect(errorType, WebResourceErrorType.HOST_LOOKUP);
      expect(url, TEST_NOT_A_WEBSITE_URL.toString());
    });

    testWidgets('event is not called with valid url',
        (WidgetTester tester) async {
      final Completer<void> onReceivedErrorCompleter = Completer<void>();

      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: InAppWebView(
            key: GlobalKey(),
            initialUrlRequest: URLRequest(
                url: Uri.parse(
                    'data:text/html;charset=utf-8;base64,PCFET0NUWVBFIGh0bWw+')),
            onReceivedError: (controller, request, error) {
              onReceivedErrorCompleter.complete();
            },
          ),
        ),
      );

      expect(onReceivedErrorCompleter.future, doesNotComplete);
    });
  }, skip: shouldSkip);
}
