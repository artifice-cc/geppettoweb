
jQuery(document).ready(function() {
    jQuery(".fields_checkboxes").hide();
    jQuery(".fields_checkboxes_header").click(
        function()
        {
            jQuery(this).parent().parent().parent().parent().next().slideToggle(500);
        });
});

jQuery(document).ready(function() {
    jQuery(".code").hide();
    jQuery(".code_header").click(
        function()
        {
            jQuery(this).parent().next().slideToggle(500);
        });
});

jQuery(document).ready(function() {
    jQuery(".download").hide();
    jQuery(".download_header").click(
        function()
        {
            jQuery(this).parent().next().next().slideToggle(500);
        });
});
