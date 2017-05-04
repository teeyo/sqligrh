package controllers;

import com.avaje.ebean.Ebean;
import models.*;
import net.sf.jasperreports.engine.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.typesafe.plugin.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import play.data.Form;
import play.db.DB;
import play.mvc.*;
import views.html.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

public class Application extends Controller {

    /**
     * Page d'accueil
     */
    public static Result index() {
        /**
         * Si l'utilisateur est connecté je le redirecte vers la page liste des collaborateur
         */
        if(estConnecte(false)){
            return redirect(routes.Application.listerCol());
        }
        /**
         * S'il n'est pas connécté je le renvoie à la page d'accueil
         */
        return ok(views.html.index.render());
    }

    /**
     * Se connecter
     */
    public static Result seConnecter(){
        Map<String, String[]> params;
        /**
         * Je retourne un tableau de valeurs contenue dans la requête de l'internaute
         */
        params = request().body().asFormUrlEncoded();
        String email = params.get("email")[0];
        String motDePasse = stringToMd5(params.get("motDePasse")[0]);
        /**
         * Je vérifie l'email est le mot de passe de l'utilisateur
         */
        Collaborateur collaborateur = verifierAuthentification(email, motDePasse);
        if(collaborateur != null){
            /**
             * J'enregistre l'ID de l'utilsateur dans la session et je le redérige vers une autre page
             */
            session().put("id",String.valueOf(collaborateur.getId()));
            flash("success","Vous ête maintenant connecté.");
            return redirect(routes.Application.listerCol());
        }else{
            /**
             * J'affiche un message d'erreur au cas ou l'authentification échoue
             */
            flash("error","E-mail/Mot de passe incorrect.");
            return redirect(routes.Application.index());
        }
    }

    /**
     * Se déconnécter
     */
    public static Result seDeconnecter(){
        /**
         * Pour se déconnecter il suffit de vider la sesion et redériger vers la page d'accueil
         */
        session().clear();
        flash("success","Vous êtes maintenant déconnecter.");
        return redirect(routes.Application.index());
    }

