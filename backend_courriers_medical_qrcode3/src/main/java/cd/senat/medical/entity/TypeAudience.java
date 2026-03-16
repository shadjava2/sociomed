package cd.senat.medical.entity;

public enum TypeAudience {
    PUBLIQUE("Publique"),
    PRIVEE("Privée"),
    COMMISSION("Commission");
    
    private final String libelle;
    
    TypeAudience(String libelle) {
        this.libelle = libelle;
    }
    
    public String getLibelle() {
        return libelle;
    }
}