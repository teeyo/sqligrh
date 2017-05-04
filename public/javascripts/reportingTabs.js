$(document).ready(function(){

    //cacher les formulaire apart celui dont la tab est active
    $("#evolutionPosteForm").hide();
    $("#matriceCompetencesForm").hide();
    $("#autresForm").hide();

    $("li").click(function(){
        var id = $(this).attr("id");
        switch(id){
            case "evolutionSalaireTab" :
                $("#reportingMenuTab li").removeClass("active");
                $("#evolutionSalaireTab").addClass("active");
                $("form").hide();
                $("#evolutionSalaireForm").show();
                break;

            case "evolutionPosteTab" :
                $("#reportingMenuTab li").removeClass("active");
                $("#evolutionPosteTab").addClass("active");
                $("form").hide();
                $("#evolutionPosteForm").show();
                break;

            case "matriceCompetencesTab" :
                $("#reportingMenuTab li").removeClass("active");
                $("#matriceCompetencesTab").addClass("active");
                $("form").hide();
                $("#matriceCompetencesForm").show();
                break;

            case "autresTab" :
                $("#reportingMenuTab li").removeClass("active");
                $("#autresTab").addClass("active");
                $("form").hide();
                $("#autresForm").show();
                break;
        }
    });

});