import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';

const WS_URL = 'ws://localhost:8080/ws/logs';

export default function LiveLogs() {
    const { projectCode } = useParams();
    const [logs, setLogs] = useState([]);
    const [connected, setConnected] = useState(false);
    const [paused, setPaused] = useState(false);
    const [filter, setFilter] = useState('');
    const wsRef = useRef(null);
    const logsEndRef = useRef(null);
    const maxLogs = 500;

    useEffect(() => {
        if (!projectCode) return;
        connectWebSocket();
        return () => {
            if (wsRef.current) {
                wsRef.current.close();
            }
        };
    }, [projectCode]);

    useEffect(() => {
        if (!paused && logsEndRef.current) {
            logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [logs, paused]);

    function connectWebSocket() {
        // Using native WebSocket with STOMP-like frame parsing
        const socket = new WebSocket(WS_URL);
        wsRef.current = socket;

        socket.onopen = () => {
            setConnected(true);
            // Send STOMP CONNECT frame
            socket.send('CONNECT\naccept-version:1.2\n\n\0');
        };

        socket.onmessage = (event) => {
            const data = event.data;
            
            // Parse STOMP frames
            if (data.startsWith('CONNECTED')) {
                // Subscribe to project-specific topic
                const subFrame = `SUBSCRIBE\nid:sub-0\ndestination:/topic/logs.${projectCode}\n\n\0`;
                socket.send(subFrame);
                return;
            }

            if (data.startsWith('MESSAGE')) {
                // Extract body from STOMP MESSAGE frame
                const bodyStart = data.indexOf('\n\n');
                if (bodyStart >= 0) {
                    const body = data.substring(bodyStart + 2).replace(/\0$/, '');
                    try {
                        const entry = JSON.parse(body);
                        setLogs(prev => {
                            const updated = [...prev, { ...entry, id: Date.now() + Math.random() }];
                            return updated.length > maxLogs ? updated.slice(-maxLogs) : updated;
                        });
                    } catch (e) {
                        // Ignore parse errors
                    }
                }
            }
        };

        socket.onclose = () => {
            setConnected(false);
            // Auto-reconnect after 3s
            setTimeout(() => {
                if (!wsRef.current || wsRef.current.readyState === WebSocket.CLOSED) {
                    connectWebSocket();
                }
            }, 3000);
        };

        socket.onerror = () => {
            setConnected(false);
        };
    }

    function getStatusColor(status) {
        if (status >= 500) return '#ef4444';
        if (status >= 400) return '#f59e0b';
        if (status >= 300) return '#3b82f6';
        if (status >= 200) return '#22c55e';
        return 'var(--text-secondary)';
    }

    function getMethodColor(method) {
        const colors = {
            GET: '#22c55e', POST: '#3b82f6', PUT: '#f59e0b',
            PATCH: '#a855f7', DELETE: '#ef4444', HEAD: '#6b7280',
        };
        return colors[method] || 'var(--text-secondary)';
    }

    const filteredLogs = filter
        ? logs.filter(l =>
            l.uri?.toLowerCase().includes(filter.toLowerCase()) ||
            l.method?.toLowerCase().includes(filter.toLowerCase()) ||
            String(l.statusCode).includes(filter)
        )
        : logs;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>📡 Live API Logs</h2>
                    <p>
                        Real-time gateway traffic for <strong>{projectCode}</strong>
                        <span style={{
                            display: 'inline-block',
                            width: 8, height: 8, borderRadius: '50%',
                            background: connected ? '#22c55e' : '#ef4444',
                            marginLeft: 8, verticalAlign: 'middle',
                            boxShadow: connected ? '0 0 6px #22c55e' : '0 0 6px #ef4444',
                        }} />
                        <span style={{ marginLeft: 6, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            {connected ? 'Connected' : 'Disconnected'}
                        </span>
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className={`btn ${paused ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setPaused(!paused)}>
                        {paused ? '▶ Resume' : '⏸ Pause'}
                    </button>
                    <button className="btn btn-secondary" onClick={() => setLogs([])}>🗑️ Clear</button>
                </div>
            </div>

            {/* Filter bar */}
            <div style={{ marginBottom: 16 }}>
                <input
                    className="form-input"
                    placeholder="Filter by URI, method, or status code..."
                    value={filter}
                    onChange={e => setFilter(e.target.value)}
                    style={{ maxWidth: 400 }}
                />
                <span style={{ marginLeft: 12, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                    {filteredLogs.length} entries {logs.length >= maxLogs ? `(capped at ${maxLogs})` : ''}
                </span>
            </div>

            {/* Log stream */}
            <div className="card" style={{
                padding: 0, fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
                fontSize: '0.8rem', maxHeight: '65vh', overflowY: 'auto',
                background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.06)',
            }}>
                {filteredLogs.length === 0 ? (
                    <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-muted)' }}>
                        <div style={{ fontSize: '2rem', marginBottom: 8 }}>📭</div>
                        <p>Waiting for API traffic...</p>
                        <p style={{ fontSize: '0.75rem' }}>
                            Logs will appear here when requests hit Kong Gateway routes for <strong>{projectCode}</strong>
                        </p>
                    </div>
                ) : (
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead>
                            <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', position: 'sticky', top: 0, background: 'var(--bg-card)', zIndex: 1 }}>
                                {['Time', 'Method', 'URI', 'Status', 'Total', 'Kong', 'Upstream', 'Client IP'].map(h => (
                                    <th key={h} style={{
                                        padding: '10px 12px', textAlign: 'left', fontSize: '0.72rem',
                                        color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px',
                                    }}>{h}</th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {filteredLogs.map((log) => (
                                <tr key={log.id} style={{
                                    borderBottom: '1px solid rgba(255,255,255,0.03)',
                                    animation: 'fadeIn 0.3s ease',
                                }}>
                                    <td style={{ padding: '6px 12px', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                                        {log.timestamp ? new Date(Number(log.timestamp)).toLocaleTimeString() : '--'}
                                    </td>
                                    <td style={{ padding: '6px 12px' }}>
                                        <span style={{
                                            color: getMethodColor(log.method), fontWeight: 600,
                                            padding: '2px 6px', background: `${getMethodColor(log.method)}15`,
                                            borderRadius: 4,
                                        }}>{log.method}</span>
                                    </td>
                                    <td style={{ padding: '6px 12px', color: 'var(--text-primary)', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                        {log.uri}
                                    </td>
                                    <td style={{ padding: '6px 12px' }}>
                                        <span style={{
                                            color: getStatusColor(log.statusCode), fontWeight: 700,
                                        }}>{log.statusCode}</span>
                                    </td>
                                    <td style={{ padding: '6px 12px', color: 'var(--text-secondary)' }}>{log.latency}ms</td>
                                    <td style={{ padding: '6px 12px', color: '#a78bfa' }}>{log.kongLatency}ms</td>
                                    <td style={{ padding: '6px 12px', color: '#f59e0b' }}>{log.upstreamLatency}ms</td>
                                    <td style={{ padding: '6px 12px', color: 'var(--text-muted)' }}>{log.clientIp}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
                <div ref={logsEndRef} />
            </div>
        </div>
    );
}
