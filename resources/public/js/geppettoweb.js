
jQuery(document).ready(function() {
    jQuery(".fields_checkboxes").hide();
    jQuery(".fields_checkboxes_header").click(
        function()
        {
            jQuery(this).parent().parent().parent().parent().next().slideToggle(500);
        });

    jQuery(".new_template_form").hide();
    jQuery(".new_template_form_header").click(
        function()
        {
            jQuery(this).parent().parent().parent().parent().next().slideToggle(500);
        });

    jQuery(".code").hide();
    jQuery(".code_header").click(
        function()
        {
            jQuery(this).parent().next().slideToggle(500);
        });

    jQuery(".update").hide();
    jQuery(".update_header").click(
        function()
        {
            jQuery(this).parent().next().next().next().slideToggle(500);
        });

    jQuery(".download").hide();
    jQuery(".download_header").click(
        function()
        {
            jQuery(this).parent().next().next().slideToggle(500);
        });

    jQuery("table.tablesorter").tablesorter({
        widgets: ['zebra']
    });

    jQuery("#geppetto-nav-column-container").height(jQuery(window).height() - 52);
    jQuery(window).resize(function() {
        jQuery("#geppetto-nav-column-container").height(jQuery(window).height() - 52);
    });

    jQuery("table.tablesorter").each(function(index) { jQuery(this).tablesorter(); });

    // from: https://gist.github.com/duncansmart/5267653
    // Hook up ACE editor to all textareas with data-editor attribute
    $(function () {
        $('textarea[data-editor]').each(function () {
            var textarea = $(this);
 
            var mode = textarea.data('editor');
 
            var editDiv = $('<div>', {
                position: 'absolute',
                width: '750px',
                height: textarea.height(),
                'class': textarea.attr('class')
            }).insertBefore(textarea);
 
            textarea.css('display', 'none');
 
            var editor = ace.edit(editDiv[0]);
            editor.setFontSize(12);
            editor.renderer.setShowGutter(false);
            editor.renderer.setShowPrintMargin(false);
            editor.getSession().setValue(textarea.val());
            editor.getSession().setMode("ace/mode/" + mode);
            editor.setTheme("ace/theme/github");
            
            // copy back to textarea on form submit...
            textarea.closest('form').submit(function () {
                textarea.val(editor.getSession().getValue());
            })
 
        });
    });
});

