import { useState, useEffect } from 'react';
import storageService from '../../services/storageService';
import { fileExplorerService } from '../../services/fileExplorerService';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { useToast } from '../../components/common/Toast';

export default function ProjectStoragePolicies({ projectCode, initialPrefix = '' }) {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [rules, setRules] = useState([]);
    const [availableFolders, setAvailableFolders] = useState([]);
    const toast = useToast();

    useEffect(() => {
        loadData();
    }, [projectCode]);

    async function loadData() {
        try {
            const [policiesRes, foldersRes] = await Promise.all([
                storageService.getPolicies(projectCode),
                fileExplorerService.listAllFolders(projectCode).catch(() => []) // Gracefully fallback if folders fail
            ]);
            
            let loadedRules = policiesRes.data || [];
            if (loadedRules.length === 0 && initialPrefix) {
                // Pre-fill with the initial prefix if it's empty
                loadedRules = [{ prefix: initialPrefix + '*', canRead: true, canWrite: false, canDelete: false, ipAddresses: [] }];
            }
            setRules(loadedRules);
            setAvailableFolders(foldersRes || []);
        } catch (err) {
            toast.error('Failed to load policies');
        } finally {
            setLoading(false);
        }
    }

    const handleAddRule = () => {
        setRules([...rules, { prefix: '', canRead: true, canWrite: false, canDelete: false, ipAddresses: [] }]);
    };

    const handleRemoveRule = (index) => {
        const newRules = [...rules];
        newRules.splice(index, 1);
        setRules(newRules);
    };

    const handleRuleChange = (index, field, value) => {
        const newRules = [...rules];
        newRules[index][field] = value;
        setRules(newRules);
    };

    const handleIpChange = (index, ipString) => {
        const newRules = [...rules];
        newRules[index].ipAddresses = ipString.split(',').map(ip => ip.trim()).filter(ip => ip.length > 0);
        setRules(newRules);
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await storageService.updatePolicies(projectCode, rules);
            toast.success('Policies saved successfully');
            await loadData();
        } catch (err) {
            toast.error(err.message || 'Failed to save policies');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div style={{ marginTop: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h4 style={{ margin: 0 }}>Anonymous Access Rules</h4>
                <button className="btn btn-secondary" onClick={handleAddRule} style={{ padding: '6px 12px', fontSize: '0.85rem' }}>
                    + Add Rule
                </button>
            </div>
            
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '20px' }}>
                Configure advanced access control for unauthenticated public requests to your bucket.
            </p>

            {rules.length === 0 ? (
                <div className="empty-state" style={{ padding: '30px', textAlign: 'center', borderRadius: '8px', background: 'rgba(255,255,255,0.02)', border: '1px dashed rgba(255,255,255,0.1)' }}>
                    <div style={{ fontSize: '2rem', marginBottom: '10px' }}>🔒</div>
                    <h5 style={{ margin: '0 0 10px 0' }}>Completely Private</h5>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>No public access rules are defined. All anonymous requests will be denied.</p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '20px' }}>
                    {rules.map((rule, index) => (
                        <div key={index} style={{ 
                            background: 'rgba(0,0,0,0.2)', 
                            border: '1px solid rgba(255,255,255,0.1)', 
                            borderRadius: '8px', 
                            padding: '16px',
                            display: 'grid',
                            gap: '16px',
                            position: 'relative'
                        }}>
                            <button 
                                onClick={() => handleRemoveRule(index)}
                                style={{ position: 'absolute', top: '10px', right: '10px', background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontSize: '1rem', padding: '4px' }}
                                title="Remove Rule"
                            >✕</button>
                            
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
                                <div className="form-group" style={{ margin: 0 }}>
                                    <label className="form-label" style={{ fontSize: '0.8rem' }}>Prefix / Folder Path</label>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <select 
                                            className="form-input" 
                                            style={{ flex: 1 }}
                                            value={availableFolders.some(f => (f + '*') === rule.prefix) || rule.prefix === '*' ? rule.prefix : 'CUSTOM'}
                                            onChange={(e) => {
                                                if (e.target.value !== 'CUSTOM') {
                                                    handleRuleChange(index, 'prefix', e.target.value);
                                                }
                                            }}
                                        >
                                            <option value="*">Entire Bucket (*)</option>
                                            {availableFolders.map(folder => (
                                                <option key={folder} value={folder + '*'}>{folder}*</option>
                                            ))}
                                            {!availableFolders.some(f => (f + '*') === rule.prefix) && rule.prefix !== '*' && (
                                                <option value="CUSTOM">Custom ({rule.prefix})</option>
                                            )}
                                            <option value="CUSTOM">-- Type custom prefix --</option>
                                        </select>
                                        
                                        {(!availableFolders.some(f => (f + '*') === rule.prefix) && rule.prefix !== '*') && (
                                            <input 
                                                type="text" 
                                                className="form-input" 
                                                style={{ flex: 1 }}
                                                value={rule.prefix} 
                                                onChange={(e) => handleRuleChange(index, 'prefix', e.target.value)}
                                                placeholder="e.g. images/*" 
                                            />
                                        )}
                                    </div>
                                </div>
                                
                                <div className="form-group" style={{ margin: 0 }}>
                                    <label className="form-label" style={{ fontSize: '0.8rem' }}>IP Whitelist (Comma separated)</label>
                                    <input 
                                        type="text" 
                                        className="form-input" 
                                        value={rule.ipAddresses ? rule.ipAddresses.join(', ') : ''} 
                                        onChange={(e) => handleIpChange(index, e.target.value)}
                                        placeholder="e.g. 192.168.1.0/24 (leave empty for any IP)" 
                                    />
                                </div>
                            </div>
                            
                            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap', padding: '8px 16px', background: 'rgba(255,255,255,0.05)', borderRadius: '6px' }}>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
                                    <input 
                                        type="checkbox" 
                                        checked={rule.canRead} 
                                        onChange={(e) => handleRuleChange(index, 'canRead', e.target.checked)}
                                    />
                                    Read (GetObject)
                                </label>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
                                    <input 
                                        type="checkbox" 
                                        checked={rule.canWrite} 
                                        onChange={(e) => handleRuleChange(index, 'canWrite', e.target.checked)}
                                    />
                                    Write (PutObject)
                                </label>
                                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.85rem' }}>
                                    <input 
                                        type="checkbox" 
                                        checked={rule.canDelete} 
                                        onChange={(e) => handleRuleChange(index, 'canDelete', e.target.checked)}
                                    />
                                    Delete (DeleteObject)
                                </label>
                            </div>
                        </div>
                    ))}
                </div>
            )}
            
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px', paddingTop: '16px', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
                <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                    {saving ? 'Saving...' : 'Save Policies'}
                </button>
            </div>
        </div>
    );
}
