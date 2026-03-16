import React from 'react';
import { Link } from 'react-router-dom';
import { FolderOpen, FileText, Stethoscope, LayoutGrid } from 'lucide-react';

/**
 * Page Dossiers — accès central aux prises en charge et documents.
 * Affiche des raccourcis vers les modules existants en attendant un éventuel module dédié.
 */
export const DossiersPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-slate-100 border border-slate-200">
          <FolderOpen className="w-6 h-6 text-[#800020]" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Dossiers</h1>
          <p className="text-sm text-slate-600">
            Accès aux prises en charge et documents
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <Link
          to="/pec"
          className="flex items-center gap-4 p-4 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 hover:border-[#800020]/30 transition-colors shadow-sm"
        >
          <div className="p-2 rounded-lg bg-[#800020]/10">
            <Stethoscope className="w-6 h-6 text-[#800020]" />
          </div>
          <div>
            <h2 className="font-semibold text-slate-800">Prises en charge</h2>
            <p className="text-sm text-slate-600">Consulter et gérer les PEC</p>
          </div>
        </Link>

        <Link
          to="/tableau-de-bord"
          className="flex items-center gap-4 p-4 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 hover:border-[#800020]/30 transition-colors shadow-sm"
        >
          <div className="p-2 rounded-lg bg-[#800020]/10">
            <LayoutGrid className="w-6 h-6 text-[#800020]" />
          </div>
          <div>
            <h2 className="font-semibold text-slate-800">Tableau de bord</h2>
            <p className="text-sm text-slate-600">Statistiques par hôpital</p>
          </div>
        </Link>

        <div className="flex items-center gap-4 p-4 rounded-xl border border-dashed border-slate-200 bg-slate-50/50">
          <div className="p-2 rounded-lg bg-slate-200">
            <FileText className="w-6 h-6 text-slate-400" />
          </div>
          <div>
            <h2 className="font-semibold text-slate-500">Documents / Archives</h2>
            <p className="text-sm text-slate-400">Module à venir</p>
          </div>
        </div>
      </div>
    </div>
  );
};
