import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import observabilityService from '../../services/observabilityService';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const ACTION_OPTIONS = ['', 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT'];
const ENTITY_OPTIONS = ['', 'PROJECT', 'USER', 'ROLE', 'SERVICE', 'ROUTE', 'PLUGIN', 'UPSTREAM', 'CONSUMER', 'TARGET'];

export default function AuditLogPage() {
    const { projectCode } = useParams();
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [expandedId, setExpandedId] = useState(null);

    // Filters
    const [action, setAction] = useState('');
    const [entityName, setEntityName] = useState('');
    const [actor, setActor] = useState('');
    const [fromDate, setFromDate] = useState('');
    const [toDate, setToDate] = useState('');

    const loadLogs = useCallback(async () => {
        setLoading(true);
        try {
            const params = { 
                page, 
                size: 15,
                sortField: ['createdAt'],
                sortOrder: [-1]
            };
            if (action) params.action = action;
            if (entityName) params.entityName = entityName;
            if (actor) params.actor = actor;
            if (fromDate) params.fromDate = fromDate;
            if (toDate) params.toDate = toDate;

            // Pass projectCode as first argument
            const res = await observabilityService.getAuditLogs(projectCode, params);
            setLogs(res.data.data || []);
            setTotalPages(res.data.totalPages || 0);
        } catch (err) {
            console.error('Failed to load audit logs:', err);
            setLogs([]);
        } finally {
            setLoading(false);
        }
    }, [page, action, entityName, actor, projectCode]);

    useEffect(() => { loadLogs(); }, [loadLogs]);

    function handleSearch(e) {
        e.preventDefault();
        setPage(0);
        loadLogs();
    }

    function handleReset() {
        setAction('');
        setEntityName('');
        setActor('');
        setFromDate('');
        setToDate('');
        setPage(0);
    }

    function getActionBadge(act) {
        const styles = { 
            CREATE: { bg: '#dcfce7', text: '#166534' },
            UPDATE: { bg: '#dbeafe', text: '#1e40af' },
            DELETE: { bg: '#fee2e2', text: '#991b1b' },
            LOGIN: { bg: '#f3f4f6', text: '#374151' },
            LOGOUT: { bg: '#f3f4f6', text: '#374151' }
        };
        const s = styles[act] || { bg: '#f3f4f6', text: '#374151' };
        return <span style={{
            display: 'inline-block',
            padding: '4px 8px', borderRadius: '4px',
            background: s.bg, color: s.text, fontSize: '11px', fontWeight: 700
        }}>{act}</span>;
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.getFullYear() + '-' +
               String(d.getMonth() + 1).padStart(2, '0') + '-' +
               String(d.getDate()).padStart(2, '0') + ' ' +
               String(d.getHours()).padStart(2, '0') + ':' +
               String(d.getMinutes()).padStart(2, '0') + ':' +
               String(d.getSeconds()).padStart(2, '0');
    }

    function formatJson(jsonStr) {
        if (!jsonStr) return <em style={{ color: 'var(--text-muted)' }}>null</em>;
        try {
            return <pre style={{
                background: 'var(--bg-secondary)', padding: '12px', borderRadius: '8px',
                overflow: 'auto', maxHeight: '300px', fontSize: '12px', margin: '4px 0'
            }}>{JSON.stringify(JSON.parse(jsonStr), null, 2)}</pre>;
        } catch {
            return <pre>{jsonStr}</pre>;
        }
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>📝 Audit Logs</h2>
                    <p>Track all configuration changes made to Kong Gateway resources</p>
                </div>
            </div>

            {/* Filter Bar */}
            <div className="card" style={{ marginBottom: '20px' }}>
                <form onSubmit={handleSearch} style={{ display: 'flex', gap: '12px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Action</label>
                        <select className="form-input" value={action} onChange={e => setAction(e.target.value)}
                            style={{ minWidth: '120px' }}>
                            {ACTION_OPTIONS.map(o => <option key={o} value={o}>{o || 'All'}</option>)}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Resource Type</label>
                        <select className="form-input" value={entityName} onChange={e => setEntityName(e.target.value)}
                            style={{ minWidth: '140px' }}>
                            {ENTITY_OPTIONS.map(o => <option key={o} value={o}>{o || 'All'}</option>)}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1, minWidth: '160px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Actor</label>
                        <input className="form-input" type="text" placeholder="Username..."
                            value={actor} onChange={e => setActor(e.target.value)} style={{ width: '100%' }} />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>From</label>
                        <input className="form-input" type="datetime-local" value={fromDate} onChange={e => setFromDate(e.target.value)} />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>To</label>
                        <input className="form-input" type="datetime-local" value={toDate} onChange={e => setToDate(e.target.value)} />
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                        <button className="btn btn-primary" type="submit" style={{ background: '#3b82f6', border: 'none' }}>🔍 Search</button>
                        <button className="btn btn-secondary" type="button" onClick={handleReset}>Clear</button>
                    </div>
                </form>
            </div>

            {/* Data Table */}
            <div className="card">
                {loading ? <LoadingSpinner text="Loading audit logs..." /> : logs.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">📝</div>
                        <h3>No audit logs found</h3>
                        <p>Configuration changes will appear here automatically</p>
                    </div>
                ) : (
                    <>
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Time</th>
                                    <th>Actor</th>
                                    <th>Action</th>
                                    <th>Resource</th>
                                    <th>Entity ID</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map(log => (
                                    <>
                                        <tr key={log.id} onClick={() => setExpandedId(expandedId === log.id ? null : log.id)}
                                            style={{ cursor: 'pointer' }}>
                                            <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>{formatDate(log.createdAt)}</td>
                                            <td style={{ fontWeight: 500 }}>{log.actor}</td>
                                            <td>{getActionBadge(log.action)}</td>
                                            <td>
                                                <span style={{ 
                                                    fontFamily: 'monospace', padding: '2px 6px', 
                                                    background: 'var(--bg-secondary)', border: '1px solid var(--border-color)', 
                                                    borderRadius: '4px', fontSize: '12px' 
                                                }}>{log.entityName}</span>
                                            </td>
                                            <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>{log.entityId?.substring(0, 12)}...</td>
                                            <td style={{ color: 'var(--primary)', fontWeight: 500 }}>{expandedId === log.id ? '▼ Hide details' : '👁️ View details'}</td>
                                        </tr>
                                        {expandedId === log.id && (
                                            <tr key={`${log.id}-detail`}>
                                                <td colSpan="6" style={{ padding: '16px' }}>
                                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                                        <div>
                                                            <strong style={{ color: 'var(--error)' }}>Before (Old Value):</strong>
                                                            {formatJson(log.oldValue)}
                                                        </div>
                                                        <div>
                                                            <strong style={{ color: 'var(--success)' }}>After (New Value):</strong>
                                                            {formatJson(log.newValue)}
                                                        </div>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </>
                                ))}
                            </tbody>
                        </table>

                        {/* Pagination */}
                        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '16px' }}>
                            <button className="btn btn-secondary" disabled={page === 0}
                                onClick={() => setPage(p => p - 1)}>← Previous</button>
                            <span style={{ padding: '8px 16px', color: 'var(--text-muted)' }}>
                                Page {page + 1} of {totalPages || 1}
                            </span>
                            <button className="btn btn-secondary" disabled={page >= totalPages - 1}
                                onClick={() => setPage(p => p + 1)}>Next →</button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
