import { useState, useEffect } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer } from 'recharts';
import observabilityService from '../../services/observabilityService';
import LoadingSpinner from '../common/LoadingSpinner';

export default function MetricsCharts({ projectCode }) {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let mounted = true;
        const loadData = async () => {
            try {
                const end = Math.floor(Date.now() / 1000);
                const start = end - 3600; // Last 1 hour
                const step = '60'; // 1 minute resolution

                // Construct PromQL query (with dummy projectCode filter logic if needed, but here we just query)
                // For a real setup, Kong metrics might need to be filtered by route or service matching the projectCode
                // Example: `sum(rate(kong_http_requests_total{route=~"${projectCode}.*"}[1m]))`
                // Here we'll use a basic rate for demonstration purposes
                const promQL = `sum(rate(kong_http_requests_total{service=~"${projectCode}\\\\..*"}[1m]))`;

                const res = await observabilityService.queryPrometheusRange(projectCode, promQL, start, end, step);

                if (mounted && res.data && res.data.data && res.data.data.result) {
                    const resultData = res.data.data.result;
                    if (resultData.length > 0) {
                        const timeSeries = resultData[0].values.map(([timestamp, value]) => ({
                            time: new Date(timestamp * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                            rps: parseFloat(value).toFixed(2),
                        }));
                        setData(timeSeries);
                    } else {
                        setData([]);
                    }
                }
            } catch (error) {
                console.error("Failed to load metrics data", error);
            } finally {
                if (mounted) setLoading(false);
            }
        };

        loadData();

        // Optional: Polling every 10s
        const intervalId = setInterval(loadData, 10000);
        return () => {
            mounted = false;
            clearInterval(intervalId);
        };
    }, [projectCode]);

    if (loading) {
        return (
            <div className="card" style={{ minHeight: 400, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <LoadingSpinner text="Loading metrics..." />
            </div>
        );
    }

    if (data.length === 0) {
        return (
            <div className="card" style={{ minHeight: 400, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <div className="empty-state">
                    <h3>No Data Available</h3>
                    <p>No traffic data found for this project in the selected time range.</p>
                </div>
            </div>
        );
    }

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            return (
                <div style={{ background: '#1e293b', border: '1px solid rgba(255,255,255,0.1)', padding: '10px 14px', borderRadius: '8px', color: '#f8fafc', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
                    <p style={{ margin: '0 0 6px 0', fontSize: '0.85rem', color: '#94a3b8' }}>{label}</p>
                    <p style={{ margin: 0, fontSize: '1rem', fontWeight: 600, color: '#3b82f6' }}>
                        Traffic: {payload[0].value} RPS
                    </p>
                </div>
            );
        }
        return null;
    };

    return (
        <div className="card" style={{ padding: '24px', position: 'relative' }}>
            <h3 style={{ marginBottom: '24px', fontSize: '1.1rem', color: 'var(--text-primary)' }}>Traffic Overview (RPS) - Last 1 Hour</h3>
            <div style={{ width: '100%', height: 350 }}>
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                        <defs>
                            <linearGradient id="colorRps" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                        <XAxis 
                            dataKey="time" 
                            stroke="#64748b" 
                            fontSize={12} 
                            tickLine={false} 
                            axisLine={false} 
                            minTickGap={30}
                        />
                        <YAxis 
                            stroke="#64748b" 
                            fontSize={12} 
                            tickLine={false} 
                            axisLine={false} 
                            tickFormatter={(value) => `${value}`}
                        />
                        <RechartsTooltip content={<CustomTooltip />} />
                        <Area 
                            type="monotone" 
                            dataKey="rps" 
                            stroke="#3b82f6" 
                            strokeWidth={3}
                            fillOpacity={1} 
                            fill="url(#colorRps)" 
                            activeDot={{ r: 6, strokeWidth: 0, fill: '#60a5fa' }}
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
