$(document).ready(function(){

    //cacher les formulaire apart celui dont la tab est active
        $(".diplomes").hide();
        $(".competences").hide();

        $("li").click(function(){
            var id = $(this).attr("id");
            switch(id){
                case "profileTab" :
                    $("#detailsTab li").removeClass("active");
                    $("#profileTab").addClass("active");
                    $("tr").hide();
                    $(".profile").show();
                    break;

                case "diplomesTab" :
                    $("#detailsTab li").removeClass("active");
                    $("#diplomesTab").addClass("active");
                    $("tr").hide();
                    $(".diplomes").show();
                    break;

                case "competencesTab" :
                    $("#detailsTab li").removeClass("active");
                    $("#competencesTab").addClass("active");
                    $("tr").hide();
                    $(".competences").show();
                    break;
            }
        });


});