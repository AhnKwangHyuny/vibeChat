import React from 'react'
import ReactDOM from 'react-dom/client'
import './styles/index.css'
import { AppProviders } from './app/providers'
import { AppRouter } from './app/router'
import Layout from './components/layout/Layout'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProviders>
      <Layout>
        <AppRouter />
      </Layout>
    </AppProviders>
  </React.StrictMode>,
)
