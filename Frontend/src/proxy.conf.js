require('dotenv').config();

if (!process.env.BACKEND_URL) {
    throw new Error(
        'Missing environment variable `BACKEND_URL`.' +
            '\n\nTo get started, run' +
            '\n\n    cp .env.example .env' +
            '\n\nand edit `.env`.' +
            '\n',
    );
}

const PROXY_CONFIG = [
    {
        context: ['/edu-sharing/rest'],
        target: process.env.BACKEND_URL,
        secure: false
    },
];

module.exports = PROXY_CONFIG;