package se.raa.ksamsok.util;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class AjaxChecker extends HttpServlet {

	/**
	 * Class used for checking if request is XMLHttpRequest,
	 * i.e. using javascript/jquery. 
	 */
	private static final long serialVersionUID = 1L;

	public AjaxChecker() {
		//TODO!!#4446
	}
	
	/**
	 * check if the request is using javascript.
	 * @param req the request object.
	 * @return boolean for ajax status.
	 */
	public static boolean isAjax(HttpServletRequest req) {
		return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
	}

}
