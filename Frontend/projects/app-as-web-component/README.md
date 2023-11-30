# Edu-Sharing as Web Component

## Build

```sh
# Serve a self-updating development build.
npm start
# Serve a one-time compiled production build.
npm run preview
# Build for production.
npm run build
```

## Configuration

Set the backend URL for edu-sharing with a global config object on `window`:

```js
window.__env = {
    EDU_SHARING_API_URL: 'http://repository.127.0.0.1.nip.io:8100/edu-sharing/rest',
};
```

For testing, you can proxy requests to the URL given in the file `.env` by setting `EDU_SHARING_API_URL` to `/edu-sharing/rest`.

## Installation

-   Build the application using `npm run build`
-   Include the copiled files from `dist/app-as-web-component` in the folder `vendor/edu-sharing` in your web root
-   Include `runtime.js`, `polyfills.js`, `scripts.js`, `main.js`, and `styles.css` in your page as shown in `example/index.html`
-   Include the web component in your DOM:
    ```html
    <edu-sharing-app></edu-sharing-app>
    ```
