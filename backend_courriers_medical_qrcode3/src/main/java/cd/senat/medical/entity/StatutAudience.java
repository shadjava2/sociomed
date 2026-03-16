package cd.senat.medical.entity;

public enum StatutAudience {
    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");
    
    private final String libelle;
    
    StatutAudience(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
}