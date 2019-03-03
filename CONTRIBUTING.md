# Contributing

## Development server
To test against a local server in development, use
```
webView.setWebViewClient(new WebViewClient() {
  @Override
  public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
    handler.proceed();
  }
}
```

