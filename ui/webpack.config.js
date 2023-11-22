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
        exclude: /\.module\.scss$/, // Exclude SCSS modules
        use: [
          miniCssExtractPlugin.loader,
          'css-loader',
          'postcss-loader',
          'resolve-url-loader',
          {
            loader: 'sass-loader',
            options: {
              sourceMap: true
            }
          }
        ]
      },
      {
        test: /\.module\.scss$/, // Handle SCSS modules separately
        use: [
          miniCssExtractPlugin.loader,
          {
            loader: 'css-loader',
            options: {
              modules: {
                localIdentName: '[local]--[hash:base64:5]'
              }
            }
          },
          'postcss-loader',
          'resolve-url-loader',
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
  plugins: [
    new miniCssExtractPlugin({
      filename: '[name].css',
      chunkFilename: '[id].css'
    })
  ],
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
