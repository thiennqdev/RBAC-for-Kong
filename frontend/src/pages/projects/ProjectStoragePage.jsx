import { useParams } from 'react-router-dom';
import FileExplorer from './FileExplorer';

export default function ProjectStoragePage() {
    const { projectCode } = useParams();

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>📁 File Browser</h2>
                    <p>Manage files and objects in the tenant's isolated storage bucket</p>
                </div>
            </div>
            
            <div className="card">
                <FileExplorer projectCode={projectCode} />
            </div>
        </div>
    );
}
