$(function(){
	var pausCheck = $('.pauseToggle'),
		paused = $('#paused'),
		formData;
	
		/*cron begin*/
	
		var cronStringValue = $('#cronstring').val();
		var currentCronValueArray = cronStringValue.split(" ");		
		var length = currentCronValueArray.length;
		var cronValue;
		var curr;
		if (length == '5') {
			cronValue = currentCronValueArray.toString();
		} else if (length == '6') {
			curr = currentCronValueArray.slice(1);
			cronValue = curr.toString();
			//ta bort sekunden i början
		} else if (length == '7') {
			//ta bort sekunden i början och året på slutet, kommer att bli obsolete
			curr = currentCronValueArray.slice(1, 6);
			cronValue = curr.toString();
		}
		
		var valueMod = cronValue.replace(/,/g, " ");
				
		
		$('#selector').cron({
		    initial: valueMod,
		    onChange: function() {
		    	$('#cronstring').val($(this).cron("value"));
		    }
		});
			
		/*cron end*/
		
	
	pausCheck.on('click', function(){
		$(paused).val(pausCheck.is(':checked') ? "true" : "false");

	});
	
	
});