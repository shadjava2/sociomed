Logo et fonds de page pour les rapports PDF
==========================================

Pour que le logo et le fond de page s'affichent dans les PDF (liste des PEC et note de prise en charge),
placez les images suivantes dans CE dossier (src/main/resources/reports/) :

  - senat-logo.png         (logo du Sénat en en-tête des rapports)
  - fond_portrait_a4.png   (note de prise en charge, format portrait A4 595x842 pt)
  - fond_paysage_a4.png    (listing des PEC, format paysage A4 842x595 pt)

Sans ces fichiers, le logo et le bandeau "background" des rapports resteront vides.

Copie rapide depuis le frontend (si les fichiers sont dans frontend/public/assets/) :
  Depuis la racine du projet :
  copy frontend\public\assets\senat-logo.png backend_courriers_medical_qrcode3\src\main\resources\reports\senat-logo.png
  copy frontend\public\assets\fond_portrait_a4.png backend_courriers_medical_qrcode3\src\main\resources\reports\fond_portrait_a4.png
  copy frontend\public\assets\fond_paysage_a4.png backend_courriers_medical_qrcode3\src\main\resources\reports\fond_paysage_a4.png
