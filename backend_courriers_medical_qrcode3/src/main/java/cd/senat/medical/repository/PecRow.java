// src/main/java/cd/senat/medical/repository/PecRow.java
package cd.senat.medical.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PecRow {
    Long getId();
    String getNumero();

    /** Date remontée par la requête native/projection */
    LocalDateTime getCreatedAt();

    Long getHopitalId();
    String getHopitalNom();

    String getBeneficiaireNom();
    String getBeneficiaireQualite();

    /** Ajouté pour le listing Jasper */
    String getEtablissement();

    BigDecimal getMontant();
}