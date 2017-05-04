package models;

import play.db.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class ArchivePosteNiveauAPP extends Model{

    @Id
    private Long id;

    @Column(nullable = false)
    private String posteNiveauApp;

    @Column(nullable = false)
    private Date dateArchivage;

    @ManyToOne
    private Collaborateur collaborateur;

    /**
     * Constructeur
     */
    public ArchivePosteNiveauAPP() {
        this.dateArchivage = new Date();
    }

    /**
     * Getters
     */
    public Long getId() {
        return id;
    }

    public String getPosteNiveauApp() {
        return posteNiveauApp;
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

    public void setPosteNiveauApp(String posteNiveauApp) {
        this.posteNiveauApp = posteNiveauApp;
    }

    public void setDateArchivage(Date dateArchivage) {
        this.dateArchivage = dateArchivage;
    }

    public void setCollaborateur(Collaborateur collaborateur) {
        this.collaborateur = collaborateur;
    }

    public static Finder<Long, ArchivePosteNiveauAPP> find =
            new Finder<Long, ArchivePosteNiveauAPP>(Long.class, ArchivePosteNiveauAPP.class);
}
