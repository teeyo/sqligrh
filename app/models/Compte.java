package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class Compte extends Model{

    @Id
    private Long id;

    @Constraints.Email(message = "cet e-mail est invalide.")
    @Constraints.Required(message = "ce champs est obligatoire.")
    @Column(unique = true, nullable = false)
    private String email;

    @Constraints.Required(message = "ce champs est obligatoire.")
    @Column(length = 32, nullable = false)
    private String motDePasse;

    private boolean actif;

    public Compte() {
        this.actif = true;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public boolean isActif() {
        return actif;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
