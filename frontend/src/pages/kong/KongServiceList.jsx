import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongServiceList() {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const [services, setServices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const { selectedKongInstance } = useContext(KongContext);

    useEffect(() => { 
        if (selectedKongInstance) loadServices(); 
    }, [projectCode, selectedKongInstance]);

    async function loadServices() {
        try {
            const res = await kongService.listServices(selectedKongInstance.id, projectCode);
            setServices(res.data || []);
        } catch (err) {
            toast.error('Failed to load services: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    async function handleDelete(id, name) {
        if (!confirm(`WARNING: Deleting service "${name}" will also cascade delete all its Routes and associated Plugins.\n\nAre you sure you want to proceed?`)) return;
        try {
            await kongService.deleteService(selectedKongInstance.id, projectCode, id);
            toast.success("Service deleted successfully!");
            loadServices();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    }

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Kong Services</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage backend API targets for <strong>{projectCode}</strong></p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong`)}>← Overview</button>
                    <button className="btn btn-primary" onClick={() => navigate('create')}>+ Create Service</button>
                </div>
            </div>

            <div className="toolbar" style={{ marginBottom: '20px' }}>
                <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
                    <span style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-muted)' }}>🔍</span>
                    <input 
                        className="form-input search-input" 
                        placeholder="Search services by name or url..." 
                        value={searchTerm}
                        onChange={e => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            <div className="card animate-fade-in" style={{ padding: 0, overflow: 'hidden' }}>
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Display Name</th>
                            <th>Target URL</th>
                            <th>Status</th>
                            <th>Sync Status</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {services.filter(s => 
                            s.displayName.toLowerCase().includes(searchTerm.toLowerCase()) || 
                            s.host.toLowerCase().includes(searchTerm.toLowerCase())
                        ).length === 0 ? (
                            <tr><td colSpan="4" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>No services match your search</td></tr>
                        ) : (
                            services.filter(s => 
                                s.displayName.toLowerCase().includes(searchTerm.toLowerCase()) || 
                                s.host.toLowerCase().includes(searchTerm.toLowerCase())
                            ).map(svc => (
                                <tr key={svc.id} className="kong-table-row">
                                    <td style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                                        <span style={{ fontSize: '1.2em' }}>📦</span>
                                        {svc.displayName}
                                    </td>
                                    <td>
                                        <code style={{ background: 'var(--bg-secondary)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.85em', border: '1px solid var(--border-color)', color: 'var(--text-primary)' }}>
                                            <span style={{ color: 'var(--accent-primary)' }}>{svc.protocol}://</span>
                                            {svc.host}:{svc.port}
                                            <span style={{ color: 'var(--text-muted)' }}>{svc.path || ''}</span>
                                        </code>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: svc.enabled ? 'rgba(34,197,94,0.15)' : 'rgba(107,114,128,0.15)',
                                            color: svc.enabled ? '#22c55e' : '#6b7280',
                                        }}>
                                            {svc.enabled ? 'Enabled' : 'Disabled'}
                                        </span>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: svc.syncStatus === 'SYNCED' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: svc.syncStatus === 'SYNCED' ? '#22c55e' : '#ef4444',
                                            border: `1px solid ${svc.syncStatus === 'SYNCED' ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`
                                        }}>
                                            {svc.syncStatus === 'SYNCED' ? '✓' : '✗'} {svc.syncStatus}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: 'right', display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                                        <button className="btn btn-secondary btn-sm" onClick={() => navigate(`${svc.id}`)}>
                                            Manage Routes ↗
                                        </button>
                                        <button className="btn btn-secondary btn-sm" onClick={() => navigate(`${svc.id}/edit`)}>
                                            ✏️
                                        </button>
                                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(svc.id, svc.displayName || svc.name)}>
                                            🗑️
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
