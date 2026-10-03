import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import userService from '../../services/userService';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function UserList() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [syncingAll, setSyncingAll] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();

    useEffect(() => {
        loadUsers();
    }, [page, search]);

    async function loadUsers() {
        setLoading(true);
        try {
            const params = { page, size: 10 };
            if (search) params.keyword = search;
            const res = await userService.list(params);
            const data = res.data;
            setUsers(data.data || data.content || []);
            setTotalPages(data.totalPages || 1);
        } catch (err) {
            toast.error('Failed to load users: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    function handleSearch(e) {
        setSearch(e.target.value);
        setPage(0);
    }

    async function handleSyncAll() {
        if (!window.confirm("Are you sure you want to sync all unsynced local users to Keycloak? This might take a moment if there are many users.")) {
            return;
        }

        setSyncingAll(true);
        try {
            const res = await userService.syncAll();
            const syncedCount = res.data;
            if (syncedCount > 0) {
                toast.success(`Successfully synced ${syncedCount} user(s) to Keycloak!`);
                loadUsers(); // Refresh the list
            } else {
                toast.info("All users are already synced. No action needed.");
            }
        } catch (err) {
            toast.error('Failed to sync users: ' + (err.response?.data?.message || err.message));
        } finally {
            setSyncingAll(false);
        }
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Users</h2>
                    <p>Manage system users and their Keycloak accounts</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                        className="btn btn-secondary"
                        onClick={handleSyncAll}
                        disabled={syncingAll}
                    >
                        {syncingAll ? 'Syncing...' : '↻ Sync Local DB to Keycloak'}
                    </button>
                    <button className="btn btn-primary" onClick={() => navigate('/users/create')}>
                        + Create User
                    </button>
                </div>
            </div>

            <div className="toolbar">
                <input
                    type="text"
                    className="search-input"
                    placeholder="🔍 Search users by name or email..."
                    value={search}
                    onChange={handleSearch}
                />
            </div>

            {loading ? (
                <LoadingSpinner />
            ) : (
                <div className="data-table-wrapper">
                    {users.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-icon">👥</div>
                            <h3>No users found</h3>
                            <p>Create your first user or try a different search</p>
                        </div>
                    ) : (
                        <>
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Username</th>
                                        <th>Email</th>
                                        <th>Name</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {users.map((u) => (
                                        <tr key={u.id}>
                                            <td>{u.id}</td>
                                            <td style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{u.username}</td>
                                            <td>{u.email}</td>
                                            <td>{[u.firstName, u.lastName].filter(Boolean).join(' ') || '-'}</td>
                                            <td><StatusBadge status={u.status} /></td>
                                            <td>
                                                <button className="btn btn-sm btn-secondary" onClick={() => navigate(`/users/${u.id}`)}>
                                                    View
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            {totalPages > 1 && (
                                <div className="pagination">
                                    <button disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
                                    {Array.from({ length: totalPages }, (_, i) => (
                                        <button key={i} className={page === i ? 'active' : ''} onClick={() => setPage(i)}>
                                            {i + 1}
                                        </button>
                                    ))}
                                    <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>→</button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
