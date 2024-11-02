# Frontend

### `nginx` configuration
The most interesting part of `nginx.conf` is here:
```nginx configuration
location / {
  index index.html;
  try_files $uri $uri/ /index.html;
  add_header Cache-Control "private; no-store";
  etag off;
  add_header Last-Modified "";
  if_modified_since off;
}
```

Here we define a configuration for the `location /` block, which is the default location block in `nginx` and matches any
request that doesn't match other specific location blocks.

Let's break down each directive and explain what happens:

1. `index index.html`;
   This directive sets the default file to serve when a **directory** is requested.
   In this case, if a request is made to a directory (e.g., http://example.com/), Nginx will try to serve the `index.html`
   file from that directory. If the file doesn't exist, Nginx will move to the next directive.
2. `try_files $uri $uri/ /index.html;`
   This directive specifies a series of files and locations that Nginx should try to serve.
   It acts as a fallback mechanism when the requested file is not found in the specified locations.
- `$uri` represents the requested URI.
- `$uri/` represents the URI followed by a trailing slash, typically used for directories.
- `/index.html` is the last fallback option.
3. `add_header Cache-Control "private; no-store";`
   This directive adds an HTTP response header named `Cache-Control` to the server's responses.
   `"private; no-store"` sets the `Cache-Control` header value, which tells the client and intermediate caching servers not to store any cached copies of the response.
   It ensures that the content is not stored in any cache, making every request hit the server directly.
4. `etag off;`
   `ETag` is an identifier that helps with caching, but disabling it ensures that the server doesn't use `ETag` for caching validation.
   This can be useful for certain scenarios where `ETags` are not necessary or could cause caching issues.
5. `add_header Last-Modified "";`
   The `Last-Modified` header is used for caching validation, but setting it to an empty value indicates that the server does not want to participate in any caching validation based on the last modification date of the resource.
6. `if_modified_since off;`
   The `if_modified_since` directive controls the behavior of the `If-Modified-Since` request header.
   Disabling it ensures that clients won't use conditional requests, which can be helpful in certain caching scenarios or when caching behavior needs to be controlled explicitly.

### `webpack-dev-server` configuration for no `api-gateway` run
Here is a `webpack-dev-server` configuration for running without `api-gateway` on:
```javascript
config.devServer = Object.assign(
  {},
  config.devServer || {},
  {
    setupMiddlewares: (middlewares, devServer) => {
      devServer.app.get("/sec/oauth-providers", (req, res) => { return res.send([]); });
      return middlewares;
    },
    proxy: [
      {
        context: ["/api/demo/**"],
        target: 'http://localhost:5421',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("X-Authorization-Id", "1");
            proxyReq.setHeader("X-Authorization-Name", "admin");
            proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
            proxyReq.setHeader("X-Authorization-Status", "ACTIVE");
        }
      },
      {
        context: ["/api/cpg/**"],
        target: 'http://localhost:5500',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("X-Authorization-Id", "1");
            proxyReq.setHeader("X-Authorization-Name", "admin");
            proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
            proxyReq.setHeader("X-Authorization-Status", "ACTIVE");
        }
      },
      {
        context: ["/api/**"],
        target: 'http://localhost:5800',
        logLevel: 'debug',
        onProxyReq: function (proxyReq, req, res) {
            proxyReq.setHeader("X-Authorization-Id", "1");
            proxyReq.setHeader("X-Authorization-Name", "admin");
            proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
            proxyReq.setHeader("X-Authorization-Status", "ACTIVE");
        }
      }
    ],
    historyApiFallback: true
  }
);
```

`setupMiddlewares` sets a stub for `/sec/oauth-providers` endpoint.

`historyApiFallback` makes `webpack-dev-server` return `index.html` in case of `404 Not Found`.

Sometimes it is useful to add authorization headers when proxying to some Spring services e.g. `save-backend`.
It can be done with setting `onPorxyReq`:
```javascript
onProxyReq: (proxyReq, req, res) => {
    proxyReq.setHeader("X-Authorization-Id", "1");
    proxyReq.setHeader("X-Authorization-Name", "admin");
    proxyReq.setHeader("X-Authorization-Roles", "ROLE_SUPER_ADMIN");
    proxyReq.setHeader("X-Authorization-Status", "ACTIVE");
}
```
Thus, we add `Authorization` and `X-Authorization-Source` headers that correspond with `admin` user headers.

### Using OAuth with a local deployment (`api-gateway` on)

* When the default [`dev-server.js`](../save-frontend/webpack.config.d/dev-server.js)
  is used, the front-end is expected to communicate directly with the back-end,
  omitting any gateway. When enabling OAuth, make sure the gateway is contacted
  instead:

    * `context`: add `/sec/**, /oauth2/**, /login/oauth2/**` to the list;
    * `target`: change to [`http://localhost:5300`](http://localhost:5300) (the
      default gateway URL);
    * `onProxyReq`: drop the entire callback, since all auth headers (`X-Authorization-Id`,
      `X-Authorization-Name`, `X_Authorization-Status` and `X-Authorization-Roles`) will be set by the gateway now
      (the gateway acts as a reverse proxy);
    * `bypass`: drop the entire callback.

  The resulting `dev-server.js` should look like this:
  ```javascript
  config.devServer = Object.assign(
      {},
      config.devServer || {},
      {
        proxy: [
          {
            context: ["/api/**", "/sec/**", "/oauth2/**", "/logout/**", "/login/oauth2/**"],
            target: 'http://localhost:5300',
            logLevel: 'debug',
          }
        ],
        historyApiFallback: true
      }
  )
  ```
  Notice that `historyApiFallback` is required for `BrowserRouter` work fine.

* Avoid potential name conflicts between local users (those authenticated using
  _HTTP Basic Auth_) and users created via an external _OAuth_ provider. For
  example, if you have a local user named `torvalds`, don't try to authenticate
  as a [_GitHub_ user with the same name](https://github.com/torvalds).