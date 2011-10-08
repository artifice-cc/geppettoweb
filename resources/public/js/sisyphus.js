jQuery(document).ready(function() {
    jQuery(".fields_checkboxes").hide();
    jQuery(".fields_checkboxes_header").click(
        function()
        {
            jQuery(this).children().slideToggle(500);
        });
});
