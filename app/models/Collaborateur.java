package models;

import play.data.validation.Constraints;
import play.data.validation.Validation;
import play.db.ebean.Model;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Collaborateur extends Model{

    @Id
    private Long id;

    @Constraints.Min(10000)
    @Constraints.Max(99999)
    @Column(length = 5, unique = true, nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private int matricule;

    @Column(nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private String nom;

    @Column(nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private String prenom;

    @Column(length = 3, nullable = false)
    private String abreviation;

    @Constraints.Required(message = "Ce champs est obligatoire.")
    @Column(length = 1, nullable = false)
    private char sexe;

    @Column(nullable = false)
    private String site;

    @Column(length = 4, nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private String bu;

    @Column(nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private Date dateEmbauche;

    @Column(nullable = false)
    private String moisBap;

    @Column(nullable = true)
    private Date dateDepart;

    private boolean ancienCollaborateur;

    private boolean participeAuSiminaire;

    @Column(nullable = true)
    private Date dateParticipation;

    @Column(length = 4, nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private String posteNiveauApp;

    @Column(length = 3, nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private String posteActuel;

    @Constraints.Min(0)
    @Column(nullable = false)
    @Constraints.Required(message = "Ce champs est obligatoire.")
    private float salaireActuel;

    @OneToOne(cascade = CascadeType.ALL)
    @Column(nullable = false)
    private Compte compte;

    @Column(nullable = false)
    @OneToOne
    private Role role;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "collaborateur")
    @Column(nullable = true)
    private List<Diplome> diplomes = new ArrayList<>();

    @OneToMany(mappedBy = "collaborateur")
    private List<Competence> competences;

    @ManyToOne
    @Column(nullable = true)
    private Collaborateur managerActuel;

    @ManyToOne
    @Column(nullable = true)
    private Collaborateur ancienManager;

    @OneToMany(mappedBy = "collaborateur", cascade = CascadeType.ALL)
    @Column(nullable = true)
    private List<ArchivePosteNiveauAPP> archivePosteNiveauAPPs;

    @OneToMany(mappedBy = "collaborateur", cascade = CascadeType.ALL)
    @Column(nullable = true)
    private List<ArchivePosteActuel> archivePosteActuels;

    @OneToMany(mappedBy = "collaborateur", cascade = CascadeType.ALL)
    @Column(nullable = true)
    private List<ArchiveSalaire> archiveSalaires;

    /**
     * Initialisation de quelques attributs dans le constructeur
     */
    public Collaborateur() {
        this.site = "Rabat";
        this.ancienCollaborateur = false;
        this.participeAuSiminaire = false;
    }

    /**
     * Getters
     */

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getAbreviation() {
        return abreviation;
    }

    public char getSexe() {
        return sexe;
    }

    public String getSite() {
        return site;
    }

    public String getBu() {
        return bu;
    }

    public Date getDateEmbauche() {
        return dateEmbauche;
    }

    public String getMoisBap() {
        return moisBap;
    }

    public Date getDateDepart() {
        return dateDepart;
    }

    public boolean isAncienCollaborateur() {
        return ancienCollaborateur;
    }

    public boolean isParticipeAuSiminaire() {
        return participeAuSiminaire;
    }

    public Date getDateParticipation() {
        return dateParticipation;
    }

    public String getPosteNiveauApp() {
        return posteNiveauApp;
    }

    public String getPosteActuel() {
        return posteActuel;
    }

    public float getSalaireActuel() {
        return salaireActuel;
    }

    public int getMatricule() {
        return matricule;
    }

    public Compte getCompte() {
        return compte;
    }

    public Role getRole() {
        return role;
    }

    public List<Diplome> getDiplomes() {
        return diplomes;
    }

    public List<Competence> getCompetences() {
        return competences;
    }

    public Collaborateur getManagerActuel(){
        return managerActuel;
    }

    public Collaborateur getAncienManager() {
        return ancienManager;
    }

    public List<ArchivePosteNiveauAPP> getArchivePosteNiveauAPPs() {
        return archivePosteNiveauAPPs;
    }

    public List<ArchivePosteActuel> getArchivePosteActuels() {
        return archivePosteActuels;
    }

    public List<ArchiveSalaire> getArchiveSalaires() {
        return archiveSalaires;
    }

    /**
     * Setters
     */

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setAbreviation(String abreviation) {
        this.abreviation = abreviation;
    }

    public void setSexe(char sexe) {
        this.sexe = sexe;
    }

    public void setSite(String site) {
        if (site == ""){
            this.site = "Rabat";
        }else{
            this.site = site;
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBu(String bu) {
        this.bu = bu;
    }

    public void setDateEmbauche(Date dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }

    public void setMoisBap(String moisBap) {
        this.moisBap = moisBap;
    }

    public void setDateDepart(Date dateDepart) {
        this.dateDepart = dateDepart;
    }

    public void setAncienCollaborateur(boolean ancienCollaborateur) {
        this.ancienCollaborateur = ancienCollaborateur;
    }

    public void setParticipeAuSiminaire(boolean participeAuSiminaire) {
        this.participeAuSiminaire = participeAuSiminaire;
    }

    public void setDateParticipation(Date dateParticipation) {
        this.dateParticipation = dateParticipation;
    }

    public void setPosteNiveauApp(String posteNiveauApp) {
        this.posteNiveauApp = posteNiveauApp;
    }

    public void setPosteActuel(String posteActuel) {
        this.posteActuel = posteActuel;
    }

    public void setSalaireActuel(float salaireActuel) {
        this.salaireActuel = salaireActuel;
    }

    public void setMatricule(int matricule) {
        this.matricule = matricule;
    }

    public void setCompte(Compte compte) {
        this.compte = compte;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setDiplomes(List<Diplome> diplomes) {
        this.diplomes = diplomes;
    }

    public void addDiplome(Diplome diplome){
        diplome.setCollaborateur(this);
        this.diplomes.add(diplome);
    }

    public void removeDiplome(Diplome diplome){
        this.diplomes.remove(diplome);
    }

    public void addCompetence(Competence c){
        this.competences.add(c);
    }

    public void removeCompetence(Competence c){
        this.competences.remove(c);
    }

    public void setCompetences(List<Competence> competences) {
        this.competences = competences;
    }

    public void setManagerActuel(Collaborateur managerActuel){
        this.managerActuel = managerActuel;
    }

    public void setAncienManager(Collaborateur ancienManager) {
        this.ancienManager = ancienManager;
    }

    public void setArchivePosteNiveauAPPs(List<ArchivePosteNiveauAPP> archivePosteNiveauAPPs) {
        this.archivePosteNiveauAPPs = archivePosteNiveauAPPs;
    }

    public void addArchivePosteNiveauAPP(ArchivePosteNiveauAPP a){
        a.setCollaborateur(this);
        this.archivePosteNiveauAPPs.add(a);
    }

    public void removeArchivePosteNiveauAPP(ArchivePosteNiveauAPP a){
        this.archivePosteNiveauAPPs.remove(a);
    }

    public void setArchivePosteActuels(List<ArchivePosteActuel> archivePosteActuels) {
        this.archivePosteActuels = archivePosteActuels;
    }

    public void addArchivePosteActuel(ArchivePosteActuel a){
        a.setCollaborateur(this);
        this.archivePosteActuels.add(a);
    }

    public void removeArchivePosteActuel(ArchivePosteActuel a){
        this.archivePosteActuels.remove(a);
    }

    public void setArchiveSalaires(List<ArchiveSalaire> archiveSalaires) {
        this.archiveSalaires = archiveSalaires;
    }

    public void addArchiveSalaire(ArchiveSalaire a){
        a.setCollaborateur(this);
        this.archiveSalaires.add(a);
    }

    public void removeArchiveSalaire(ArchiveSalaire a){
        this.archiveSalaires.remove(a);
    }

    /**
     * S'en servir de l'API finder, pour simplifier les requête aux niveau BDD
     */
    public static Finder<Long,Collaborateur> find = new Finder<Long,Collaborateur>(Long.class, Collaborateur.class);

    /**
     * Vérification des champs uniques
     */
    public ArrayList<String> estValide(){
        ArrayList<String> errMsg = new ArrayList<>();
        if (find.where("matricule = :matricule").setParameter("matricule", this.matricule).findRowCount() > 0 ){
            errMsg.add("Le matricule  existe déjà, veuillez choisir un autre.");
        }
        if (find.where("compte.email = :email").setParameter("email", this.compte.getEmail()).findRowCount() > 0){
            errMsg.add("L'email  existe déjà, veuillez choisir un autre.");
        }
        return errMsg;
    }
}
