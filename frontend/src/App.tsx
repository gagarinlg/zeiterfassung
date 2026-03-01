import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { DateFormatProvider } from './context/DateFormatContext'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import TimeTrackingPage from './pages/TimeTrackingPage'
import VacationPage from './pages/VacationPage'
import VacationApprovalPage from './pages/VacationApprovalPage'
import AdminPage from './pages/AdminPage'
import UserSettingsPage from './pages/UserSettingsPage'
import PasswordResetRequestPage from './pages/PasswordResetRequestPage'
import PasswordResetConfirmPage from './pages/PasswordResetConfirmPage'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <DateFormatProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<PasswordResetRequestPage />} />
        <Route path="/reset-password" element={<PasswordResetConfirmPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/settings" element={<UserSettingsPage />} />
          <Route path="/time-tracking" element={<TimeTrackingPage />} />
          <Route path="/vacation" element={<VacationPage />} />
          <Route
            path="/vacation/approvals"
            element={
              <ProtectedRoute requiredPermission="vacation.approve">
                <VacationApprovalPage />
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
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
      </DateFormatProvider>
    </AuthProvider>
  )
}

export default App
