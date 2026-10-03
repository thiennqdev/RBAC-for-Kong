import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import kongService from '../../services/kongService';
import KongInstanceSelector from '../../components/kong/KongInstanceSelector';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongOverview() {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    
    const [counts, setCounts] = useState({
        services: 0,
        upstreams: 0,
        plugins: 0
    });
    const [loading, setLoading] = useState(true);
    const { selectedKongInstance } = useContext(KongContext);

    useEffect(() => {
        async function fetchCounts() {
            try {
                const [servicesRes, upstreamsRes, pluginsRes] = await Promise.all([
                    kongService.listServices(selectedKongInstance.id, projectCode).catch(() => ({ data: [] })),
                    kongService.listUpstreams(selectedKongInstance.id, projectCode).catch(() => ({ data: [] })),
                    kongService.listPlugins(selectedKongInstance.id, projectCode).catch(() => ({ data: [] }))
                ]);
                
                setCounts({
                    services: servicesRes.data?.length || 0,
                    upstreams: upstreamsRes.data?.length || 0,
                    plugins: pluginsRes.data?.length || 0
                });
            } catch (err) {
                console.error("Failed to fetch kong stats", err);
            } finally {
                setLoading(false);
            }
        }
        if (selectedKongInstance) {
            fetchCounts();
        }
    }, [projectCode, selectedKongInstance]);

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>API Gateway Management</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage Kong Gateway resources for tenant: <strong style={{ color: 'var(--accent-primary)' }}>{projectCode}</strong></p>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <KongInstanceSelector />
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}`)}>← Back to Project</button>
                </div>
            </div>

            <div className="detail-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
                <div className="card" style={{ cursor: 'pointer', transition: 'transform 0.2s', borderTop: '4px solid #3b82f6', position: 'relative' }} onClick={() => navigate('services')}>
                    {!loading && <div style={{ position: 'absolute', top: '20px', right: '20px', background: 'rgba(59,130,246,0.1)', color: '#3b82f6', padding: '4px 12px', borderRadius: '16px', fontWeight: 'bold' }}>{counts.services}</div>}
                    <h3 style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>🌐 Services</h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: '1.5' }}>Manage backend services that Kong forwards traffic to. Think of these as your upstream APIs.</p>
                </div>
                
                <div className="card" style={{ cursor: 'pointer', transition: 'transform 0.2s', borderTop: '4px solid #10b981', position: 'relative' }} onClick={() => navigate('upstreams')}>
                    {!loading && <div style={{ position: 'absolute', top: '20px', right: '20px', background: 'rgba(16,185,129,0.1)', color: '#10b981', padding: '4px 12px', borderRadius: '16px', fontWeight: 'bold' }}>{counts.upstreams}</div>}
                    <h3 style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>⚖️ Upstreams</h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: '1.5' }}>Configure load balancing and health checking for multiple target instances.</p>
                </div>

                <div className="card" style={{ cursor: 'pointer', transition: 'transform 0.2s', borderTop: '4px solid #8b5cf6', position: 'relative' }} onClick={() => navigate('plugins')}>
                    {!loading && <div style={{ position: 'absolute', top: '20px', right: '20px', background: 'rgba(139,92,246,0.1)', color: '#8b5cf6', padding: '4px 12px', borderRadius: '16px', fontWeight: 'bold' }}>{counts.plugins}</div>}
                    <h3 style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>🔌 Plugins</h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: '1.5' }}>Configure Rate Limiting, JWT Auth, CORS, and more at the Service or Route level.</p>
                </div>
            </div>
        </div>
    );
}
