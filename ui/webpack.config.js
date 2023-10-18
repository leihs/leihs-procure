const HtmlWebpackPlugin = require('html-webpack-plugin')
const miniCssExtractPlugin = require('mini-css-extract-plugin')

const baseConfig = {
  output: {
    filename: '[name].js',
    libraryTarget: 'umd'
  },
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader'
        }
      },
      {
        test: /\.(scss)$/,
        use: [
          {
            loader: miniCssExtractPlugin.loader
          },
          {
            loader: 'css-loader'
          },
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: {
                plugins: () => [require('autoprefixer')]
              }
            }
          },
          {
            loader: 'resolve-url-loader' // support paths relative to source file in url()
          },
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true
            }
          }
        ]
      },
      {
        test: /\.(png|jp(e*)g|gif)$/,
        type: 'asset/resource'
      },
      {
        test: /\.svg$/,
        use: ['@svgr/webpack']
      }
    ]
  },
  plugins: [new miniCssExtractPlugin()],
  performance: {
    maxEntrypointSize: 1000000,
    maxAssetSize: 1000000
  }
}

module.exports = [
  // Theme + client side components bundle
  {
    ...baseConfig,
    entry: {
      'admin-ui': './src/admin-ui.js'
    },
    externals: {
      react: 'react',
      'react-dom': 'react-dom'
    }
  },
  // Build "test app"
  {
    ...baseConfig,
    entry: {
      'test-app': './src/test-app.js'
    },
    plugins: [
      ...baseConfig.plugins,
      new HtmlWebpackPlugin({
        template: './src/test-app.html',
        filename: './test-app.html'
      })
    ]
  }
]
