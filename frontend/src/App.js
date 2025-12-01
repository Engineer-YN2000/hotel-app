import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { I18nProvider, ServerError } from './components/common';
import { TopPage, ReservationNext } from './components/pages';

function App() {
  return (
    <div className="App">
      <Suspense fallback={<div>Loading...</div>}>
        <I18nProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<TopPage />} />
              <Route path="/reservation-next" element={<ReservationNext />} />
              <Route path="/server-error" element={<ServerError />} />
            </Routes>
          </BrowserRouter>
        </I18nProvider>
      </Suspense>
    </div>
  );
}

export default App;
