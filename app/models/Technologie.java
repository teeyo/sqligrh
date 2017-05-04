package models;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
public class Technologie extends Model{

    @Id
    private Long id;

    @Constraints.Required(message = "ce champ est obligatoire.")
    @Column(nullable = false, unique = true)
    private String nom;

    @OneToMany(mappedBy = "technologie", cascade = CascadeType.ALL)
    private List<Competence> competences;

    public Technologie() {
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public List<Competence> getCompetences() {
        return competences;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setCompetences(List<Competence> competences) {
        this.competences = competences;
    }

    public void addCompetence(Competence c){
        competences.add(c);
    }

    public void removeCompetence(Competence c){
        competences.remove(c);
    }

    public static Finder<Long,Technologie> find = new Finder<Long,Technologie>(Long.class, Technologie.class);
}
