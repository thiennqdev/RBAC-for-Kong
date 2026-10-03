import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongUpstreamDetail() {
    const { projectCode, upstreamId } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const { selectedKongInstance } = useContext(KongContext);

    const [upstream, setUpstream] = useState(null);
    const [targets, setTargets] = useState([]);
    const [loading, setLoading] = useState(true);

    // Add Target Modal
    const [showModal, setShowModal] = useState(false);
    const [targetForm, setTargetForm] = useState({ target: '', weight: 100, tags: '' });
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (selectedKongInstance) loadData();
    }, [projectCode, upstreamId, selectedKongInstance]);

    async function loadData() {
        setLoading(true);
        try {
            const [upstreamRes, targetsRes] = await Promise.all([
                kongService.getUpstream(selectedKongInstance.id, projectCode, upstreamId),
                kongService.listTargets(selectedKongInstance.id, projectCode, upstreamId)
            ]);
            setUpstream(upstreamRes.data);
            setTargets(targetsRes.data || []);
        } catch (err) {
            toast.error('Failed to load upstream details: ' + err.message);
            if (!upstream) navigate(`/projects/${projectCode}/kong/upstreams`);
        } finally {
            setLoading(false);
        }
    }

    const handleFormChange = (e) => {
        const { name, value, type } = e.target;
        setTargetForm(prev => ({
            ...prev,
            [name]: type === 'number' ? parseInt(value) || 0 : value
        }));
    };

    const handleAddTarget = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        try {
            const payload = { ...targetForm, tags: targetForm.tags ? targetForm.tags.split(',').map(t => t.trim()).filter(t => t !== '') : null };
            if (payload.tags && payload.tags.length === 0) payload.tags = null;
            await kongService.addTarget(selectedKongInstance.id, projectCode, upstreamId, payload);
            toast.success('Target added successfully');
            setShowModal(false);
            setTargetForm({ target: '', weight: 100, tags: '' });
            loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleDeleteTarget = async (targetId, targetAddr) => {
        if (!confirm(`Are you sure you want to remove target "${targetAddr}" from this upstream?`)) return;
        try {
            await kongService.deleteTarget(selectedKongInstance.id, projectCode, upstreamId, targetId);
            toast.success('Target removed successfully');
            loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    };

    if (loading && !upstream) return <LoadingSpinner />;

    const algorithmLabels = {
        'round-robin': '🔄 Round Robin',
        'consistent-hashing': '#️⃣ Consistent Hashing',
        'least-connections': '📉 Least Connections',
        'latency': '⚡ Latency',
    };

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Upstream: <span style={{ color: 'var(--accent-primary)' }}>{upstream?.displayName || upstream?.name}</span></h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage targets (backend nodes) for this upstream</p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong/upstreams`)}>← Back to Upstreams</button>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong/upstreams/${upstreamId}/edit`)}>✏️ Edit Upstream</button>
                </div>
            </div>

            {/* Upstream Info */}
            <div className="card" style={{ marginBottom: '24px', display: 'flex', gap: '40px', flexWrap: 'wrap' }}>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Full Name (on Kong)</div>
                    <code style={{ fontSize: '1.05em', color: 'var(--text-primary)' }}>{upstream?.name}</code>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Algorithm</div>
                    <span style={{ fontWeight: 600 }}>
                        {algorithmLabels[upstream?.algorithm] || upstream?.algorithm || 'round-robin'}
                    </span>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Slots</div>
                    <span style={{ fontWeight: 600 }}>{upstream?.slots ?? 10000}</span>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Sync Status</div>
                    <span style={{
                        display: 'inline-flex', alignItems: 'center', gap: '4px',
                        padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                        background: upstream?.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
                        color: upstream?.syncStatus === 'SYNCED' ? '#10b981' : '#ef4444',
                        border: `1px solid ${upstream?.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`
                    }}>
                        {upstream?.syncStatus === 'SYNCED' ? '✓' : '✗'} {upstream?.syncStatus}
                    </span>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Active Targets</div>
                    <span style={{ fontWeight: 700, fontSize: '1.2em', color: 'var(--accent-primary)' }}>{targets.length}</span>
                </div>
            </div>

            {/* Targets Section */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3>🎯 Targets (Backend Nodes)</h3>
                <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Add Target</button>
            </div>

            <div className="card animate-fade-in" style={{ padding: 0, overflow: 'hidden' }}>
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Target Address</th>
                            <th>Weight</th>
                            <th>Status</th>
                            <th>Health</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {targets.length === 0 ? (
                            <tr>
                                <td colSpan="5" style={{ textAlign: 'center', padding: '48px 20px', color: 'var(--text-secondary)' }}>
                                    <div style={{ fontSize: '2rem', marginBottom: '8px' }}>🎯</div>
                                    <p style={{ margin: 0, fontWeight: 500 }}>No targets configured</p>
                                    <p style={{ margin: '4px 0 0', fontSize: '0.9em', color: 'var(--text-muted)' }}>
                                        Add backend server addresses (IP:Port) to enable load balancing
                                    </p>
                                </td>
                            </tr>
                        ) : (
                            targets.map(t => (
                                <tr key={t.id} className="kong-table-row">
                                    <td>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            <div style={{
                                                width: '32px', height: '32px', borderRadius: '6px',
                                                background: 'rgba(16,185,129,0.1)', color: '#10b981',
                                                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.9rem'
                                            }}>🖥️</div>
                                            <code style={{ fontSize: '0.95em', fontWeight: 600 }}>{t.target}</code>
                                        </div>
                                    </td>
                                    <td>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                            <div style={{
                                                width: '60px', height: '6px', borderRadius: '3px',
                                                background: 'var(--bg-secondary)', overflow: 'hidden'
                                            }}>
                                                <div style={{
                                                    width: `${Math.min((t.weight / 1000) * 100, 100)}%`,
                                                    height: '100%', borderRadius: '3px',
                                                    background: 'linear-gradient(90deg, #10b981, #3b82f6)'
                                                }}></div>
                                            </div>
                                            <span style={{ fontWeight: 600, fontSize: '0.9em' }}>{t.weight}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: t.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: t.syncStatus === 'SYNCED' ? '#10b981' : '#ef4444',
                                            border: `1px solid ${t.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`
                                        }}>
                                            {t.syncStatus === 'SYNCED' ? '✓' : '✗'} {t.syncStatus}
                                        </span>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: t.health === 'HEALTHY' ? 'rgba(16,185,129,0.15)' : (t.health === 'UNHEALTHY' ? 'rgba(239,68,68,0.15)' : 'rgba(107,114,128,0.15)'),
                                            color: t.health === 'HEALTHY' ? '#10b981' : (t.health === 'UNHEALTHY' ? '#ef4444' : '#6b7280'),
                                        }}>
                                            {t.health === 'HEALTHY' ? '💚' : (t.health === 'UNHEALTHY' ? '💔' : '⚪')} {t.health || 'UNKNOWN'}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: 'right' }}>
                                        <button className="btn btn-danger btn-sm" onClick={() => handleDeleteTarget(t.id, t.target)}>
                                            🗑️ Remove
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Add Target Modal */}
            {showModal && (
                <div className="modal-overlay" style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100
                }}>
                    <div className="card animate-slide-down" style={{ width: '100%', maxWidth: '500px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3 style={{ margin: 0 }}>Add Target</h3>
                            <button style={{ background: 'transparent', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: 'var(--text-muted)' }} onClick={() => setShowModal(false)}>×</button>
                        </div>
                        <form onSubmit={handleAddTarget}>
                            <div className="form-group">
                                <label className="form-label">Target Address <span style={{ color: 'red' }}>*</span></label>
                                <input
                                    type="text"
                                    name="target"
                                    className="form-input"
                                    value={targetForm.target}
                                    onChange={handleFormChange}
                                    required
                                    placeholder="e.g. 192.168.1.10:8080"
                                />
                                <small style={{ color: 'var(--text-muted)' }}>
                                    The target address (IP or hostname) and port. Format: host:port
                                </small>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Weight</label>
                                <input
                                    type="number"
                                    name="weight"
                                    className="form-input"
                                    value={targetForm.weight}
                                    onChange={handleFormChange}
                                    min="0"
                                    max="65535"
                                    placeholder="100"
                                />
                                <small style={{ color: 'var(--text-muted)' }}>
                                    The weight this target gets within the upstream (0-65535). Higher weight = more traffic. Set to 0 to temporarily disable.
                                </small>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Tags</label>
                                <input
                                    type="text"
                                    name="tags"
                                    className="form-input"
                                    value={targetForm.tags}
                                    onChange={handleFormChange}
                                    placeholder="e.g. internal, v1"
                                />
                                <small style={{ color: 'var(--text-muted)' }}>Comma separated list of tags.</small>
                            </div>

                            <div style={{ marginTop: '32px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? 'Adding...' : 'Add Target'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
