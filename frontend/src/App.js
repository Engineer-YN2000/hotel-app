import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import {
  I18nProvider,
  ServerError,
  SessionExpiredError,
} from './components/common';
import {
  TopPage,
  ReservationInputPage,
  ReservationConfirmPage,
} from './components/pages';

function App() {
  return (
    <div className="App">
      <Suspense fallback={<div>Loading...</div>}>
        <I18nProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<TopPage />} />
              <Route
                path="/reservation/:reservationId"
                element={<ReservationInputPage />}
              />
              <Route
                path="/reservation/:reservationId/confirm"
                element={<ReservationConfirmPage />}
              />
              <Route path="/server-error" element={<ServerError />} />
              <Route
                path="/session-expired"
                element={<SessionExpiredError />}
              />
            </Routes>
          </BrowserRouter>
        </I18nProvider>
      </Suspense>
    </div>
  );
}

export default App;
