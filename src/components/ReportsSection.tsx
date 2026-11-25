import React, { useMemo, useState } from 'react';
import type { RecordCategory, RecordOrigin, SoilRecord } from '../types/soil';

const CATEGORIES: Array<{ key: RecordCategory; label: string }> = [
  { key: 'SENSOR_READING', label: 'Sensor Readings' },
  { key: 'SOIL_SCAN', label: 'Soil Scans' },
  { key: 'CROP_SCAN', label: 'Crop Scans' },
  { key: 'PLANT_HEALTH_SCAN', label: 'Plant Health' },
  { key: 'NPK_REPORT', label: 'NPK Reports' },
  { key: 'AI_RECOMMENDATION', label: 'AI Advice' },
];

function originStyle(origin: RecordOrigin): string {
  switch (origin) {
    case 'LIVE_SENSOR':
      return 'bg-success-container text-on-success-container';
    case 'DEMO_DATA':
      return 'bg-warning-container text-on-warning-container';
    case 'LAB_REPORT':
      return 'bg-light-secondary-container text-light-on-secondary-container';
    case 'REFERENCE_ESTIMATE':
      return 'bg-light-error-container text-light-on-error-container';
    default:
      return 'bg-soil-container text-on-soil-container';
  }
}

interface ReportsSectionProps {
  records: SoilRecord[];
  onDelete: (id: string) => void;
  onClearDemo: () => void;
  onSeedDemo: () => void;
  onResetAll: () => void;
}

