const HtmlWebpackPlugin = require('html-webpack-plugin');

config.plugins.push(
    new HtmlWebpackPlugin({
        template: 'index.html',
        publicPath: '/',
    })
);

if (config.mode === "production") {
    config.optimization = {
        // todo: use https://webpack.js.org/guides/output-management/ instead of manually adding js files into html
        splitChunks: {
            cacheGroups: {
                commons: {
                    test: /[\\/]node_modules[\\/]/,
                    name: 'vendors',
                    chunks: 'all'
                }
            }
        }
    };
}
