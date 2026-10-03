export default function StatusBadge({ status }) {
    const normalized = status?.toUpperCase() || 'UNKNOWN';
    let className = 'badge ';

    switch (normalized) {
        case 'ACTIVE':
            className += 'badge-active';
            break;
        case 'LOCKED':
        case 'DELETED':
        case 'INACTIVE':
            className += 'badge-locked';
            break;
        default:
            className += 'badge-pending';
    }

    return <span className={className}>{normalized}</span>;
}