export const ReportsSection: React.FC<ReportsSectionProps> = ({
  records,
  onDelete,
  onClearDemo,
  onSeedDemo,
  onResetAll,
}) => {
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState<RecordCategory | 'ALL'>('ALL');
  const [selected, setSelected] = useState<SoilRecord | null>(null);
  const [confirmReset, setConfirmReset] = useState(false);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return records.filter((r) => {
      const catOk = catFilter === 'ALL' || r.category === catFilter;
      const qOk =
        !q ||
        r.title.toLowerCase().includes(q) ||
        r.summary.toLowerCase().includes(q) ||
        r.dataSource.toLowerCase().includes(q);
      return catOk && qOk;
    });
  }, [records, search, catFilter]);

  const exportRecord = (r: SoilRecord) => {
    const text = `FarmerBuddy (SMART SOIL) Report\nID: ${r.id}\nDate: ${new Date(r.timestampMs).toLocaleString()}\nOrigin: ${r.origin}\nSource: ${r.dataSource}\nTitle: ${r.title}\n\n${r.fullDetails}`;
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `farmerbuddy-${r.id.slice(0, 8)}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <main className="flex-1 px-page-margin py-gutter-md pb-28 flex flex-col gap-gutter-md">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <span className="font-label text-label-sm uppercase tracking-wider text-light-primary">
            FarmerBuddy · SMART SOIL
          </span>
          <h1 className="font-headline text-headline-lg text-light-on-surface">Reports &amp; History</h1>
          <p className="font-body text-body-sm text-light-on-surface-variant">{records.length} saved record(s)</p>
        </div>

        <div className="flex gap-2">
          {records.some((r) => r.isDemo) && (
            <button
              type="button"
              onClick={onClearDemo}
              className="min-h-[40px] px-3 rounded-full bg-light-surface-container text-light-on-surface font-label text-label-sm"
            >
              Clear Demo
            </button>
          )}
          <button
            type="button"
            onClick={() => setConfirmReset(true)}
            className="min-h-[40px] px-3 rounded-full bg-light-error-container text-light-on-error-container font-label text-label-sm"
          >
            Reset All Data
          </button>
        </div>
      </div>

      <input
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Search reports, crops, or sources..."
        className="w-full rounded-full border border-light-outline-variant bg-light-surface-container-lowest px-4 py-2.5 font-body text-body-md"
      />

      <div className="flex gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          onClick={() => setCatFilter('ALL')}
          className={`px-3.5 py-1.5 rounded-full font-label text-label-sm shrink-0 ${
            catFilter === 'ALL' ? 'bg-light-primary text-light-on-primary' : 'bg-light-surface-container text-light-on-surface-variant'
          }`}
        >
          All
        </button>
        {CATEGORIES.map((c) => (
          <button
            key={c.key}
            type="button"
            onClick={() => setCatFilter(c.key)}
            className={`px-3.5 py-1.5 rounded-full font-label text-label-sm shrink-0 ${
              catFilter === c.key ? 'bg-light-primary text-light-on-primary' : 'bg-light-surface-container text-light-on-surface-variant'
            }`}
          >
            {c.label}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-lg text-center flex flex-col items-center gap-3">
          <span className="material-symbols-outlined text-[40px] text-light-primary" aria-hidden="true">description</span>
          <h2 className="font-headline text-title-lg">
            {records.length === 0 ? 'No Reports Saved Yet' : 'No Matching Reports'}
          </h2>
          <p className="font-body text-body-sm text-light-on-surface-variant max-w-md">
            {records.length === 0
              ? 'Save a sensor snapshot, scan result, NPK report, or AI plan—or load illustrative sample records for your exhibition walkthrough.'
              : 'Clear your search query or category filter to view saved records.'}
          </p>
          {records.length === 0 && (
            <button
              type="button"
              onClick={onSeedDemo}
              className="min-h-[44px] px-5 rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-md"
            >
              Load Exhibition Sample Records
            </button>
          )}
        </div>
      ) : (
        <div className="flex flex-col gap-gutter-sm">
          {filtered.map((rec) => (
            <article
              key={rec.id}
              onClick={() => setSelected(rec)}
              className="bg-light-surface-container-lowest rounded-lg clay-card p-gutter-md cursor-pointer hover:ring-2 hover:ring-light-primary/40 transition-all"
            >
              <div className="flex items-center justify-between gap-2 mb-1">
                <span className={`px-2.5 py-0.5 rounded-full font-label text-label-sm ${originStyle(rec.origin)}`}>
                  {rec.origin.replace(/_/g, ' ')}
                </span>
                <span className="font-mono text-[11px] text-light-on-surface-variant">
                  {new Date(rec.timestampMs).toLocaleString()}
                </span>
              </div>
              <h2 className="font-headline text-title-md text-light-on-surface">{rec.title}</h2>
              <p className="font-body text-body-sm text-light-on-surface-variant line-clamp-2">{rec.summary}</p>
              <div className="mt-2 flex items-center justify-between text-body-sm">
                <span className="font-label text-label-sm text-light-primary truncate">Source: {rec.dataSource}</span>
                <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                  <button
                    type="button"
                    onClick={() => exportRecord(rec)}
                    className="px-2.5 py-1 rounded-full bg-light-surface-container font-label text-label-sm"
                  >
                    Export
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(rec.id)}
                    className="px-2.5 py-1 rounded-full bg-light-error-container text-light-on-error-container font-label text-label-sm"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-end sm:items-center justify-center p-4" onClick={() => setSelected(null)}>
          <div
            className="bg-light-surface rounded-lg max-w-lg w-full max-h-[85vh] overflow-y-auto p-gutter-lg flex flex-col gap-3"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex justify-between items-center">
              <span className={`px-2.5 py-0.5 rounded-full font-label text-label-sm ${originStyle(selected.origin)}`}>
                {selected.origin.replace(/_/g, ' ')}
              </span>
              <button type="button" onClick={() => setSelected(null)} className="font-label text-label-md text-light-primary">
                Close
              </button>
            </div>
            <h2 className="font-headline text-headline-md">{selected.title}</h2>
            <p className="font-mono text-[11px] text-light-on-surface-variant">
              ID: {selected.id} • {new Date(selected.timestampMs).toLocaleString()}
            </p>
            {selected.imageDataUrl && (
              <img src={selected.imageDataUrl} alt="Saved report attachment" className="w-full max-h-56 object-cover rounded" />
            )}
            <pre className="bg-light-surface-container-lowest p-3 rounded font-body text-body-sm whitespace-pre-wrap">
              {selected.fullDetails}
            </pre>
            <button
              type="button"
              onClick={() => exportRecord(selected)}
              className="min-h-[44px] rounded-full bg-light-primary text-light-on-primary clay-btn-primary font-label text-label-md"
            >
              Export Report (.txt)
            </button>
          </div>
        </div>
      )}

      {confirmReset && (
        <div className="fixed inset-0 z-50 bg-black/45 flex items-center justify-center p-4">
          <div className="bg-light-surface rounded-lg max-w-sm w-full p-gutter-lg flex flex-col gap-3">
            <h2 className="font-headline text-title-lg">Reset All Project Data?</h2>
            <p className="font-body text-body-sm text-light-on-surface-variant">
              This permanently clears all saved reports and resets crop thresholds and ADC calibration to defaults.
            </p>
            <div className="flex justify-end gap-2 mt-2">
              <button type="button" onClick={() => setConfirmReset(false)} className="px-4 py-2 rounded-full bg-light-surface-container font-label text-label-md">
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  onResetAll();
                  setConfirmReset(false);
                }}
                className="px-4 py-2 rounded-full bg-light-error text-light-on-error font-label text-label-md"
              >
                Reset Everything
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};
