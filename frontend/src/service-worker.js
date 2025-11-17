/* eslint-env serviceworker */
import { clientsClaim } from 'workbox-core';
import { ExpirationPlugin } from 'workbox-expiration';
import { precacheAndRoute, createHandlerBoundToURL } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { StaleWhileRevalidate } from 'workbox-strategies';

clientsClaim();

// CRAが生成した静的アセットをプリキャッシュします
precacheAndRoute(self.__WB_MANIFEST);

// App Shell (index.html) の設定
const fileExtensionRegexp = new RegExp('/[^/?]+\\.[^/]+$');
registerRoute(
  ({ request, url }) => {
    if (request.mode !== 'navigate') {
      return false;
    }
    if (url.pathname.startsWith('/_')) {
      return false;
    }
    if (url.pathname.match(fileExtensionRegexp)) {
      return false;
    }
    return true;
  },
  createHandlerBoundToURL(process.env.PUBLIC_URL + '/index.html'),
);

// ------------------------------
// 「部屋タイプごとの定員並びに総室数」をキャッシュする
// ここでは、起動時に取得するデータを想定し '/api/initial-data' としています。
// StaleWhileRevalidate戦略:
// 1. キャッシュがあれば即座に返す (高速表示)
// 2. 同時にネットワークリクエストを実行
// 3. レスポンスがあればキャッシュを更新 (次回以降に備える)
// ------------------------------
registerRoute(
  ({ url }) => url.pathname.startsWith('/api/initial-data'),
  new StaleWhileRevalidate({
    cacheName: 'api-initial-data-cache',
    plugins: [
      new ExpirationPlugin({
        maxEntries: 1, // このAPIのキャッシュは1件のみ保持
        maxAgeSeconds: 60 * 60 * 24, // 1日間キャッシュ (0:00のバッチ更新 を考慮)
      }),
    ],
  }),
);
