package models;

import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class ArchivePosteActuel extends Model{

    //poste niveu, poste actuel, salaire
    @Id
    private Long id;

    @Column(nullable = false)
    private String posteActuel;

    @Column(nullable = false)
    private Date dateArchivage;

    @ManyToOne
    private Collaborateur collaborateur;

    /**
     * Constructeur
     */
    public ArchivePosteActuel() {
        this.dateArchivage = new Date();
    }

    /**
     * Getters
     */
    public Long getId() {
        return id;
    }

    public String getPosteActuel() {
        return posteActuel;
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

    public void setPosteActuel(String posteActuel) {
        this.posteActuel = posteActuel;
    }

    public void setDateArchivage(Date dateArchivage) {
        this.dateArchivage = dateArchivage;
    }

    public void setCollaborateur(Collaborateur collaborateur) {
        this.collaborateur = collaborateur;
    }

    public static Finder<Long,ArchivePosteActuel> find =
            new Finder<Long,ArchivePosteActuel>(Long.class, ArchivePosteActuel.class);
}
