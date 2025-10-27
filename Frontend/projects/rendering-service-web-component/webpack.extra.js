module.exports = {
    optimization: {
        splitChunks: false,
        runtimeChunk: false,
    },
    output: {
        umdNamedDefine: true,
        library: 'esrendering',
        libraryTarget: 'amd',
        publicPath: '',
    },
};
