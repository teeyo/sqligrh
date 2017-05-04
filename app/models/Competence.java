package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Competence extends Model {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Constraints.Max(5)
    @Constraints.Min(0)
    @Column(length = 1, nullable = false)
    private int niveau;

    @Column(nullable = false)
    @ManyToOne
    private Technologie technologie;

    @Column(nullable = false)
    @ManyToOne
    private Collaborateur collaborateur;

    public Competence() {
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getNiveau() {
        return niveau;
    }

    public Technologie getTechnologie() {
        return technologie;
    }

    public Collaborateur getCollaborateur() {
        return collaborateur;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setNiveau(int niveau) {
        this.niveau = niveau;
    }

    public void setTechnologie(Technologie technologie) {
        this.technologie = technologie;
    }

    public void setCollaborateur(Collaborateur collaborateur) {
        this.collaborateur = collaborateur;
    }

    public static Finder<Long,Competence> find = new Finder<Long,Competence>(Long.class, Competence.class);
}
