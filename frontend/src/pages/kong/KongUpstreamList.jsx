import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongUpstreamList() {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const [upstreams, setUpstreams] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const { selectedKongInstance } = useContext(KongContext);
    
    useEffect(() => { 
        if (selectedKongInstance) loadUpstreams(); 
    }, [projectCode, selectedKongInstance]);

    async function loadUpstreams() {
        try {
            const res = await kongService.listUpstreams(selectedKongInstance.id, projectCode);
            setUpstreams(res.data || []);
        } catch (err) {
            toast.error('Failed to load upstreams: ' + err.message);
        } finally {
            setLoading(false);
        }
    }


    async function handleDelete(id, upstreamName) {
        if (!confirm(`Are you sure you want to delete upstream "${upstreamName}"?`)) return;
        try {
            await kongService.deleteUpstream(selectedKongInstance.id, projectCode, id);
            toast.success("Upstream deleted!");
            loadUpstreams();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    }

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>⚖️ Kong Upstreams</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage Load Balancing and Targets for <strong>{projectCode}</strong></p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong`)}>← Overview</button>
                    <button className="btn btn-primary" style={{ background: '#10b981', borderColor: '#10b981' }} onClick={() => navigate('create')}>
                        + Add Upstream
                    </button>
                </div>
            </div>

            <div className="toolbar" style={{ marginBottom: '20px' }}>
                <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
                    <span style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-muted)' }}>🔍</span>
                    <input 
                        className="form-input search-input" 
                        placeholder="Search upstreams..." 
                        value={searchTerm}
                        onChange={e => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            <div className="card animate-fade-in" style={{ padding: 0, overflow: 'hidden' }}>
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Upstream Name</th>
                            <th>Status</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {upstreams.filter(u => u.name.toLowerCase().includes(searchTerm.toLowerCase())).length === 0 ? (
                            <tr><td colSpan="3" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>No upstreams match your search.</td></tr>
                        ) : (
                            upstreams.filter(u => u.name.toLowerCase().includes(searchTerm.toLowerCase())).map(u => (
                                <tr key={u.id} className="kong-table-row">
                                    <td style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '10px' }}>
                                        <div style={{ width: '32px', height: '32px', borderRadius: '6px', background: 'rgba(59,130,246,0.1)', color: '#3b82f6', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.1rem' }}>
                                            ⚖️
                                        </div>
                                        <a
                                            onClick={() => navigate(`/projects/${projectCode}/kong/upstreams/${u.id}`)}
                                            style={{ color: 'var(--accent-primary)', cursor: 'pointer', textDecoration: 'none' }}
                                            onMouseEnter={e => e.target.style.textDecoration = 'underline'}
                                            onMouseLeave={e => e.target.style.textDecoration = 'none'}
                                        >
                                            {u.displayName || u.name}
                                        </a>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: u.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: u.syncStatus === 'SYNCED' ? '#10b981' : '#ef4444',
                                            border: `1px solid ${u.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`
                                        }}>
                                            {u.syncStatus === 'SYNCED' ? '✓' : '✗'} {u.syncStatus}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: 'right', display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                                        <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/projects/${projectCode}/kong/upstreams/${u.id}`)}>
                                            🎯 Targets
                                        </button>
                                        <button className="btn btn-secondary btn-sm" onClick={() => navigate(`${u.id}/edit`)}>
                                            ✏️ Edit
                                        </button>
                                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(u.id, u.displayName || u.name)}>
                                            🗑️ Delete
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
