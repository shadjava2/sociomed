package cd.senat.medical.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * DTO simple (label, value) pour exposer des statistiques agrégées.
 * 
 * Exemple d'usage :
 *   - Nombre de prises en charge par hôpital (label = nom de l'hôpital, value = total)
 *   - Répartition par catégorie, etc.
 */
public class LabelCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Libellé (ex: nom de l'hôpital, catégorie, etc.) */
    private String label;

    /** Valeur numérique associée (ex: nombre de PEC) */
    private Long value;

    public LabelCountDTO() {
    }

    public LabelCountDTO(String label, Long value) {
        this.label = label;
        this.value = value;
    }

    public static LabelCountDTO of(String label, Long value) {
        return new LabelCountDTO(label, value);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "LabelCountDTO{" +
                "label='" + label + '\'' +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabelCountDTO that)) return false;
        return Objects.equals(label, that.label) &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, value);
    }
}
