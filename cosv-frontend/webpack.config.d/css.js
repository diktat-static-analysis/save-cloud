const MiniCssExtractPlugin = require("mini-css-extract-plugin");

config.module.rules.push(
    {
        test: /\.scss$/,
        use: [
            MiniCssExtractPlugin.loader,  // creates CSS files from css-loader's output
            'css-loader', // translates CSS into CommonJS
            {
                loader: 'postcss-loader', // Run postcss actions
                options: {
                    postcssOptions: {
                        plugins: [
                            "autoprefixer",
                        ],
                    },
                },
            },
            'sass-loader', // compiles Sass to CSS, using Node Sass by default
        ]
    },
    {
        // loader for fonts
        test: /\.(eot|ttf|woff|woff2)$/,
        use: {
            loader: 'file-loader',
        }
    },
);



config.plugins.push(
    new MiniCssExtractPlugin()
)