import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { pecPublicService, PecVerifyResponse, PecVerifyStatus } from '../services/pecPublicService';
import { getPublicApiBaseUrl } from '../config/publicApi';
import { Skeleton } from '../components/ui/Skeleton';

function badgeClasses(status: PecVerifyStatus) {
  switch (status) {
    case 'VALID':
      return 'bg-green-600 text-white';
    case 'EXPIRED':
      return 'bg-amber-600 text-white';
    case 'INVALID':
      return 'bg-red-600 text-white';
    case 'NOT_FOUND':
      return 'bg-slate-600 text-white';
    default:
      return 'bg-slate-700 text-white';
  }
}

function badgeLabel(status: PecVerifyStatus) {
  switch (status) {
    case 'VALID':
      return 'VALIDE';
    case 'EXPIRED':
      return 'LIEN EXPIRÉ';
    case 'INVALID':
      return 'LIEN INVALIDE';
    case 'NOT_FOUND':
      return 'INTROUVABLE';
    default:
      return 'ERREUR';
  }
}

export default function PublicPecVerifyPage() {
  const { token } = useParams();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<PecVerifyResponse | null>(null);
  const [logoError, setLogoError] = useState(false);
  const [photoLoaded, setPhotoLoaded] = useState(false);

  const safeToken = useMemo(() => (token ? String(token) : ''), [token]);
  const showPhotoSkeleton = data?.photoUrl && safeToken && !photoLoaded;

  useEffect(() => {
    document.title = "Vérifier l'authenticité — PEC Sénat";
    return () => {
      document.title = "PRISE EN CHARGE MEDICAL";
    };
  }, []);

  useEffect(() => {
    if (!safeToken) {
      setData({ status: 'INVALID', message: 'Token manquant.' });
      setLoading(false);
      return;
    }

    let cancelled = false;
    setPhotoLoaded(false);

    pecPublicService
      .verify(safeToken)
      .then((res) => {
        if (!cancelled) {
          setData(res);
          setLoading(false);
        }
      })
      .catch((err: any) => {
        if (cancelled) return;
        const body = err?.response?.data;
        if (body && typeof body === 'object' && body.status) {
          setData(body as PecVerifyResponse);
        } else {
          setData({ status: 'ERROR', message: 'Erreur réseau ou serveur.' });
        }
        setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [safeToken]);

  const status: PecVerifyStatus = (data?.status || 'ERROR') as PecVerifyStatus;

  const tokenExpiry = data?.tokenExpiresAt ? new Date(data.tokenExpiresAt) : null;
  const isLinkActive = status === 'VALID' && tokenExpiry && tokenExpiry > new Date();

  const formatDate = (d: string | null | undefined) =>
    d ? new Date(d).toLocaleDateString('fr-FR') : '-';

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <div className="mx-auto max-w-3xl px-4 py-10">
        <header className="flex items-center gap-3">
          {!logoError ? (
            <img
              src="/assets/senat-logo.png"
              alt="Sénat RDC"
              className="h-12 w-12 flex-shrink-0 object-contain"
              onError={() => setLogoError(true)}
            />
          ) : (
            <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-lg bg-[#800020] text-lg font-bold text-white">
              S
            </div>
          )}
          <div>
            <h1 className="text-lg font-semibold">SÉNAT — RDC</h1>
            <p className="text-sm text-slate-600">Prise en charge médicale</p>
          </div>
        </header>

        <p className="mt-4 rounded-lg border border-slate-200 bg-white px-4 py-3 text-center text-sm font-medium text-slate-700">
          Scanner pour vérifier l'authenticité (sans authentification)
        </p>

        <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0 flex-1">
              <div className="text-sm text-slate-600">Statut du document</div>
              {loading ? (
                <Skeleton className="mt-1 h-8 w-40" />
              ) : (
                <div className="mt-1 text-2xl font-bold">{badgeLabel(status)}</div>
              )}
            </div>
            {loading ? (
              <Skeleton className="h-9 w-24 rounded-full" />
            ) : (
              <div className={`inline-flex items-center rounded-full px-4 py-2 text-sm font-semibold ${badgeClasses(status)}`}>
                {badgeLabel(status)}
              </div>
            )}
          </div>

          <div className="mt-3 min-h-[1.25rem] text-slate-700">
            {loading ? (
              <Skeleton className="h-4 w-full max-w-sm" />
            ) : (
              data?.message || ''
            )}
          </div>

          {/* Skeleton progressif pendant la vérification */}
          {loading && (
            <div className="mt-6 space-y-4 rounded-xl bg-slate-50 p-4 ring-1 ring-slate-200">
              <div className="flex items-center gap-3">
                <Skeleton className="h-8 w-8 shrink-0 rounded-full" />
                <Skeleton className="h-5 flex-1 max-w-md" />
              </div>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                <Skeleton className="h-28 w-28 shrink-0 rounded-lg" />
                <div className="grid min-w-0 flex-1 grid-cols-1 gap-3 sm:grid-cols-2">
                  {Array.from({ length: 8 }, (_, i) => `skeleton-${i}`).map((id, i) => (
                    <div key={id} className="space-y-1">
                      <Skeleton className="h-3 w-24" />
                      <Skeleton className="h-4 w-full" />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {!loading && status === 'VALID' && (
            <div className="mt-6 space-y-4 rounded-xl bg-green-50 p-4 ring-1 ring-green-200">
              <div className="flex items-center gap-2 text-green-800">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-green-600 text-white" aria-hidden>✓</span>
                <span className="font-semibold">Document valide — Ce QR code est une facilitation pour vérifier l'authenticité.</span>
              </div>
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
                {(data?.photoUrl && safeToken) ? (
                  <div className="relative h-28 w-28 flex-shrink-0">
                    {showPhotoSkeleton && (
                      <Skeleton className="absolute inset-0 h-28 w-28 rounded-lg" />
                    )}
                    <img
                      src={`${getPublicApiBaseUrl()}/api/pec/public/${encodeURIComponent(safeToken)}/photo`}
                      alt="Bénéficiaire"
                      className={`h-28 w-28 rounded-lg border border-slate-200 object-cover bg-slate-100 ${showPhotoSkeleton ? 'invisible' : 'visible'}`}
                      onLoad={() => setPhotoLoaded(true)}
                      onError={(e) => {
                        setPhotoLoaded(true);
                        (e.target as HTMLImageElement).style.display = 'none';
                      }}
                    />
                  </div>
                ) : null}
                <div className="grid min-w-0 flex-1 grid-cols-1 gap-3 sm:grid-cols-2">
                  <div>
                    <div className="text-xs text-slate-600">Numéro PEC</div>
                    <div className="text-base font-semibold">{data?.numero || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">Date d'émission</div>
                    <div className="text-base font-semibold">{formatDate(data?.dateEmission)}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">Date expiration (lien)</div>
                    <div className="text-base font-semibold">
                      <span className={isLinkActive ? 'text-green-700' : 'text-amber-700'}>
                        {isLinkActive ? 'Actif' : 'Expiré'}
                      </span>
                      {tokenExpiry && (
                        <span className="ml-1 text-sm text-slate-600">
                          (jusqu'au {formatDate(data?.tokenExpiresAt)})
                        </span>
                      )}
                    </div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">NOM &amp; POST-NOMS</div>
                    <div className="text-base font-semibold">{[data?.nom, data?.postnom].filter(Boolean).join(' ') || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">PRÉNOM</div>
                    <div className="text-base font-semibold">{data?.prenom || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">SEXE</div>
                    <div className="text-base font-semibold">{data?.genre || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">ÂGE</div>
                    <div className="text-base font-semibold">{data?.age ?? '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">Adresse du malade</div>
                    <div className="text-base font-semibold">{data?.adresseMalade || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">Qualité du malade</div>
                    <div className="text-base font-semibold">{data?.qualiteMalade || '-'}</div>
                  </div>
                  <div>
                    <div className="text-xs text-slate-600">Établissement (est adressé à)</div>
                    <div className="text-base font-semibold">{data?.etablissement || '-'}</div>
                  </div>
                  <div className="sm:col-span-2">
                    <div className="text-xs text-slate-600">Motif</div>
                    <div className="text-base font-semibold">{data?.motif || '-'}</div>
                  </div>
                  {data?.dateExpiration && (
                    <div>
                      <div className="text-xs text-slate-600">Date expiration (PEC)</div>
                      <div className="text-base font-semibold">{formatDate(data.dateExpiration)}</div>
                    </div>
                  )}
                  {data?.createdByFullname && (
                    <div className="sm:col-span-2">
                      <div className="text-xs text-slate-600">Médecin du Sénat / Émis par</div>
                      <div className="text-base font-semibold">{data.createdByFullname}</div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Informations si non valide */}
          {status !== 'VALID' && !loading && (
            <div className="mt-6 rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
              <p className="font-medium">Document non valide — Ce QR code est une facilitation pour vérifier l'authenticité.</p>
              <p className="mt-2">En cas de doute ou de fraude, contactez le service médical du Sénat.</p>
            </div>
          )}
        </div>

        <p className="mt-6 text-center text-xs text-slate-500">
          Vérification d'authenticité des notes de prise en charge — Application officielle Sénat RDC
        </p>
      </div>
    </div>
  );
}
