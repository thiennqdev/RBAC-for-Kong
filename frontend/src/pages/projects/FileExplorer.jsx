import React, { useState, useEffect, useRef, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { fileExplorerService } from '../../services/fileExplorerService';
import { useToast } from '../../components/common/Toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ProjectStoragePolicies from './ProjectStoragePolicies';
import './FileExplorer.css';

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const formatDate = (dateString) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleString();
};

const getFileType = (filename) => {
  if (!filename) return 'other';
  const ext = filename.split('.').pop().toLowerCase();
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'jfif', 'avif', 'bmp', 'ico', 'tiff'].includes(ext)) return 'image';
  if (['mp4', 'webm', 'ogg', 'mov'].includes(ext)) return 'video';
  if (['pdf', 'doc', 'docx', 'txt', 'csv', 'xls', 'xlsx'].includes(ext)) return 'document';
  return 'other';
};

const FileExplorer = ({ projectCode }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [currentPrefix, setCurrentPrefix] = useState('');
  const [usage, setUsage] = useState({ usedBytes: 0, quotaBytes: 1 });
  const [storageEnabled, setStorageEnabled] = useState(true);
  
  // Filtering and Selection
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('all');
  const [selectedFiles, setSelectedFiles] = useState([]);
  
  // Modals
  const [showFolderModal, setShowFolderModal] = useState(false);
  const [folderName, setFolderName] = useState('');
  
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const [previewFile, setPreviewFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  
  const [showShareModal, setShowShareModal] = useState(false);
  const [shareFile, setShareFile] = useState(null);
  const [shareExpiry, setShareExpiry] = useState(1);
  const [shareUrl, setShareUrl] = useState('');

  const [showPoliciesModal, setShowPoliciesModal] = useState(false);

  const toast = useToast();
  const fileInputRef = useRef(null);

  const fetchData = async (prefix) => {
    setLoading(true);
    try {
      const [filesRes, usageRes] = await Promise.all([
        fileExplorerService.listFiles(projectCode, prefix),
        fileExplorerService.getStorageUsage(projectCode).catch(() => null)
      ]);
      setFiles(filesRes || []);
      setCurrentPrefix(prefix);
      if (usageRes) setUsage(usageRes);
      setSelectedFiles([]); // Reset selection on navigate
      setStorageEnabled(true);
    } catch (error) {
      console.error('Failed to load data:', error);
      const errorMsg = error.response?.data?.message || error.message || '';
      if (errorMsg.includes('Storage not enabled') || error.response?.status === 404) {
        setStorageEnabled(false);
      } else {
        toast.error(errorMsg || 'Failed to load files');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData('');
  }, [projectCode]);

  const handleNavigate = (prefix) => {
    fetchData(prefix);
  };

  const handleBreadcrumbClick = (index) => {
    if (index === -1) {
      handleNavigate('');
      return;
    }
    const parts = currentPrefix.split('/').filter(Boolean);
    const newPrefix = parts.slice(0, index + 1).join('/') + '/';
    handleNavigate(newPrefix);
  };

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileSelect = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const targetPath = currentPrefix + file.name;
      const { url, finalFilePath } = await fileExplorerService.getUploadUrl(projectCode, targetPath);
      await fileExplorerService.uploadFileDirectly(url, file);
      
      const uploadedName = finalFilePath.split('/').pop();
      if (uploadedName !== file.name) {
        toast.success(`File renamed to ${uploadedName} due to collision`);
      } else {
        toast.success('File uploaded successfully');
      }
      fetchData(currentPrefix);
    } catch (error) {
      console.error('Upload failed:', error);
      toast.error('Failed to upload file');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDelete = async (fileItem) => {
    if (!window.confirm(`Are you sure you want to delete ${fileItem.name}?`)) return;
    try {
      await fileExplorerService.deleteFile(projectCode, fileItem.path);
      toast.success('File deleted successfully');
      fetchData(currentPrefix);
    } catch (error) {
      console.error('Delete failed:', error);
      toast.error('Failed to delete file');
    }
  };

  const handleBulkDelete = async () => {
    if (!window.confirm(`Are you sure you want to delete ${selectedFiles.length} items?`)) return;
    try {
      await fileExplorerService.bulkDeleteFiles(projectCode, selectedFiles);
      toast.success('Files deleted successfully');
      fetchData(currentPrefix);
    } catch (error) {
      console.error('Bulk delete failed:', error);
      toast.error('Failed to delete some files');
    }
  };

  const handleCreateFolder = async (e) => {
    e.preventDefault();
    if (!folderName.trim()) return;
    try {
      const folderPath = currentPrefix + folderName.trim();
      await fileExplorerService.createFolder(projectCode, folderPath);
      toast.success('Folder created');
      setShowFolderModal(false);
      setFolderName('');
      fetchData(currentPrefix);
    } catch (error) {
      toast.error('Failed to create folder');
    }
  };

  const handleDownload = async (fileItem) => {
    try {
      const { url } = await fileExplorerService.getDownloadUrl(projectCode, fileItem.path);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileItem.name;
      a.target = '_blank';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    } catch (error) {
      toast.error('Failed to get download link');
    }
  };
  
  const openPreview = async (fileItem) => {
    try {
      const { url } = await fileExplorerService.getDownloadUrl(projectCode, fileItem.path, null, true);
      setPreviewUrl(url);
      setPreviewFile(fileItem);
      setShowPreviewModal(true);
    } catch (error) {
      toast.error('Failed to load preview');
    }
  };

  const openShare = (fileItem) => {
    setShareFile(fileItem);
    setShareExpiry(1);
    setShareUrl('');
    setShowShareModal(true);
  };

  const generateShareLink = async () => {
    try {
      const { url } = await fileExplorerService.getDownloadUrl(projectCode, shareFile.path, shareExpiry);
      setShareUrl(url);
      toast.success('Share link generated');
    } catch (error) {
      toast.error('Failed to generate link');
    }
  };

  const toggleSelection = (path) => {
    setSelectedFiles(prev => prev.includes(path) ? prev.filter(p => p !== path) : [...prev, path]);
  };

  const toggleAll = (e) => {
    if (e.target.checked) {
      setSelectedFiles(filteredFiles.filter(f => !f.dir).map(f => f.path));
    } else {
      setSelectedFiles([]);
    }
  };

  // Derived state
  const filteredFiles = useMemo(() => {
    return files.filter(f => {
      if (searchQuery && !f.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
      if (filterType !== 'all' && !f.dir) {
        if (getFileType(f.name) !== filterType) return false;
      }
      return true;
    });
  }, [files, searchQuery, filterType]);

  const breadcrumbParts = currentPrefix.split('/').filter(Boolean);
  const usagePercent = Math.min(100, (usage.usedBytes / usage.quotaBytes) * 100) || 0;
  const isNearQuota = usagePercent > 90;

  return (
    <div className="file-explorer" style={{ display: 'flex', flexDirection: 'column', minHeight: '600px' }}>
      {/* Quota Bar */}
      <div className="quota-container">
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.85rem' }}>
          <span>Storage Usage</span>
          <span style={{ color: isNearQuota ? '#f87171' : '#94a3b8' }}>
            {formatSize(usage.usedBytes)} / {formatSize(usage.quotaBytes)} ({usagePercent.toFixed(1)}%)
          </span>
        </div>
        <div style={{ width: '100%', height: '6px', background: 'rgba(0,0,0,0.3)', borderRadius: '3px', overflow: 'hidden' }}>
          <div style={{ width: `${usagePercent}%`, height: '100%', background: isNearQuota ? '#ef4444' : 'linear-gradient(90deg, #6366f1, #8b5cf6)', transition: 'width 0.3s ease' }} />
        </div>
      </div>

      <div className="file-explorer-header">
        <div className="file-explorer-breadcrumb">
          <div className="breadcrumb-item" onClick={() => handleBreadcrumbClick(-1)}>
            <span>🏠</span> Root
          </div>
          {breadcrumbParts.map((part, index) => (
            <React.Fragment key={index}>
              <span className="breadcrumb-separator">/</span>
              <div className="breadcrumb-item" onClick={() => handleBreadcrumbClick(index)}>{part}</div>
            </React.Fragment>
          ))}
        </div>
        
        <div className="file-explorer-actions">
          {/* Toolbar Search & Filter */}
          <div style={{ display: 'flex', gap: '8px', marginRight: '16px' }}>
            <input 
              type="text" 
              className="form-input" 
              placeholder="Search files..." 
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              style={{ width: '150px', padding: '6px 12px' }}
            />
            <select className="form-input" value={filterType} onChange={e => setFilterType(e.target.value)} style={{ padding: '6px 12px' }}>
              <option value="all">All Types</option>
              <option value="image">Images</option>
              <option value="document">Documents</option>
              <option value="video">Videos</option>
            </select>
          </div>

          {selectedFiles.length > 0 && (
            <button className="btn btn-danger" onClick={handleBulkDelete} style={{ marginRight: '8px' }}>
              <span>🗑️</span> Delete ({selectedFiles.length})
            </button>
          )}

          <button className="btn btn-secondary" onClick={() => setShowPoliciesModal(true)} disabled={!storageEnabled} style={{ marginRight: '8px' }}>
            <span>🔒</span> Access Policies
          </button>
          
          <button className="btn btn-secondary" onClick={() => setShowFolderModal(true)} disabled={!storageEnabled}>
            <span>📁</span> New Folder
          </button>

          <input type="file" ref={fileInputRef} style={{ display: 'none' }} onChange={handleFileSelect} />
          <button className="upload-btn" onClick={handleUploadClick} disabled={loading || uploading || !storageEnabled}>
            {uploading ? <><span>⏳</span> Uploading...</> : <><span>⬆️</span> Upload</>}
          </button>
          <button className="action-btn refresh-btn" onClick={() => fetchData(currentPrefix)} title="Refresh">
            <span style={{ display: 'inline-block', transform: loading ? 'rotate(180deg)' : 'none', transition: 'transform 0.3s' }}>🔄</span>
          </button>
        </div>
      </div>

      <div className="file-explorer-body" style={{ flex: 1, position: 'relative' }}>
        {!storageEnabled ? (
          <div className="empty-state">
            <span style={{ fontSize: '64px', marginBottom: '16px', display: 'block' }}>⚠️</span>
            <h3>Storage Not Enabled</h3>
            <p>Storage Management has not yet enabled on this Project, please contact the System Admin</p>
          </div>
        ) : loading && !uploading && files.length === 0 ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '48px' }}><LoadingSpinner /></div>
        ) : filteredFiles.length === 0 && currentPrefix === '' ? (
          <div className="empty-state">
            <span style={{ fontSize: '64px', marginBottom: '16px', display: 'block' }}>📂</span>
            <h3>No files found</h3>
            <p>Upload a file or create a folder to get started.</p>
          </div>
        ) : (
          <table className="file-explorer-table">
            <thead>
              <tr>
                <th style={{ width: '40px', textAlign: 'center' }}>
                  <input type="checkbox" onChange={toggleAll} checked={selectedFiles.length > 0 && selectedFiles.length === filteredFiles.filter(f=>!f.dir).length} />
                </th>
                <th>Name</th>
                <th style={{ width: '120px' }}>Size</th>
                <th style={{ width: '180px' }}>Modified</th>
                <th style={{ width: '180px', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {currentPrefix !== '' && (
                <tr className="file-row">
                  <td colSpan="5">
                    <div className="file-name-cell is-dir" onClick={() => handleBreadcrumbClick(breadcrumbParts.length - 2)}>
                      <span className="file-icon">⤴️</span>
                      ..
                    </div>
                  </td>
                </tr>
              )}
              {filteredFiles.map(file => (
                <tr key={file.path} className="file-row" style={{ background: selectedFiles.includes(file.path) ? 'var(--bg-secondary)' : '' }}>
                  <td style={{ textAlign: 'center' }}>
                    {!file.dir && (
                      <input type="checkbox" checked={selectedFiles.includes(file.path)} onChange={() => toggleSelection(file.path)} />
                    )}
                  </td>
                  <td>
                    <div className={`file-name-cell ${file.dir ? 'is-dir' : ''}`} onClick={() => file.dir ? handleNavigate(file.path) : openPreview(file)}>
                      <span className="file-icon">
                        {file.dir ? '📁' : '📄'}
                      </span>
                      {file.name}
                    </div>
                  </td>
                  <td>{file.dir ? '-' : formatSize(file.size)}</td>
                  <td>{formatDate(file.lastModified)}</td>
                  <td style={{ textAlign: 'right' }}>
                    <div className="file-actions" style={{ justifyContent: 'flex-end' }}>
                      {file.dir ? (
                        <>
                          {/* Folder actions can go here in the future if needed */}
                        </>
                      ) : (
                        <>
                          <button className="action-btn" title="Preview" onClick={() => openPreview(file)}>👁️</button>
                          <button className="action-btn" title="Share" onClick={() => openShare(file)}>🔗</button>
                          <button className="action-btn" title="Download" onClick={() => handleDownload(file)}>⬇️</button>
                        </>
                      )}
                      <button className="action-btn delete" title="Delete" onClick={() => handleDelete(file)}>🗑️</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Modals */}
      {showFolderModal && createPortal(
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '400px' }}>
            <div className="modal-header">
              <h3>Create Folder</h3>
              <button className="btn-close" onClick={() => setShowFolderModal(false)}>✕</button>
            </div>
            <form onSubmit={handleCreateFolder}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Folder Name</label>
                  <input type="text" className="form-input" autoFocus value={folderName} onChange={e => setFolderName(e.target.value)} placeholder="e.g. images" required />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowFolderModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create</button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}

      {showShareModal && createPortal(
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h3>Share File</h3>
              <button className="btn-close" onClick={() => setShowShareModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ marginBottom: 16 }}>Create a temporary link to share <strong>{shareFile?.name}</strong>.</p>
              <div className="form-group">
                <label className="form-label">Link Expiry</label>
                <select className="form-input" value={shareExpiry} onChange={e => setShareExpiry(Number(e.target.value))}>
                  <option value={1}>1 Hour</option>
                  <option value={12}>12 Hours</option>
                  <option value={24}>24 Hours</option>
                  <option value={168}>7 Days</option>
                </select>
              </div>
              {shareUrl && (
                <div style={{ marginTop: 16 }}>
                  <label className="form-label">Presigned URL:</label>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <input type="text" className="form-input" value={shareUrl} readOnly />
                    <button className="btn btn-primary" onClick={() => { navigator.clipboard.writeText(shareUrl); toast.success('Copied!'); }}>Copy</button>
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowShareModal(false)}>Close</button>
              {!shareUrl && <button className="btn btn-primary" onClick={generateShareLink}>Generate Link</button>}
            </div>
          </div>
        </div>,
        document.body
      )}

      {showPreviewModal && previewFile && createPortal(
        <div className="modal-overlay" style={{ zIndex: 9999 }} onClick={() => setShowPreviewModal(false)}>
          <div className="modal-content" style={{ 
            width: getFileType(previewFile.name) === 'image' ? 'fit-content' : '80%', 
            height: getFileType(previewFile.name) === 'image' ? 'fit-content' : '80vh', 
            maxWidth: '90vw',
            maxHeight: '90vh',
            display: 'flex', flexDirection: 'column' 
          }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{previewFile.name}</h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="btn btn-primary btn-sm" onClick={() => handleDownload(previewFile)}>⬇️</button>
                <button className="btn-close" onClick={() => setShowPreviewModal(false)}>✕</button>
              </div>
            </div>
            <div className="modal-body" style={{ flex: 1, padding: 0, overflow: 'hidden', background: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {getFileType(previewFile.name) === 'image' ? (
                <img src={previewUrl} alt={previewFile.name} style={{ maxWidth: '100%', maxHeight: 'calc(90vh - 60px)', display: 'block' }} />
              ) : (
                <iframe src={previewUrl} title={previewFile.name} style={{ width: '100%', height: '100%', minHeight: '60vh', border: 'none', background: '#fff' }} />
              )}
            </div>
          </div>
        </div>,
        document.body
      )}
      {showPoliciesModal && createPortal(
        <div style={{
          position: 'fixed', inset: 0, zIndex: 9999,
          background: 'rgba(0,0,0,0.6)', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          padding: '20px'
        }}>
          <div className="card" style={{ width: '90%', maxWidth: '1400px', maxHeight: '90vh', overflowY: 'auto', position: 'relative' }}>
            <button 
                onClick={() => setShowPoliciesModal(false)}
                style={{ position: 'absolute', top: 16, right: 16, background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '1.2rem' }}
            >✕</button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: 0 }}>
                <div style={{ fontSize: '1.8rem' }}>🔒</div>
                <div>
                    <h3 style={{ margin: 0 }}>Access Policies</h3>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Manage public access rules for your storage bucket</p>
                </div>
            </div>
            <ProjectStoragePolicies projectCode={projectCode} initialPrefix={currentPrefix} />
          </div>
        </div>,
        document.body
      )}

    </div>
  );
};

export default FileExplorer;
