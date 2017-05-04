package models;

import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class ArchiveSalaire extends Model {

    @Id
    private Long id;

    @Column(nullable = false)
    private float salaire;

    @Column(nullable = false)
    private Date dateArchivage;

    @ManyToOne
    private Collaborateur collaborateur;

    /**
     * constructeur
     */
    public ArchiveSalaire() {
        this.dateArchivage = new Date();
    }

    /**
     * Getters
     */
    public Long getId() {
        return id;
    }

    public float getSalaire() {
        return salaire;
    }

    public Date getDateArchivage() {
        return dateArchivage;
    }

    public Collaborateur getCollaborateur() {
        return collaborateur;
    }

    /**
     * Setters
     */
    public void setId(Long id) {
        this.id = id;
    }

    public void setSalaire(float salaire) {
        this.salaire = salaire;
    }

    public void setDateArchivage(Date dateArchivage) {
        this.dateArchivage = dateArchivage;
    }

    public void setCollaborateur(Collaborateur collaborateur) {
        this.collaborateur = collaborateur;
    }

    public static Finder<Long,ArchiveSalaire> find = new Finder<Long,ArchiveSalaire>(Long.class, ArchiveSalaire.class);
}
