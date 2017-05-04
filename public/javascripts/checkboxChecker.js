$(document).ready(function(){

    if($("#ancienCollaborateur").is(":checked")){
        $("#dateDepart_field").slideDown();
    }else{
        $("#dateDepart_field").slideUp();
    }

    if($("#participeAuSiminaire").is(":checked")){
        $("#dateParticipation_field").slideDown();
    }else{
        $("#dateParticipation_field").slideUp();
    }

    $("#ancienCollaborateur").change(function(){
        if(this.checked){
            $("#dateDepart_field").slideDown();
        }else{
            $("#dateDepart").val("");
            $("#dateDepart_field").slideUp();
        }
    });

    $("#participeAuSiminaire").change(function(){
        if(this.checked){
            $("#dateParticipation_field").slideDown();
        }else{
            $("#dateParticipation").val("");
            $("#dateParticipation_field").slideUp();
        }
    });
});
