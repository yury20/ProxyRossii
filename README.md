# ProxyRossii
Delivering packages. With delay.

The simple TCP proxy with delaying response is an excellent tool for load testing. You can emulate long response under high load.

Just write your custom server configuration in proxyR.properties file and pass it in the first argument (or compile the project if you prefer to use a config in jar). Each server's config starts an isolated thread and listens defined port.
