import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Mail } from 'lucide-react';

export const ForgotPassword: React.FC = () => {
  return (
    <div
      className="min-h-screen flex items-center justify-center bg-gradient-to-br from-sky-400 via-sky-200 to-white p-4 pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]"
      style={{
        paddingLeft: 'max(1rem, env(safe-area-inset-left))',
        paddingRight: 'max(1rem, env(safe-area-inset-right))',
      }}
    >
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-auto max-h-[calc(100vh-2rem)] py-6 px-4 sm:p-8">
        <div className="flex flex-col items-center gap-4 mb-6">
          <div className="p-3 rounded-full bg-sky-100">
            <Mail className="w-8 h-8 text-sky-600" />
          </div>
          <div className="text-center">
            <h1 className="text-xl font-bold text-slate-800">Mot de passe oublié</h1>
            <p className="text-sm text-slate-600 mt-1">
              Pour réinitialiser votre mot de passe, contactez l’administrateur système ou le support technique du Sénat.
            </p>
          </div>
        </div>

        <div className="rounded-xl bg-slate-50 border border-slate-200 p-4 text-sm text-slate-700">
          <p className="font-medium text-slate-800 mb-1">À faire :</p>
          <ul className="list-disc list-inside space-y-1 text-slate-600">
            <li>Contacter votre administrateur</li>
            <li>Ou envoyer une demande au service support avec votre identifiant</li>
          </ul>
        </div>

        <Link
          to="/login"
          className="mt-6 w-full inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl border border-slate-200 bg-white text-slate-700 font-medium hover:bg-slate-50 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          Retour à la connexion
        </Link>
      </div>
    </div>
  );
};
