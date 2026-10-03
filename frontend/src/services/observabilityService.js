import api from './api';

const observabilityService = {
    // ========== Audit Logs ==========
    getAuditLogs: (projectCode, params = {}) =>
        api.get(`/projects/${projectCode}/audit-logs`, { params }),

    // ========== Gateway Logs ==========
    searchGatewayLogs: (projectCode, payload = {}) =>
        api.post(`/projects/${projectCode}/gateway-logs/search`, payload),

    traceRequest: (projectCode, requestId) =>
        api.get(`/projects/${projectCode}/gateway-logs/trace/${requestId}`),

    // ========== Prometheus Metrics ==========
    /**
     * Instant query – returns the current value of a PromQL expression.
     */
    queryPrometheus: (projectCode, query) =>
        api.get(`/projects/${projectCode}/metrics/query?query=${encodeURIComponent(query)}`),

    /**
     * Range query – returns time-series data for charting.
     * @param {string} projectCode
     * @param {string} query   PromQL expression
     * @param {number} start   epoch seconds
     * @param {number} end     epoch seconds
     * @param {string} step    resolution (e.g. "60" for 1-min buckets)
     */
    queryPrometheusRange: (projectCode, query, start, end, step = '60') =>
        api.get(`/projects/${projectCode}/metrics/query_range?query=${encodeURIComponent(query)}&start=${start}&end=${end}&step=${step}`),
};

export default observabilityService;