    /**
     * Nouveau collaborateur, affichage d'un formulaire d'ajout d'un nouveau collaborateur
     */
    public static Result nouveauCol() {
        /**
         * Tester si l'utilisateur est toujours connecté
         */
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * Vérifier si l'utilisateur est autorisé à ajouter un nouveau collaborateur
         */
        Collaborateur collaborateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!estAutoriseNiveau1(collaborateur)){
            flash("error","Vous n'êtes pas autorisé pour ajouter un nouveau collaborateur.");
            return redirect(routes.Application.listerCol());
        }
        /**
         * Je créé le formulaire d'ajout
         */
        Form colForm = Form.form(Collaborateur.class).bindFromRequest();
        List<Role> roles = Role.find.all();
        List<Collaborateur> managersRH = Collaborateur.find.where("role.description = :role")
                .setParameter("role","Manager RH").findList();
        return ok(views.html.nouveauColForm.render(colForm, roles, managersRH));
    }

    /**
     * Ajout d'un nouveau collaborateur
     */
    @play.db.ebean.Transactional
    public static Result ajouterCol(){
        /**
         * Vérifier si l'utilisateur est connecté
         */
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * J'associe la requête de l'utilisateur à un objet collaborateur
         */
        Form colForm = Form.form(Collaborateur.class).bindFromRequest();
        /**
         * Tester si le formulaire contient des erreurs (Validation)
         */
        if (colForm.hasErrors()){
            flash("error","Veuillez vérifier les données que vous venez d'entrer.");
            return redirect(routes.Application.nouveauCol());
        }
        /**
         * Vérifier si l'utilisateur est autorisé à ajouter un nouveau collaborateur
         */
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!estAutoriseNiveau1(utilisateur)){
            flash("error","Vous n'êtes pas autorisé pour ajouter un nouveau collaborateur.");
            return redirect(routes.Application.listerCol());
        }
        Collaborateur collaborateur = (Collaborateur)colForm.get();
        /**
         * Vérification des champs uniques
         */
        if (collaborateur.estValide().size() > 0){
            for (int i = 0; i<collaborateur.estValide().size(); i++){
                flash("error",collaborateur.estValide().get(i));
            }
            return redirect(routes.Application.nouveauCol());
        }
        /**
         * je crypte le mot de passe en utilisant l'algorithme MD5 avant de persister l'objet
         */
        collaborateur.getCompte().setMotDePasse(stringToMd5(collaborateur.getCompte().getMotDePasse()));
        collaborateur.setCompte(collaborateur.getCompte());
        /**
         * Récupérer le role depuis la BD et l'affécter au collaborateur
         */
        Role role = Role.find.where("description = :description").
                setParameter("description", collaborateur.getRole().getDescription()).findUnique();
        collaborateur.setRole(role);
        /**
         * Génération de l'abréviation
         */
        String abreviation = collaborateur.getPrenom().charAt(0) + collaborateur.getNom().substring(0, 2);
        collaborateur.setAbreviation(abreviation);
        /**
         * Génération du mois BAP
         */
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        int jour = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[0]);
        int mois = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[1]);
        if (jour < 15){
            collaborateur.setMoisBap(getNomDuMois(mois));
        }else{
            collaborateur.setMoisBap(getNomDuMois(mois+1));
        }
        /**
         * Affectation des managers
         */
        if(collaborateur.getManagerActuel().getId()!=-1){
            Collaborateur managerActuel = Collaborateur.find.byId(collaborateur.getManagerActuel().getId());
            if(managerActuel!=null){
                collaborateur.setManagerActuel(managerActuel);
            }
        }else{
            collaborateur.setManagerActuel(null);
        }
        /**
         * L'archivage du poste niveau APP
         */
        ArchivePosteNiveauAPP archivePosteNiveauAPP = new ArchivePosteNiveauAPP();
        archivePosteNiveauAPP.setPosteNiveauApp(collaborateur.getPosteNiveauApp());
        collaborateur.addArchivePosteNiveauAPP(archivePosteNiveauAPP);
        archivePosteNiveauAPP.save();
        /**
         * L'archivage du poste actuel
         */
        ArchivePosteActuel archivePosteActuel = new ArchivePosteActuel();
        archivePosteActuel.setPosteActuel(collaborateur.getPosteActuel());
        collaborateur.addArchivePosteActuel(archivePosteActuel);
        archivePosteActuel.save();
        /**
         * L'archivage du poste actuel
         */
        ArchiveSalaire archiveSalaire = new ArchiveSalaire();
        archiveSalaire.setSalaire(collaborateur.getSalaireActuel());
        collaborateur.addArchiveSalaire(archiveSalaire);
        archiveSalaire.save();
        /**
         * Pérsistance
         */
        collaborateur.save();
        /**
         * Envoi des mails
         */
        /**
         * je récupère les ambassadeurs RH pour les notifier
         */
        List<Collaborateur> ambassadeursRH = Collaborateur.find.where("role.description = :role")
                .setParameter("role","Ambassadeur RH").findList();
        if(ambassadeursRH.size() <= 0){
            flash("error","Aucun ambassadeur RH trouvé. Vous êtes prier de choisir un ambassadeur RH");
        }else {
            for(Collaborateur c : ambassadeursRH){
                MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
                mail.setSubject("Notification | Ajout d'un nouveau collaborateur");
                mail.addRecipient(c.getCompte().getEmail());
                mail.addFrom("rabat.sqli@gmail.com");
                mail.send( "text", "<html><body>" +
                        "<h1>SQLiGRH : un collaborateur a été ajouté avec succès.</h1>" +
                        "<p>le collaborateur porte le matricule "+collaborateur.getMatricule()+"" +
                        ", son nom est : "+collaborateur.getNom()+" "+collaborateur.getPrenom()+".</p>" +
                        "<p>Consultez le site pour plus d'informations.</p></body></html>");
            }
        }
        /**
         * Envoi du mail de bienvenue
         */
        MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
        mail.setSubject("SQLi Rabat vous souhaite la bienvenue");
        mail.addRecipient(collaborateur.getCompte().getEmail());
        mail.addFrom("rabat.sqli@gmail.com");
        mail.send( "text", "<html><body>" +
                "<h1>SQLiGRH : bienvenue dans l'équipe SQLi.</h1>" +
                "<p>SQLi Rabat vous souhaite la bienvenu, ici nous somme plus qu'une équipe," +
                "nous somme une famille. Et aujourd'hui vous faite partie de cette grande famille.</p></body></html>");
        /**
         * mail de désignation du manager RH
         */
        if(collaborateur.getManagerActuel() != null){

            MailerAPI mail1 = play.Play.application().plugin(MailerPlugin.class).email();
            mail1.setSubject("Notification | désignation du manager RH");
            mail1.addRecipient(collaborateur.getCompte().getEmail());
            mail1.addRecipient(collaborateur.getManagerActuel().getCompte().getEmail());
            mail1.addFrom("rabat.sqli@gmail.com");
            mail1.send( "text", "<html><body>" +
                    "<h1>SQLiGRH : Affectation de manager RH.</h1>" +
                    "<p>Le manager RH "+collaborateur.getManagerActuel().getNom()+", a été choisit comme " +
                    "manager RH pour le collaborateur "+collaborateur.getNom()+" (ayant le matricule " +
                    +collaborateur.getMatricule()+").</p></body></html>");

            /**
             * mail de pour le manager afin qu'il prépare la planification
             */
            if(collaborateur.getManagerActuel() != null){

                MailerAPI mail2 = play.Play.application().plugin(MailerPlugin.class).email();
                mail2.setSubject("Notification | invitation pour préparer la plannification");
                mail2.addRecipient(collaborateur.getManagerActuel().getCompte().getEmail());
                mail2.addFrom("rabat.sqli@gmail.com");
                mail2.send( "text", "<html><body>" +
                        "<h1>SQLiGRH : vous êtes inviter à préparer une plannification pour les rituels suivants :</h1>" +
                        "<p>- bilan de période d'essaie.</p>" +
                        "<p>- bilan intermédiaire de performance.</p>" +
                        "<p>- bilan annuel de performance.</p>" +
                        "</body></html>");
            }
        }

        flash("success", "Ajout avec succès.");
        return redirect(routes.Application.index());
    }

    /**
     * Une fonction retournant le nom du mois à partir de son numéro
     */
    private static String getNomDuMois(int m){
        --m;
        if (m==12){
            m=0;
        }
        String mois = "invalide : "+m;
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] lesMois = dfs.getMonths();
        if (m >= 0 && m <= 11 ) {
            mois = lesMois[m];
        }
        return mois;
    }

    /**
     * Edition d'un collaborateur (affichage d'un formulaire contenant les infos d'un collaborateur)
     */
    public static Result editerCol(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * Tester si le collaborateur passé en paramètre existe
         */
        if(Collaborateur.find.byId(id) == null){
            flash("error","Le collaborateur que vous essayer d'éditer n'existe pas.");
            return redirect(routes.Application.listerCol());
        }
        /**
         * Créer un formulaire, et associer le collaborateur voulu à ce formulaire
         */
        Form editForm = Form.form(Collaborateur.class).fill(Collaborateur.find.byId(id));
        List<Collaborateur> managersRH = Collaborateur.find.where("role.description = :role")
                .setParameter("role","manager RH").findList();
        Collaborateur collaborateur = Collaborateur.find.byId(id);
        List<Role> roles = Role.find.all();
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        return ok(editColForm.render(collaborateur, editForm,managersRH, roles, utilisateur));
    }

    /**
     * Mise à jour d'un collaborateur
     */
    @play.db.ebean.Transactional
    public static Result majCol(Long id){
        /**
         * vérfier si l'utilisateur est connécté
         */
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }

        /**
         * Tester si le collaborateur passé en paramètre existe, parce que l'ID peut être altérer dans le lien
         */
        if(Collaborateur.find.byId(id) == null){
            flash("error","Le collaborateur que vous essayer d'éditer n'existe pas.");
            return redirect(routes.Application.listerCol());
        }
        /**
         * affecter les valeurs du formulaire aux attributs de l'objet après avoir tester l'existance des erreurs
         * sur le formulaire
         */
        Form<Collaborateur> majForm = Form.form(Collaborateur.class).bindFromRequest();
        if(majForm.hasErrors()) {
            flash("error","Il existe des erreurs, vérifiez les données que vous venez d'entrer.");
            return redirect(routes.Application.editerCol(id));
        }
        Collaborateur collaborateur = majForm.get();
        collaborateur.setId(id);
        /**
         * Re-génération de l'abréviation
         */
        String abreviation = collaborateur.getPrenom().charAt(0) + collaborateur.getNom().substring(0,2);
        collaborateur.setAbreviation(abreviation);
        /**
         * Génération du mois BAP
         */
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        int jour = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[0]);
        int mois = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[1]);
        if (jour < 15){
            collaborateur.setMoisBap(getNomDuMois(mois));
        }else{
            collaborateur.setMoisBap(getNomDuMois(mois+1));
        }

        /**
         * Modification des managers (ancien et actuel)
         */
        if(collaborateur.getManagerActuel().getId() != -1){
            if (collaborateur.getManagerActuel().getId() == -2){
                Collaborateur managerActuel = Collaborateur.find.byId(collaborateur.getManagerActuel().getId());
                collaborateur.setAncienManager(managerActuel);
                collaborateur.setManagerActuel(null);
                Ebean.createSqlUpdate("UPDATE Collaborateur SET manager_actuel_id = NULL WHERE id = :id")
                        .setParameter("id",id)
                        .execute();
            }else{
                Collaborateur managerActuel = Collaborateur.find.byId(collaborateur.getManagerActuel().getId());
                collaborateur.setAncienManager(managerActuel.getManagerActuel());
                collaborateur.setManagerActuel(managerActuel);
            }
        }else{
            collaborateur.setManagerActuel(null);
        }


        /**
         * Manipulation des dates (date départ + date participation au siminaire)
         */
        if(collaborateur.isAncienCollaborateur() != true){
            collaborateur.setDateDepart(null);
            Ebean.createSqlUpdate("UPDATE Collaborateur SET date_depart = NULL WHERE id = :id")
                    .setParameter("id",id)
                    .execute();
        }
        if(collaborateur.isParticipeAuSiminaire() != true){
            collaborateur.setDateParticipation(null);
            Ebean.createSqlUpdate("UPDATE Collaborateur SET date_participation = NULL WHERE id = :id")
                    .setParameter("id",id)
                    .execute();
        }
        /**
         * L'archivage du poste niveau APP
         */
        String posteNiveauApp = Collaborateur.find.byId(id).getPosteNiveauApp();
        if(!posteNiveauApp.equals(collaborateur.getPosteNiveauApp())){
            ArchivePosteNiveauAPP archivePosteNiveauAPP = new ArchivePosteNiveauAPP();
            archivePosteNiveauAPP.setPosteNiveauApp(posteNiveauApp);
            collaborateur.addArchivePosteNiveauAPP(archivePosteNiveauAPP);
            archivePosteNiveauAPP.save();
        }
        /**
         * L'archivage du poste actuel
         */
        String posteActuel = Collaborateur.find.byId(id).getPosteActuel();
        if(!posteActuel.equals(collaborateur.getPosteActuel())){
            ArchivePosteActuel archivePosteActuel = new ArchivePosteActuel();
            archivePosteActuel.setPosteActuel(posteActuel);
            collaborateur.addArchivePosteActuel(archivePosteActuel);
            archivePosteActuel.save();
        }
        /**
         * L'archivage du poste actuel
         */
        float salaire = Collaborateur.find.byId(id).getSalaireActuel();
        if(salaire != collaborateur.getSalaireActuel()){
            ArchiveSalaire archiveSalaire = new ArchiveSalaire();
            archiveSalaire.setSalaire(salaire);
            collaborateur.addArchiveSalaire(archiveSalaire);
            archiveSalaire.save();
        }
        /**
         * Pérsistance
         */
        Role role = Role.find.byId(collaborateur.getRole().getId());
        collaborateur.setRole(role);
        Long idCompte = Collaborateur.find.byId(id).getCompte().getId();
        if(collaborateur.getCompte().getMotDePasse().equals("")){
            collaborateur.getCompte().setMotDePasse(null);
        }else{
            collaborateur.getCompte().setMotDePasse(stringToMd5(collaborateur.getCompte().getMotDePasse()));
        }
        collaborateur.getCompte().update(idCompte);
        collaborateur.setDiplomes(Collaborateur.find.byId(id).getDiplomes());
        collaborateur.setCompetences(Collaborateur.find.byId(id).getCompetences());
        collaborateur.update();
        flash("success", "Modification avec succès du collaborateur "+collaborateur.getNom() +" "+
                collaborateur.getPrenom()+".");
        return redirect(routes.Application.listerCol());
    }

    /**
     * Supprimer un collaborateur
     */
    public static Result supprimerCol(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * Vérifier si l'utilisateur est autorisé à ajouter un nouveau collaborateur
         */
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!estAutoriseNiveau1(utilisateur)){
            flash("error","Vous n'êtes pas autorisé pour ajouter un nouveau collaborateur.");
            return redirect(routes.Application.listerCol());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(id);
        if (collaborateur != null){
            collaborateur.delete();
            flash("success","Collabborateur supprimer avec succès.");
        }else{
            flash("error","collaborateur n'existe pas.");
        }
        return redirect(routes.Application.listerCol());
    }

    /**
     * Afficher une liste des collaborateurs
     */
    public static Result listerCol(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * Lister tous les collaborateur except l'utilisateur (l'internaute) vu qu'il aura un raccourcis vers
         * son profil dans le menu (mon profil)
         */
        List<Collaborateur> collaborateurs = Collaborateur.find.where("id != :id")
                .setParameter("id",Long.parseLong(session().get("id"))).findList();
        return ok(views.html.listCol.render(collaborateurs));
    }

    /**
     * Afficher toutes les informations d'un collaborateur dont l'id est passé comme paramètre
     */
    public static Result detaillerCol(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(id);
        if (collaborateur != null){
            return ok(views.html.detailsCol.render(collaborateur));
        }else{
            flash("error","collaborateur n'existe pas.");
            return redirect(routes.Application.listerCol());
        }
    }

    /**
     * Vérifier si l'utilisateur est connécté, le paramètre boolean indique si le message d'erreur sera visible ou pas
     */
    private static boolean estConnecte(boolean showMsg){
        if (session().get("id") != null){
            return true;
        }else{
            if(showMsg){
                flash("error","Vous n'êtes pas connecter.");
            }
            return false;
        }
    }

    /**
     * ajouter des roles + un admin automatiquement - A supprimer plutard
     */
    @play.db.ebean.Transactional
    public static Result magicalAdd(){
        Role role = new Role();
        role.setDescription("Manager RH");
        role.save();
        Role role1 = new Role();
        role1.setDescription("Responsable RH");
        role1.save();
        Role role2 = new Role();
        role2.setDescription("Administrateur");
        role2.save();
        Role role3 = new Role();
        role3.setDescription("Collaborateur");
        role3.save();
        Collaborateur collaborateur = new Collaborateur();
        collaborateur.setNom("Oussama");
        collaborateur.setPrenom("Taoufik");
        collaborateur.setAbreviation("XXX");
        collaborateur.setSexe('M');
        collaborateur.setBu("BU11");
        collaborateur.setMatricule(10000);
        collaborateur.setDateEmbauche(new Date());
        collaborateur.setMoisBap("janvier");
        collaborateur.setPosteActuel("DCI");
        collaborateur.setPosteNiveauApp("APPP");
        collaborateur.setRole(role2);
        collaborateur.setSalaireActuel(14000);
        Compte compte = new Compte();
        compte.setEmail("taoufik.oussama@gmail.com");
        compte.setMotDePasse(stringToMd5("oussama"));
        Diplome diplome = new Diplome();
        diplome.setEcole("ENSET");
        diplome.setNiveau("BAC +5");
        diplome.setPromotion("2013/2014");
        diplome.setTypeDiplome("Privé");
        diplome.setTypeEcole("Internationale");
        diplome.setIntitule("Master SID");
        collaborateur.addDiplome(diplome);
        collaborateur.setCompte(compte);
        collaborateur.save();
        flash("success", "Ajout de l'administrateur avec succès.");
        return redirect(routes.Application.index());
    }

    /**
     * Nouveau diplome
     */
    public static Result nouveauDiplome(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Form diplomeForm = Form.form(Diplome.class).bindFromRequest();
        Collaborateur collaborateur= Collaborateur.find.byId(id);
        if (collaborateur == null){
            flash("error","Opération échouée, collaborateur introuvable.");
            return redirect(routes.Application.index());
        }
        return ok(views.html.nouveauDiplomeForm.render(diplomeForm, collaborateur));
    }

    /**
     * Affécter le diplome au collaborateur
     */
    public static Result ajouterDiplome(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur collaborateur= Collaborateur.find.byId(id);
        Form diplomeForm = Form.form(Diplome.class).bindFromRequest();
        if (diplomeForm.hasErrors()){
            /**
             * Générer les messages d'érreurs relatives au formulaire
             */
            flash("error","Veuillez vérifier les informations que vous avez entrer.");
            return redirect(routes.Application.nouveauDiplome(id));
        }
        if (collaborateur == null){
            flash("error","Opération échouée, collaborateur introuvable.");
            return redirect(routes.Application.index());
        }
        Diplome diplome = (Diplome)diplomeForm.get();
        collaborateur.addDiplome(diplome);
        collaborateur.save();
        flash("success","Ajout du diplome "+diplome.getIntitule()+" avec succès.");
        return redirect(routes.Application.nouveauDiplome(id));
    }

    /**
     * Editer un diplôme
     */
    public static Result editerDiplome(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Diplome diplome = Diplome.find.byId(id);
        if (diplome == null){
            flash("error","Diplôme introuvable.");
            return redirect(routes.Application.index());
        }
        Form editionDipForm = Form.form(Diplome.class).fill(diplome);
        Collaborateur collaborateur = Collaborateur.find.byId(diplome.getCollaborateur().getId());
        return ok(views.html.editDiplomeForm.render(editionDipForm,diplome));
    }

    /**
     * Mettre à jour un diplôme
     */
    public static Result majDiplome(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Form<Diplome> majForm = Form.form(Diplome.class).bindFromRequest();
        if(majForm.hasErrors()) {
            flash("error","Il existe des erreurs dans le formulaire.");
            return redirect(routes.Application.editerDiplome(id));
        }
        Diplome diplome = majForm.get();
        diplome.update(id);
        flash("success", "Le diplome a été modifié avec succès.");
        return redirect(routes.Application.detaillerCol(diplome.getCollaborateur().getId()));
    }

    /**
     * Suppression d'un diplôme
     */
    public static Result supprimerDiplome(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Diplome diplome = Diplome.find.byId(id);
        Long idCol = diplome.getCollaborateur().getId();
        if (diplome != null){
            diplome.delete();
            flash("success","Diplôme supprimer avec succès.");
        }else{
            flash("error","Diplôme n'existe pas.");
        }
        return redirect(routes.Application.detaillerCol(idCol));
    }

    /**
     * la fonction qui crypte un String en utilisant l'algorithme MD5
     */
    private static String stringToMd5(String original){
        StringBuffer sb = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(original.getBytes());
            byte[] digest = md.digest();
            for (byte b : digest){
                sb.append(Integer.toHexString((int)b & 0xff));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * Vérification de l'email et du mot de passe
     */
    private static Collaborateur verifierAuthentification(String email, String motDePasse){
        Collaborateur collaborateur;
        collaborateur = Collaborateur.find.where("compte.email = :email and compte.motDePasse = :motDePasse")
                .setParameter("email", email)
                .setParameter("motDePasse",motDePasse).findUnique();
        if (collaborateur == null){
            return null;
        }else{
            return collaborateur;
        }
    }

    /**
     * Formulaire nouvelle compétence
     */
    public static Result nouvelleCompetence(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        if (Collaborateur.find.byId(id) == null){
            flash("error","Collaborateur n'existe pas.");
            return redirect(routes.Application.listerCol());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(id);
        Form nouvelleCompetenceForm = Form.form(Competence.class);
        List<Technologie> technologies = Technologie.find.all();
        return ok(views.html.ajoutCompetenceForm.render(nouvelleCompetenceForm, collaborateur, technologies));
    }

    /**
     * Ajouter une nouvelle compétence
     */
    @play.db.ebean.Transactional
    public static Result ajouterCompetence(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        if(Collaborateur.find.byId(id) == null){
            flash("error","Collaborateur introuvable.");
            return redirect(routes.Application.listerCol());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(id);
        Competence competence = (Competence)Form.form(Competence.class).bindFromRequest().get();
        collaborateur.addCompetence(competence);
        competence.setCollaborateur(collaborateur);
        /**
         * Ajout de la technologie (en cas de nouvelle technologie)
         */
        Map<String, String[]> params = request().body().asFormUrlEncoded();
        String nouvelleTech = params.get("nouvelleTech")[0];
        if(competence.getTechnologie().getId() == -1){
            Technologie tech = new Technologie();
            tech.setNom(nouvelleTech);
            competence.setTechnologie(tech);
            tech.save();
            flash("success","Ajout de la technologie "+nouvelleTech+" avec succès.");
        }else{
            if(nouvelleTech.equals("")){
                flash("error","Veuillez choisir une technologie de la liste ou bien saisir une, si la technologie " +
                        "ne se trouve pas dans la liste.");
                return redirect(routes.Application.nouvelleCompetence(id));
            }
            Technologie tech = Technologie.find.byId(competence.getTechnologie().getId());
            if(tech!=null){
                competence.setTechnologie(tech);
                flash("success","Ajout de la technologie "+tech.getNom()+" avec succès.");
            }
        }
        competence.save();
        flash("success","Ajout de la compétence "+competence.getNom()+" avec succès.");
        return redirect(routes.Application.index());

    }

    /**
     * Edition d'une compétence
     */
    public static Result editerCompetence(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if (utilisateur == null || !estAutoriseNiveau1(utilisateur)){
            flash("error","Vous n'avez pas les droits necessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        Competence competence = Competence.find.byId(id);
        Form editerCompetenceForm = Form.form(Competence.class).fill(competence);
        List<Technologie> technologies = Technologie.find.all();
        return ok(views.html.editCompetenceForm.render(editerCompetenceForm, competence, technologies));
    }

    /**
     * Mise à jour d'une compétence
     */
    public static Result majCompetence(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if (utilisateur == null || !estAutoriseNiveau1(utilisateur)){
            flash("error","Vous n'avez pas les droits necessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        Competence competence = (Competence)Form.form(Competence.class).bindFromRequest().get();
        /**
         * Ajout de la technologie (en cas de nouvelle technologie)
         */
        Map<String, String[]> params = request().body().asFormUrlEncoded();
        String nouvelleTech = params.get("nouvelleTech")[0];
        if(!nouvelleTech.equals("")){
            Technologie tech = new Technologie();
            tech.setNom(nouvelleTech);
            competence.setTechnologie(tech);
            tech.save();
            flash("success","Ajout de la technologie "+nouvelleTech+" avec succès.");
        }else{
            Technologie tech = Technologie.find.byId(competence.getTechnologie().getId());
            if(tech!=null){
                competence.setTechnologie(tech);
                flash("success","Ajout de la technologie "+tech.getNom()+" avec succès.");
            }
        }
        competence.update(id);
        flash("success","Ajout de la compétence "+competence.getNom()+" avec succès.");
        return redirect(routes.Application.listerCol());
    }

    /**
     * Suppression d'un compétence
     */
    public static Result supprimerCompetence(Long id){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if (utilisateur == null || !estAutoriseNiveau1(utilisateur)){
            flash("error","Vous n'avez pas les droits necessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        Competence competence = Competence.find.byId(id);
        if(competence != null){
            flash("success","Suppression de la compétence "+competence.getNom()+" avec succès.");
            competence.delete();
        }else{
            flash("error","compétence introuvable.");
        }
        return redirect(routes.Application.listerCol());
    }

    /**
     * Liste des technologies
     */
    public static Result listerTechnologies(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if (collaborateur == null || !estAutoriseNiveau1(collaborateur)){
            flash("error","Vous n'avez pas les droits necessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        List<Technologie> technologies = Technologie.find.all();
        return ok(listTechnologies.render(technologies,collaborateur));
    }

    /**
     * Editer une technologie
     */
    public static Result editerTechnologie(Long idCol, Long idTech){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        if(Technologie.find.byId(idTech)==null || Collaborateur.find.byId(idCol) == null){
            flash("error","Technologie/Collaborateur introuvable.");
            return redirect(routes.Application.listerCol());
        }
        Technologie technologie = Technologie.find.byId(idTech);
        Form editerTechnologieForm = Form.form(Technologie.class).fill(technologie);
        Collaborateur collaborateur = Collaborateur.find.byId(idCol);
        if(!collaborateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        return ok(editTechnologieForm.render(editerTechnologieForm,technologie,collaborateur));
    }

    /**
     * Mise à jour d'une technologie
     */
    public static Result majTechnologie(Long idCol, Long idTech){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(idCol);
        if(!collaborateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        Form  form = Form.form(Technologie.class).bindFromRequest();
        if (form.hasErrors()){
            flash("error","Veuillez vérifier les données que vous venez d'entrer.");
        }
        Technologie technologie = (Technologie)form.get();
        technologie.update(idTech);
        flash("success","Modification de la technologie "+technologie.getNom()+" avec succès.");
        return redirect(routes.Application.listerCol());
    }

    /**
     * Mise à jour d'une technologie
     */
    public static Result supprimerTechnologie(Long idCol, Long idTech){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur collaborateur = Collaborateur.find.byId(idCol);
        if(!collaborateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour mettre à jour les technologies.");
            return redirect(routes.Application.listerCol());
        }
        Technologie technologie = Technologie.find.byId(idTech);
        if(technologie == null){
            flash("error","Technologie introuvable.");
        }
        technologie.delete();
        flash("success","Suppression de la technologie "+technologie.getNom()+" avec succès.");
        return redirect(routes.Application.listerCol());
    }

    /**
     * Vérifier le rôle de l'utilisateur afin de permettre ou non l'accès à quelques fonctionalités
     */
    private static boolean estAutoriseNiveau1(Collaborateur collaborateur){
        if(collaborateur.getRole().getDescription().equals("Ambassadeur RH") ||
                collaborateur.getRole().getDescription().equals("Responsable de production") ||
                collaborateur.getRole().getDescription().equals("Manager agence") ||
                collaborateur.getRole().getDescription().equals("Administrateur")){
            return true;
        }else{
            return false;
        }
    }

    /**
     * Accèss à la page de l'export du document excel des collabnorateurs
     */
    public static Result exportCollaborateurs(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        /**
         * Servir la page de reporting
         */
        return ok(views.html.exporterListeCol.render());
    }

    /**
     * Exportation de fichier Excel des collaborateurs
     */
    public static Result exporterCollaborateurs(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        /**
         * Je crée la feuille excel
         */
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Liste des collaborateurs");

        List<Collaborateur> collaborateurs = Collaborateur.find.all();
        /**
         * Je crée une entête du fichier Excel contenant les titres des colonnes
         */
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Matricule");
        row.createCell(1).setCellValue("Nom");
        row.createCell(2).setCellValue("Prenom");
        row.createCell(3).setCellValue("Abreviation");
        row.createCell(4).setCellValue("Sexe");
        row.createCell(5).setCellValue("Site");
        row.createCell(6).setCellValue("BU");
        row.createCell(7).setCellValue("Date embauche");
        row.createCell(8).setCellValue("Mois BAP");
        row.createCell(9).setCellValue("Est ancien collaborateur");
        row.createCell(10).setCellValue("Date de départ");
        row.createCell(11).setCellValue("A participé au siminaire");
        row.createCell(12).setCellValue("Date de participation");
        row.createCell(13).setCellValue("Poste niveau APP");
        row.createCell(14).setCellValue("Poste actuel");
        row.createCell(15).setCellValue("Salaire actuel");
        row.createCell(16).setCellValue("E-mail");
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        /**
         * pour chaque ligne contenu dans la liste des collaborateur je crée une ligne dans Excel
         * et je met pour chaque cellule la valeur convenable
         */
        for(int i=1; i<=collaborateurs.size(); i++){
            Row row1 = sheet.createRow(i);
            row1.createCell(0).setCellValue(collaborateurs.get(i-1).getMatricule());
            row1.createCell(1).setCellValue(collaborateurs.get(i-1).getNom());
            row1.createCell(2).setCellValue(collaborateurs.get(i-1).getPrenom());
            row1.createCell(3).setCellValue(collaborateurs.get(i-1).getAbreviation());
            row1.createCell(4).setCellValue(String.valueOf(collaborateurs.get(i-1).getSexe()+" "));
            row1.createCell(5).setCellValue(collaborateurs.get(i-1).getSite());
            row1.createCell(6).setCellValue(collaborateurs.get(i-1).getBu());
            row1.createCell(7).setCellValue(dateFormat.format(collaborateurs.get(i-1).getDateEmbauche()));
            row1.createCell(8).setCellValue(collaborateurs.get(i-1).getMoisBap());
            row1.createCell(9).setCellValue(collaborateurs.get(i-1).isAncienCollaborateur());
            if(collaborateurs.get(i-1).getDateDepart()!=null){
                row1.createCell(10).setCellValue(dateFormat.format(collaborateurs.get(i - 1).getDateDepart()));
            }
            row1.createCell(11).setCellValue(collaborateurs.get(i-1).isParticipeAuSiminaire());
            if(collaborateurs.get(i-1).getDateParticipation()!=null){
                row1.createCell(12).setCellValue(dateFormat.format(collaborateurs.get(i-1).getDateParticipation()));
            }
            row1.createCell(13).setCellValue(collaborateurs.get(i-1).getPosteNiveauApp());
            row1.createCell(14).setCellValue(collaborateurs.get(i-1).getPosteActuel());
            row1.createCell(15).setCellValue(collaborateurs.get(i-1).getSalaireActuel());
            row1.createCell(16).setCellValue(collaborateurs.get(i-1).getCompte().getEmail());
        }

        try{
            /**
             * Je crée un fichier Excel temporaire
             */
            File excel =  File.createTempFile("Collaborateurs",".xlsx");
            FileOutputStream fileOutputStream = new FileOutputStream(excel.getPath());
            workbook.write(fileOutputStream);
            fileOutputStream.close();
            /**
             * je serve le fichier excel temporaire que je viens de créer plus haut
             */
            return ok(excel);
        }catch(Exception e){
            flash("error",e.getMessage());
        }
        return redirect(routes.Application.listerCol());
    }

    /**
     * Import de masse : formulaire d'importation du fichier Excel contenant la liste des collaborateurs
     */
    public static Result importCollaborateurs(){
        if(!estConnecte(true)){
        return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour importer la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        return ok(views.html.importerColForm.render());
    }

    /**
     * Import de mase : importation du fichier Excel contenant la liste des collaborateurs
     */
    @play.db.ebean.Transactional
    public static Result importerCollaborateurs(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        /**
         * J'extract le fichier de la requête de l'utilisateur
         */
        Http.MultipartFormData body;
        body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file = body.getFile("excel");
        /**
         * Traiter les fichiers xls (Excel 79-2003)
         */
        if(file.getContentType().equals("application/vnd.ms-excel")){
            try{
                File excel = file.getFile();
                FileInputStream fileInputStream = new FileInputStream(excel);
                HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
                /**
                 * Je choisis la première feuille de ce document Excel, et je boucle sur l'ensemble des lignes existantes
                 */
                HSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                while(rowIterator.hasNext()) {
                Row row = rowIterator.next();
                    /**
                     * J'ignore la première ligne parcequ'elle représente l'entête du fichier Excel
                     */
                if(row.getRowNum()==0){
                    continue;
                }
                    /**
                     * Pour chque ligne j'instancie un nouveau collaborateur, et je prend les valeurs depuis les cellules
                     */
                Collaborateur collaborateur = new Collaborateur();
                collaborateur.setMatricule((int)row.getCell(0).getNumericCellValue());
                collaborateur.setNom(row.getCell(1).getStringCellValue());
                collaborateur.setPrenom(row.getCell(2).getStringCellValue());
                collaborateur.setSexe(row.getCell(3).getStringCellValue().charAt(0));
                if(row.getCell(4)!=null){
                    collaborateur.setSite(row.getCell(4).getStringCellValue());
                }
                collaborateur.setBu(row.getCell(5).getStringCellValue());
                collaborateur.setDateEmbauche(row.getCell(6).getDateCellValue());
                collaborateur.setAncienCollaborateur(row.getCell(7).getBooleanCellValue());
                if(row.getCell(8)!= null){
                    collaborateur.setDateDepart(row.getCell(8).getDateCellValue());
                }
                collaborateur.setParticipeAuSiminaire(row.getCell(9).getBooleanCellValue());
                if(row.getCell(10)!=null){
                    collaborateur.setDateParticipation(row.getCell(10).getDateCellValue());
                }
                collaborateur.setPosteNiveauApp(row.getCell(11).getStringCellValue());
                collaborateur.setPosteActuel(row.getCell(12).getStringCellValue());
                collaborateur.setSalaireActuel((float)row.getCell(13).getNumericCellValue());
                /**
                * Génération du mois BAP
                */
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                int jour = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[0]);
                int mois = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[1]);
                if (jour < 15){
                    collaborateur.setMoisBap(getNomDuMois(mois));
                }else{
                    collaborateur.setMoisBap(getNomDuMois(mois+1));
                }
                Compte compte = new Compte();
                compte.setEmail(row.getCell(14).getStringCellValue());
                compte.setMotDePasse(stringToMd5(row.getCell(15).getStringCellValue()));
                collaborateur.setCompte(compte);
                Role role = Role.find.where("description = :role").setParameter("role","Collaborateur").findUnique();
                collaborateur.setRole(role);
                collaborateur.setAbreviation(collaborateur.getPrenom().charAt(0) + collaborateur.getNom().substring(0, 2));
                    /**
                     * J'essaie d'ajouter le collaborateur à la base de donneés, si cela se passe bien, je passe au
                     * suivant, sinon je log l'erreur dans la session et je continue
                     */
                try{
                    collaborateur.save();
                }catch(Exception e){
                    flash("error","Impossible d'ajouter le collaborateur ayant le matricule "
                            +collaborateur.getMatricule());
                }
            }
            fileInputStream.close();
            }catch(Exception e){
                flash("error",e.getMessage());
            }
            /**
             * De même je traite les fichiers xlsx (Excel 2007, 2010, 2013,...)
             */
        }else if(file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
            try{
                File excel = file.getFile();
                FileInputStream fileInputStream = new FileInputStream(excel);
                XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                while(rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if(row.getRowNum()==0){
                        continue;
                    }
                    Collaborateur collaborateur = new Collaborateur();
                    collaborateur.setMatricule((int)row.getCell(0).getNumericCellValue());
                    collaborateur.setNom(row.getCell(1).getStringCellValue());
                    collaborateur.setPrenom(row.getCell(2).getStringCellValue());
                    collaborateur.setSexe(row.getCell(3).getStringCellValue().charAt(0));
                    if(row.getCell(4)!=null){
                        collaborateur.setSite(row.getCell(4).getStringCellValue());
                    }
                    collaborateur.setBu(row.getCell(5).getStringCellValue());
                    collaborateur.setDateEmbauche(row.getCell(6).getDateCellValue());
                    collaborateur.setAncienCollaborateur(row.getCell(7).getBooleanCellValue());
                    if(row.getCell(8)!= null){
                        collaborateur.setDateDepart(row.getCell(8).getDateCellValue());
                    }
                    collaborateur.setParticipeAuSiminaire(row.getCell(9).getBooleanCellValue());
                    if(row.getCell(10)!=null){
                        collaborateur.setDateParticipation(row.getCell(10).getDateCellValue());
                    }
                    collaborateur.setPosteNiveauApp(row.getCell(11).getStringCellValue());
                    collaborateur.setPosteActuel(row.getCell(12).getStringCellValue());
                    collaborateur.setSalaireActuel((float)row.getCell(13).getNumericCellValue());
                    /**
                     * Génération du mois BAP
                     */
                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    int jour = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[0]);
                    int mois = Integer.parseInt(dateFormat.format(collaborateur.getDateEmbauche()).split("-")[1]);
                    if (jour < 15){
                        collaborateur.setMoisBap(getNomDuMois(mois));
                    }else{
                        collaborateur.setMoisBap(getNomDuMois(mois+1));
                    }
                    Compte compte = new Compte();
                    compte.setEmail(row.getCell(14).getStringCellValue());
                    compte.setMotDePasse(stringToMd5(row.getCell(15).getStringCellValue()));
                    collaborateur.setCompte(compte);
                    Role role = Role.find.where("description = :role").setParameter("role","Collaborateur").findUnique();
                    collaborateur.setRole(role);
                    collaborateur.setAbreviation(collaborateur.getPrenom().charAt(0) + collaborateur.getNom().substring(0, 2));
                    try{
                        collaborateur.save();
                    }catch(Exception e){
                        flash("error",e.getMessage());
                    }
                }
                fileInputStream.close();
            }catch(Exception e){
                flash("error",e.getMessage());
            }
        }else{
            /**
             * Au cas ou le mime type n'est pas celui de Excel 2003, ni 2007, j'affiche un message d'erreur
             */
            flash("error","Seules les fichiers Excel 97-2003, 2007+ sont accèptés (xls et xslx).");
            return redirect(routes.Application.importCollaborateurs());
        }
        return redirect(routes.Application.listerCol());
    }

    /**
     * Page d'accueil du reporting
     */
    public static Result accueilReporting(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        List<Collaborateur> collaborateurs = Collaborateur.find.all();
        return ok(views.html.reporting.render(collaborateurs));
    }

    /**
     * Evolution du salaire
     */
    public static Result evolutionSalaire(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        /**
         * Je traite la requête de l'utilisateur
         */
        Long idCollaborateur = Long.parseLong(request().body().asFormUrlEncoded().get("collaborateur")[0]);
        String stringDate1 = request().body().asFormUrlEncoded().get("date1")[0];
        String stringDate2 = request().body().asFormUrlEncoded().get("date2")[0];
        /**
         * Tester si tous les paramètres sont présents
         */
        if(idCollaborateur == null || stringDate1.equals("") || stringDate2.equals("")){
            flash("error","Le choix du collaborateur et la date début et date fin est obligatoire.");
            return redirect(routes.Application.accueilReporting());
        }
        try{
            /**
             * Je calcule la moyenne d'augmentation de salaire que je passerai comme paramètre pour le générateur de
             * rapport
             */
            Date date1 = dateFormat.parse(stringDate1);
            Date date2 = dateFormat.parse(stringDate2);

            List<ArchiveSalaire> archiveSalaires = ArchiveSalaire.find.where("dateArchivage BETWEEN :date1 AND :date2")
                    .setParameter("date1",date1)
                    .setParameter("date2",date2)
                    .orderBy("salaire DESC")
                    .findList();
            float moyenneAugmentation = (archiveSalaires.get(0).getSalaire()
                    - archiveSalaires.get(archiveSalaires.size()-1).getSalaire()) / archiveSalaires.size();

            /**
             * Création du rapport, on spécifiant le fichier template du rapport
             */
            String report = "public/templates/EvolutionSalaire.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            /**
             * Affecter les valeurs aux paramètres
             */
            parameters.put("idCollaborateur",idCollaborateur);
            parameters.put("date1",stringDate1);
            parameters.put("date2",stringDate2);
            parameters.put("moyenneAugmentation",String.valueOf(moyenneAugmentation));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,parameters, DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("RapportEvolutionSalaire",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.listerCol());
        }
    }

    /**
     * Evolution de poste
     */
    public static Result evolutionPoste(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        Long idCollaborateur = Long.parseLong(request().body().asFormUrlEncoded().get("collaborateur")[0]);
        /**
         * Tester si le choix du collaborateur est fait
         */
        if(idCollaborateur == null){
            flash("error","Le choix du collaborateur est obligatoire.");
            return redirect(routes.Application.accueilReporting());
        }
        try{
            /**
             * Création du rapport
             */
            String report = "public/templates/evolutionDePoste.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            /**
             * Affecter les valeurs aux paramètres
             */
            parameters.put("idCollaborateur",idCollaborateur);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,parameters,DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("RapportEvolutionPoste",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.listerCol());
        }
    }

    /**
     * Evolution de poste
     */
    public static Result matriceDeCompetence(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        Long idCollaborateur = Long.parseLong(request().body().asFormUrlEncoded().get("collaborateur")[0]);
        /**
         * Tester si le choix du collaborateur est fait
         */
        if(idCollaborateur == null){
            flash("error","Le choix du collaborateur est obligatoire.");
            return redirect(routes.Application.accueilReporting());
        }
        try{
            /**
             * Création du rapport
             */
            String report = "public/templates/matriceDeCompetence.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            /**
             * Affecter les valeurs aux paramètres
             */
            parameters.put("idCollaborateur",idCollaborateur);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,parameters,DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("RapportEvolutionPoste",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.listerCol());
        }
    }

    /**
     * Ratio féminin masculin
     */
    public static Result ratioFemininMasculin(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        try{
            /**
             * Création du rapport
             */
            String report = "public/templates/ratioFemininMasculin.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            /**
             * Affecter les valeurs aux paramètres
             */
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("ratioFemininMasculin",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.listerCol());
        }
    }

    /**
     * Rapport des nouveaux recrus
     */
    public static Result nouveauxRecrus(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        try{
            /**
             * Création du rapport
             */
            String report = "public/templates/nouveauxRecrus.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("nouveauxRecrus",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.accueilReporting());
        }
    }

    /**
     * Rapport des nouveaux recrus
     */
    public static Result pourcentageTechnologies(){
        if(!estConnecte(true)){
            return redirect(routes.Application.index());
        }
        Collaborateur utilisateur = Collaborateur.find.byId(Long.parseLong(session().get("id")));
        if(!utilisateur.getRole().getDescription().equals("Administrateur")){
            flash("error","Vous n'avez pas les droits nécessaires pour exporter la liste des collaborateurs.");
            return redirect(routes.Application.listerCol());
        }
        try{
            /**
             * Création du rapport
             */
            String report = "public/templates/technologies.jrxml";
            JasperReport jasperReport = JasperCompileManager.compileReport(report);
            Map parameters = new HashMap();
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,null,DB.getConnection());
            /**
             * création d'un fichier PDF temporaire qui contiendra mon rapport
             */
            File pdf = File.createTempFile("technologies",".pdf");
            FileOutputStream outputStream = new FileOutputStream(pdf);
            JasperExportManager.exportReportToPdfStream(jasperPrint,outputStream);
            outputStream.close();
            /**
             * je serve un fichier PDF comme réponse
             */
            return ok(pdf);
        }catch(Exception e){
            flash("error",e.getMessage());
            return redirect(routes.Application.accueilReporting());
        }
    }
}
