import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import observabilityService from '../../services/observabilityService';

const GRAFANA_URL = 'http://localhost:3000';

export default function MetricsDashboard() {
    const { projectCode } = useParams();
    const [stats, setStats] = useState({
        totalRequests: 0,
        errorRate: '0%',
        avgLatency: '0ms',
        activeServices: 0
    });
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('overview');

    const tabs = [
        { id: 'overview', label: '📊 Overview', icon: '📊' },
        { id: 'latency', label: '⏱️ Latency', icon: '⏱️' },
        { id: 'errors', label: '⚠️ Errors', icon: '⚠️' },
    ];

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>📈 API Observability & Monitoring</h2>
                    <p>Golden Signals: Traffic, Latency, Error Rate</p>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <a
                        href={GRAFANA_URL}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-primary"
                    >
                        🔗 Open Grafana
                    </a>
                </div>
            </div>

            {/* Golden Signals Overview Cards */}
            <div style={{
                display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: 16, marginBottom: 24,
            }}>
                <SignalCard
                    title="Traffic (RPS)"
                    description="Requests per second through Kong Gateway"
                    icon="🌐"
                    color="#3b82f6"
                    metric="kong_http_requests_total"
                />
                <SignalCard
                    title="Error Rate"
                    description="Percentage of 4xx/5xx responses"
                    icon="⚠️"
                    color="#ef4444"
                    metric="rate(kong_http_requests_total{code=~'5..'})"
                />
                <SignalCard
                    title="AVG Latency"
                    description="Average request processing time"
                    icon="⏱️"
                    color="#f59e0b"
                    metric="kong_request_latency_ms"
                />
                <SignalCard
                    title="Upstream Latency"
                    description="Backend service processing time"
                    icon="🔧"
                    color="#a855f7"
                    metric="kong_upstream_latency_ms"
                />
            </div>

            {/* Tab Navigation */}
            <div style={{ display: 'flex', gap: 4, marginBottom: 16, borderBottom: '1px solid rgba(255,255,255,0.08)', paddingBottom: 2 }}>
                {tabs.map(tab => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`btn ${activeTab === tab.id ? 'btn-primary' : 'btn-secondary'}`}
                        style={{ fontSize: '0.85rem', borderRadius: '8px 8px 0 0' }}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Grafana Embedded Dashboard */}
            <div className="card" style={{ padding: 0, overflow: 'hidden', minHeight: 500 }}>
                <iframe
                    src={`${GRAFANA_URL}/d/kong-official/kong-official?orgId=1&refresh=10s&theme=dark&kiosk`}
                    width="100%"
                    height="600"
                    style={{ border: 'none', borderRadius: 8 }}
                    title="Grafana Kong Dashboard"
                />
            </div>

            {/* Explanation Section */}
            <div className="card" style={{ marginTop: 16 }}>
                <h3 style={{ marginBottom: 12, color: 'var(--text-primary)' }}>📖 Understanding Latency Metrics</h3>
                <div style={{
                    display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                    gap: 16,
                }}>
                    <div style={{ padding: 16, background: 'rgba(168,85,247,0.08)', borderRadius: 8, borderLeft: '3px solid #a855f7' }}>
                        <strong style={{ color: '#a855f7' }}>Kong Latency</strong>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                            Time Kong spends processing the request (routing, plugin execution, etc).
                            <br /><em>High value → Gateway is overloaded or plugins are misconfigured.</em>
                        </p>
                    </div>
                    <div style={{ padding: 16, background: 'rgba(245,158,11,0.08)', borderRadius: 8, borderLeft: '3px solid #f59e0b' }}>
                        <strong style={{ color: '#f59e0b' }}>Upstream Latency</strong>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                            Time the backend service takes to process and respond.
                            <br /><em>High value → Backend code is slow or database queries are heavy.</em>
                        </p>
                    </div>
                    <div style={{ padding: 16, background: 'rgba(59,130,246,0.08)', borderRadius: 8, borderLeft: '3px solid #3b82f6' }}>
                        <strong style={{ color: '#3b82f6' }}>Total Latency</strong>
                        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                            End-to-end time from client request to response.
                            <br /><em>Total = Kong Latency + Upstream Latency + Network.</em>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}

function SignalCard({ title, description, icon, color, metric }) {
    return (
        <div className="card" style={{
            padding: '20px 24px',
            borderLeft: `3px solid ${color}`,
            cursor: 'default',
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 4 }}>
                        {title}
                    </div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                        —
                    </div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 4 }}>
                        {description}
                    </div>
                </div>
                <div style={{ fontSize: '1.8rem', opacity: 0.6 }}>{icon}</div>
            </div>
            <div style={{ marginTop: 8, fontSize: '0.68rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                {metric}
            </div>
        </div>
    );
}
