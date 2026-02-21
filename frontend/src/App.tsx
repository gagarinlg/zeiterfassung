import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import TimeTrackingPage from './pages/TimeTrackingPage'
import VacationPage from './pages/VacationPage'
import VacationApprovalPage from './pages/VacationApprovalPage'
import AdminPage from './pages/AdminPage'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
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
    </AuthProvider>
  )
}

export default App
