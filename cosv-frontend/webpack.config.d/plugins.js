const BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;

//config.plugins.push(
//    new BundleAnalyzerPlugin()
//);

config.resolve.fallback = {
    "os": require.resolve("os-browserify/browser"),
    "path": require.resolve("path-browserify")
}