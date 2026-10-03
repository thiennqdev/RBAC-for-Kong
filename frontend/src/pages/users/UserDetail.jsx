import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import userService from '../../services/userService';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function UserDetail() {
    const { userId } = useParams();
    const [user, setUser] = useState(null);
    const [editing, setEditing] = useState(false);
    const [form, setForm] = useState({});
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const toast = useToast();

    useEffect(() => { loadUser(); }, [userId]);

    async function loadUser() {
        try {
            const res = await userService.get(userId);
            setUser(res.data);
            setForm({ email: res.data.email, firstName: res.data.firstName || '', lastName: res.data.lastName || '' });
        } catch (err) {
            toast.error('Failed to load user: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    async function handleUpdate(e) {
        e.preventDefault();
        setSubmitting(true);
        try {
            const res = await userService.update(userId, form);
            setUser(res.data);
            setEditing(false);
            toast.success('User updated successfully!');
        } catch (err) {
            toast.error(err.message);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleLock() {
        try {
            await userService.lock(userId);
            toast.success('User locked');
            loadUser();
        } catch (err) {
            toast.error(err.message);
        }
    }

    async function handleUnlock() {
        try {
            await userService.unlock(userId);
            toast.success('User unlocked');
            loadUser();
        } catch (err) {
            toast.error(err.message);
        }
    }

    if (loading) return <LoadingSpinner />;
    if (!user) return <div className="empty-state"><h3>User not found</h3></div>;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>User: {user.username}</h2>
                    <p>Manage user details and account status</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate('/users')}>← Back</button>
                    {!editing && <button className="btn btn-primary" onClick={() => setEditing(true)}>Edit</button>}
                    {user.status === 'ACTIVE' ? (
                        <button className="btn btn-warning" onClick={handleLock}>🔒 Lock</button>
                    ) : (
                        <button className="btn btn-success" onClick={handleUnlock}>🔓 Unlock</button>
                    )}
                </div>
            </div>

            <div className="card" style={{ maxWidth: '700px' }}>
                {editing ? (
                    <form onSubmit={handleUpdate}>
                        <div className="form-group">
                            <label className="form-label">Email</label>
                            <input className="form-input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                        </div>
                        <div className="form-row">
                            <div className="form-group">
                                <label className="form-label">First Name</label>
                                <input className="form-input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Last Name</label>
                                <input className="form-input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                            </div>
                        </div>
                        <div className="form-actions">
                            <button type="button" className="btn btn-secondary" onClick={() => setEditing(false)}>Cancel</button>
                            <button type="submit" className="btn btn-primary" disabled={submitting}>{submitting ? 'Saving...' : 'Save Changes'}</button>
                        </div>
                    </form>
                ) : (
                    <div className="detail-grid">
                        <div className="detail-item">
                            <div className="detail-label">ID</div>
                            <div className="detail-value">{user.id}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Keycloak ID</div>
                            <div className="detail-value" style={{ fontSize: '0.8rem', wordBreak: 'break-all' }}>{user.keycloakId}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Username</div>
                            <div className="detail-value">{user.username}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Email</div>
                            <div className="detail-value">{user.email}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">First Name</div>
                            <div className="detail-value">{user.firstName || '-'}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Last Name</div>
                            <div className="detail-value">{user.lastName || '-'}</div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Status</div>
                            <div className="detail-value"><StatusBadge status={user.status} /></div>
                        </div>
                        <div className="detail-item">
                            <div className="detail-label">Created At</div>
                            <div className="detail-value">{user.createdAt ? new Date(user.createdAt).toLocaleString() : '-'}</div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
