/**
 * 
 */
$(function(){
	var pausCheck = $('.pauseToggle'),
		cronValue = $('#cronstring'),
		currCronValue = $('#cronstring').val(),
		formData;
	
	pausCheck.on('click', function(){
		//denna bara en mockup
		$(cronValue).val(pausCheck.is(':checked') ? "PAUSAD" : currCronValue);
		$('#runService').prop('disabled', pausCheck.is(':checked') ? true : false);
		formData = $('form').serialize();
		formData += '&action=update';
		console.log(formData);
		$.post('serviceaction.jsp', formData)
		.done(function(data){
			updateForm(data);
		});
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