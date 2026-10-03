export default function LoadingSpinner({ text = 'Loading...' }) {
    return (
        <div className="loading-container">
            <div className="spinner" />
            <p>{text}</p>
        </div>
    );
}
