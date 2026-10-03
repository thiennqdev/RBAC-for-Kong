import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import CopyToClipboard from '../../components/common/CopyToClipboard';
import { useToast } from '../../components/common/Toast';
import { useAuth } from '../../context/AuthContext';
import memberService from '../../services/memberService';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongServiceDetail() {
    const { projectCode, serviceId } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const { user } = useAuth();
    const { selectedKongInstance } = useContext(KongContext);
    
    const [service, setService] = useState(null);
    const [routes, setRoutes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [userRole, setUserRole] = useState(null);
    const [processingRouteId, setProcessingRouteId] = useState(null);
    
    // Route Modal State
    const [showModal, setShowModal] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [currentRouteId, setCurrentRouteId] = useState(null);
    const [routeForm, setRouteForm] = useState({
        name: '',
        paths: '',
        methods: '',
        protocols: 'http, https',
        stripPath: true,
        preserveHost: false,
        hosts: '',
        headers: '',
        snis: '',
        sources: '',
        destinations: '',
        regexPriority: 0,
        tags: ''
    });
    const [showAdvanced, setShowAdvanced] = useState(false);

    useEffect(() => {
        if (selectedKongInstance) loadData();
    }, [projectCode, serviceId, selectedKongInstance]);

    async function loadData() {
        setLoading(true);
        try {
            const [serviceRes, routesRes, membersRes] = await Promise.all([
                kongService.getService(selectedKongInstance.id, projectCode, serviceId),
                kongService.listRoutes(selectedKongInstance.id, projectCode, serviceId),
                memberService.list(projectCode)
            ]);
            setService(serviceRes.data);
            setRoutes(routesRes.data || []);
            if (user?.username && membersRes.data) {
                const me = membersRes.data.find(m => m.username === user.username);
                if (me) setUserRole(me.roleName);
            }
        } catch (err) {
            toast.error('Failed to load data: ' + err.message);
            if (!service) navigate(`/projects/${projectCode}/kong/services`);
        } finally {
            setLoading(false);
        }
    }

    const openCreateModal = () => {
        setIsEditing(false);
        setRouteForm({
            name: '',
            paths: '',
            methods: '',
            protocols: 'http, https',
            stripPath: true,
            preserveHost: false,
            hosts: '',
            headers: '',
            snis: '',
            sources: '',
            destinations: '',
            regexPriority: 0,
            tags: ''
        });
        setShowAdvanced(false);
        setShowModal(true);
    };

    const openEditModal = (route) => {
        setIsEditing(true);
        setCurrentRouteId(route.id);
        setRouteForm({
            name: route.displayName || route.name,
            paths: route.paths ? route.paths.join(', ') : '',
            methods: route.methods ? route.methods.join(', ') : '',
            protocols: route.protocols ? route.protocols.join(', ') : 'http, https',
            stripPath: route.stripPath !== false, // default true
            preserveHost: route.preserveHost === true, // default false
            hosts: route.hosts ? route.hosts.join(', ') : '',
            headers: route.headers ? JSON.stringify(route.headers) : '',
            snis: route.snis ? route.snis.join(', ') : '',
            sources: route.sources ? JSON.stringify(route.sources) : '',
            destinations: route.destinations ? JSON.stringify(route.destinations) : '',
            regexPriority: route.regexPriority || 0,
            tags: route.tags ? route.tags.join(', ') : ''
        });
        setShowAdvanced(false);
        setShowModal(true);
    };

    const handleFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        setRouteForm(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handlePathsBlur = (e) => {
        let val = e.target.value.trim();
        if (val) {
            const pathArr = val.split(',').map(p => {
                let s = p.trim();
                if (s && !s.startsWith(`/${projectCode}`)) {
                    return s.startsWith('/') ? `/${projectCode}${s}` : `/${projectCode}/${s}`;
                }
                return s;
            });
            setRouteForm(prev => ({ ...prev, paths: pathArr.join(', ') }));
        }
    };

    const parseCommaSeparated = (str) => {
        if (!str) return [];
        return str.split(',').map(s => s.trim().toUpperCase()).filter(s => s !== '');
    };
    
    const parsePaths = (str) => {
        if (!str) return [];
        return str.split(',').map(s => s.trim()).filter(s => s !== '');
    };

    const handleSaveRoute = async (e) => {
        e.preventDefault();
        try {
            const payload = {
                name: routeForm.name,
                paths: parsePaths(routeForm.paths),
                methods: parseCommaSeparated(routeForm.methods),
                protocols: parseCommaSeparated(routeForm.protocols).map(s => s.toLowerCase()),
                stripPath: routeForm.stripPath,
                preserveHost: routeForm.preserveHost,
                hosts: parsePaths(routeForm.hosts), // parse as comma separated list
                snis: parsePaths(routeForm.snis),
                regexPriority: Number(routeForm.regexPriority) || 0,
                tags: parsePaths(routeForm.tags)
            };
            
            try {
                if (routeForm.headers) payload.headers = JSON.parse(routeForm.headers);
                if (routeForm.sources) payload.sources = JSON.parse(routeForm.sources);
                if (routeForm.destinations) payload.destinations = JSON.parse(routeForm.destinations);
            } catch (err) {
                toast.error("Invalid JSON format in Headers, Sources, or Destinations");
                return;
            }
            
            // Cleanup empty arrays so Kong doesn't complain if they are not needed
            if (payload.paths.length === 0) payload.paths = null;
            if (payload.methods.length === 0) payload.methods = null;
            if (payload.hosts.length === 0) payload.hosts = null;
            if (payload.snis.length === 0) payload.snis = null;
            if (payload.tags.length === 0) payload.tags = null;

            if (isEditing) {
                await kongService.updateRoute(selectedKongInstance.id, projectCode, currentRouteId, payload);
                toast.success('Route updated successfully');
            } else {
                await kongService.createRoute(selectedKongInstance.id, projectCode, serviceId, payload);
                toast.success('Route created successfully');
            }
            setShowModal(false);
            loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    };

    const handleDeleteRoute = async (id, name) => {
        if (!confirm(`Are you sure you want to delete route "${name}"?`)) return;
        setProcessingRouteId(id);
        try {
            await kongService.deleteRoute(selectedKongInstance.id, projectCode, id);
            toast.success("Route deleted successfully");
            await loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        } finally {
            setProcessingRouteId(null);
        }
    };

    const handleRequestApproval = async (id, name) => {
        setProcessingRouteId(id);
        try {
            await kongService.requestRouteApproval(selectedKongInstance.id, projectCode, id);
            toast.success(`Approval requested for "${name}"`);
            await loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        } finally {
            setProcessingRouteId(null);
        }
    };

    const handleApprove = async (id, name) => {
        setProcessingRouteId(id);
        try {
            await kongService.approveRoute(selectedKongInstance.id, projectCode, id);
            toast.success(`Route "${name}" approved successfully`);
            await loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        } finally {
            setProcessingRouteId(null);
        }
    };

    const handleReject = async (id, name) => {
        if (!confirm(`Are you sure you want to reject route "${name}"?`)) return;
        setProcessingRouteId(id);
        try {
            await kongService.rejectRoute(selectedKongInstance.id, projectCode, id);
            toast.success(`Route "${name}" rejected`);
            await loadData();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        } finally {
            setProcessingRouteId(null);
        }
    };

    if (loading && !service) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Service Details: <span style={{ color: 'var(--accent-primary)' }}>{service?.displayName || service?.name}</span></h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage routes for this service</p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong/services`)}>← Back to Services</button>
            </div>

            {/* Service Info Box */}
            <div className="card" style={{ marginBottom: '24px', display: 'flex', gap: '32px', flexWrap: 'wrap' }}>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Target URL</div>
                    <code style={{ fontSize: '1.1em', color: 'var(--text-primary)' }}>
                        {service.protocol}://{service.host}:{service.port}{service.path || ''}
                    </code>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Status</div>
                    <span style={{ color: service.enabled ? '#22c55e' : '#6b7280', fontWeight: 600 }}>
                        {service.enabled ? 'Enabled' : 'Disabled'}
                    </span>
                </div>
                <div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85em', marginBottom: '4px' }}>Sync Status</div>
                    <span style={{ color: service.syncStatus === 'SYNCED' ? '#22c55e' : '#ef4444', fontWeight: 600 }}>
                        {service.syncStatus === 'SYNCED' ? '✓ SYNCED' : '✗ ' + service.syncStatus}
                    </span>
                </div>
            </div>

            {/* Routes Section */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3>Routes</h3>
                <button className="btn btn-primary" onClick={openCreateModal}>+ Add Route</button>
            </div>

            <div className="card animate-fade-in" style={{ padding: 0, overflow: 'hidden' }}>
                <table className="data-table">
                    <thead>
                        <tr>
                            <th>Route Name</th>
                            <th>Paths</th>
                            <th>Methods</th>
                            <th>Status</th>
                            <th>Sync Status</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {routes.length === 0 ? (
                            <tr><td colSpan="5" style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>No routes configured for this service</td></tr>
                        ) : (
                            routes.map(route => (
                                <tr key={route.id} className="kong-table-row">
                                    <td style={{ fontWeight: 600 }}>{route.displayName || route.name}</td>
                                    <td>
                                        {route.paths && route.paths.length > 0 ? (
                                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                                {route.paths.map(p => (
                                                    <span key={p} style={{ background: 'var(--bg-secondary)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.85em', border: '1px solid var(--border-color)', display: 'inline-flex', alignItems: 'center' }}>
                                                        {p}
                                                        <CopyToClipboard text={p} />
                                                    </span>
                                                ))}
                                            </div>
                                        ) : <span style={{ color: 'var(--text-muted)' }}>Any</span>}
                                    </td>
                                    <td>
                                        {route.methods && route.methods.length > 0 ? (
                                            <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                                                {route.methods.map(m => <span key={m} style={{ background: 'rgba(59,130,246,0.1)', color: '#3b82f6', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8em', fontWeight: 'bold' }}>{m}</span>)}
                                            </div>
                                        ) : <span style={{ color: 'var(--text-muted)' }}>Any</span>}
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: route.status === 'ACTIVE' ? 'rgba(34,197,94,0.15)' 
                                                        : route.status === 'DRAFT' ? 'rgba(107,114,128,0.15)' 
                                                        : route.status === 'REJECTED' ? 'rgba(239,68,68,0.15)' 
                                                        : 'rgba(234,179,8,0.15)',
                                            color: route.status === 'ACTIVE' ? '#22c55e' 
                                                   : route.status === 'DRAFT' ? '#6b7280' 
                                                   : route.status === 'REJECTED' ? '#ef4444' 
                                                   : '#eab308',
                                        }}>
                                            {route.status}
                                        </span>
                                    </td>
                                    <td>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '4px 10px', borderRadius: '12px', fontSize: '0.8em', fontWeight: 600,
                                            background: route.syncStatus === 'SYNCED' ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: route.syncStatus === 'SYNCED' ? '#22c55e' : '#ef4444',
                                        }}>
                                            {route.syncStatus === 'SYNCED' ? '✓ SYNCED' : '✗ PENDING'}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: 'right', display: 'flex', gap: '8px', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                                        {(route.status === 'DRAFT' || route.status === 'REJECTED') && (
                                            <button 
                                                className="btn btn-primary btn-sm" 
                                                onClick={() => handleRequestApproval(route.id, route.displayName || route.name)}
                                                disabled={processingRouteId === route.id}
                                                style={processingRouteId === route.id ? { background: '#9ca3af', borderColor: '#9ca3af', cursor: 'not-allowed' } : {}}
                                            >
                                                {processingRouteId === route.id ? 'Requesting...' : 'Request Approval'}
                                            </button>
                                        )}
                                        {route.status === 'PENDING_APPROVAL' && (
                                            userRole === 'PROJECT_ADMIN' ? (
                                                <>
                                                    <button 
                                                        className="btn btn-success btn-sm" 
                                                        onClick={() => handleApprove(route.id, route.displayName || route.name)} 
                                                        disabled={processingRouteId === route.id}
                                                        style={processingRouteId === route.id ? { background: '#9ca3af', borderColor: '#9ca3af', cursor: 'not-allowed', color: 'white' } : { background: '#22c55e', borderColor: '#22c55e', color: 'white' }}
                                                    >
                                                        {processingRouteId === route.id ? 'Approving...' : 'Approve'}
                                                    </button>
                                                    <button 
                                                        className="btn btn-danger btn-sm" 
                                                        onClick={() => handleReject(route.id, route.displayName || route.name)}
                                                        disabled={processingRouteId === route.id}
                                                        style={processingRouteId === route.id ? { background: '#9ca3af', borderColor: '#9ca3af', cursor: 'not-allowed' } : {}}
                                                    >
                                                        {processingRouteId === route.id ? 'Rejecting...' : 'Reject'}
                                                    </button>
                                                </>
                                            ) : (
                                                <span style={{ color: 'var(--text-muted)', fontSize: '0.85em', fontStyle: 'italic', display: 'flex', alignItems: 'center' }}>
                                                    Waiting for Approval...
                                                </span>
                                            )
                                        )}
                                        <button className="btn btn-secondary btn-sm" onClick={() => openEditModal(route)} disabled={processingRouteId === route.id}>✏️</button>
                                        <button className="btn btn-danger btn-sm" onClick={() => handleDeleteRoute(route.id, route.displayName || route.name)} disabled={processingRouteId === route.id}>🗑️</button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Route Modal/Drawer */}
            {showModal && (
                <div className="modal-overlay" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100 }}>
                    <div className="card animate-slide-down" style={{ width: '100%', maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3 style={{ margin: 0 }}>{isEditing ? 'Edit Route' : 'Create Route'}</h3>
                            <button style={{ background: 'transparent', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: 'var(--text-muted)' }} onClick={() => setShowModal(false)}>×</button>
                        </div>
                        <form onSubmit={handleSaveRoute}>
                            <div className="form-group">
                                <label className="form-label">Route Name <span style={{ color: 'red' }}>*</span></label>
                                <input 
                                    type="text" 
                                    name="name"
                                    className="form-input" 
                                    value={routeForm.name}
                                    onChange={handleFormChange}
                                    required
                                    placeholder="e.g. users-v1"
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Paths</label>
                                <input 
                                    type="text" 
                                    name="paths"
                                    className="form-input" 
                                    value={routeForm.paths}
                                    onChange={handleFormChange}
                                    onBlur={handlePathsBlur}
                                    placeholder={`e.g. /${projectCode}/users, /${projectCode}/api/v1`}
                                />
                                <small style={{ color: 'var(--text-muted)' }}>Comma separated. A list of paths that match this Route.</small>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Methods</label>
                                <input 
                                    type="text" 
                                    name="methods"
                                    className="form-input" 
                                    value={routeForm.methods}
                                    onChange={handleFormChange}
                                    placeholder="e.g. GET, POST, PUT"
                                />
                                <small style={{ color: 'var(--text-muted)' }}>Comma separated. If left empty, all methods are matched.</small>
                            </div>
                            
                            <div className="form-group">
                                <label className="form-label">Protocols</label>
                                <input 
                                    type="text" 
                                    name="protocols"
                                    className="form-input" 
                                    value={routeForm.protocols}
                                    onChange={handleFormChange}
                                    placeholder="e.g. http, https"
                                />
                            </div>

                            <div style={{ display: 'flex', gap: '24px', marginTop: '16px' }}>
                                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <input 
                                        type="checkbox" 
                                        name="stripPath"
                                        id="stripPath"
                                        checked={routeForm.stripPath}
                                        onChange={handleFormChange}
                                        style={{ width: '18px', height: '18px' }}
                                    />
                                    <label htmlFor="stripPath" style={{ margin: 0, fontWeight: 500 }}>Strip Path</label>
                                </div>
                                <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <input 
                                        type="checkbox" 
                                        name="preserveHost"
                                        id="preserveHost"
                                        checked={routeForm.preserveHost}
                                        onChange={handleFormChange}
                                        style={{ width: '18px', height: '18px' }}
                                    />
                                    <label htmlFor="preserveHost" style={{ margin: 0, fontWeight: 500 }}>Preserve Host</label>
                                </div>
                            </div>
                            
                            <div style={{ marginTop: '24px', borderTop: '1px solid var(--border-color)', paddingTop: '16px' }}>
                                <button 
                                    type="button" 
                                    className="btn btn-secondary" 
                                    onClick={() => setShowAdvanced(!showAdvanced)}
                                    style={{ marginBottom: '16px' }}
                                >
                                    {showAdvanced ? 'Hide Advanced Settings' : 'Show Advanced Settings'}
                                </button>

                                {showAdvanced && (
                                    <div className="animate-fade-in" style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '16px', background: 'var(--bg-secondary)', padding: '16px', borderRadius: '8px' }}>
                                        <div className="form-group">
                                            <label className="form-label">Hosts</label>
                                            <input type="text" name="hosts" className="form-input" value={routeForm.hosts} onChange={handleFormChange} placeholder="e.g. example.com, foo.test.com" />
                                            <small style={{ color: 'var(--text-muted)' }}>Comma separated. A list of domain names that match this Route.</small>
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">Headers (JSON)</label>
                                            <textarea name="headers" className="form-input" value={routeForm.headers} onChange={handleFormChange} placeholder='e.g. {"x-my-header": ["foo", "bar"]}' rows="2" />
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">SNIs</label>
                                            <input type="text" name="snis" className="form-input" value={routeForm.snis} onChange={handleFormChange} placeholder="e.g. example.com" />
                                            <small style={{ color: 'var(--text-muted)' }}>Comma separated. A list of SNIs that match this Route when using stream routing.</small>
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">Sources (JSON)</label>
                                            <textarea name="sources" className="form-input" value={routeForm.sources} onChange={handleFormChange} placeholder='e.g. [{"ip": "10.1.0.0/16", "port": 1234}]' rows="2" />
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">Destinations (JSON)</label>
                                            <textarea name="destinations" className="form-input" value={routeForm.destinations} onChange={handleFormChange} placeholder='e.g. [{"ip": "10.2.0.0/16", "port": 8080}]' rows="2" />
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">Regex Priority</label>
                                            <input type="number" name="regexPriority" className="form-input" value={routeForm.regexPriority} onChange={handleFormChange} />
                                        </div>
                                        <div className="form-group">
                                            <label className="form-label">Tags</label>
                                            <input type="text" name="tags" className="form-input" value={routeForm.tags} onChange={handleFormChange} placeholder="e.g. internal, payment, v1" />
                                            <small style={{ color: 'var(--text-muted)' }}>Comma separated list of tags.</small>
                                        </div>
                                    </div>
                                )}
                            </div>

                            <div style={{ marginTop: '32px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">
                                    {isEditing ? 'Save Changes' : 'Create Route'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
