/**
 * 
 */
$(function(){
	var pausCheck = $('.pauseToggle'),
		cronValue = $('#cronstring'),
		currCronValue = $('#cronstring').val(),
		paused = $('#paused'),
		formData;
	
	pausCheck.on('click', function(){
		$('#runService').prop('disabled', pausCheck.is(':checked') ? true : false);
		$(paused).val(pausCheck.is(':checked') ? "true" : "false");
//		formData = $('form').serialize();
//		formData += '&action=update';		
//		$.post('serviceaction.jsp', formData)
//		.done(function(data){
//			updateForm(data);
//		});
	});
	
	function updateForm (data) {
		$.each($.parseJSON(data), function(key, value) {
			var $elem = $('form').find('[name=' + key + ']');
			if ($elem.is('select')) {
				$('option', $elem).each(function() {
	                if (this.value == value)
	                    this.selected = true;
	            });
			} else if ($elem.is('textarea')) {
				$elem.val(value);
	        } else {
	            switch($elem.attr("type")) {
	                case "text":
	                case "hidden":
	                	$elem.val(value);  
	                    break;
	                case "checkbox":
	                    if (value == '1')
	                    	$elem.prop('checked', true);
	                    else
	                    	$elem.prop('checked', false);
	                    break;
	            }
	        }
		});
	} 
});