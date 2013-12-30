
jQuery(document).ready(function() {
    jQuery(".fields_checkboxes").hide();
    jQuery(".fields_checkboxes_header").click(
        function()
        {
            jQuery(this).parent().parent().parent().parent().next().slideToggle(500);
        });
});

jQuery(document).ready(function() {
    jQuery(".new_template_form").hide();
    jQuery(".new_template_form_header").click(
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
    jQuery(".update").hide();
    jQuery(".update_header").click(
        function()
        {
            jQuery(this).parent().next().next().next().slideToggle(500);
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

jQuery(document).ready(function() {
    jQuery("table.tablesorter").tablesorter({
        widgets: ['zebra']
    });
});

jQuery(document).ready(function() {
    jQuery("#geppetto-nav-column-container").height(jQuery(window).height() - 52);
    jQuery(window).resize(function() {
        jQuery("#geppetto-nav-column-container").height(jQuery(window).height() - 52);
    });
});

