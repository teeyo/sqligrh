package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Diplome extends Model{

    @Id
    private Long id;

    @Column(nullable = false)
    @Constraints.Required(message = "ce champs est obligatoire.")
    private String niveau;

    @Column(nullable = false)
    @Constraints.Required(message = "ce champs est obligatoire.")
    private String ecole;

    @Column(nullable = false)
    @Constraints.Required(message = "ce champs est obligatoire.")
    private String typeEcole;

    @Column(nullable = false)
    @Constraints.Required(message = "ce champs est obligatoire.")
    private String typeDiplome;

    @Column(nullable = false)
    @Constraints.Required(message = "ce champs est obligatoire.")
    private String promotion;

    @Constraints.Required(message = "ce champs est obligatoire.")
    @Column(unique = true, nullable = false)
    private String intitule;

    @ManyToOne
    private Collaborateur collaborateur;

    public Diplome() {
    }

    public Long getId() {
        return id;
    }

    public String getNiveau() {
        return niveau;
    }

    public String getEcole() {
        return ecole;
    }

    public String getTypeEcole() {
        return typeEcole;
    }

    public String getTypeDiplome() {
        return typeDiplome;
    }

    public String getPromotion() {
        return promotion;
    }

    public String getIntitule() {
        return intitule;
    }

    public Collaborateur getCollaborateur() {
        return collaborateur;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public void setEcole(String ecole) {
        this.ecole = ecole;
    }

    public void setTypeEcole(String typeEcole) {
        this.typeEcole = typeEcole;
    }

    public void setTypeDiplome(String typeDiplome) {
        this.typeDiplome = typeDiplome;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public void setIntitule(String intitule) {
        this.intitule = intitule;
    }

    public void setCollaborateur(Collaborateur collaborateur) {
        this.collaborateur = collaborateur;
    }

    public static Finder<Long,Diplome> find = new Finder<Long,Diplome>(Long.class, Diplome.class);
}
