import { useParams, useNavigate } from 'react-router-dom';

export default function PlaceholderPage({ title }) {
    const { projectCode } = useParams();
    const navigate = useNavigate();
    return (
        <div className="card">
            <h2>{title}</h2>
            <p>Work in progress.</p>
            <button className="btn btn-secondary" onClick={() => navigate(-1)}>Go Back</button>
        </div>
    );
}
