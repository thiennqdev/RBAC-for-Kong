import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import kongService from '../../services/kongService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';
import { KongContext } from '../../context/KongContext';
import { useContext } from 'react';

export default function KongPluginList() {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    const toast = useToast();
    const [plugins, setPlugins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const { selectedKongInstance } = useContext(KongContext);
    
    // Lists for dropdowns
    const [servicesList, setServicesList] = useState([]);
    const [allRoutes, setAllRoutes] = useState([]);
    
    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [currentPluginId, setCurrentPluginId] = useState(null);
    
    // Form state
    const [formData, setFormData] = useState({
        pluginName: 'rate-limiting',
        scope: 'SERVICE',
        scopeEntityId: '',
        enabled: true,
        config: {}
    });

    // Fallback JSON config editor
    const [jsonConfig, setJsonConfig] = useState('{}');

    useEffect(() => { 
        if (selectedKongInstance) {
            loadPlugins(); 
            loadServicesAndRoutes();
        }
    }, [projectCode, selectedKongInstance]);

    async function loadServicesAndRoutes() {
        try {
            const resServices = await kongService.listServices(selectedKongInstance.id, projectCode);
            const services = resServices.data || [];
            setServicesList(services);

            const routesPromises = services.map(s => kongService.listRoutes(selectedKongInstance.id, projectCode, s.id));
            const routesResults = await Promise.all(routesPromises);
            
            let allRts = [];
            routesResults.forEach((res, index) => {
                const rts = res.data || [];
                const srvName = services[index].serviceName || services[index].name || `ID:${services[index].id}`;
                rts.forEach(r => {
                    r.serviceName = srvName;
                });
                allRts = [...allRts, ...rts];
            });
            setAllRoutes(allRts);
        } catch (err) {
            console.error("Failed to load services and routes", err);
            // Don't show toast error here to avoid spamming if not used
        }
    }

    async function loadPlugins() {
        setLoading(true);
        try {
            const res = await kongService.listPlugins(selectedKongInstance.id, projectCode);
            setPlugins(res.data || []);
        } catch (err) {
            toast.error('Failed to load plugins: ' + err.message);
        } finally {
            setLoading(false);
        }
    }

    const openCreateModal = () => {
        setIsEditing(false);
        setFormData({
            pluginName: 'rate-limiting',
            scope: 'SERVICE',
            scopeEntityId: '',
            enabled: true,
            config: {}
        });
        setJsonConfig('{}');
        setShowModal(true);
    };

    const openEditModal = (plugin) => {
        setIsEditing(true);
        setCurrentPluginId(plugin.id);
        setFormData({
            pluginName: plugin.pluginName,
            scope: plugin.scope,
            scopeEntityId: plugin.scopeEntityId,
            enabled: plugin.enabled,
            config: plugin.config || {}
        });
        setJsonConfig(JSON.stringify(plugin.config || {}, null, 2));
        setShowModal(true);
    };

    const handleFormChange = (e) => {
        const { name, value, type, checked } = e.target;
        if (name === 'scopeEntityId') {
            setFormData(prev => ({ ...prev, scopeEntityId: value }));
        } else if (name === 'enabled') {
            setFormData(prev => ({ ...prev, enabled: checked }));
        } else if (name === 'pluginName' || name === 'scope') {
            setFormData(prev => ({ ...prev, [name]: value, config: {}, scopeEntityId: '' }));
            setJsonConfig('{}');
        } else {
            // Nested config fields
            setFormData(prev => ({
                ...prev,
                config: {
                    ...prev.config,
                    [name]: type === 'number' ? Number(value) : (type === 'checkbox' ? checked : value)
                }
            }));
        }
    };

    const handleJsonChange = (e) => {
        setJsonConfig(e.target.value);
    };

    const parseCommaSeparated = (str) => {
        if (!str) return [];
        return str.split(',').map(s => s.trim()).filter(s => s !== '');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        let finalConfig = { ...formData.config };
        
        // Transform dynamic fields if needed
        if (formData.pluginName === 'cors') {
            if (typeof finalConfig.origins === 'string') finalConfig.origins = parseCommaSeparated(finalConfig.origins);
            if (typeof finalConfig.methods === 'string') finalConfig.methods = parseCommaSeparated(finalConfig.methods);
            if (typeof finalConfig.headers === 'string') finalConfig.headers = parseCommaSeparated(finalConfig.headers);
        } else if (!['rate-limiting', 'cors', 'jwt'].includes(formData.pluginName)) {
            // Use JSON fallback
            try {
                finalConfig = JSON.parse(jsonConfig);
            } catch (err) {
                toast.error("Invalid JSON configuration");
                return;
            }
        }

        const payload = {
            pluginName: formData.pluginName,
            scope: formData.scope,
            scopeEntityId: parseInt(formData.scopeEntityId),
            enabled: formData.enabled,
            config: finalConfig
        };

        try {
            if (isEditing) {
                await kongService.updatePlugin(selectedKongInstance.id, projectCode, currentPluginId, payload);
                toast.success("Plugin updated successfully!");
            } else {
                await kongService.createPlugin(selectedKongInstance.id, projectCode, payload);
                toast.success("Plugin created successfully!");
            }
            setShowModal(false);
            loadPlugins();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    };

    const handleDelete = async (id) => {
        if (!confirm("Are you sure you want to delete this plugin?")) return;
        try {
            await kongService.deletePlugin(selectedKongInstance.id, projectCode, id);
            toast.success("Plugin deleted!");
            loadPlugins();
        } catch (err) {
            toast.error(err.response?.data?.message || err.message);
        }
    };

    const renderDynamicForm = () => {
        const { pluginName, config } = formData;
        
        if (pluginName === 'rate-limiting') {
            return (
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '16px' }}>
                    <div className="form-group" style={{ flex: 1, minWidth: '120px' }}>
                        <label className="form-label">Second</label>
                        <input type="number" name="second" className="form-input" value={config.second || ''} onChange={handleFormChange} placeholder="e.g. 5" />
                    </div>
                    <div className="form-group" style={{ flex: 1, minWidth: '120px' }}>
                        <label className="form-label">Minute</label>
                        <input type="number" name="minute" className="form-input" value={config.minute || ''} onChange={handleFormChange} placeholder="e.g. 100" />
                    </div>
                    <div className="form-group" style={{ flex: 1, minWidth: '120px' }}>
                        <label className="form-label">Hour</label>
                        <input type="number" name="hour" className="form-input" value={config.hour || ''} onChange={handleFormChange} />
                    </div>
                </div>
            );
        } else if (pluginName === 'cors') {
            return (
                <div style={{ marginTop: '16px' }}>
                    <div className="form-group">
                        <label className="form-label">Origins</label>
                        <input type="text" name="origins" className="form-input" value={Array.isArray(config.origins) ? config.origins.join(', ') : (config.origins || '')} onChange={handleFormChange} placeholder="e.g. http://localhost:3000, *" />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Methods</label>
                        <input type="text" name="methods" className="form-input" value={Array.isArray(config.methods) ? config.methods.join(', ') : (config.methods || '')} onChange={handleFormChange} placeholder="e.g. GET, POST, OPTIONS" />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Headers</label>
                        <input type="text" name="headers" className="form-input" value={Array.isArray(config.headers) ? config.headers.join(', ') : (config.headers || '')} onChange={handleFormChange} placeholder="e.g. Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Auth-Token" />
                    </div>
                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <input type="checkbox" name="credentials" id="credentials" checked={config.credentials || false} onChange={handleFormChange} style={{ width: '18px', height: '18px' }} />
                        <label htmlFor="credentials" style={{ margin: 0, fontWeight: 500 }}>Allow Credentials</label>
                    </div>
                </div>
            );
        } else if (pluginName === 'jwt') {
            return (
                <div style={{ marginTop: '16px' }}>
                    <div className="form-group">
                        <label className="form-label">Cookie Names (Optional)</label>
                        <input type="text" name="cookie_names" className="form-input" value={Array.isArray(config.cookie_names) ? config.cookie_names.join(', ') : (config.cookie_names || '')} onChange={handleFormChange} placeholder="e.g. auth_token" />
                    </div>
                    <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <input type="checkbox" name="secret_is_base64" id="secret_is_base64" checked={config.secret_is_base64 || false} onChange={handleFormChange} style={{ width: '18px', height: '18px' }} />
                        <label htmlFor="secret_is_base64" style={{ margin: 0, fontWeight: 500 }}>Secret is Base64</label>
                    </div>
                </div>
            );
        } else {
            return (
                <div className="form-group" style={{ marginTop: '16px' }}>
                    <label className="form-label">JSON Configuration</label>
                    <textarea 
                        className="form-input" 
                        rows="6" 
                        value={jsonConfig} 
                        onChange={handleJsonChange}
                        style={{ fontFamily: 'monospace' }}
                    />
                    <small style={{ color: 'var(--text-muted)' }}>Provide a valid JSON object for the plugin configuration.</small>
                </div>
            );
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>🔌 Kong Plugins</h2>
                    <p style={{ color: 'var(--text-secondary)' }}>Manage Rate Limiting, JWT Auth, and Security Rules for <strong>{projectCode}</strong></p>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                    <button className="btn btn-secondary" onClick={() => navigate(`/projects/${projectCode}/kong`)}>← Overview</button>
                    <button className="btn btn-primary" style={{ background: '#8b5cf6', borderColor: '#8b5cf6' }} onClick={openCreateModal}>
                        + Add Plugin
                    </button>
                </div>
            </div>

            <div className="toolbar" style={{ marginBottom: '20px' }}>
                <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
                    <span style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-muted)' }}>🔍</span>
                    <input 
                        className="form-input search-input" 
                        placeholder="Search plugins by name..." 
                        value={searchTerm}
                        onChange={e => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            {plugins.filter(p => p.pluginName.toLowerCase().includes(searchTerm.toLowerCase())).length === 0 ? (
                <div className="card animate-fade-in" style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
                    <div style={{ fontSize: '3rem', marginBottom: '16px' }}>🧩</div>
                    <h3 style={{ color: 'var(--text-secondary)' }}>No plugins match your search.</h3>
                    <p>Add a plugin to apply API gateway policies.</p>
                </div>
            ) : (
                <div className="plugin-grid animate-fade-in">
                    {plugins.filter(p => p.pluginName.toLowerCase().includes(searchTerm.toLowerCase())).map(p => {
                        let icon = '🔌';
                        if (p.pluginName === 'rate-limiting') icon = '⏱️';
                        if (p.pluginName === 'jwt') icon = '🔐';
                        if (p.pluginName === 'cors') icon = '🌐';
                        if (p.pluginName === 'ip-restriction') icon = '🛡️';

                        return (
                            <div key={p.id} className="plugin-card" style={{ opacity: p.enabled ? 1 : 0.6 }}>
                                <div className="plugin-card-header">
                                    <div className="plugin-icon">{icon}</div>
                                    <div>
                                        <div className="plugin-title">{p.pluginName} {p.enabled ? '' : '(Disabled)'}</div>
                                        <span style={{
                                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                                            padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600, marginTop: '4px',
                                            background: p.syncStatus === 'SYNCED' ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: p.syncStatus === 'SYNCED' ? '#10b981' : '#ef4444',
                                        }}>
                                            {p.syncStatus === 'SYNCED' ? '✓ SYNCED' : '✗ PENDING'}
                                        </span>
                                    </div>
                                </div>
                                <div className="plugin-meta">
                                    <div><strong>Scope:</strong> {p.scope}</div>
                                    <div><strong>Target ID:</strong> #{p.scopeEntityId}</div>
                                </div>
                                <div className="plugin-actions" style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                                    <button className="btn btn-secondary btn-sm" style={{ flex: 1 }} onClick={() => openEditModal(p)}>
                                        ✏️ Edit
                                    </button>
                                    <button className="btn btn-secondary btn-sm" style={{ flex: 1, color: '#ef4444', borderColor: '#fee2e2', background: '#fef2f2' }} onClick={() => handleDelete(p.id)}>
                                        🗑️ Remove
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {/* Plugin Modal */}
            {showModal && (
                <div className="modal-overlay" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 100 }}>
                    <div className="card animate-slide-down" style={{ width: '100%', maxWidth: '600px', maxHeight: '90vh', overflowY: 'auto', borderTop: '4px solid #8b5cf6' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <div style={{ background: 'rgba(139,92,246,0.15)', color: '#8b5cf6', padding: '8px', borderRadius: '8px' }}>🔌</div>
                                <h3 style={{ margin: 0, fontSize: '1.2rem' }}>{isEditing ? 'Edit Plugin' : 'Add New Plugin'}</h3>
                            </div>
                            <button style={{ background: 'transparent', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: 'var(--text-muted)' }} onClick={() => setShowModal(false)}>×</button>
                        </div>
                        
                        <form onSubmit={handleSubmit}>
                            <div style={{ display: 'flex', gap: '16px' }}>
                                <div className="form-group" style={{ flex: 1 }}>
                                    <label className="form-label">Plugin Type</label>
                                    <select className="form-input" name="pluginName" value={formData.pluginName} onChange={handleFormChange} disabled={isEditing}>
                                        <option value="rate-limiting">Rate Limiting</option>
                                        <option value="jwt">JWT Auth</option>
                                        <option value="cors">CORS</option>
                                        <option value="ip-restriction">IP Restriction</option>
                                    </select>
                                </div>
                                <div className="form-group" style={{ flex: 1 }}>
                                    <label className="form-label">Scope</label>
                                    <select className="form-input" name="scope" value={formData.scope} onChange={handleFormChange} disabled={isEditing}>
                                        <option value="SERVICE">Service Level</option>
                                        <option value="ROUTE">Route Level</option>
                                    </select>
                                </div>
                            </div>
                            
                            <div className="form-group">
                                <label className="form-label">Target Entity <span style={{ color: 'red' }}>*</span></label>
                                <select 
                                    name="scopeEntityId" 
                                    className="form-input" 
                                    value={formData.scopeEntityId} 
                                    onChange={handleFormChange} 
                                    required 
                                    disabled={isEditing}
                                >
                                    <option value="" disabled>Select {formData.scope === 'SERVICE' ? 'Service' : 'Route'}</option>
                                    {formData.scope === 'SERVICE' && servicesList.map(s => (
                                        <option key={s.id} value={s.id}>
                                            {s.serviceName || s.name || `Service #${s.id}`}
                                        </option>
                                    ))}
                                    {formData.scope === 'ROUTE' && allRoutes.map(r => {
                                        const routeLabel = r.name || (r.paths && r.paths.length > 0 ? r.paths[0] : `Route #${r.id}`);
                                        return (
                                            <option key={r.id} value={r.id}>
                                                {routeLabel} (Service: {r.serviceName})
                                            </option>
                                        );
                                    })}
                                </select>
                            </div>

                            <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: '16px 0' }}>
                                <input type="checkbox" name="enabled" id="plugin_enabled" checked={formData.enabled} onChange={handleFormChange} style={{ width: '18px', height: '18px' }} />
                                <label htmlFor="plugin_enabled" style={{ margin: 0, fontWeight: 500 }}>Enable Plugin</label>
                            </div>

                            <hr style={{ margin: '24px 0', borderColor: 'var(--border-color)', borderStyle: 'solid' }} />
                            
                            <h4 style={{ margin: '0 0 16px 0' }}>Configuration</h4>
                            {renderDynamicForm()}

                            <div style={{ marginTop: '32px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary" style={{ background: '#8b5cf6', borderColor: '#8b5cf6' }}>
                                    {isEditing ? 'Save Changes' : 'Create Plugin'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
