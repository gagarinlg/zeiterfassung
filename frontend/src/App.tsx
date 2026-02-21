import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import TimeTrackingPage from './pages/TimeTrackingPage'
import VacationPage from './pages/VacationPage'
import AdminPage from './pages/AdminPage'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/time-tracking"
          element={
            <ProtectedRoute>
              <TimeTrackingPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/vacation"
          element={
            <ProtectedRoute>
              <VacationPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute requiredPermission="admin.users.manage">
              <AdminPage />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
