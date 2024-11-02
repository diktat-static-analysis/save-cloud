config.devServer = Object.assign(
    {},
    config.devServer || {},
    {
        port: 8081,
        setupMiddlewares: (middlewares, devServer) => {
            devServer.app.get("/sec/oauth-providers", (req, res) => { return res.send([]); });
            return middlewares;
        },
        proxy: [
            {
                context: ["/api/**"],
                target: 'http://localhost:5700',
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