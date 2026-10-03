import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import userService from '../services/userService';
import projectService from '../services/projectService';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { useAuth } from '../context/AuthContext';

export default function Dashboard() {
    const [stats, setStats] = useState({ users: 0, projects: 0 });
    const [loading, setLoading] = useState(true);
    const [recentUsers, setRecentUsers] = useState([]);
    const navigate = useNavigate();
    const { user } = useAuth();
    const isSystemAdmin = user?.roles?.includes('SYSTEM_ADMIN');

    useEffect(() => {
        loadStats();
    }, []);

    async function loadStats() {
        try {
            if (isSystemAdmin) {
                const [usersRes, projectsRes] = await Promise.all([
                    userService.list({ page: 0, size: 5 }),
                    projectService.list({ page: 0, size: 1 }),
                ]);

                const userData = usersRes.data;
                const projectData = projectsRes.data;

                setStats({
                    users: userData.totalElements ?? userData.data?.length ?? 0,
                    projects: projectData.totalElements ?? projectData.data?.length ?? 0,
                });

                setRecentUsers(userData.data?.slice(0, 5) || []);
            } else {
                const projectsRes = await projectService.list({ page: 0, size: 1 });
                const projectData = projectsRes.data;
                setStats(prev => ({
                    ...prev,
                    projects: projectData.totalElements ?? projectData.data?.length ?? 0,
                }));
            }
        } catch (err) {
            console.error('Failed to load stats:', err);
        } finally {
            setLoading(false);
        }
    }

    if (loading) return <LoadingSpinner text="Loading dashboard..." />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>{isSystemAdmin ? 'System Dashboard' : 'My Dashboard'}</h2>
                    <p>{isSystemAdmin ? 'Overview of your RBAC system' : 'Overview of your assigned projects'}</p>
                </div>
            </div>

            <div className="stats-grid">
                {isSystemAdmin && (
                    <div className="stat-card" onClick={() => navigate('/users')} style={{ cursor: 'pointer' }}>
                        <div className="stat-icon users">👥</div>
                        <div className="stat-info">
                            <h3>Total Users</h3>
                            <div className="stat-value">{stats.users}</div>
                        </div>
                    </div>
                )}
                <div className="stat-card" onClick={() => navigate('/projects')} style={{ cursor: 'pointer' }}>
                    <div className="stat-icon projects">📁</div>
                    <div className="stat-info">
                        <h3>Total Projects</h3>
                        <div className="stat-value">{stats.projects}</div>
                    </div>
                </div>
            </div>

            {isSystemAdmin && (
                <div className="card">
                    <h3 style={{ marginBottom: '16px' }}>Recent Users</h3>
                    {recentUsers.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-icon">👥</div>
                            <h3>No users yet</h3>
                            <p>Create your first user to get started</p>
                        </div>
                    ) : (
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Email</th>
                                    <th>Status</th>
                                    <th>Created</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentUsers.map((u) => (
                                    <tr key={u.id} onClick={() => navigate(`/users/${u.id}`)} style={{ cursor: 'pointer' }}>
                                        <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{u.username}</td>
                                        <td>{u.email}</td>
                                        <td>
                                            <span className={`badge ${u.status === 'ACTIVE' ? 'badge-active' : 'badge-locked'}`}>
                                                {u.status}
                                            </span>
                                        </td>
                                        <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}
        </div>
    );
}
