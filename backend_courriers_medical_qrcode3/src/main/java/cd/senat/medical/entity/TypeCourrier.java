package cd.senat.medical.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TypeCourrier {
    RECU("Reçu"),
    ENVOYE("Envoyé");

    private final String libelle;

    TypeCourrier(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static TypeCourrier from(String value) {
        if (value == null) return null;
        value = value.trim().toUpperCase();
        switch (value) {
            case "RECU":
                return RECU;
            case "ENVOYE":
                return ENVOYE;
            default:
                throw new IllegalArgumentException("TypeCourrier invalide: " + value);
        }
    }
}
