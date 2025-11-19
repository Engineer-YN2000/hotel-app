import React, { Suspense } from 'react';
import I18nProvider from './components/common/I18nProvider';
import TopPage from './components/pages/TopPage';

function App() {
  return (
    <div className="App">
      <Suspense fallback={<div>Loading...</div>}>
        <I18nProvider>
          {/* TODO: 将来的に React Router を導入 */}
          <TopPage />
        </I18nProvider>
      </Suspense>
    </div>
  );
}

export default App;
