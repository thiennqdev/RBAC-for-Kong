import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import observabilityService from '../../services/observabilityService';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const METHOD_OPTIONS = ['', 'GET', 'POST', 'PUT', 'PATCH', 'DELETE'];
const STATUS_OPTIONS = ['', 200, 201, 400, 401, 403, 404, 500, 502, 503];

export default function GatewayLogPage() {
    const { projectCode } = useParams();
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [totalHits, setTotalHits] = useState(0);
    const [page, setPage] = useState(0);

    // Filters
    const [apiPath, setApiPath] = useState('');
    const [method, setMethod] = useState('');
    const [statusCode, setStatusCode] = useState('');
    const [clientIp, setClientIp] = useState('');
    const [fromDate, setFromDate] = useState('');
    const [toDate, setToDate] = useState('');

    const loadLogs = useCallback(async () => {
        setLoading(true);
        try {
            const payload = {
                page,
                size: 30,
                sortField: ['createdAt'],
                sortOrder: [-1]
            };

            // Scope logs to this project context
            // Prepend projectCode to apiPath filter if not already included
            let searchPath = apiPath;
            if (!searchPath) searchPath = `/${projectCode}`;
            else if (!searchPath.startsWith(`/${projectCode}`)) searchPath = `/${projectCode}${searchPath}`;

            payload.uri = searchPath;

            if (method) payload.method = method;
            if (statusCode) payload.statusCode = parseInt(statusCode);
            if (clientIp) payload.clientIp = clientIp;
            if (fromDate) payload.fromDate = fromDate;
            if (toDate) payload.toDate = toDate;

            const res = await observabilityService.searchGatewayLogs(projectCode, payload);
            setLogs(res.data?.data || []);
            setTotalHits(res.data?.totalElements || 0);
        } catch (err) {
            console.error('Failed to load gateway logs:', err);
            setLogs([]);
        } finally {
            setLoading(false);
        }
    }, [page, apiPath, method, statusCode, clientIp, projectCode]);

    useEffect(() => { loadLogs(); }, [loadLogs]);

    function handleSearch(e) {
        e.preventDefault();
        setPage(0);
        loadLogs();
    }

    function handleReset() {
        setApiPath('');
        setMethod('');
        setStatusCode('');
        setClientIp('');
        setFromDate('');
        setToDate('');
        setPage(0);
    }

    const httpStatusCodes = {
        200: 'OK', 201: 'CREATED', 202: 'ACCEPTED', 204: 'NO CONTENT',
        301: 'MOVED PERMANENTLY', 302: 'FOUND', 304: 'NOT MODIFIED',
        400: 'BAD REQUEST', 401: 'UNAUTHORIZED', 403: 'FORBIDDEN', 404: 'NOT FOUND', 405: 'METHOD NOT ALLOWED', 409: 'CONFLICT', 429: 'TOO MANY REQUESTS',
        500: 'INTERNAL SERVER ERROR', 502: 'BAD GATEWAY', 503: 'SERVICE UNAVAILABLE', 504: 'GATEWAY TIMEOUT'
    };

    function getStatusBadge(status) {
        if (!status) return '-';

        let bgColor = '#6b7280'; // default gray
        if (status >= 500) bgColor = '#ef4444'; // Red for 5xx
        else if (status >= 400) bgColor = '#ff8000ff'; // Dark yellow/orange for 4xx
        else if (status >= 200 && status < 300) bgColor = '#22c55e'; // Green for 2xx
        else if (status >= 300 && status < 400) bgColor = '#3b82f6'; // Blue for 3xx

        const statusText = httpStatusCodes[status] || '';
        const displayText = statusText ? `${status} ${statusText}` : status;

        return (
            <span style={{
                display: 'inline-flex', alignItems: 'center',
                padding: '4px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 700,
                background: bgColor, color: '#ffffff'
            }}>
                {displayText}
            </span>
        );
    }

    function formatTimestamp(ts) {
        if (!ts) return '-';
        const d = new Date(ts);
        return d.getFullYear() + '-' +
               String(d.getMonth() + 1).padStart(2, '0') + '-' +
               String(d.getDate()).padStart(2, '0') + ' ' +
               String(d.getHours()).padStart(2, '0') + ':' +
               String(d.getMinutes()).padStart(2, '0') + ':' +
               String(d.getSeconds()).padStart(2, '0');
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>🌐 Gateway Access Logs</h2>
                    <p>Real-time request logs flowing through Kong Gateway (powered by Elasticsearch)</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <span className="badge badge-active" style={{ fontSize: '13px' }}>
                        Total: {totalHits.toLocaleString()} hits
                    </span>
                    <button className="btn btn-primary" onClick={loadLogs}>🔄 Refresh</button>
                </div>
            </div>

            {/* Filter Bar */}
            <div className="card" style={{ marginBottom: '20px' }}>
                <form onSubmit={handleSearch} style={{ display: 'flex', gap: '12px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>API Path</label>
                        <input className="form-input" type="text" placeholder="/api/v1/..."
                            value={apiPath} onChange={e => setApiPath(e.target.value)} style={{ minWidth: '200px' }} />
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Method</label>
                        <select className="form-input" value={method} onChange={e => setMethod(e.target.value)}
                            style={{ minWidth: '100px' }}>
                            {METHOD_OPTIONS.map(o => <option key={o} value={o}>{o || 'All'}</option>)}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Status Code</label>
                        <select className="form-input" value={statusCode} onChange={e => setStatusCode(e.target.value)}
                            style={{ minWidth: '100px' }}>
                            {STATUS_OPTIONS.map(o => <option key={o} value={o}>{o || 'All'}</option>)}
                        </select>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1, minWidth: '140px' }}>
                        <label style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Client IP</label>
                        <input className="form-input" type="text" placeholder="e.g. 192.168.1.1"
                            value={clientIp} onChange={e => setClientIp(e.target.value)} style={{ width: '100%' }} />
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
                {loading ? <LoadingSpinner text="Querying Elasticsearch..." /> : logs.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">🌐</div>
                        <h3>No gateway logs found</h3>
                        <p>Send some requests through Kong Gateway (port 8000) to see logs here</p>
                    </div>
                ) : (
                    <div style={{ overflowX: 'auto' }}>
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>Method</th>
                                    <th>Path</th>
                                    <th>Status</th>
                                    <th>Client IP</th>
                                    <th>Latency</th>
                                    <th>Service</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map((log, idx) => (
                                    <tr key={idx}>
                                        <td style={{ fontSize: '12px', whiteSpace: 'nowrap' }}>
                                            {formatTimestamp(log.createdAt || Number(log.timestamp))}
                                        </td>
                                        <td>
                                            <span style={{
                                                display: 'inline-block',
                                                padding: '4px 8px', borderRadius: '4px',
                                                background: log.method === 'GET' ? '#dbeafe' :
                                                    log.method === 'POST' ? '#dcfce7' :
                                                    log.method === 'DELETE' ? '#fee2e2' : '#fef3c7',
                                                color: log.method === 'GET' ? '#1e40af' :
                                                    log.method === 'POST' ? '#166534' :
                                                    log.method === 'DELETE' ? '#991b1b' : '#92400e',
                                                fontSize: '11px', fontWeight: 700, minWidth: '50px', textAlign: 'center'
                                            }}>
                                                {log.method || '-'}
                                            </span>
                                        </td>
                                        <td style={{ fontFamily: 'monospace', fontSize: '12px', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                            {log.uri || '-'}
                                        </td>
                                        <td>{getStatusBadge(log.statusCode)}</td>
                                        <td style={{ fontSize: '12px' }}>{log.clientIp || '-'}</td>
                                        <td style={{ fontSize: '12px', fontFamily: 'monospace' }}>
                                            {log.latency != null ? `${log.latency}ms` : '-'}
                                        </td>
                                        <td style={{ fontSize: '12px' }}>{log.serviceName || '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                {logs.length > 0 && (
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '16px' }}>
                        <button className="btn btn-secondary" disabled={page === 0}
                            onClick={() => setPage(p => p - 1)}>← Previous</button>
                        <span style={{ padding: '8px 16px', color: 'var(--text-muted)' }}>
                            Page {page + 1} • Showing {logs.length} of {totalHits.toLocaleString()}
                        </span>
                        <button className="btn btn-secondary" disabled={logs.length < 30}
                            onClick={() => setPage(p => p + 1)}>Next →</button>
                    </div>
                )}
            </div>
        </div>
    );
}
