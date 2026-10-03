import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './components/common/Toast';
import { KongProvider } from './context/KongContext';
import Layout from './components/Layout/Layout';
import LoadingSpinner from './components/common/LoadingSpinner';
import Dashboard from './pages/Dashboard';
import UserList from './pages/users/UserList';
import UserCreate from './pages/users/UserCreate';
import UserDetail from './pages/users/UserDetail';
import ProjectList from './pages/projects/ProjectList';
import ProjectCreate from './pages/projects/ProjectCreate';
import ProjectDetail from './pages/projects/ProjectDetail';
import ProjectMembers from './pages/projects/ProjectMembers';
import ProjectStoragePage from './pages/projects/ProjectStoragePage';
import KongInstanceList from './pages/system/KongInstanceList';
import KongInstanceCreate from './pages/system/KongInstanceCreate';
import KongInstanceEdit from './pages/system/KongInstanceEdit';
import AuditLogPage from './pages/observability/AuditLogPage';
import GatewayLogPage from './pages/observability/GatewayLogPage';
import LiveLogs from './pages/observability/LiveLogs';
import MetricsDashboard from './pages/observability/MetricsDashboard';

import KongOverview from './pages/kong/KongOverview';
import KongServiceList from './pages/kong/KongServiceList';
import KongServiceCreate from './pages/kong/KongServiceCreate';
import KongServiceEdit from './pages/kong/KongServiceEdit';
import KongServiceDetail from './pages/kong/KongServiceDetail';
import KongPluginList from './pages/kong/KongPluginList';
import KongUpstreamList from './pages/kong/KongUpstreamList';
import KongUpstreamCreate from './pages/kong/KongUpstreamCreate';
import KongUpstreamEdit from './pages/kong/KongUpstreamEdit';
import KongUpstreamDetail from './pages/kong/KongUpstreamDetail';

import { Navigate } from 'react-router-dom';

function RequireSystemAdmin({ children }) {
    const { user } = useAuth();
    if (!user?.roles?.includes('SYSTEM_ADMIN')) {
        return <Navigate to="/" replace />;
    }
    return children;
}

function RequireTenantAdmin({ children }) {
    const { user } = useAuth();
    if (user?.roles?.includes('SYSTEM_ADMIN')) {
        return <Navigate to="/" replace />;
    }
    return children;
}

function AppRoutes() {
    const { loading } = useAuth();

    if (loading) {
        return (
            <div className="login-screen">
                <div className="login-card">
                    <h1>RBAC Admin</h1>
                    <p>Connecting to authentication server...</p>
                    <LoadingSpinner text="Initializing Keycloak..." />
                </div>
            </div>
        );
    }

    return (
        <Routes>
            <Route element={<Layout />}>
                <Route path="/" element={<Dashboard />} />

                {/* System Admin Only Routes */}
                <Route path="/users" element={<RequireSystemAdmin><UserList /></RequireSystemAdmin>} />
                <Route path="/users/create" element={<RequireSystemAdmin><UserCreate /></RequireSystemAdmin>} />
                <Route path="/users/:userId" element={<RequireSystemAdmin><UserDetail /></RequireSystemAdmin>} />
                <Route path="/kong-instances" element={<RequireSystemAdmin><KongInstanceList /></RequireSystemAdmin>} />
                <Route path="/kong-instances/create" element={<RequireSystemAdmin><KongInstanceCreate /></RequireSystemAdmin>} />
                <Route path="/kong-instances/:id/edit" element={<RequireSystemAdmin><KongInstanceEdit /></RequireSystemAdmin>} />
                {/* Mixed Access Routes */}
                <Route path="/projects" element={<ProjectList />} />
                <Route path="/projects/create" element={<RequireSystemAdmin><ProjectCreate /></RequireSystemAdmin>} />
                <Route path="/projects/:projectCode" element={<ProjectDetail />} />
                <Route path="/projects/:projectCode/members" element={<ProjectMembers />} />
                <Route path="/projects/:projectCode/storage" element={<RequireTenantAdmin><ProjectStoragePage /></RequireTenantAdmin>} />

                {/* Kong Gateway Routes */}
                <Route path="/projects/:projectCode/kong" element={<RequireTenantAdmin><KongOverview /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/services" element={<RequireTenantAdmin><KongServiceList /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/services/create" element={<RequireTenantAdmin><KongServiceCreate /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/services/:serviceId" element={<RequireTenantAdmin><KongServiceDetail /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/services/:serviceId/edit" element={<RequireTenantAdmin><KongServiceEdit /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/upstreams" element={<RequireTenantAdmin><KongUpstreamList /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/upstreams/create" element={<RequireTenantAdmin><KongUpstreamCreate /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/upstreams/:upstreamId" element={<RequireTenantAdmin><KongUpstreamDetail /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/upstreams/:upstreamId/edit" element={<RequireTenantAdmin><KongUpstreamEdit /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/kong/plugins" element={<RequireTenantAdmin><KongPluginList /></RequireTenantAdmin>} />

                {/* Observability Routes */}
                <Route path="/projects/:projectCode/audit-logs" element={<RequireTenantAdmin><AuditLogPage /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/gateway-logs" element={<RequireTenantAdmin><GatewayLogPage /></RequireTenantAdmin>} />
                <Route path="/projects/:projectCode/live-logs" element={<RequireTenantAdmin><LiveLogs /></RequireTenantAdmin>} />
            </Route>
        </Routes>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <KongProvider>
                    <ToastProvider>
                        <AppRoutes />
                    </ToastProvider>
                </KongProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}
