import { useState, useEffect } from 'react';
import observabilityService from '../../services/observabilityService';

function SignalCard({ title, description, icon, color, value }) {
    return (
        <div className="card" style={{
            padding: '20px 24px',
            borderLeft: `3px solid ${color}`,
            cursor: 'default',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
        }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 4 }}>
                        {title}
                    </div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                        {value}
                    </div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 4 }}>
                        {description}
                    </div>
                </div>
                <div style={{ fontSize: '1.8rem', opacity: 0.6 }}>{icon}</div>
            </div>
        </div>
    );
}

export default function LiveSignalCards({ projectCode }) {
    const [metrics, setMetrics] = useState({
        rps: '—',
        errorRate: '—',
        avgLatency: '—',
        upstreamLatency: '—',
    });

    useEffect(() => {
        let mounted = true;

        const loadMetrics = async () => {
            try {
                // Fetch RPS
                const rpsRes = await observabilityService.queryPrometheus(projectCode, `sum(rate(kong_http_requests_total{service=~"${projectCode}\\\\..*"}[1m]))`);
                const rpsValue = rpsRes.data?.data?.result?.[0]?.value?.[1];

                // Fetch Error Rate (5xx)
                const errRes = await observabilityService.queryPrometheus(projectCode, `sum(rate(kong_http_requests_total{code=~"5..", service=~"${projectCode}\\\\..*"}[1m])) / sum(rate(kong_http_requests_total{service=~"${projectCode}\\\\..*"}[1m])) * 100`);
                const errValue = errRes.data?.data?.result?.[0]?.value?.[1];

                // Fetch Avg Latency (kong)
                const latRes = await observabilityService.queryPrometheus(projectCode, `sum(rate(kong_request_latency_ms_sum{service=~"${projectCode}\\\\..*"}[1m])) / sum(rate(kong_request_latency_ms_count{service=~"${projectCode}\\\\..*"}[1m]))`);
                const latValue = latRes.data?.data?.result?.[0]?.value?.[1];

                // Fetch Upstream Latency
                const upLatRes = await observabilityService.queryPrometheus(projectCode, `sum(rate(kong_upstream_latency_ms_sum{service=~"${projectCode}\\\\..*"}[1m])) / sum(rate(kong_upstream_latency_ms_count{service=~"${projectCode}\\\\..*"}[1m]))`);
                const upLatValue = upLatRes.data?.data?.result?.[0]?.value?.[1];

                if (mounted) {
                    setMetrics({
                        rps: rpsValue ? parseFloat(rpsValue).toFixed(2) : '0.00',
                        errorRate: errValue && !isNaN(errValue) ? `${parseFloat(errValue).toFixed(2)}%` : '0.00%',
                        avgLatency: latValue && !isNaN(latValue) ? `${parseFloat(latValue).toFixed(0)} ms` : '0 ms',
                        upstreamLatency: upLatValue && !isNaN(upLatValue) ? `${parseFloat(upLatValue).toFixed(0)} ms` : '0 ms',
                    });
                }
            } catch (error) {
                console.error("Failed to fetch live metrics", error);
                if (mounted) {
                    setMetrics({
                        rps: 'ERR',
                        errorRate: error.message || 'Error',
                        avgLatency: 'ERR',
                        upstreamLatency: 'ERR',
                    });
                }
            }
        };

        loadMetrics();
        const intervalId = setInterval(loadMetrics, 5000); // refresh every 5s

        return () => {
            mounted = false;
            clearInterval(intervalId);
        };
    }, [projectCode]);

    return (
        <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: 16, marginBottom: 24,
        }}>
            <SignalCard
                title="Traffic (RPS)"
                description="Requests per second through Gateway"
                icon="🌐"
                color="#3b82f6"
                value={metrics.rps}
            />
            <SignalCard
                title="Error Rate"
                description="Percentage of 5xx responses"
                icon="⚠️"
                color="#ef4444"
                value={metrics.errorRate}
            />
            <SignalCard
                title="AVG Latency"
                description="Average request processing time"
                icon="⏱️"
                color="#f59e0b"
                value={metrics.avgLatency}
            />
            <SignalCard
                title="Upstream Latency"
                description="Backend service processing time"
                icon="🔧"
                color="#a855f7"
                value={metrics.upstreamLatency}
            />
        </div>
    );
}
