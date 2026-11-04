self.addEventListener('install', (event) => {
  event.waitUntil(self.skipWaiting());
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    (async () => {
      let url = new URL('/cliente', self.location.origin);
      try {
        const target = new URL(event.notification.data?.url, self.location.origin);
        if (target.origin === self.location.origin && target.pathname === '/cliente') url = target;
      } catch {
        // Uma notificação antiga pode não conter o endereço da senha.
      }
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      const existing = windows.find((client) => client.url === url.href);
      if (existing) return existing.focus();
      return self.clients.openWindow(url.href);
    })(),
  );
});
